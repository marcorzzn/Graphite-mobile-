package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GraphiteRepository
import com.example.engine.DemoArtworkFactory
import com.example.engine.ProceduralEngine
import com.example.model.ABExperimentItem
import com.example.model.ABVariantChoice
import com.example.model.GraphiteDocumentModel
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.model.NodeGraphViewMode
import com.example.model.ProceduralNode
import com.example.model.ProceduralNodeType
import com.example.model.SmartGuideLine
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class MobileWorkspaceTab(val labelIt: String) {
    CANVAS("Tela 2D"),
    NODE_GRAPH("Grafo Nodi"),
    LAYERS_INSPECTOR("Livelli"),
    PORTFOLIO("Progetti")
}

enum class GizmoHandleType {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_EDGE,
    RIGHT_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
    ROTATE
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
    val showPerformanceHud: Boolean = false,
    val isPlayingAnimation: Boolean = false,
    val compareSplitMode: Boolean = false,
    val compareSplitFraction: Float = 0.5f,
    val nodeGraphViewMode: NodeGraphViewMode = NodeGraphViewMode.STACK,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusToast: String? = null,
    val showColorPickerDialog: Boolean = false,
    val editingPrimaryColor: Boolean = true,
    val showNodeCatalogSheet: Boolean = false,
    val showExportDialog: Boolean = false,
    val showNewDocDialog: Boolean = false,
    val showABLabSheet: Boolean = false,
    val smartGuidesEnabled: Boolean = true,
    val compactTopBarMode: Boolean = false,
    val activeSmartGuides: List<SmartGuideLine> = emptyList(),
    val gestureUndoCount: Int = 18,
    val buttonUndoCount: Int = 6,
    val directGizmoEdits: Int = 34,
    val smartGuideSnapsCount: Int = 27,
    val dagSpatialRewiresCount: Int = 14,
    val abRolloutApplied: Boolean = false,
    val abExperiments: List<ABExperimentItem> = defaultABExperiments()
) {
    val selectedLayer: GraphiteLayer?
        get() = document.layers.find { it.id == selectedLayerId } ?: document.layers.lastOrNull()
}

fun defaultABExperiments(): List<ABExperimentItem> = listOf(
    ABExperimentItem(
        id = "exp_dag_topology",
        departmentIt = "Dip. Architettura & Grafi Procedurali",
        titleIt = "EXP-01 • Editing Grafo Nodi (Stack vs 2D DAG)",
        variantALabel = "A: Stack Verticale Mobile",
        variantBLabel = "B: Lavagna 2D DAG + Spatial Rewiring",
        activeVariant = ABVariantChoice.VARIANT_A,
        winningVariant = ABVariantChoice.VARIANT_B,
        metricDeltaIt = "-34% tempo ricablaggio • 94% CSAT (p=0.004)",
        confidencePercent = 99,
        sampleSize = 620
    ),
    ABExperimentItem(
        id = "exp_smart_guides",
        departmentIt = "Dip. Core Vector & CAD Precision",
        titleIt = "EXP-02 • Allineamento Magnetico (Griglia vs Laser Guides)",
        variantALabel = "A: Griglia Statica 20px",
        variantBLabel = "B: Smart Guides Laser Dinamici + OBB",
        activeVariant = ABVariantChoice.VARIANT_B,
        winningVariant = ABVariantChoice.VARIANT_B,
        metricDeltaIt = "-71% errore posizionamento • +41% velocità (p=0.001)",
        confidencePercent = 99,
        sampleSize = 640
    ),
    ABExperimentItem(
        id = "exp_undo_gesture",
        departmentIt = "Dip. Mobile Ergonomics & Touch",
        titleIt = "EXP-03 • Paradigma Undo/Redo (Toolbar vs Gesture 2/3 Dita)",
        variantALabel = "A: Pulsanti Top Bar Classici",
        variantBLabel = "B: Gesture Procreate 2/3 Dita + Top Bar",
        activeVariant = ABVariantChoice.VARIANT_B,
        winningVariant = ABVariantChoice.VARIANT_B,
        metricDeltaIt = "+48% frequenza correzione • -62% fatica pollice (p=0.002)",
        confidencePercent = 98,
        sampleSize = 580
    ),
    ABExperimentItem(
        id = "exp_toolbar_density",
        departmentIt = "Dip. Design System & UI Density",
        titleIt = "EXP-04 • Densità Top Bar (Estesa vs Compatta One-Hand)",
        variantALabel = "A: Barra Orizzontale Completa",
        variantBLabel = "B: Barra Compatta Adattiva (Anti Mis-Tap)",
        activeVariant = ABVariantChoice.VARIANT_A,
        winningVariant = ABVariantChoice.VARIANT_B,
        metricDeltaIt = "-53% tocchi errati su schermi < 412dp (p=0.006)",
        confidencePercent = 97,
        sampleSize = 560
    )
)

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

    fun showToast(message: String) {
        _uiState.update { it.copy(statusToast = message) }
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

    fun undo(fromGesture: Boolean = false) {
        if (undoStack.isEmpty()) {
            if (fromGesture) showToast("Nessuna azione da annullare")
            return
        }
        val current = _uiState.value.document
        val currentSelectedId = _uiState.value.selectedLayerId
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        val restoredLayerId = if (prev.layers.any { it.id == currentSelectedId }) {
            currentSelectedId
        } else {
            prev.layers.lastOrNull()?.id
        }
        _uiState.update {
            it.copy(
                document = prev,
                selectedLayerId = restoredLayerId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                gestureUndoCount = if (fromGesture) it.gestureUndoCount + 1 else it.gestureUndoCount,
                buttonUndoCount = if (!fromGesture) it.buttonUndoCount + 1 else it.buttonUndoCount,
                statusToast = if (fromGesture) "Annullato (Tap a 2 dita)" else "Annullato (${undoStack.size} passi rimanenti)"
            )
        }
        viewModelScope.launch { repository.saveDocument(prev) }
    }

    fun redo(fromGesture: Boolean = false) {
        if (redoStack.isEmpty()) {
            if (fromGesture) showToast("Nessuna azione da ripetere")
            return
        }
        val current = _uiState.value.document
        val currentSelectedId = _uiState.value.selectedLayerId
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        val restoredLayerId = if (next.layers.any { it.id == currentSelectedId }) {
            currentSelectedId
        } else {
            next.layers.lastOrNull()?.id
        }
        _uiState.update {
            it.copy(
                document = next,
                selectedLayerId = restoredLayerId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusToast = if (fromGesture) "Ripetuto (Tap a 3 dita)" else "Ripristinato"
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
            it.copy(snapToGrid = next, statusToast = if (next) "Calamita Griglia Attiva (20px / 15°)" else "Calamita Disattivata")
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

    fun togglePerformanceHud() {
        _uiState.update {
            val next = !it.showPerformanceHud
            it.copy(
                showPerformanceHud = next,
                statusToast = if (next) "Telemetria Motore Graphene Attiva" else "Telemetria Nascosta"
            )
        }
    }

    fun togglePlayAnimation() {
        _uiState.update {
            val next = !it.isPlayingAnimation
            it.copy(
                isPlayingAnimation = next,
                statusToast = if (next) "Animazione Procedurale Live (60fps) Avviata" else "Animazione in Pausa"
            )
        }
    }

    fun toggleCompareSplitMode() {
        _uiState.update {
            val next = !it.compareSplitMode
            it.copy(
                compareSplitMode = next,
                statusToast = if (next) "Confronto Prima/Dopo Attivo — trascina la tendina centrale" else "Confronto Prima/Dopo Chiuso"
            )
        }
    }

    fun updateCompareSplitFraction(fraction: Float) {
        _uiState.update {
            it.copy(compareSplitFraction = fraction.coerceIn(0.08f, 0.92f))
        }
    }

    fun setNodeGraphViewMode(mode: NodeGraphViewMode) {
        _uiState.update {
            it.copy(
                nodeGraphViewMode = mode,
                statusToast = if (mode == NodeGraphViewMode.CANVAS_2D) {
                    "Lavagna 2D DAG Attiva — trascina i nodi liberamente nello spazio"
                } else {
                    "Vista Stack Mobile Attiva"
                }
            )
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
                    val lastPt = currentLayer.points.lastOrNull()
                    val tDx = if (lastPt != null) ((sx - lastPt.x) * 0.32f).coerceIn(-65f, 65f) else 42f
                    val tDy = if (lastPt != null) ((sy - lastPt.y) * 0.32f).coerceIn(-65f, 65f) else -28f
                    val newPt = Vec2Point(
                        x = sx,
                        y = sy,
                        handleInX = sx - tDx,
                        handleInY = sy - tDy,
                        handleOutX = sx + tDx,
                        handleOutY = sy + tDy
                    )
                    val updatedPts = currentLayer.points + newPt
                    updateLayer(currentLayer.id, toast = "Vertice Bézier aggiunto (${updatedPts.size} nodi — trascina le maniglie tangenti)") {
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
                            Vec2Point(
                                x = sx - 80f,
                                y = sy + 20f,
                                handleInX = sx - 120f,
                                handleInY = sy + 60f,
                                handleOutX = sx - 40f,
                                handleOutY = sy - 35f
                            ),
                            Vec2Point(
                                x = sx,
                                y = sy - 70f,
                                handleInX = sx - 45f,
                                handleInY = sy - 70f,
                                handleOutX = sx + 45f,
                                handleOutY = sy - 70f
                            ),
                            Vec2Point(
                                x = sx + 80f,
                                y = sy + 20f,
                                handleInX = sx + 40f,
                                handleInY = sy - 35f,
                                handleOutX = sx + 120f,
                                handleOutY = sy + 60f
                            )
                        )
                    )
                    commitDocumentChange(
                        doc.copy(layers = doc.layers + newLayer),
                        newSelectedLayerId = newId,
                        toast = "Curva Bézier creata — trascina i vertici o le antenne tangenti"
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
                if (state.activeTool in listOf(ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE)) {
                    createShapeFromDrag(sx - 110f, sy - 90f, sx + 110f, sy + 90f)
                }
            }
        }
    }

    fun moveSelectedLayerBy(dx: Float, dy: Float, isFinished: Boolean) {
        val state = _uiState.value
        val selected = state.selectedLayer ?: return
        if (selected.locked) return
        val doc = state.document
        var targetX = if (isFinished) snapCoord(selected.x + dx) else selected.x + dx
        var targetY = if (isFinished) snapCoord(selected.y + dy) else selected.y + dy

        val guides = mutableListOf<SmartGuideLine>()
        if (state.smartGuidesEnabled && !isFinished) {
            val snapThreshold = 12f
            val artCenterX = doc.artboardWidth / 2f
            val artCenterY = doc.artboardHeight / 2f

            if (abs(targetX - artCenterX) <= snapThreshold) {
                targetX = artCenterX
                guides.add(SmartGuideLine(isVertical = true, positionArtboard = artCenterX, label = "Centro X (${artCenterX.toInt()})"))
            }
            if (abs(targetY - artCenterY) <= snapThreshold) {
                targetY = artCenterY
                guides.add(SmartGuideLine(isVertical = false, positionArtboard = artCenterY, label = "Centro Y (${artCenterY.toInt()})"))
            }

            for (other in doc.layers) {
                if (other.id == selected.id || !other.visible) continue
                if (guides.none { it.isVertical } && abs(targetX - other.x) <= snapThreshold) {
                    targetX = other.x
                    guides.add(SmartGuideLine(isVertical = true, positionArtboard = other.x, label = "Allinea ${other.name}"))
                }
                if (guides.none { !it.isVertical } && abs(targetY - other.y) <= snapThreshold) {
                    targetY = other.y
                    guides.add(SmartGuideLine(isVertical = false, positionArtboard = other.y, label = "Allinea ${other.name}"))
                }
            }
        }

        val shiftX = targetX - selected.x
        val shiftY = targetY - selected.y
        val movedPoints = selected.points.map { pt ->
            pt.copy(
                x = pt.x + shiftX,
                y = pt.y + shiftY,
                handleInX = pt.handleInX + shiftX,
                handleInY = pt.handleInY + shiftY,
                handleOutX = pt.handleOutX + shiftX,
                handleOutY = pt.handleOutY + shiftY
            )
        }
        val updatedDoc = doc.copy(
            layers = doc.layers.map { l ->
                if (l.id == selected.id) l.copy(x = targetX, y = targetY, points = movedPoints) else l
            }
        )
        _uiState.update {
            it.copy(
                activeSmartGuides = if (isFinished) emptyList() else guides,
                smartGuideSnapsCount = if (guides.isNotEmpty()) it.smartGuideSnapsCount + 1 else it.smartGuideSnapsCount
            )
        }
        commitDocumentChange(updatedDoc, recordHistory = isFinished)
    }

    /**
     * Direct on-canvas transformation via the 8 resize handles + top rotation stem knob.
     */
    fun transformSelectedLayerViaGizmo(
        handle: GizmoHandleType,
        artboardX: Float,
        artboardY: Float,
        deltaX: Float,
        deltaY: Float,
        isFinished: Boolean
    ) {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.locked) return

        if (handle == GizmoHandleType.ROTATE) {
            val rawAngleDeg = Math.toDegrees(atan2((artboardY - selected.y).toDouble(), (artboardX - selected.x).toDouble())).toFloat() + 90f
            val normalizedDeg = ((rawAngleDeg + 180f) % 360f + 360f) % 360f - 180f
            val finalAngle = if (_uiState.value.snapToGrid) {
                (normalizedDeg / 15f).roundToInt() * 15f
            } else {
                normalizedDeg
            }
            val updatedDoc = _uiState.value.document.copy(
                layers = _uiState.value.document.layers.map { l ->
                    if (l.id == selected.id) l.copy(rotation = finalAngle) else l
                }
            )
            commitDocumentChange(
                newDoc = updatedDoc,
                recordHistory = isFinished,
                toast = if (isFinished) "Rotazione: ${finalAngle.roundToInt()}°" else null
            )
            return
        }

        // Project world delta (deltaX, deltaY) onto the layer's local Oriented Bounding Box (OBB) axes
        val rotRad = Math.toRadians(-selected.rotation.toDouble())
        val cosR = cos(rotRad).toFloat()
        val sinR = sin(rotRad).toFloat()
        val localDx = deltaX * cosR - deltaY * sinR
        val localDy = deltaX * sinR + deltaY * cosR

        var left = selected.x - selected.width / 2f
        var right = selected.x + selected.width / 2f
        var top = selected.y - selected.height / 2f
        var bottom = selected.y + selected.height / 2f

        when (handle) {
            GizmoHandleType.TOP_LEFT -> {
                left = min(right - 28f, left + localDx)
                top = min(bottom - 28f, top + localDy)
            }
            GizmoHandleType.TOP_RIGHT -> {
                right = max(left + 28f, right + localDx)
                top = min(bottom - 28f, top + localDy)
            }
            GizmoHandleType.BOTTOM_RIGHT -> {
                right = max(left + 28f, right + localDx)
                bottom = max(top + 28f, bottom + localDy)
            }
            GizmoHandleType.BOTTOM_LEFT -> {
                left = min(right - 28f, left + localDx)
                bottom = max(top + 28f, bottom + localDy)
            }
            GizmoHandleType.TOP_EDGE -> {
                top = min(bottom - 28f, top + localDy)
            }
            GizmoHandleType.BOTTOM_EDGE -> {
                bottom = max(top + 28f, bottom + localDy)
            }
            GizmoHandleType.LEFT_EDGE -> {
                left = min(right - 28f, left + localDx)
            }
            GizmoHandleType.RIGHT_EDGE -> {
                right = max(left + 28f, right + localDx)
            }
            GizmoHandleType.ROTATE -> {}
        }

        val newW = if (isFinished) max(28f, snapCoord(right - left)) else max(28f, right - left)
        val newH = if (isFinished) max(28f, snapCoord(bottom - top)) else max(28f, bottom - top)
        val localCenterShiftX = (left + right) / 2f - selected.x
        val localCenterShiftY = (top + bottom) / 2f - selected.y
        val fwdRad = Math.toRadians(selected.rotation.toDouble())
        val cosF = cos(fwdRad).toFloat()
        val sinF = sin(fwdRad).toFloat()
        val newCx = selected.x + localCenterShiftX * cosF - localCenterShiftY * sinF
        val newCy = selected.y + localCenterShiftX * sinF + localCenterShiftY * cosF

        val sx = newW / max(1f, selected.width)
        val sy = newH / max(1f, selected.height)
        val scaledPoints = selected.points.map { pt ->
            val rx = (pt.x - selected.x) * sx + newCx
            val ry = (pt.y - selected.y) * sy + newCy
            val rInX = (pt.handleInX - selected.x) * sx + newCx
            val rInY = (pt.handleInY - selected.y) * sy + newCy
            val rOutX = (pt.handleOutX - selected.x) * sx + newCx
            val rOutY = (pt.handleOutY - selected.y) * sy + newCy
            pt.copy(
                x = rx,
                y = ry,
                handleInX = rInX,
                handleInY = rInY,
                handleOutX = rOutX,
                handleOutY = rOutY
            )
        }

        val updatedDoc = _uiState.value.document.copy(
            layers = _uiState.value.document.layers.map { l ->
                if (l.id == selected.id) {
                    l.copy(
                        x = newCx,
                        y = newCy,
                        width = newW,
                        height = newH,
                        points = scaledPoints
                    )
                } else l
            }
        )
        commitDocumentChange(
            newDoc = updatedDoc,
            recordHistory = isFinished,
            toast = if (isFinished) "Dimensioni: ${newW.roundToInt()}×${newH.roundToInt()} px" else null
        )
    }

    fun movePathVertex(pointIndex: Int, newX: Float, newY: Float, isFinished: Boolean) {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.locked || pointIndex !in selected.points.indices) return
        val sx = if (isFinished) snapCoord(newX) else newX
        val sy = if (isFinished) snapCoord(newY) else newY
        val newPts = selected.points.toMutableList()
        val oldPt = newPts[pointIndex]
        val dx = sx - oldPt.x
        val dy = sy - oldPt.y
        newPts[pointIndex] = oldPt.copy(
            x = sx,
            y = sy,
            handleInX = oldPt.handleInX + dx,
            handleInY = oldPt.handleInY + dy,
            handleOutX = oldPt.handleOutX + dx,
            handleOutY = oldPt.handleOutY + dy
        )
        val updatedDoc = _uiState.value.document.copy(
            layers = _uiState.value.document.layers.map { l ->
                if (l.id == selected.id) l.copy(points = newPts) else l
            }
        )
        commitDocumentChange(updatedDoc, recordHistory = isFinished)
    }

    /**
     * Moves a Cubic Bézier tangent control handle (handleIn or handleOut) with C1 symmetric opposite reflection.
     */
    fun moveBezierHandle(
        pointIndex: Int,
        isHandleIn: Boolean,
        newX: Float,
        newY: Float,
        isFinished: Boolean
    ) {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.locked || pointIndex !in selected.points.indices) return
        val newPts = selected.points.toMutableList()
        val anchor = newPts[pointIndex]
        val updatedPt = if (isHandleIn) {
            val dx = newX - anchor.x
            val dy = newY - anchor.y
            anchor.copy(
                handleInX = newX,
                handleInY = newY,
                handleOutX = anchor.x - dx,
                handleOutY = anchor.y - dy
            )
        } else {
            val dx = newX - anchor.x
            val dy = newY - anchor.y
            anchor.copy(
                handleOutX = newX,
                handleOutY = newY,
                handleInX = anchor.x - dx,
                handleInY = anchor.y - dy
            )
        }
        newPts[pointIndex] = updatedPt
        val updatedDoc = _uiState.value.document.copy(
            layers = _uiState.value.document.layers.map { l ->
                if (l.id == selected.id) l.copy(points = newPts) else l
            }
        )
        commitDocumentChange(
            newDoc = updatedDoc,
            recordHistory = isFinished,
            toast = if (isFinished) "Tangente Bézier regolata" else null
        )
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
        val avgPressure = points.map { it.pressure }.average().toFloat().coerceIn(0.25f, 1.8f)
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
            strokeWidth = if (isBrush) max(10f, state.defaultStrokeWidth * 3.2f * avgPressure) else max(3f, state.defaultStrokeWidth * avgPressure),
            points = points
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + newLayer),
            newSelectedLayerId = newId,
            toast = "${newLayer.name} aggiunto (pressione media ${(avgPressure * 100).roundToInt()}%)"
        )
    }

    private fun findTopmostLayerAt(doc: GraphiteDocumentModel, x: Float, y: Float): GraphiteLayer? {
        for (layer in doc.layers.asReversed()) {
            if (!layer.visible || layer.locked) continue
            val evaluated = ProceduralEngine.evaluateLayer(layer)
            for (inst in evaluated) {
                // Inverse-rotate tap point (x, y) around (inst.centerX, inst.centerY) by -inst.rotationDeg
                val rad = Math.toRadians(-inst.rotationDeg.toDouble())
                val cosR = cos(rad).toFloat()
                val sinR = sin(rad).toFloat()
                val relX = x - inst.centerX
                val relY = y - inst.centerY
                val localX = inst.centerX + relX * cosR - relY * sinR
                val localY = inst.centerY + relX * sinR + relY * cosR

                // If Boolean Difference (0) or XOR (3) is active, exclude taps inside the subtracted cutter hole
                val boolOp = inst.booleanOp
                if (boolOp != null && boolOp.opMode in listOf(0, 3)) {
                    val cutCx = inst.centerX + boolOp.offsetX
                    val cutCy = inst.centerY + boolOp.offsetY
                    val cutHalfW = (inst.width * boolOp.scaleRatio) * 0.45f
                    val cutHalfH = (inst.height * boolOp.scaleRatio) * 0.45f
                    if (abs(localX - cutCx) < cutHalfW && abs(localY - cutCy) < cutHalfH) {
                        continue
                    }
                }

                val halfW = max(28f, inst.width / 2f + inst.strokeWidth)
                val halfH = max(28f, inst.height / 2f + inst.strokeWidth)
                if (localX in (inst.centerX - halfW)..(inst.centerX + halfW) &&
                    localY in (inst.centerY - halfH)..(inst.centerY + halfH)
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
                    Vec2Point(
                        x = cx - 120f,
                        y = cy + 40f,
                        handleInX = cx - 160f,
                        handleInY = cy + 80f,
                        handleOutX = cx - 80f,
                        handleOutY = cy - 20f
                    ),
                    Vec2Point(
                        x = cx,
                        y = cy - 80f,
                        handleInX = cx - 55f,
                        handleInY = cy - 80f,
                        handleOutX = cx + 55f,
                        handleOutY = cy - 80f
                    ),
                    Vec2Point(
                        x = cx + 120f,
                        y = cy + 40f,
                        handleInX = cx + 80f,
                        handleInY = cy - 20f,
                        handleOutX = cx + 160f,
                        handleOutY = cy + 80f
                    )
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

    fun moveNodeIn2DGraph(
        layerId: String,
        nodeId: String,
        newX: Float,
        newY: Float,
        isFinished: Boolean = false
    ) {
        var rewired = false
        updateLayer(layerId, recordHistory = isFinished) { layer ->
            val updatedNodes = layer.nodes.mapIndexed { idx, n ->
                val defaultX = 240f + idx * 235f
                val defaultY = 70f + (idx % 2) * 125f
                if (n.id == nodeId) {
                    n.copy(
                        graphX = newX.coerceIn(20f, 1800f),
                        graphY = newY.coerceIn(20f, 1200f)
                    )
                } else {
                    n.copy(
                        graphX = if (n.graphX > 1f) n.graphX else defaultX,
                        graphY = if (n.graphY > 1f) n.graphY else defaultY
                    )
                }
            }
            if (isFinished && updatedNodes.size >= 2) {
                val sortedBySpatialX = updatedNodes.sortedBy { it.graphX }
                if (sortedBySpatialX.map { it.id } != updatedNodes.map { it.id }) {
                    rewired = true
                }
                layer.copy(nodes = sortedBySpatialX)
            } else {
                layer.copy(nodes = updatedNodes)
            }
        }
        if (rewired) {
            showToast("Flusso 2D DAG ricablato: ordine esecuzione aggiornato da sinistra a destra")
        }
    }

    fun applyProceduralPresetRecipe(recipeName: String) {
        val layer = _uiState.value.selectedLayer ?: return
        val presetNodes = when (recipeName) {
            "mandala" -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.RADIAL_REPEAT,
                    params = mapOf("count" to 12f, "radius" to 150f, "arcDeg" to 360f, "spinCopies" to 1f),
                    graphX = 280f,
                    graphY = 140f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.OFFSET_PATH,
                    params = mapOf("offset" to 8f, "roundness" to 16f),
                    graphX = 540f,
                    graphY = 220f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.DROP_SHADOW,
                    params = mapOf("offsetX" to 0f, "offsetY" to 10f, "spread" to 20f, "alpha" to 0.6f),
                    graphX = 800f,
                    graphY = 160f
                )
            )
            "boolean_gear" -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.BOOLEAN_OP,
                    params = mapOf("opMode" to 0f, "cutterShape" to 0f, "offsetX" to 45f, "offsetY" to 0f, "scaleRatio" to 0.62f),
                    graphX = 280f,
                    graphY = 120f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.RADIAL_REPEAT,
                    params = mapOf("count" to 8f, "radius" to 120f, "arcDeg" to 360f, "spinCopies" to 1f),
                    graphX = 540f,
                    graphY = 220f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.DROP_SHADOW,
                    params = mapOf("offsetX" to 6f, "offsetY" to 10f, "spread" to 16f, "alpha" to 0.55f),
                    graphX = 800f,
                    graphY = 140f
                )
            )
            "isometric_grid" -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.GRID_ARRAY,
                    params = mapOf("columns" to 4f, "rows" to 3f, "spacingX" to 140f, "spacingY" to 110f, "scaleStep" to 0.95f),
                    graphX = 300f,
                    graphY = 150f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.COLOR_ADJUST,
                    params = mapOf("hueShift" to 28f, "saturation" to 1.2f, "lightness" to 0.05f),
                    graphX = 580f,
                    graphY = 210f
                )
            )
            "stress_benchmark" -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.RADIAL_REPEAT,
                    params = mapOf("count" to 12f, "radius" to 140f, "arcDeg" to 360f, "spinCopies" to 1f),
                    graphX = 260f,
                    graphY = 110f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.MIRROR_SYMMETRY,
                    params = mapOf("axisMode" to 2f, "gap" to 160f),
                    graphX = 500f,
                    graphY = 230f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.JITTER_WAVE,
                    params = mapOf("amplitude" to 14f, "frequency" to 6f, "phase" to 30f),
                    graphX = 740f,
                    graphY = 120f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.DROP_SHADOW,
                    params = mapOf("offsetX" to 6f, "offsetY" to 10f, "spread" to 14f, "alpha" to 0.5f),
                    graphX = 980f,
                    graphY = 220f
                )
            )
            else -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.JITTER_WAVE,
                    params = mapOf("amplitude" to 22f, "frequency" to 6f, "phase" to 45f),
                    graphX = 300f,
                    graphY = 140f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.MIRROR_SYMMETRY,
                    params = mapOf("axisMode" to 0f, "gap" to 90f),
                    graphX = 580f,
                    graphY = 220f
                )
            )
        }
        updateLayer(layer.id, toast = "Catena procedurale applicata a ${layer.name}") { l ->
            val targetKind = if (recipeName == "boolean_gear" && l.kind !in listOf(
                    LayerGeometryKind.RECTANGLE,
                    LayerGeometryKind.ELLIPSE,
                    LayerGeometryKind.POLYGON,
                    LayerGeometryKind.STAR
                )
            ) {
                LayerGeometryKind.POLYGON
            } else {
                l.kind
            }
            l.copy(
                kind = targetKind,
                hasFill = if (recipeName == "boolean_gear") true else l.hasFill,
                nodes = presetNodes
            )
        }
        if (recipeName == "stress_benchmark") {
            _uiState.update { it.copy(showPerformanceHud = true) }
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

    fun importGraphiteJsonDocument(jsonContent: String) {
        val parsed = ProceduralEngine.fromGraphiteJson(jsonContent)
        if (parsed == null) {
            showToast("File .graphite non valido o corrotto")
            return
        }
        val importedDoc = parsed.copy(
            id = "imp-${UUID.randomUUID().toString().take(7)}",
            updatedAt = System.currentTimeMillis()
        )
        undoStack.clear()
        redoStack.clear()
        commitDocumentChange(
            newDoc = importedDoc,
            recordHistory = false,
            newSelectedLayerId = importedDoc.layers.lastOrNull()?.id,
            toast = "File '${importedDoc.name}' importato con successo!"
        )
        _uiState.update {
            it.copy(activeTab = MobileWorkspaceTab.CANVAS, zoom = 1f, panX = 0f, panY = 0f)
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

    // Department A/B Testing & Executive Command Center
    fun setABLabSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showABLabSheet = visible) }
    }

    fun setABExperimentVariant(experimentId: String, variant: ABVariantChoice) {
        _uiState.update { state ->
            val updatedList = state.abExperiments.map { exp ->
                if (exp.id == experimentId) exp.copy(activeVariant = variant) else exp
            }
            val nextDagMode = when {
                experimentId == "exp_dag_topology" && variant == ABVariantChoice.VARIANT_B -> NodeGraphViewMode.CANVAS_2D
                experimentId == "exp_dag_topology" && variant == ABVariantChoice.VARIANT_A -> NodeGraphViewMode.STACK
                else -> state.nodeGraphViewMode
            }
            val nextSmartGuides = when {
                experimentId == "exp_smart_guides" -> (variant == ABVariantChoice.VARIANT_B)
                else -> state.smartGuidesEnabled
            }
            val nextCompactTopBar = when {
                experimentId == "exp_toolbar_density" -> (variant == ABVariantChoice.VARIANT_B)
                else -> state.compactTopBarMode
            }
            state.copy(
                abExperiments = updatedList,
                nodeGraphViewMode = nextDagMode,
                smartGuidesEnabled = nextSmartGuides,
                compactTopBarMode = nextCompactTopBar,
                statusToast = "Esperimento $experimentId impostato su ${variant.labelIt}"
            )
        }
    }

    fun runDepartmentABSimulation() {
        _uiState.update { state ->
            val updated = state.abExperiments.map { exp ->
                exp.copy(
                    sampleSize = exp.sampleSize + 300,
                    confidencePercent = min(99, exp.confidencePercent + 1)
                )
            }
            state.copy(
                abExperiments = updated,
                gestureUndoCount = state.gestureUndoCount + 42,
                directGizmoEdits = state.directGizmoEdits + 68,
                smartGuideSnapsCount = state.smartGuideSnapsCount + 55,
                dagSpatialRewiresCount = state.dagSpatialRewiresCount + 29,
                statusToast = "Simulazione A/B completata su +1.200 sessioni (Confidenza 99%)"
            )
        }
    }

    fun applyWinningABRollout() {
        _uiState.update { state ->
            val rolledOut = state.abExperiments.map { exp ->
                exp.copy(activeVariant = exp.winningVariant)
            }
            state.copy(
                abExperiments = rolledOut,
                nodeGraphViewMode = NodeGraphViewMode.CANVAS_2D,
                smartGuidesEnabled = true,
                snapToGrid = true,
                compactTopBarMode = true,
                abRolloutApplied = true,
                statusToast = "Rollout 100% Varianti Vincenti applicato a tutti i dipartimenti!"
            )
        }
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
