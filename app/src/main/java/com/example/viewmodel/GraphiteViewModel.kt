package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GraphiteRepository
import com.example.engine.DemoArtworkFactory
import com.example.engine.ProceduralEngine
import com.example.model.BlendModeType
import com.example.model.GraphiteDocumentModel
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.model.ProceduralNode
import com.example.model.ProceduralNodeType
import com.example.model.ToolType
import com.example.model.Vec2Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MobileWorkspaceTab(val labelIt: String) {
    CANVAS("Tela 2D"),
    NODE_GRAPH("Grafo Nodi"),
    LAYERS_INSPECTOR("Livelli"),
    PORTFOLIO("Progetti")
}

data class EditorUiState(
    val activeTab: MobileWorkspaceTab = MobileWorkspaceTab.CANVAS,
    val document: GraphiteDocumentModel = DemoArtworkFactory.createIsometricFountain(),
    val selectedLayerId: String? = "iso-basin-outer",
    val selectedNodeId: String? = null,
    val activeTool: ToolType = ToolType.SELECT,
    val primaryColorHex: Long = 0xFF65BBE5L,
    val secondaryColorHex: Long = 0xFF68C587L,
    val defaultStrokeWidth: Float = 4f,
    val defaultPolygonSides: Int = 6,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val showRulers: Boolean = true,
    val wireframeOverlay: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusToast: String? = null,
    val showColorPickerDialog: Boolean = false,
    val editingPrimaryColor: Boolean = true,
    val showNodeCatalogSheet: Boolean = false,
    val showExportDialog: Boolean = false,
    val showNewDocDialog: Boolean = false
) {
    val selectedLayer: GraphiteLayer?
        get() = document.layers.find { it.id == selectedLayerId } ?: document.layers.lastOrNull()
}

class GraphiteViewModel(
    private val repository: GraphiteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val savedDocuments: StateFlow<List<GraphiteDocumentModel>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val undoStack = ArrayDeque<GraphiteDocumentModel>()
    private val redoStack = ArrayDeque<GraphiteDocumentModel>()

    init {
        viewModelScope.launch {
            val initialDoc = repository.ensureSeeded()
            _uiState.update {
                it.copy(
                    document = initialDoc,
                    selectedLayerId = initialDoc.layers.lastOrNull()?.id
                )
            }
        }
    }

    private fun commitDocumentChange(
        newDoc: GraphiteDocumentModel,
        recordHistory: Boolean = true,
        newSelectedLayerId: String? = _uiState.value.selectedLayerId,
        toast: String? = null
    ) {
        val currentDoc = _uiState.value.document
        if (recordHistory && currentDoc != newDoc) {
            undoStack.addLast(currentDoc)
            if (undoStack.size > 40) undoStack.removeFirst()
            redoStack.clear()
        }
        val validSelectedId = if (newDoc.layers.any { it.id == newSelectedLayerId }) {
            newSelectedLayerId
        } else {
            newDoc.layers.lastOrNull()?.id
        }
        _uiState.update { state ->
            state.copy(
                document = newDoc,
                selectedLayerId = validSelectedId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusToast = toast ?: state.statusToast
            )
        }
        viewModelScope.launch {
            repository.saveDocument(newDoc)
        }
    }

    fun selectTab(tab: MobileWorkspaceTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectTool(tool: ToolType) {
        _uiState.update {
            it.copy(
                activeTool = tool,
                statusToast = "${tool.labelIt}: ${tool.hintIt}"
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(statusToast = null) }
    }

    fun selectLayer(layerId: String?) {
        _uiState.update { state ->
            val layer = state.document.layers.find { it.id == layerId }
            state.copy(
                selectedLayerId = layerId,
                selectedNodeId = layer?.nodes?.firstOrNull()?.id
            )
        }
    }

    fun selectNode(nodeId: String?) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.document
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        _uiState.update {
            it.copy(
                document = prev,
                selectedLayerId = prev.layers.lastOrNull()?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusToast = "Annullato (${undoStack.size} passi rimanenti)"
            )
        }
        viewModelScope.launch { repository.saveDocument(prev) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.document
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _uiState.update {
            it.copy(
                document = next,
                selectedLayerId = next.layers.lastOrNull()?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusToast = "Ripristinato"
            )
        }
        viewModelScope.launch { repository.saveDocument(next) }
    }

    fun swapWorkingColors() {
        _uiState.update {
            it.copy(
                primaryColorHex = it.secondaryColorHex,
                secondaryColorHex = it.primaryColorHex,
                statusToast = "Colori di lavoro invertiti"
            )
        }
    }

    fun resetWorkingColors() {
        _uiState.update {
            it.copy(
                primaryColorHex = 0xFF65BBE5L,
                secondaryColorHex = 0xFFEEEFF2L,
                statusToast = "Colori di lavoro ripristinati"
            )
        }
    }

    fun openColorPicker(isPrimary: Boolean) {
        _uiState.update {
            it.copy(showColorPickerDialog = true, editingPrimaryColor = isPrimary)
        }
    }

    fun closeColorPicker() {
        _uiState.update { it.copy(showColorPickerDialog = false) }
    }

    fun setWorkingColor(colorHex: Long, applyToSelectedLayer: Boolean = true) {
        val isPrimary = _uiState.value.editingPrimaryColor
        _uiState.update { state ->
            if (isPrimary) state.copy(primaryColorHex = colorHex)
            else state.copy(secondaryColorHex = colorHex)
        }
        if (applyToSelectedLayer) {
            val selected = _uiState.value.selectedLayer ?: return
            if (isPrimary) {
                updateLayer(selected.id) { it.copy(fillColorHex = colorHex) }
            } else {
                updateLayer(selected.id) { it.copy(strokeColorHex = colorHex, gradientEndColorHex = colorHex) }
            }
        }
    }

    fun setNodeCatalogVisible(visible: Boolean) {
        _uiState.update { it.copy(showNodeCatalogSheet = visible) }
    }

    fun setExportDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showExportDialog = visible) }
    }

    fun setNewDocDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showNewDocDialog = visible) }
    }

    fun updateViewport(zoom: Float, panX: Float, panY: Float) {
        _uiState.update {
            it.copy(
                zoom = zoom.coerceIn(0.25f, 5.0f),
                panX = panX.coerceIn(-1500f, 1500f),
                panY = panY.coerceIn(-1500f, 1500f)
            )
        }
    }

    fun resetViewport() {
        _uiState.update {
            it.copy(zoom = 1f, panX = 0f, panY = 0f, statusToast = "Vista centrata al 100%")
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleSnap() {
        _uiState.update {
            val next = !it.snapToGrid
            it.copy(snapToGrid = next, statusToast = if (next) " Calamita Griglia Attiva" else "Calamita Disattivata")
        }
    }

    fun toggleRulers() {
        _uiState.update { it.copy(showRulers = !it.showRulers) }
    }

    fun toggleWireframe() {
        _uiState.update {
            val next = !it.wireframeOverlay
            it.copy(wireframeOverlay = next, statusToast = if (next) "Modalità Contorno Vettoriale (Wireframe)" else "Modalità Render Completo")
        }
    }

    private fun snapCoord(value: Float): Float {
        return if (_uiState.value.snapToGrid) {
            (value / 20f).roundToInt() * 20f
        } else {
            value
        }
    }

    fun handleCanvasTap(artboardX: Float, artboardY: Float) {
        val state = _uiState.value
        val doc = state.document
        val sx = snapCoord(artboardX)
        val sy = snapCoord(artboardY)

        when (state.activeTool) {
            ToolType.SELECT, ToolType.NAVIGATE, ToolType.PATH -> {
                val hit = findTopmostLayerAt(doc, artboardX, artboardY)
                if (hit != null) {
                    selectLayer(hit.id)
                }
            }
            ToolType.EYEDROPPER -> {
                val hit = findTopmostLayerAt(doc, artboardX, artboardY)
                val sampled = hit?.fillColorHex ?: doc.artboardBgHex
                _uiState.update {
                    it.copy(
                        primaryColorHex = sampled,
                        activeTool = ToolType.SELECT,
                        statusToast = "Colore campionato: ${ProceduralEngine.hexToSvgColor(sampled)}"
                    )
                }
            }
            ToolType.FILL -> {
                val hit = findTopmostLayerAt(doc, artboardX, artboardY) ?: state.selectedLayer
                if (hit != null) {
                    updateLayer(hit.id, toast = "Riempimento applicato a ${hit.name}") {
                        it.copy(hasFill = true, fillColorHex = state.primaryColorHex, useGradient = false)
                    }
                } else {
                    commitDocumentChange(
                        doc.copy(artboardBgHex = state.primaryColorHex),
                        toast = "Colore sfondo tavola aggiornato"
                    )
                }
            }
            ToolType.GRADIENT -> {
                val hit = findTopmostLayerAt(doc, artboardX, artboardY) ?: state.selectedLayer
                if (hit != null) {
                    updateLayer(hit.id, toast = "Sfumatura applicata a ${hit.name}") {
                        it.copy(
                            hasFill = true,
                            useGradient = true,
                            fillColorHex = state.primaryColorHex,
                            gradientEndColorHex = state.secondaryColorHex
                        )
                    }
                }
            }
            ToolType.PEN, ToolType.SPLINE -> {
                val targetKind = if (state.activeTool == ToolType.SPLINE) LayerGeometryKind.SPLINE else LayerGeometryKind.BEZIER_PATH
                val currentLayer = state.selectedLayer
                if (currentLayer != null && currentLayer.kind == targetKind && !currentLayer.locked) {
                    val updatedPts = currentLayer.points + Vec2Point(sx, sy)
                    updateLayer(currentLayer.id, toast = "Punto aggiunto (${updatedPts.size} vertici)") {
                        it.copy(points = updatedPts)
                    }
                } else {
                    val newId = "layer-${UUID.randomUUID().toString().take(6)}"
                    val newLayer = GraphiteLayer(
                        id = newId,
                        name = if (targetKind == LayerGeometryKind.SPLINE) "Spline ${doc.layers.size + 1}" else "Tracciato Bézier ${doc.layers.size + 1}",
                        kind = targetKind,
                        x = sx,
                        y = sy,
                        width = 240f,
                        height = 240f,
                        hasFill = false,
                        strokeColorHex = state.primaryColorHex,
                        strokeWidth = max(4f, state.defaultStrokeWidth),
                        points = listOf(
                            Vec2Point(sx - 60f, sy),
                            Vec2Point(sx, sy - 60f),
                            Vec2Point(sx + 60f, sy)
                        )
                    )
                    commitDocumentChange(
                        doc.copy(layers = doc.layers + newLayer),
                        newSelectedLayerId = newId,
                        toast = "Nuovo tracciato creato — tocca per aggiungere altri vertici"
                    )
                }
            }
            ToolType.TEXT -> {
                val newId = "text-${UUID.randomUUID().toString().take(6)}"
                val newLayer = GraphiteLayer(
                    id = newId,
                    name = "Testo ${doc.layers.size + 1}",
                    kind = LayerGeometryKind.TEXT,
                    x = sx,
                    y = sy,
                    width = 320f,
                    height = 90f,
                    fillColorHex = state.primaryColorHex,
                    strokeWidth = 0f,
                    textContent = "Graphite Studio",
                    fontSize = 54f
                )
                commitDocumentChange(
                    doc.copy(layers = doc.layers + newLayer),
                    newSelectedLayerId = newId,
                    toast = "Livello Testo inserito"
                )
                _uiState.update { it.copy(activeTool = ToolType.SELECT) }
            }
            else -> {
                // Quick tap with a shape tool creates a default-sized procedural shape at tap point
                if (state.activeTool in listOf(ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE)) {
                    createShapeFromDrag(sx - 110f, sy - 90f, sx + 110f, sy + 90f)
                }
            }
        }
    }

    fun moveSelectedLayerBy(dx: Float, dy: Float, isFinished: Boolean) {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.locked) return
        val newX = if (isFinished) snapCoord(selected.x + dx) else selected.x + dx
        val newY = if (isFinished) snapCoord(selected.y + dy) else selected.y + dy
        val movedPoints = selected.points.map { pt ->
            pt.copy(x = pt.x + (newX - selected.x), y = pt.y + (newY - selected.y))
        }
        val updatedDoc = _uiState.value.document.copy(
            layers = _uiState.value.document.layers.map { l ->
                if (l.id == selected.id) l.copy(x = newX, y = newY, points = movedPoints) else l
            }
        )
        commitDocumentChange(updatedDoc, recordHistory = isFinished)
    }

    fun movePathVertex(pointIndex: Int, newX: Float, newY: Float, isFinished: Boolean) {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.locked || pointIndex !in selected.points.indices) return
        val sx = if (isFinished) snapCoord(newX) else newX
        val sy = if (isFinished) snapCoord(newY) else newY
        val newPts = selected.points.toMutableList()
        newPts[pointIndex] = newPts[pointIndex].copy(x = sx, y = sy)
        val updatedDoc = _uiState.value.document.copy(
            layers = _uiState.value.document.layers.map { l ->
                if (l.id == selected.id) l.copy(points = newPts) else l
            }
        )
        commitDocumentChange(updatedDoc, recordHistory = isFinished)
    }

    fun createShapeFromDrag(startX: Float, startY: Float, endX: Float, endY: Float) {
        val state = _uiState.value
        val doc = state.document
        val cx = snapCoord((startX + endX) / 2f)
        val cy = snapCoord((startY + endY) / 2f)
        val w = max(48f, abs(endX - startX))
        val h = max(48f, abs(endY - startY))
        val newId = "shape-${UUID.randomUUID().toString().take(6)}"

        val (kind, namePrefix) = when (state.activeTool) {
            ToolType.RECTANGLE -> LayerGeometryKind.RECTANGLE to "Rettangolo"
            ToolType.ELLIPSE -> LayerGeometryKind.ELLIPSE to "Ellisse"
            ToolType.POLYGON -> LayerGeometryKind.POLYGON to "Poligono"
            ToolType.STAR -> LayerGeometryKind.STAR to "Stella"
            ToolType.LINE -> LayerGeometryKind.LINE to "Linea"
            else -> LayerGeometryKind.RECTANGLE to "Forma"
        }

        val pts = if (kind == LayerGeometryKind.LINE) {
            listOf(Vec2Point(snapCoord(startX), snapCoord(startY)), Vec2Point(snapCoord(endX), snapCoord(endY)))
        } else emptyList()

        val newLayer = GraphiteLayer(
            id = newId,
            name = "$namePrefix ${doc.layers.size + 1}",
            kind = kind,
            x = cx,
            y = cy,
            width = w,
            height = h,
            hasFill = kind != LayerGeometryKind.LINE,
            fillColorHex = state.primaryColorHex,
            strokeColorHex = if (kind == LayerGeometryKind.LINE) state.primaryColorHex else state.secondaryColorHex,
            strokeWidth = max(3f, state.defaultStrokeWidth),
            polygonSides = state.defaultPolygonSides,
            points = pts
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + newLayer),
            newSelectedLayerId = newId,
            toast = "${newLayer.name} creato"
        )
    }

    fun addFreehandOrBrushStroke(points: List<Vec2Point>, isBrush: Boolean) {
        if (points.size < 2) return
        val state = _uiState.value
        val doc = state.document
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val newId = "stroke-${UUID.randomUUID().toString().take(6)}"
        val newLayer = GraphiteLayer(
            id = newId,
            name = if (isBrush) "Pennellata ${doc.layers.size + 1}" else "Tracciato Libero ${doc.layers.size + 1}",
            kind = if (isBrush) LayerGeometryKind.BRUSH_STROKE else LayerGeometryKind.FREEHAND,
            x = (minX + maxX) / 2f,
            y = (minY + maxY) / 2f,
            width = max(32f, maxX - minX),
            height = max(32f, maxY - minY),
            hasFill = false,
            strokeColorHex = state.primaryColorHex,
            strokeWidth = if (isBrush) max(14f, state.defaultStrokeWidth * 3f) else max(4f, state.defaultStrokeWidth),
            points = points
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + newLayer),
            newSelectedLayerId = newId,
            toast = "${newLayer.name} aggiunto"
        )
    }

    private fun findTopmostLayerAt(doc: GraphiteDocumentModel, x: Float, y: Float): GraphiteLayer? {
        for (layer in doc.layers.asReversed()) {
            if (!layer.visible || layer.locked) continue
            val evaluated = ProceduralEngine.evaluateLayer(layer)
            for (inst in evaluated) {
                val halfW = max(28f, inst.width / 2f + inst.strokeWidth)
                val halfH = max(28f, inst.height / 2f + inst.strokeWidth)
                if (x in (inst.centerX - halfW)..(inst.centerX + halfW) &&
                    y in (inst.centerY - halfH)..(inst.centerY + halfH)
                ) {
                    return layer
                }
                if (inst.points.any { hypot(it.x - x, it.y - y) <= 36f }) {
                    return layer
                }
            }
        }
        return null
    }

    // Layer Operations
    fun updateLayer(
        layerId: String,
        recordHistory: Boolean = true,
        toast: String? = null,
        transform: (GraphiteLayer) -> GraphiteLayer
    ) {
        val doc = _uiState.value.document
        val newLayers = doc.layers.map { if (it.id == layerId) transform(it) else it }
        commitDocumentChange(doc.copy(layers = newLayers), recordHistory = recordHistory, toast = toast)
    }

    fun addQuickLayer(kind: LayerGeometryKind) {
        val state = _uiState.value
        val doc = state.document
        val cx = doc.artboardWidth / 2f
        val cy = doc.artboardHeight / 2f
        val newId = "layer-${UUID.randomUUID().toString().take(6)}"
        val newLayer = GraphiteLayer(
            id = newId,
            name = "${kind.labelIt} ${doc.layers.size + 1}",
            kind = kind,
            x = cx,
            y = cy,
            width = 240f,
            height = 240f,
            fillColorHex = state.primaryColorHex,
            gradientEndColorHex = state.secondaryColorHex,
            strokeColorHex = 0xFFEEEFF2L,
            strokeWidth = 4f,
            points = if (kind in listOf(LayerGeometryKind.BEZIER_PATH, LayerGeometryKind.SPLINE, LayerGeometryKind.LINE)) {
                listOf(
                    Vec2Point(cx - 120f, cy + 40f),
                    Vec2Point(cx, cy - 80f),
                    Vec2Point(cx + 120f, cy + 40f)
                )
            } else emptyList()
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + newLayer),
            newSelectedLayerId = newId,
            toast = "Livello ${newLayer.name} aggiunto"
        )
    }

    fun duplicateLayer(layerId: String) {
        val doc = _uiState.value.document
        val orig = doc.layers.find { it.id == layerId } ?: return
        val copyId = "layer-${UUID.randomUUID().toString().take(6)}"
        val duplicated = orig.copy(
            id = copyId,
            name = "${orig.name} (Copia)",
            x = orig.x + 30f,
            y = orig.y + 30f,
            nodes = orig.nodes.map { it.copy(id = "node-${UUID.randomUUID().toString().take(6)}") }
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + duplicated),
            newSelectedLayerId = copyId,
            toast = "Livello duplicato con catena nodi"
        )
    }

    fun deleteLayer(layerId: String) {
        val doc = _uiState.value.document
        if (doc.layers.size <= 1) {
            _uiState.update { it.copy(statusToast = "Il documento deve contenere almeno un livello") }
            return
        }
        val filtered = doc.layers.filterNot { it.id == layerId }
        commitDocumentChange(
            doc.copy(layers = filtered),
            newSelectedLayerId = filtered.lastOrNull()?.id,
            toast = "Livello rimosso"
        )
    }

    fun moveLayerOrder(layerId: String, moveUpInVisualStack: Boolean) {
        val doc = _uiState.value.document
        val idx = doc.layers.indexOfFirst { it.id == layerId }
        if (idx == -1) return
        // Note: last in list is rendered on top
        val targetIdx = if (moveUpInVisualStack) idx + 1 else idx - 1
        if (targetIdx !in doc.layers.indices) return
        val mutable = doc.layers.toMutableList()
        val item = mutable.removeAt(idx)
        mutable.add(targetIdx, item)
        commitDocumentChange(doc.copy(layers = mutable))
    }

    // Procedural Node Graph Operations
    fun addProceduralNodeToSelected(nodeType: ProceduralNodeType) {
        val layer = _uiState.value.selectedLayer ?: return
        val newNode = ProceduralNode(
            id = "node-${UUID.randomUUID().toString().take(6)}",
            type = nodeType
        )
        updateLayer(layer.id, toast = "Nodo '${nodeType.titleIt}' collegato a ${layer.name}") {
            it.copy(nodes = it.nodes + newNode)
        }
        _uiState.update {
            it.copy(selectedNodeId = newNode.id, showNodeCatalogSheet = false)
        }
    }

    fun toggleNodeEnabled(layerId: String, nodeId: String) {
        updateLayer(layerId) { layer ->
            layer.copy(
                nodes = layer.nodes.map { n ->
                    if (n.id == nodeId) n.copy(enabled = !n.enabled) else n
                }
            )
        }
    }

    fun updateNodeParam(
        layerId: String,
        nodeId: String,
        paramKey: String,
        newValue: Float,
        recordHistory: Boolean = false
    ) {
        updateLayer(layerId, recordHistory = recordHistory) { layer ->
            layer.copy(
                nodes = layer.nodes.map { n ->
                    if (n.id == nodeId) {
                        val updatedParams = n.params.toMutableMap()
                        updatedParams[paramKey] = newValue
                        n.copy(params = updatedParams)
                    } else n
                }
            )
        }
    }

    fun moveNodeOrder(layerId: String, nodeId: String, delta: Int) {
        val layer = _uiState.value.document.layers.find { it.id == layerId } ?: return
        val idx = layer.nodes.indexOfFirst { it.id == nodeId }
        val target = idx + delta
        if (idx == -1 || target !in layer.nodes.indices) return
        val mutable = layer.nodes.toMutableList()
        val item = mutable.removeAt(idx)
        mutable.add(target, item)
        updateLayer(layerId, toast = "Ordine esecuzione nodi aggiornato") {
            it.copy(nodes = mutable)
        }
    }

    fun removeNode(layerId: String, nodeId: String) {
        updateLayer(layerId, toast = "Nodo rimosso dal grafo") { layer ->
            layer.copy(nodes = layer.nodes.filterNot { it.id == nodeId })
        }
    }

    fun applyProceduralPresetRecipe(recipeName: String) {
        val layer = _uiState.value.selectedLayer ?: return
        val presetNodes = when (recipeName) {
            "mandala" -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.RADIAL_REPEAT,
                    params = mapOf("count" to 12f, "radius" to 150f, "arcDeg" to 360f, "spinCopies" to 1f)
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.OFFSET_PATH,
                    params = mapOf("offset" to 8f, "roundness" to 16f)
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.DROP_SHADOW,
                    params = mapOf("offsetX" to 0f, "offsetY" to 10f, "spread" to 20f, "alpha" to 0.6f)
                )
            )
            "isometric_grid" -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.GRID_ARRAY,
                    params = mapOf("columns" to 4f, "rows" to 3f, "spacingX" to 140f, "spacingY" to 110f, "scaleStep" to 0.95f)
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.COLOR_ADJUST,
                    params = mapOf("hueShift" to 28f, "saturation" to 1.2f, "lightness" to 0.05f)
                )
            )
            else -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.JITTER_WAVE,
                    params = mapOf("amplitude" to 22f, "frequency" to 6f, "phase" to 45f)
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.MIRROR_SYMMETRY,
                    params = mapOf("axisMode" to 0f, "gap" to 90f)
                )
            )
        }
        updateLayer(layer.id, toast = "Catena procedurale applicata a ${layer.name}") {
            it.copy(nodes = presetNodes)
        }
    }

    // Portfolio & Demo Artworks
    fun openDemoArtwork(demoId: String) {
        val demoDoc = DemoArtworkFactory.createDocumentForDemo(demoId)
        undoStack.clear()
        redoStack.clear()
        commitDocumentChange(
            newDoc = demoDoc,
            recordHistory = false,
            newSelectedLayerId = demoDoc.layers.lastOrNull()?.id,
            toast = "Opera demo '${demoDoc.name}' aperta"
        )
        _uiState.update {
            it.copy(activeTab = MobileWorkspaceTab.CANVAS, zoom = 1f, panX = 0f, panY = 0f)
        }
    }

    fun openSavedDocument(doc: GraphiteDocumentModel) {
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
            it.copy(
                document = doc,
                selectedLayerId = doc.layers.lastOrNull()?.id,
                activeTab = MobileWorkspaceTab.CANVAS,
                zoom = 1f,
                panX = 0f,
                panY = 0f,
                canUndo = false,
                canRedo = false,
                statusToast = "Documento '${doc.name}' caricato"
            )
        }
    }

    fun createNewDocument(name: String, width: Int, height: Int, bgHex: Long) {
        val newDoc = GraphiteDocumentModel(
            id = "doc-${UUID.randomUUID().toString().take(8)}",
            name = name.ifBlank { "Progetto Senza Titolo" },
            artboardWidth = width.coerceIn(256, 4096),
            artboardHeight = height.coerceIn(256, 4096),
            artboardBgHex = bgHex,
            layers = listOf(
                GraphiteLayer(
                    id = "layer-initial",
                    name = "Forma Principale",
                    kind = LayerGeometryKind.POLYGON,
                    x = width / 2f,
                    y = height / 2f,
                    width = min(width, height) * 0.28f,
                    height = min(width, height) * 0.28f,
                    polygonSides = 6,
                    fillColorHex = 0xFF65BBE5L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF68C587L,
                    strokeColorHex = 0xFFFBF9EBL,
                    strokeWidth = 4f
                )
            )
        )
        undoStack.clear()
        redoStack.clear()
        commitDocumentChange(
            newDoc = newDoc,
            recordHistory = false,
            newSelectedLayerId = newDoc.layers.first().id,
            toast = "Nuovo documento '${newDoc.name}' (${width}×${height}) creato"
        )
        _uiState.update {
            it.copy(
                activeTab = MobileWorkspaceTab.CANVAS,
                showNewDocDialog = false,
                zoom = 1f,
                panX = 0f,
                panY = 0f
            )
        }
    }

    fun deleteSavedDocument(docId: String) {
        viewModelScope.launch {
            repository.deleteDocument(docId)
            _uiState.update { it.copy(statusToast = "Documento eliminato dall'archivio locale") }
        }
    }

    fun updateArtboardSettings(name: String, width: Int, height: Int, bgHex: Long) {
        val doc = _uiState.value.document
        commitDocumentChange(
            doc.copy(
                name = name.ifBlank { doc.name },
                artboardWidth = width.coerceIn(256, 4096),
                artboardHeight = height.coerceIn(256, 4096),
                artboardBgHex = bgHex
            ),
            toast = "Impostazioni tavola aggiornate"
        )
    }

    companion object {
        fun provideFactory(repository: GraphiteRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GraphiteViewModel(repository) as T
                }
            }
    }
}
