package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GraphiteRepository
import com.example.engine.DemoArtworkFactory
import com.example.engine.GraphiteGuideEngine
import com.example.engine.GuideResolutionResult
import com.example.engine.PhotoAndVideoEngine
import com.example.engine.ProceduralEngine
import com.example.model.*
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
    val showCommandPalette: Boolean = false,
    val showLiveDagDrawer: Boolean = false,
    val workspaceMode: StudioWorkspaceMode = StudioWorkspaceMode.PRO,
    val smartGuidesEnabled: Boolean = true,
    val compactTopBarMode: Boolean = false,
    val activeSmartGuides: List<SmartGuideLine> = emptyList(),
    val gestureUndoCount: Int = 18,
    val buttonUndoCount: Int = 6,
    val directGizmoEdits: Int = 34,
    val smartGuideSnapsCount: Int = 27,
    val dagSpatialRewiresCount: Int = 14,
    val abRolloutApplied: Boolean = false,
    val abExperiments: List<ABExperimentItem> = defaultABExperiments(),
    // 4 Contextual Workspace Modes & Unified Creative Suite
    val creativeDomainMode: CreativeDomainMode = CreativeDomainMode.MODALITA_DESIGN,
    val activeSuiteSubPanel: SuiteSubPanel = SuiteSubPanel.DESIGN_TEMPLATES,
    val showCreativeSuiteSheet: Boolean = false,
    // Graphite Guide (Deterministic Non-AI Assistant)
    val guideState: GraphiteGuideState = GraphiteGuideState.ACTIVE,
    val guideQueryText: String = "",
    val lastGuideResolution: GuideResolutionResult? = null,
    val activeSpotlightTag: String? = null,
    val showGuidePttModal: Boolean = false,
    // Picsart Engine State
    val photoAdjustments: PhotoAdjustmentsState = PhotoAdjustmentsState(),
    val retouchState: RetouchAndBgState = RetouchAndBgState(),
    val filterEffectsState: FilterAndEffectsState = FilterAndEffectsState(),
    val collageState: CollageComposerState = CollageComposerState(),
    val activeBrushPreset: RasterBrushPreset = RasterBrushPreset.WATERCOLOR,
    val brushSizePx: Float = 18f,
    val brushHardness: Float = 0.78f,
    val brushOpacity: Float = 0.92f,
    val bucketTolerance: Float = 0.24f,
    val videoTimeline: VideoTimelineState = VideoTimelineState(),
    val lastVideoExportSummary: String? = null,
    // Canva Engine State
    val brandKit: LocalBrandKit = LocalBrandKit(),
    val presentationSlides: List<PresentationSlide> = listOf(
        PresentationSlide(
            id = "slide-1",
            title = "Copertina Progetto",
            subtitle = "Architettura 100% Locale",
            speakerNotes = "Benvenuti alla presentazione architetturale Graphite Offline Studio.",
            transition = VideoTransitionStyle.DISSOLVE,
            entranceAnim = SlideEntranceAnimation.SLIDE_UP
        ),
        PresentationSlide(
            id = "slide-2",
            title = "Sistema Design & Palette",
            subtitle = "Brand Kit & Varianti Componenti",
            speakerNotes = "Mostra l'applicazione del Brand Kit locale e delle varianti componenti.",
            transition = VideoTransitionStyle.SLIDE_LEFT,
            entranceAnim = SlideEntranceAnimation.ZOOM_POP
        ),
        PresentationSlide(
            id = "slide-3",
            title = "Metriche & Esportazione",
            subtitle = "Zero Cloud Latency",
            speakerNotes = "Riepilogo performance 100% locale senza dipendenze cloud.",
            transition = VideoTransitionStyle.ZOOM_IN,
            entranceAnim = SlideEntranceAnimation.FADE_IN
        )
    ),
    val activeSlideIndex: Int = 0,
    val isPlayingPresentation: Boolean = false,
    val presentationElapsedSeconds: Int = 0,
    val scheduledPosts: List<SocialPostScheduleItem> = listOf(
        SocialPostScheduleItem(
            id = "post-1",
            title = "Lancio Collezione Estate",
            channel = "Instagram",
            dateTimeLabel = "2026-05-15 18:30",
            captionText = "✨ Lancio Collezione Estate — Creato 100% in locale con Graphite Studio. #DesignLocale #VisualArt #MadeWithGraphite",
            formatLabel = "Instagram Post 1:1",
            watermarkEnabled = false
        ),
        SocialPostScheduleItem(
            id = "post-2",
            title = "Dietro le Quinte Procedurali",
            channel = "Instagram Story",
            dateTimeLabel = "2026-05-18 12:00",
            captionText = "Come nasce Dietro le Quinte Procedurali: dalla bozza vettoriale al risultato finale in 3 passaggi.",
            formatLabel = "Story 9:16",
            watermarkEnabled = true
        )
    ),
    // Figma & FigJam Engine State
    val autoLayoutState: FigmaAutoLayoutState = FigmaAutoLayoutState(),
    val masterComponents: List<FigmaComponentMaster> = listOf(
        FigmaComponentMaster(
            id = "comp-cta-primary",
            name = "Button / Primary CTA",
            sourceLayerId = "iso-basin-outer",
            activeVariant = ComponentVariantState.DEFAULT,
            overrideLabel = "Inizia Ora",
            overrideFillHex = 0xFF65BBE5L
        ),
        FigmaComponentMaster(
            id = "comp-card-feature",
            name = "Card / Feature Tile",
            sourceLayerId = "iso-basin-outer",
            activeVariant = ComponentVariantState.HOVER,
            overrideLabel = "Modulo Offline",
            overrideFillHex = 0xFF1E293BL
        )
    ),
    val prototypeLinks: List<PrototypeLink> = listOf(
        PrototypeLink(
            id = "proto-1",
            sourceLayerId = "iso-basin-outer",
            sourceLayerName = "Vasca Isometrica",
            targetSlideTitle = "Schermata Dettaglio Prodotto",
            trigger = PrototypeTriggerType.ON_TAP,
            transition = PrototypeTransitionType.SMART_ANIMATE
        )
    ),
    val isPlayingPrototype: Boolean = false,
    val activePrototypeScreenName: String = "Home Principale",
    val offlineComments: List<OfflineCommentThread> = listOf(
        OfflineCommentThread(
            id = "comm-1",
            author = "Elena R. (Lead UI)",
            message = "Aumentiamo il contrasto del pulsante primario per accessibilità WCAG AAA.",
            artboardX = 540f,
            artboardY = 360f,
            createdAtLabel = "Oggi, 10:15",
            isResolved = false
        ),
        OfflineCommentThread(
            id = "comm-2",
            author = "Marco V. (Art Director)",
            message = "Ottima la griglia Auto Layout a 16px, approvata per il rilascio.",
            artboardX = 320f,
            artboardY = 680f,
            createdAtLabel = "Ieri, 17:40",
            isResolved = true
        )
    ),
    val versionSnapshots: List<VersionSnapshotItem> = listOf(
        VersionSnapshotItem(
            id = "snap-v1",
            versionTag = "v1.0",
            titleIt = "Layout Iniziale",
            author = "Locale",
            timestampLabel = "Ieri, 14:20",
            layerCount = 4,
            nodeCount = 4,
            diffSummaryIt = "Base vettoriale 4 livelli, palette ciano",
            serializedJson = "{}"
        ),
        VersionSnapshotItem(
            id = "snap-v2",
            versionTag = "v1.1",
            titleIt = "Brand Kit & Componenti",
            author = "Locale",
            timestampLabel = "Oggi, 09:45",
            layerCount = 6,
            nodeCount = 6,
            diffSummaryIt = "+2 livelli, Auto Layout attivo, 6 nodi DAG",
            serializedJson = "{}"
        )
    ),
    val activeDiffReport: String? = null,
    val figJamStickies: List<FigJamStickyItem> = listOf(
        FigJamStickyItem(
            id = "sticky-1",
            text = "Obiettivo Q3: 100% Zero-Cloud Latency su filtri LUT e grafi",
            author = "Team Core",
            colorHex = 0xFFFEF08AL,
            x = 180f,
            y = 220f,
            votes = 5
        ),
        FigJamStickyItem(
            id = "sticky-2",
            text = "Nuovi preset Social 4:5 e 9:16 con Magic Resize istantaneo",
            author = "Team Social",
            colorHex = 0xFFA7F3D0L,
            x = 520f,
            y = 220f,
            votes = 4
        )
    ),
    val figJamTimerSeconds: Int = 300,
    val figJamTimerRunning: Boolean = false
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
        val currentLayer = _uiState.value.selectedLayer
        if (tool == ToolType.PATH && currentLayer != null && currentLayer.points.isEmpty() && currentLayer.kind != LayerGeometryKind.TEXT) {
            bakeSelectedLayerToBezierPath()
            return
        }
        _uiState.update {
            it.copy(
                activeTool = tool,
                statusToast = "${tool.labelIt}: ${tool.hintIt}"
            )
        }
    }

    fun setWorkspaceMode(mode: StudioWorkspaceMode) {
        _uiState.update { state ->
            state.copy(
                workspaceMode = mode,
                showRulers = mode == StudioWorkspaceMode.PRO,
                showPerformanceHud = if (mode != StudioWorkspaceMode.PRO) false else state.showPerformanceHud,
                showLiveDagDrawer = if (mode == StudioWorkspaceMode.ZEN) false else state.showLiveDagDrawer,
                compactTopBarMode = mode == StudioWorkspaceMode.EASY,
                statusToast = "${mode.labelIt}: ${mode.descriptionIt}"
            )
        }
    }

    fun cycleWorkspaceMode() {
        val next = when (_uiState.value.workspaceMode) {
            StudioWorkspaceMode.PRO -> StudioWorkspaceMode.EASY
            StudioWorkspaceMode.EASY -> StudioWorkspaceMode.ZEN
            StudioWorkspaceMode.ZEN -> StudioWorkspaceMode.PRO
        }
        setWorkspaceMode(next)
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
                    val firstPt = currentLayer.points.firstOrNull()
                    if (firstPt != null && currentLayer.points.size >= 3 && !currentLayer.closedPath &&
                        hypot(sx - firstPt.x, sy - firstPt.y) <= 28f
                    ) {
                        updateLayer(currentLayer.id, toast = "Tracciato Bézier chiuso (Contorno Chiuso + Riempimento attivo)") {
                            it.copy(closedPath = true, hasFill = true)
                        }
                    } else {
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
                        updateLayer(currentLayer.id, toast = "Vertice Bézier aggiunto (${updatedPts.size} nodi — tocca il 1° vertice per chiudere)") {
                            it.copy(points = updatedPts)
                        }
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
            val selHalfW = selected.width / 2f
            val selHalfH = selected.height / 2f

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
                val otherLeft = other.x - other.width / 2f
                val otherRight = other.x + other.width / 2f
                val otherTop = other.y - other.height / 2f
                val otherBottom = other.y + other.height / 2f

                if (guides.none { it.isVertical }) {
                    when {
                        abs(targetX - other.x) <= snapThreshold -> {
                            targetX = other.x
                            guides.add(SmartGuideLine(isVertical = true, positionArtboard = other.x, label = "Centro ${other.name}"))
                        }
                        abs((targetX - selHalfW) - otherLeft) <= snapThreshold -> {
                            targetX = otherLeft + selHalfW
                            guides.add(SmartGuideLine(isVertical = true, positionArtboard = otherLeft, label = "Bordo Sin. ${other.name}"))
                        }
                        abs((targetX + selHalfW) - otherRight) <= snapThreshold -> {
                            targetX = otherRight - selHalfW
                            guides.add(SmartGuideLine(isVertical = true, positionArtboard = otherRight, label = "Bordo Des. ${other.name}"))
                        }
                    }
                }
                if (guides.none { !it.isVertical }) {
                    when {
                        abs(targetY - other.y) <= snapThreshold -> {
                            targetY = other.y
                            guides.add(SmartGuideLine(isVertical = false, positionArtboard = other.y, label = "Centro ${other.name}"))
                        }
                        abs((targetY - selHalfH) - otherTop) <= snapThreshold -> {
                            targetY = otherTop + selHalfH
                            guides.add(SmartGuideLine(isVertical = false, positionArtboard = otherTop, label = "Bordo Sup. ${other.name}"))
                        }
                        abs((targetY + selHalfH) - otherBottom) <= snapThreshold -> {
                            targetY = otherBottom - selHalfH
                            guides.add(SmartGuideLine(isVertical = false, positionArtboard = otherBottom, label = "Bordo Inf. ${other.name}"))
                        }
                    }
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
     * Moves a Cubic Bézier tangent control handle (handleIn or handleOut) with C1 symmetric opposite reflection
     * when `anchor.smooth` is true, or C0 independent cusp movement when `anchor.smooth` is false.
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
                handleOutX = if (anchor.smooth) anchor.x - dx else anchor.handleOutX,
                handleOutY = if (anchor.smooth) anchor.y - dy else anchor.handleOutY
            )
        } else {
            val dx = newX - anchor.x
            val dy = newY - anchor.y
            anchor.copy(
                handleOutX = newX,
                handleOutY = newY,
                handleInX = if (anchor.smooth) anchor.x - dx else anchor.handleInX,
                handleInY = if (anchor.smooth) anchor.y - dy else anchor.handleInY
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
            toast = if (isFinished) {
                if (anchor.smooth) "Tangente Bézier C¹ (Simmetrica) regolata" else "Tangente Cuspide C⁰ (Indipendente) regolata"
            } else null
        )
    }

    fun toggleBezierClosedPath() {
        val selected = _uiState.value.selectedLayer ?: return
        val nextClosed = !selected.closedPath
        updateLayer(
            layerId = selected.id,
            toast = if (nextClosed) "Tracciato Chiuso (Riempimento attivo)" else "Tracciato Aperto"
        ) {
            it.copy(
                closedPath = nextClosed,
                hasFill = if (nextClosed) true else it.hasFill
            )
        }
    }

    fun toggleBezierVerticesSmoothMode() {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.points.isEmpty()) return
        val nextSmooth = !selected.points.first().smooth
        updateLayer(
            layerId = selected.id,
            toast = if (nextSmooth) "Modalità Vertici: C¹ Morbidi Simmetrici" else "Modalità Vertici: C⁰ Cuspidi Indipendenti"
        ) { layer ->
            layer.copy(
                points = layer.points.map { pt -> pt.copy(smooth = nextSmooth) }
            )
        }
    }

    fun toggleLiveDagDrawer() {
        _uiState.update {
            val next = !it.showLiveDagDrawer
            it.copy(
                showLiveDagDrawer = next,
                statusToast = if (next) "Split-Workspace Attivo: Tela 2D + Mini-DAG Procedurale" else "Split-Workspace Chiuso"
            )
        }
    }

    /**
     * Patch v3.5 (Zero-Defect Vector Audit):
     * Converts any primitive layer (Rectangle, Ellipse, Polygon, Star) or evaluated DAG output
     * into an editable closed Cubic Bézier path (`BEZIER_PATH`) with draggable anchor vertices and C¹/C⁰ handles.
     */
    fun bakeSelectedLayerToBezierPath() {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.locked || selected.kind == LayerGeometryKind.TEXT) return
        val bakedPoints = ProceduralEngine.bakeLayerToEditablePoints(selected)
        if (bakedPoints.size < 2) return
        val evaluatedFirst = ProceduralEngine.evaluateLayer(selected).firstOrNull()
        updateLayer(
            layerId = selected.id,
            recordHistory = true,
            toast = "${selected.name} convertito in Tracciato Bézier (${bakedPoints.size} vertici modificabili)"
        ) { layer ->
            layer.copy(
                kind = LayerGeometryKind.BEZIER_PATH,
                x = evaluatedFirst?.centerX ?: layer.x,
                y = evaluatedFirst?.centerY ?: layer.y,
                width = evaluatedFirst?.width ?: layer.width,
                height = evaluatedFirst?.height ?: layer.height,
                closedPath = true,
                points = bakedPoints
            )
        }
        _uiState.update { it.copy(activeTool = ToolType.PATH) }
    }

    /**
     * Patch v3.5 (Zero-Defect Pathfinder Booleans):
     * Combines the selected layer with the adjacent visible layer using Skia Path.Op algebra
     * (`0 = Sottrai / Fora`, `1 = Unisci`, `2 = Interseca`, `3 = Escludi XOR`).
     */
    fun combineSelectedWithBelowBoolean(opMode: Int) {
        val state = _uiState.value
        val doc = state.document
        val selected = state.selectedLayer ?: return
        if (selected.locked) return

        val selIdx = doc.layers.indexOfFirst { it.id == selected.id }
        val partner = doc.layers.take(selIdx).lastOrNull { it.visible && !it.locked }
            ?: doc.layers.firstOrNull { it.id != selected.id && it.visible && !it.locked }

        val cutterShapeCode = when (selected.kind) {
            LayerGeometryKind.ELLIPSE -> 0f
            LayerGeometryKind.RECTANGLE -> 1f
            else -> 2f
        }
        val opLabel = when (opMode) {
            0 -> "Sottrazione (Foro)"
            1 -> "Unione Vettoriale"
            2 -> "Intersezione"
            else -> "Esclusione (XOR)"
        }

        if (partner != null && doc.layers.size >= 2) {
            val relOx = (selected.x - partner.x).coerceIn(-180f, 180f)
            val relOy = (selected.y - partner.y).coerceIn(-180f, 180f)
            val ratio = (max(selected.width, selected.height) / max(24f, max(partner.width, partner.height))).coerceIn(0.2f, 1.4f)
            val boolNode = ProceduralNode(
                id = "node-bool-${UUID.randomUUID().toString().take(5)}",
                type = ProceduralNodeType.BOOLEAN_OP,
                params = mapOf(
                    "opMode" to opMode.toFloat(),
                    "cutterShape" to cutterShapeCode,
                    "offsetX" to relOx,
                    "offsetY" to relOy,
                    "scaleRatio" to ratio
                ),
                graphX = 320f,
                graphY = 140f
            )
            val newLayers = doc.layers.mapNotNull { l ->
                when (l.id) {
                    selected.id -> null // Cutter layer merged into partner's boolean graph
                    partner.id -> l.copy(
                        nodes = l.nodes.filterNot { it.type == ProceduralNodeType.BOOLEAN_OP } + boolNode
                    )
                    else -> l
                }
            }
            commitDocumentChange(
                newDoc = doc.copy(layers = newLayers),
                recordHistory = true,
                newSelectedLayerId = partner.id,
                toast = "Pathfinder $opLabel applicata tra '${selected.name}' e '${partner.name}'"
            )
        } else {
            val boolNode = ProceduralNode(
                id = "node-bool-${UUID.randomUUID().toString().take(5)}",
                type = ProceduralNodeType.BOOLEAN_OP,
                params = mapOf(
                    "opMode" to opMode.toFloat(),
                    "cutterShape" to 0f,
                    "offsetX" to 36f,
                    "offsetY" to 0f,
                    "scaleRatio" to 0.6f
                ),
                graphX = 320f,
                graphY = 140f
            )
            updateLayer(
                layerId = selected.id,
                recordHistory = true,
                toast = "Pathfinder $opLabel applicata a '${selected.name}'"
            ) { l ->
                l.copy(nodes = l.nodes.filterNot { it.type == ProceduralNodeType.BOOLEAN_OP } + boolNode)
            }
        }
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
        val smoothedPoints = if (points.size >= 4) {
            points.mapIndexed { idx, pt ->
                if (idx == 0 || idx == points.lastIndex) {
                    pt
                } else {
                    val prev = points[idx - 1]
                    val next = points[idx + 1]
                    val sx = prev.x * 0.22f + pt.x * 0.56f + next.x * 0.22f
                    val sy = prev.y * 0.22f + pt.y * 0.56f + next.y * 0.22f
                    val sp = prev.pressure * 0.2f + pt.pressure * 0.6f + next.pressure * 0.2f
                    pt.copy(x = sx, y = sy, handleInX = sx, handleInY = sy, handleOutX = sx, handleOutY = sy, pressure = sp)
                }
            }
        } else points

        val state = _uiState.value
        val doc = state.document
        val minX = smoothedPoints.minOf { it.x }
        val maxX = smoothedPoints.maxOf { it.x }
        val minY = smoothedPoints.minOf { it.y }
        val maxY = smoothedPoints.maxOf { it.y }
        val avgPressure = smoothedPoints.map { it.pressure }.average().toFloat().coerceIn(0.25f, 1.8f)
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
            points = smoothedPoints
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + newLayer),
            newSelectedLayerId = newId,
            toast = "${newLayer.name} aggiunto (Stabilizzato • pressione ${(avgPressure * 100).roundToInt()}%)"
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

    /**
     * Phase 3 (TP-B3): Quick Artboard Alignment & Multi-Layer Distribution with full point/handle shifting.
     */
    fun alignSelectedLayer(alignment: ArtboardAlignment) {
        val doc = _uiState.value.document
        if (alignment == ArtboardAlignment.DISTRIBUTE_H || alignment == ArtboardAlignment.DISTRIBUTE_V) {
            val unlocked = doc.layers.filter { it.visible && !it.locked }
            if (unlocked.size < 2) {
                showToast("Servono almeno 2 livelli sbloccati per la distribuzione")
                return
            }
            val isHoriz = alignment == ArtboardAlignment.DISTRIBUTE_H
            val sorted = if (isHoriz) unlocked.sortedBy { it.x } else unlocked.sortedBy { it.y }
            val minPos = if (isHoriz) sorted.first().x else sorted.first().y
            val maxPos = if (isHoriz) sorted.last().x else sorted.last().y
            val span = max(120f, maxPos - minPos)
            val step = span / (sorted.size - 1).coerceAtLeast(1)
            val targetMap = sorted.mapIndexed { idx, l ->
                l.id to (minPos + idx * step)
            }.toMap()

            val updatedLayers = doc.layers.map { l ->
                val targetCoord = targetMap[l.id] ?: return@map l
                val dx = if (isHoriz) targetCoord - l.x else 0f
                val dy = if (!isHoriz) targetCoord - l.y else 0f
                val shiftedPts = l.points.map { pt ->
                    pt.copy(
                        x = pt.x + dx,
                        y = pt.y + dy,
                        handleInX = pt.handleInX + dx,
                        handleInY = pt.handleInY + dy,
                        handleOutX = pt.handleOutX + dx,
                        handleOutY = pt.handleOutY + dy
                    )
                }
                l.copy(x = l.x + dx, y = l.y + dy, points = shiftedPts)
            }
            commitDocumentChange(
                doc.copy(layers = updatedLayers),
                recordHistory = true,
                toast = "${alignment.labelIt} (${unlocked.size} livelli)"
            )
            return
        }

        val selected = _uiState.value.selectedLayer ?: return
        if (selected.locked) return
        val halfW = selected.width / 2f
        val halfH = selected.height / 2f
        val targetX = when (alignment) {
            ArtboardAlignment.ALIGN_LEFT -> halfW
            ArtboardAlignment.ALIGN_CENTER_H -> doc.artboardWidth / 2f
            ArtboardAlignment.ALIGN_RIGHT -> doc.artboardWidth - halfW
            else -> selected.x
        }
        val targetY = when (alignment) {
            ArtboardAlignment.ALIGN_TOP -> halfH
            ArtboardAlignment.ALIGN_CENTER_V -> doc.artboardHeight / 2f
            ArtboardAlignment.ALIGN_BOTTOM -> doc.artboardHeight - halfH
            else -> selected.y
        }
        val dx = targetX - selected.x
        val dy = targetY - selected.y
        val shiftedPts = selected.points.map { pt ->
            pt.copy(
                x = pt.x + dx,
                y = pt.y + dy,
                handleInX = pt.handleInX + dx,
                handleInY = pt.handleInY + dy,
                handleOutX = pt.handleOutX + dx,
                handleOutY = pt.handleOutY + dy
            )
        }
        updateLayer(
            layerId = selected.id,
            recordHistory = true,
            toast = "${selected.name}: ${alignment.labelIt}"
        ) { l ->
            l.copy(x = targetX, y = targetY, points = shiftedPts)
        }
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
            "sticker_outline" -> listOf(
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.OFFSET_PATH,
                    params = mapOf("offset" to 22f, "roundness" to 18f),
                    graphX = 280f,
                    graphY = 130f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.STROKE_DASH,
                    params = mapOf("dashLen" to 24f, "gapLen" to 10f, "phase" to 0f),
                    graphX = 540f,
                    graphY = 210f
                ),
                ProceduralNode(
                    id = "node-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.DROP_SHADOW,
                    params = mapOf("offsetX" to 8f, "offsetY" to 12f, "spread" to 16f, "alpha" to 0.7f),
                    graphX = 800f,
                    graphY = 140f
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
        val trimmed = jsonContent.trim()
        if (trimmed.startsWith("<svg", ignoreCase = true) || trimmed.startsWith("<?xml", ignoreCase = true)) {
            importSvgDocument(trimmed, mergeIntoCurrent = false)
            return
        }
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

    /**
     * Phase 3 (TP-A3 & TP-C3): Bidirectional SVG 1.1 Vector & Cubic Bézier Importer.
     */
    fun importSvgDocument(svgContent: String, mergeIntoCurrent: Boolean = false) {
        val currentDoc = _uiState.value.document
        val parsed = ProceduralEngine.importFromSvg(svgContent, currentDoc)
        if (parsed == null || parsed.layers.isEmpty()) {
            showToast("Codice SVG non valido o privo di forme vettoriali supportate")
            return
        }
        if (mergeIntoCurrent) {
            val reindexedLayers = parsed.layers.mapIndexed { idx, l ->
                l.copy(id = "svg-${UUID.randomUUID().toString().take(6)}-$idx")
            }
            val mergedDoc = currentDoc.copy(
                layers = currentDoc.layers + reindexedLayers,
                updatedAt = System.currentTimeMillis()
            )
            commitDocumentChange(
                newDoc = mergedDoc,
                recordHistory = true,
                newSelectedLayerId = reindexedLayers.last().id,
                toast = "${reindexedLayers.size} livelli vettoriali SVG importati nella tavola attiva"
            )
        } else {
            val importedDoc = parsed.copy(
                id = "svg-doc-${UUID.randomUUID().toString().take(6)}",
                name = "Progetto SVG Importato",
                updatedAt = System.currentTimeMillis()
            )
            commitDocumentChange(
                newDoc = importedDoc,
                recordHistory = true,
                newSelectedLayerId = importedDoc.layers.last().id,
                toast = "Documento SVG importato (${importedDoc.layers.size} livelli vettoriali)"
            )
        }
        _uiState.update {
            it.copy(activeTab = MobileWorkspaceTab.CANVAS, showExportDialog = false)
        }
    }

    /**
     * Phase 3 (TP-A3): Live SVG 1.1 Round-Trip verification on the active document.
     */
    fun roundTripCurrentDocumentViaSvg() {
        val currentDoc = _uiState.value.document
        val exportedSvg = ProceduralEngine.exportToSvg(currentDoc)
        val reimported = ProceduralEngine.importFromSvg(exportedSvg, currentDoc)
        if (reimported != null && reimported.layers.isNotEmpty()) {
            val roundTripDoc = reimported.copy(
                id = currentDoc.id,
                name = currentDoc.name,
                updatedAt = System.currentTimeMillis()
            )
            commitDocumentChange(
                newDoc = roundTripDoc,
                recordHistory = true,
                newSelectedLayerId = roundTripDoc.layers.last().id,
                toast = "Round-Trip SVG 1.1 verificato (${roundTripDoc.layers.size} livelli ricostruiti)"
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

    // Department A/B Testing & Executive Command Center + Universal Command Palette (Phase 5 TP-D5)
    fun setABLabSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showABLabSheet = visible) }
    }

    fun setCommandPaletteVisible(visible: Boolean) {
        _uiState.update { it.copy(showCommandPalette = visible) }
    }

    fun executeEnterpriseCommand(commandId: String) {
        _uiState.update { it.copy(showCommandPalette = false) }
        when (commandId) {
            "cmd_bake_to_path" -> {
                bakeSelectedLayerToBezierPath()
            }
            "cmd_boolean_subtract_layers" -> {
                combineSelectedWithBelowBoolean(opMode = 0)
            }
            "cmd_cycle_workspace_mode" -> {
                cycleWorkspaceMode()
            }
            "cmd_align_center" -> {
                alignSelectedLayer(ArtboardAlignment.ALIGN_CENTER_H)
                alignSelectedLayer(ArtboardAlignment.ALIGN_CENTER_V)
            }
            "cmd_distribute_h" -> {
                alignSelectedLayer(ArtboardAlignment.DISTRIBUTE_H)
            }
            "cmd_macro_mandala" -> {
                applyProceduralPresetRecipe("mandala")
            }
            "cmd_macro_boolean_gear" -> {
                applyProceduralPresetRecipe("boolean_gear")
            }
            "cmd_macro_sticker_outline" -> {
                applyProceduralPresetRecipe("sticker_outline")
            }
            "cmd_toggle_perf_120hz" -> {
                togglePerformanceHud()
            }
            "cmd_export_handoff" -> {
                setExportDialogVisible(true)
            }
            "cmd_rollout_ab" -> {
                applyWinningABRollout()
            }
        }
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

    // =========================================================================
    // 4 CONTEXTUAL WORKSPACE MODES & UNIFIED CREATIVE SUITE NAVIGATION
    // =========================================================================

    fun setCreativeDomainMode(mode: CreativeDomainMode) {
        val defaultPanel = when (mode) {
            CreativeDomainMode.MODALITA_FOTO -> SuiteSubPanel.PHOTO_CROP_TRANSFORM
            CreativeDomainMode.MODALITA_DESIGN -> SuiteSubPanel.DESIGN_TEMPLATES
            CreativeDomainMode.MODALITA_UI_UX -> SuiteSubPanel.UIUX_AUTO_LAYOUT
            CreativeDomainMode.MODALITA_DISEGNO -> SuiteSubPanel.DRAW_BRUSH_STUDIO
        }
        _uiState.update {
            it.copy(
                creativeDomainMode = mode,
                activeSuiteSubPanel = defaultPanel,
                statusToast = "${mode.labelIt}: ${mode.descriptionIt}"
            )
        }
    }

    fun openSuiteSubPanel(panel: SuiteSubPanel) {
        _uiState.update {
            it.copy(
                creativeDomainMode = panel.domain,
                activeSuiteSubPanel = panel,
                showCreativeSuiteSheet = true,
                activeSpotlightTag = null
            )
        }
    }

    fun toggleCreativeSuiteSheet(show: Boolean? = null) {
        _uiState.update {
            val next = show ?: !it.showCreativeSuiteSheet
            it.copy(showCreativeSuiteSheet = next)
        }
    }

    // =========================================================================
    // GRAPHITE GUIDE — DETERMINISTIC RULE-BASED ASSISTANT (100% NON-AI)
    // =========================================================================

    fun cycleGraphiteGuideState() {
        _uiState.update { state ->
            val next = when (state.guideState) {
                GraphiteGuideState.ACTIVE -> GraphiteGuideState.HIDDEN_PTT
                GraphiteGuideState.HIDDEN_PTT -> GraphiteGuideState.OFF
                GraphiteGuideState.OFF -> GraphiteGuideState.ACTIVE
            }
            state.copy(
                guideState = next,
                showGuidePttModal = false,
                activeSpotlightTag = if (next == GraphiteGuideState.OFF) null else state.activeSpotlightTag,
                statusToast = "Graphite Guide: ${next.hintIt}"
            )
        }
    }

    fun setGraphiteGuideState(newState: GraphiteGuideState) {
        _uiState.update {
            it.copy(
                guideState = newState,
                showGuidePttModal = false,
                statusToast = "Graphite Guide impostato su ${newState.badgeIt}"
            )
        }
    }

    fun toggleGuidePttModal(show: Boolean? = null) {
        _uiState.update {
            val next = show ?: !it.showGuidePttModal
            it.copy(showGuidePttModal = next)
        }
    }

    fun updateGuideQuery(query: String) {
        _uiState.update { state ->
            val resolution = if (query.isNotBlank()) {
                GraphiteGuideEngine.parseCommand(
                    rawQuery = query,
                    activeDomain = state.creativeDomainMode,
                    selectedLayer = state.selectedLayer
                )
            } else {
                null
            }
            state.copy(
                guideQueryText = query,
                lastGuideResolution = resolution,
                activeSpotlightTag = resolution?.spotlightTag
            )
        }
    }

    fun executeGuideCommand(query: String = _uiState.value.guideQueryText) {
        val state = _uiState.value
        val resolution = GraphiteGuideEngine.parseCommand(
            rawQuery = query,
            activeDomain = state.creativeDomainMode,
            selectedLayer = state.selectedLayer
        )
        _uiState.update {
            it.copy(
                guideQueryText = query,
                lastGuideResolution = resolution,
                activeSpotlightTag = resolution.spotlightTag,
                creativeDomainMode = resolution.targetDomain,
                activeSuiteSubPanel = resolution.targetSubPanel,
                showCreativeSuiteSheet = true,
                statusToast = "Guida Deterministica: ${resolution.titleIt}"
            )
        }
    }

    fun triggerGuideInstantAction(actionId: String?) {
        when (actionId) {
            "guide_act_remove_bg", "guide_act_portrait_retouch" -> openSuiteSubPanel(SuiteSubPanel.PHOTO_RETOUCH_BG)
            "guide_act_auto_tonal_curve" -> {
                applyToneCurvePreset("S_CONTRAST")
                openSuiteSubPanel(SuiteSubPanel.PHOTO_TONAL_CURVES)
            }
            "guide_act_lut_charcoal" -> {
                applyLutAndEffects(
                    _uiState.value.filterEffectsState.copy(activeLut = PhotoFilterLutPreset.CHARCOAL_SKETCH)
                )
                openSuiteSubPanel(SuiteSubPanel.PHOTO_FILTERS_EFFECTS)
            }
            "guide_act_lut_vintage" -> {
                applyLutAndEffects(
                    _uiState.value.filterEffectsState.copy(activeLut = PhotoFilterLutPreset.VINTAGE)
                )
                openSuiteSubPanel(SuiteSubPanel.PHOTO_FILTERS_EFFECTS)
            }
            "guide_act_crop_straighten" -> openSuiteSubPanel(SuiteSubPanel.PHOTO_CROP_TRANSFORM)
            "guide_act_generate_collage" -> {
                generateCollageOnCanvas()
                openSuiteSubPanel(SuiteSubPanel.PHOTO_COLLAGE)
            }
            "guide_act_open_video" -> openSuiteSubPanel(SuiteSubPanel.VIDEO_TIMELINE)
            "guide_act_magic_resize_story" -> applyMagicResize(1080, 1920, "Story 9:16")
            "guide_act_apply_brand_kit" -> applyLocalBrandKitToDocument()
            "guide_act_start_presentation" -> togglePresentationPlayer(true)
            "guide_act_schedule_post" -> openSuiteSubPanel(SuiteSubPanel.DESIGN_SOCIAL_PLANNER)
            "guide_act_apply_autolayout" -> {
                applyFigmaAutoLayout(_uiState.value.autoLayoutState.copy(enabled = true))
            }
            "guide_act_create_component" -> openSuiteSubPanel(SuiteSubPanel.UIUX_COMPONENTS)
            "guide_act_launch_prototype" -> togglePrototypePlayer(true)
            "guide_act_add_sticky" -> {
                addFigJamStickyNote("Idea Brainstorming Locale")
                openSuiteSubPanel(SuiteSubPanel.UIUX_FIGJAM_BOARD)
            }
            "guide_act_save_version_snapshot" -> {
                saveVersionSnapshot("v1.${_uiState.value.versionSnapshots.size + 1}")
                openSuiteSubPanel(SuiteSubPanel.UIUX_COLLAB_HISTORY)
            }
            "guide_act_inspect_code" -> openSuiteSubPanel(SuiteSubPanel.UIUX_HANDOFF_INSPECT)
            "guide_act_bake_vector_network" -> {
                selectTool(ToolType.PATH)
                openSuiteSubPanel(SuiteSubPanel.DRAW_VECTOR_BOOLEAN)
            }
            else -> toggleCreativeSuiteSheet(true)
        }
    }

    fun dismissGuideSpotlight() {
        _uiState.update { it.copy(activeSpotlightTag = null, lastGuideResolution = null) }
    }

    // =========================================================================
    // PHASE 1: PICSART ENGINE (CROP, TONAL, CURVES, RETOUCH, LUT, COLLAGE, VIDEO)
    // =========================================================================

    fun applyPhotoAdjustmentsToLayer(newAdjustments: PhotoAdjustmentsState) {
        val state = _uiState.value
        val target = state.selectedLayer
        _uiState.update { it.copy(photoAdjustments = newAdjustments) }
        if (target != null) {
            val adjusted = PhotoAndVideoEngine.applyPhotoAdjustmentsToLayer(target, newAdjustments)
            updateLayer(
                layerId = target.id,
                toast = "Regolazioni fotografiche applicate a ${target.name}"
            ) { adjusted }
        }
    }

    fun applyCropRatioToArtboard(ratio: PhotoCropAspectRatio, straightenDeg: Float = _uiState.value.photoAdjustments.horizonStraightenDeg) {
        val state = _uiState.value
        val doc = state.document
        val (newW, newH) = when (ratio) {
            PhotoCropAspectRatio.FREE -> doc.artboardWidth to doc.artboardHeight
            PhotoCropAspectRatio.RATIO_1_1 -> {
                val side = min(doc.artboardWidth, doc.artboardHeight)
                side to side
            }
            PhotoCropAspectRatio.RATIO_4_5 -> {
                val w = doc.artboardWidth
                w to (w * 5 / 4)
            }
            PhotoCropAspectRatio.RATIO_16_9 -> {
                val w = doc.artboardWidth
                w to (w * 9 / 16)
            }
            PhotoCropAspectRatio.RATIO_9_16 -> {
                val w = min(doc.artboardWidth, 1080)
                w to (w * 16 / 9)
            }
        }
        val updatedAdj = state.photoAdjustments.copy(
            cropRatio = ratio,
            horizonStraightenDeg = straightenDeg.coerceIn(-45f, 45f)
        )
        _uiState.update { it.copy(photoAdjustments = updatedAdj) }
        commitDocumentChange(
            doc.copy(artboardWidth = newW, artboardHeight = newH),
            toast = "Crop ${ratio.labelIt} (${newW}×${newH}px) applicato"
        )
    }

    fun rotateArtboard90Degrees() {
        val doc = _uiState.value.document
        commitDocumentChange(
            doc.copy(artboardWidth = doc.artboardHeight, artboardHeight = doc.artboardWidth),
            toast = "Tavola ruotata di 90° (${doc.artboardHeight}×${doc.artboardWidth}px)"
        )
    }

    fun flipSelectedLayerAxes(horizontal: Boolean) {
        val adj = _uiState.value.photoAdjustments
        val nextAdj = if (horizontal) {
            adj.copy(flipHorizontal = !adj.flipHorizontal)
        } else {
            adj.copy(flipVertical = !adj.flipVertical)
        }
        applyPhotoAdjustmentsToLayer(nextAdj)
    }

    fun applyToneCurvePreset(presetName: String) {
        val points = when (presetName) {
            "S_CONTRAST" -> listOf(
                CurveControlPoint(0f, 0f),
                CurveControlPoint(0.25f, 0.16f),
                CurveControlPoint(0.50f, 0.50f),
                CurveControlPoint(0.75f, 0.84f),
                CurveControlPoint(1f, 1f)
            )
            "MATTE_FADE" -> listOf(
                CurveControlPoint(0f, 0.10f),
                CurveControlPoint(0.25f, 0.28f),
                CurveControlPoint(0.50f, 0.52f),
                CurveControlPoint(0.75f, 0.76f),
                CurveControlPoint(1f, 0.92f)
            )
            "HIGH_KEY" -> listOf(
                CurveControlPoint(0f, 0.05f),
                CurveControlPoint(0.25f, 0.36f),
                CurveControlPoint(0.50f, 0.64f),
                CurveControlPoint(0.75f, 0.88f),
                CurveControlPoint(1f, 1f)
            )
            else -> listOf(
                CurveControlPoint(0f, 0f),
                CurveControlPoint(0.25f, 0.25f),
                CurveControlPoint(0.50f, 0.50f),
                CurveControlPoint(0.75f, 0.75f),
                CurveControlPoint(1f, 1f)
            )
        }
        val nextAdj = _uiState.value.photoAdjustments.copy(rgbCurvePoints = points)
        applyPhotoAdjustmentsToLayer(nextAdj)
    }

    fun updateRetouchAndBgState(newRetouch: RetouchAndBgState) {
        val state = _uiState.value
        _uiState.update { it.copy(retouchState = newRetouch) }
        val (updatedDoc, msg) = PhotoAndVideoEngine.applyRetouchAndBgOperation(
            doc = state.document,
            selectedLayer = state.selectedLayer,
            retouch = newRetouch
        )
        commitDocumentChange(updatedDoc, toast = msg)
    }

    fun applyLutAndEffects(newEffects: FilterAndEffectsState) {
        val state = _uiState.value
        val target = state.selectedLayer
        _uiState.update { it.copy(filterEffectsState = newEffects) }
        if (target != null) {
            val updated = PhotoAndVideoEngine.applyFilterAndEffectsToLayer(target, newEffects)
            updateLayer(
                layerId = target.id,
                toast = "Filtro LUT ${newEffects.activeLut.labelIt} applicato"
            ) { updated }
        }
    }

    fun generateCollageOnCanvas(collageConfig: CollageComposerState = _uiState.value.collageState) {
        val state = _uiState.value
        val updatedDoc = PhotoAndVideoEngine.generateCollageOnDocument(
            doc = state.document,
            collage = collageConfig
        )
        _uiState.update { it.copy(collageState = collageConfig) }
        commitDocumentChange(
            updatedDoc,
            newSelectedLayerId = updatedDoc.layers.firstOrNull()?.id,
            toast = "Collage ${collageConfig.cellCount} celle (${collageConfig.shapeMask.labelIt}) generato"
        )
    }

    fun insertLocalStickerOnCanvas(sticker: LocalStickerAsset) {
        val state = _uiState.value
        val doc = state.document
        val layer = GraphiteLayer(
            id = "sticker-${sticker.id}-${UUID.randomUUID().toString().take(4)}",
            name = "Sticker: ${sticker.titleIt}",
            kind = sticker.kind,
            x = doc.artboardWidth / 2f,
            y = doc.artboardHeight / 2f,
            width = 210f,
            height = 210f,
            polygonSides = sticker.polygonSides,
            cornerRadius = 24f,
            fillColorHex = sticker.colorHex,
            strokeColorHex = 0xFFFFFFFFL,
            strokeWidth = 4f
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + layer),
            newSelectedLayerId = layer.id,
            toast = "Sticker '${sticker.titleIt}' aggiunto alla tavola"
        )
    }

    fun applyRasterBrushPreset(
        preset: RasterBrushPreset,
        sizePx: Float = preset.defaultSizePx,
        hardness: Float = preset.defaultHardness,
        opacity: Float = preset.defaultOpacity
    ) {
        _uiState.update {
            it.copy(
                activeBrushPreset = preset,
                brushSizePx = sizePx.coerceIn(2f, 120f),
                brushHardness = hardness.coerceIn(0.05f, 1f),
                brushOpacity = opacity.coerceIn(0.05f, 1f),
                defaultStrokeWidth = sizePx.coerceIn(2f, 120f),
                activeTool = if (preset == RasterBrushPreset.BUCKET_FILL) ToolType.FILL else ToolType.BRUSH,
                statusToast = "Pennello ${preset.labelIt} attivo (${sizePx.toInt()}px)"
            )
        }
    }

    fun updateVideoTimeline(newTimeline: VideoTimelineState) {
        val summary = PhotoAndVideoEngine.buildMp4ExportSummary(newTimeline)
        _uiState.update {
            it.copy(
                videoTimeline = newTimeline,
                lastVideoExportSummary = summary,
                statusToast = "Timeline Video aggiornata"
            )
        }
    }

    fun splitSelectedVideoClip() {
        val tl = _uiState.value.videoTimeline
        val first = tl.clips.firstOrNull() ?: return
        val midSec = (first.trimStartSec + first.trimEndSec) / 2f
        if (midSec <= first.trimStartSec + 0.2f) return
        val clipA = first.copy(trimEndSec = midSec, title = "${first.title} (Parte A)")
        val clipB = first.copy(
            id = "clip-${UUID.randomUUID().toString().take(5)}",
            trimStartSec = midSec,
            title = "${first.title} (Parte B)"
        )
        val updatedClips = listOf(clipA, clipB) + tl.clips.drop(1)
        updateVideoTimeline(tl.copy(clips = updatedClips))
    }

    fun exportLocalVideoMp4() {
        val summary = PhotoAndVideoEngine.buildMp4ExportSummary(_uiState.value.videoTimeline)
        _uiState.update {
            it.copy(
                lastVideoExportSummary = summary,
                statusToast = summary
            )
        }
    }

    // =========================================================================
    // PHASE 2: CANVA ENGINE (TEMPLATES, BRAND KIT, PRESENTATIONS, SOCIAL)
    // =========================================================================

    fun applyCanvaTemplate(template: LocalCanvaTemplate) {
        val doc = PhotoAndVideoEngine.applyCanvaTemplateToDocument(template)
        commitDocumentChange(
            doc,
            newSelectedLayerId = doc.layers.firstOrNull()?.id,
            toast = "Template '${template.titleIt}' (${doc.artboardWidth}×${doc.artboardHeight}) caricato"
        )
    }

    fun applyLocalBrandKitToDocument(kit: LocalBrandKit = _uiState.value.brandKit) {
        val doc = _uiState.value.document
        val updatedDoc = PhotoAndVideoEngine.applyBrandKitToDocument(doc, kit)
        _uiState.update { it.copy(brandKit = kit) }
        commitDocumentChange(
            updatedDoc,
            toast = "Brand Kit '${kit.brandName}' applicato con 1-Tap a tutti i livelli"
        )
    }

    fun toggleBrandStyleLock() {
        _uiState.update { state ->
            val nextKit = state.brandKit.copy(isStyleLocked = !state.brandKit.isStyleLocked)
            state.copy(
                brandKit = nextKit,
                statusToast = if (nextKit.isStyleLocked) "Stili Brand bloccati contro modifiche accidentali" else "Stili Brand sbloccati"
            )
        }
    }

    fun applyMagicResize(targetWidth: Int, targetHeight: Int, formatLabel: String) {
        val resized = PhotoAndVideoEngine.magicResizeDocument(
            doc = _uiState.value.document,
            targetW = targetWidth,
            targetH = targetHeight,
            formatLabel = formatLabel
        )
        commitDocumentChange(
            resized,
            toast = "Magic Resize completato: $formatLabel (${targetWidth}×${targetHeight}px)"
        )
    }

    fun addPresentationSlide(title: String, speakerNotes: String) {
        val nextIndex = _uiState.value.presentationSlides.size + 1
        val slide = PresentationSlide(
            id = "slide-$nextIndex-${UUID.randomUUID().toString().take(4)}",
            title = title.ifBlank { "Slide $nextIndex" },
            subtitle = "Sezione #$nextIndex",
            speakerNotes = speakerNotes.ifBlank { "Note del relatore per la slide $nextIndex." },
            transition = VideoTransitionStyle.SLIDE_LEFT,
            entranceAnim = SlideEntranceAnimation.SLIDE_UP
        )
        _uiState.update {
            it.copy(
                presentationSlides = it.presentationSlides + slide,
                activeSlideIndex = it.presentationSlides.size,
                statusToast = "Slide '${slide.title}' aggiunta allo Storyboard"
            )
        }
    }

    fun selectPresentationSlide(index: Int) {
        _uiState.update { state ->
            val safeIdx = index.coerceIn(0, max(0, state.presentationSlides.lastIndex))
            state.copy(activeSlideIndex = safeIdx)
        }
    }

    fun togglePresentationPlayer(play: Boolean? = null) {
        _uiState.update { state ->
            val next = play ?: !state.isPlayingPresentation
            state.copy(
                isPlayingPresentation = next,
                showCreativeSuiteSheet = if (next) false else state.showCreativeSuiteSheet,
                statusToast = if (next) "Modalità Presentazione Fullscreen avviata" else "Modalità Presentazione terminata"
            )
        }
    }

    fun scheduleLocalSocialPost(title: String, channel: String, dateText: String, categoryTone: String, includeWatermark: Boolean) {
        val projectTitle = title.ifBlank { _uiState.value.document.name }
        val caption = PhotoAndVideoEngine.generateDeterministicCaption(categoryTone, projectTitle)
        val item = SocialPostScheduleItem(
            id = "post-${UUID.randomUUID().toString().take(6)}",
            title = projectTitle,
            channel = channel,
            dateTimeLabel = dateText.ifBlank { "2026-06-01 18:00" },
            captionText = caption,
            formatLabel = "${_uiState.value.document.artboardWidth}×${_uiState.value.document.artboardHeight}",
            watermarkEnabled = includeWatermark
        )
        _uiState.update {
            it.copy(
                scheduledPosts = listOf(item) + it.scheduledPosts,
                statusToast = "Post pianificato nel calendario offline per $channel"
            )
        }
        viewModelScope.launch {
            repository.saveScheduledPost(item)
        }
    }

    // =========================================================================
    // PHASE 3: FIGMA & FIGJAM ENGINE (AUTO LAYOUT, COMPONENTS, PROTOTYPE, DIFF)
    // =========================================================================

    fun applyDeviceFramePreset(preset: DeviceFramePreset) {
        val resized = PhotoAndVideoEngine.magicResizeDocument(
            doc = _uiState.value.document,
            targetW = preset.widthPx,
            targetH = preset.heightPx,
            formatLabel = preset.labelIt
        )
        commitDocumentChange(
            resized,
            toast = "Frame Dispositivo ${preset.labelIt} (${preset.widthPx}×${preset.heightPx}) applicato"
        )
    }

    fun applyFigmaAutoLayout(config: FigmaAutoLayoutState) {
        val updatedDoc = PhotoAndVideoEngine.applyFigmaAutoLayout(
            doc = _uiState.value.document,
            config = config
        )
        _uiState.update { it.copy(autoLayoutState = config) }
        commitDocumentChange(
            updatedDoc,
            toast = "Figma Auto Layout (${config.direction.labelIt}, gap ${config.spacingPx.toInt()}px) applicato"
        )
    }

    fun instantiateFigmaComponent(master: FigmaComponentMaster, variant: ComponentVariantState = master.activeVariant) {
        val doc = _uiState.value.document
        val fill = when (variant) {
            ComponentVariantState.DEFAULT -> master.overrideFillHex ?: 0xFF65BBE5L
            ComponentVariantState.HOVER -> 0xFF38BDF8L
            ComponentVariantState.PRESSED -> 0xFF0284C7L
            ComponentVariantState.DISABLED -> 0xFF475569L
        }
        val newLayer = GraphiteLayer(
            id = "inst-${UUID.randomUUID().toString().take(6)}",
            name = "${master.name} [${variant.labelIt}]",
            kind = LayerGeometryKind.RECTANGLE,
            x = doc.artboardWidth / 2f,
            y = doc.artboardHeight / 2f,
            width = 280f * variant.scaleMod,
            height = 76f * variant.scaleMod,
            cornerRadius = 18f,
            opacity = variant.alphaMod,
            fillColorHex = fill,
            strokeColorHex = 0xFFFFFFFFL,
            strokeWidth = 2f,
            textContent = "${master.overrideLabel.ifBlank { master.name }} (${variant.labelIt})"
        )
        commitDocumentChange(
            doc.copy(layers = doc.layers + newLayer),
            newSelectedLayerId = newLayer.id,
            toast = "Istanza '${master.name}' (${variant.labelIt}) inserita"
        )
    }

    fun addPrototypeLink(targetFrameName: String, trigger: PrototypeTriggerType, transition: PrototypeTransitionType) {
        val sel = _uiState.value.selectedLayer ?: _uiState.value.document.layers.firstOrNull()
        val sourceId = sel?.id ?: "root"
        val sourceName = sel?.name ?: "Tavola Principale"
        val link = PrototypeLink(
            id = "proto-${UUID.randomUUID().toString().take(5)}",
            sourceLayerId = sourceId,
            sourceLayerName = sourceName,
            targetSlideTitle = targetFrameName.ifBlank { "Schermata Checkout" },
            trigger = trigger,
            transition = transition
        )
        _uiState.update {
            it.copy(
                prototypeLinks = it.prototypeLinks + link,
                statusToast = "Connessione Prototipo aggiunta (${trigger.labelIt} → ${link.targetSlideTitle})"
            )
        }
    }

    fun togglePrototypePlayer(play: Boolean? = null) {
        _uiState.update { state ->
            val next = play ?: !state.isPlayingPrototype
            state.copy(
                isPlayingPrototype = next,
                showCreativeSuiteSheet = if (next) false else state.showCreativeSuiteSheet,
                statusToast = if (next) "Player Interattivo Figma avviato" else "Player Prototipo chiuso"
            )
        }
    }

    fun triggerPrototypeNavigation(targetScreen: String) {
        _uiState.update {
            it.copy(
                activePrototypeScreenName = targetScreen,
                statusToast = "Smart Animate → $targetScreen"
            )
        }
    }

    fun addOfflineComment(authorName: String, message: String) {
        if (message.isBlank()) return
        val sel = _uiState.value.selectedLayer
        val anchorX = sel?.x ?: (_uiState.value.document.artboardWidth / 2f)
        val anchorY = sel?.y ?: (_uiState.value.document.artboardHeight / 2f)
        val item = OfflineCommentThread(
            id = "comm-${UUID.randomUUID().toString().take(6)}",
            author = authorName.ifBlank { "Designer Locale" },
            message = message.trim(),
            artboardX = anchorX,
            artboardY = anchorY,
            createdAtLabel = "Adesso",
            isResolved = false
        )
        _uiState.update {
            it.copy(
                offlineComments = listOf(item) + it.offlineComments,
                statusToast = "Commento ancorato a (${anchorX.toInt()}, ${anchorY.toInt()})"
            )
        }
        viewModelScope.launch {
            repository.saveOfflineComment(
                documentId = _uiState.value.document.id,
                author = item.author,
                message = item.message,
                artboardX = item.artboardX,
                artboardY = item.artboardY,
                createdAtLabel = item.createdAtLabel,
                id = item.id,
                isResolved = item.isResolved
            )
        }
    }

    fun toggleCommentResolved(commentId: String) {
        _uiState.update { state ->
            val updated = state.offlineComments.map {
                if (it.id == commentId) it.copy(isResolved = !it.isResolved) else it
            }
            state.copy(offlineComments = updated)
        }
    }

    fun saveVersionSnapshot(versionLabel: String, author: String = "Locale") {
        val doc = _uiState.value.document
        val totalNodes = doc.layers.sumOf { it.nodes.size }
        val prevDoc = DemoArtworkFactory.createIsometricFountain()
        val diffReport = "Visual Diff: " + PhotoAndVideoEngine.computeVisualDiffBetweenSnapshots(
            olderDoc = prevDoc,
            currentDoc = doc
        )
        val tag = "v1.${_uiState.value.versionSnapshots.size + 1}"
        val snap = VersionSnapshotItem(
            id = "snap-${UUID.randomUUID().toString().take(6)}",
            versionTag = tag,
            titleIt = versionLabel.ifBlank { "$tag — Snapshot" },
            author = author,
            timestampLabel = "Adesso",
            layerCount = doc.layers.size,
            nodeCount = totalNodes,
            diffSummaryIt = diffReport,
            serializedJson = ProceduralEngine.toGraphiteJson(doc)
        )
        _uiState.update {
            it.copy(
                versionSnapshots = listOf(snap) + it.versionSnapshots,
                activeDiffReport = diffReport,
                statusToast = "Versione '${snap.titleIt}' salvata nella cronologia locale"
            )
        }
        viewModelScope.launch {
            repository.saveVersionSnapshot(
                doc = doc,
                versionTag = snap.versionTag,
                titleIt = snap.titleIt,
                author = snap.author,
                diffSummaryIt = snap.diffSummaryIt
            )
        }
    }

    fun addFigJamStickyNote(text: String, colorHex: Long = 0xFFFEF08AL) {
        if (text.isBlank()) return
        val doc = _uiState.value.document
        val sticky = FigJamStickyItem(
            id = "sticky-${UUID.randomUUID().toString().take(5)}",
            text = text.trim(),
            author = "Team Locale",
            colorHex = colorHex,
            x = doc.artboardWidth / 2f,
            y = doc.artboardHeight / 2f,
            votes = 1
        )
        val layer = GraphiteLayer(
            id = sticky.id,
            name = "Sticky: ${sticky.text.take(18)}",
            kind = LayerGeometryKind.RECTANGLE,
            x = sticky.x,
            y = sticky.y,
            width = 240f,
            height = 200f,
            cornerRadius = 18f,
            fillColorHex = colorHex,
            strokeColorHex = 0xFF0F172AL,
            strokeWidth = 2f,
            textContent = sticky.text
        )
        _uiState.update {
            it.copy(figJamStickies = listOf(sticky) + it.figJamStickies)
        }
        commitDocumentChange(
            doc.copy(layers = doc.layers + layer),
            newSelectedLayerId = layer.id,
            toast = "Nota adesiva FigJam aggiunta alla lavagna"
        )
    }

    fun voteFigJamSticky(stickyId: String) {
        _uiState.update { state ->
            val updated = state.figJamStickies.map {
                if (it.id == stickyId) it.copy(votes = it.votes + 1) else it
            }
            state.copy(
                figJamStickies = updated,
                statusToast = "Voto aggiunto alla nota FigJam!"
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
