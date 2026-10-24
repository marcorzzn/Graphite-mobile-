package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.DataArtboard
import com.example.ui.theme.DataColor
import com.example.ui.theme.DataGeneral
import com.example.ui.theme.DataGraphic
import com.example.ui.theme.DataNumber
import com.example.ui.theme.DataRaster
import com.example.ui.theme.DataVector

enum class ToolGroup(val labelIt: String) {
    GENERAL("Generale"),
    VECTOR("Vettoriale"),
    RASTER("Raster")
}

enum class ToolType(
    val labelIt: String,
    val labelEn: String,
    val group: ToolGroup,
    val hintIt: String
) {
    // General
    SELECT("Seleziona", "Select", ToolGroup.GENERAL, "Tocca un livello per selezionarlo o trascina le maniglie per trasformarlo"),
    ARTBOARD("Tavola", "Artboard", ToolGroup.GENERAL, "Trascina o ridimensiona l'area della tavola da disegno"),
    NAVIGATE("Naviga", "Navigate", ToolGroup.GENERAL, "Trascina con 1 o 2 dita per spostare e zoomare la vista"),
    EYEDROPPER("Contagocce", "Eyedropper", ToolGroup.GENERAL, "Tocca qualsiasi elemento sulla tela per campionare il colore primario"),
    FILL("Riempimento", "Fill", ToolGroup.GENERAL, "Tocca un livello per applicare il colore primario o secondario"),
    GRADIENT("Sfumatura", "Gradient", ToolGroup.GENERAL, "Trascina su un livello per applicare una sfumatura lineare o radiale"),

    // Vector
    PATH("Nodi Tracciato", "Path", ToolGroup.VECTOR, "Tocca e trascina i vertici/ancore del tracciato selezionato"),
    PEN("Penna Bézier", "Pen", ToolGroup.VECTOR, "Tocca sulla tela per aggiungere punti al tracciato vettoriale"),
    FREEHAND("Mano Libera", "Freehand", ToolGroup.VECTOR, "Disegna a mano libera un tracciato vettoriale continuo"),
    SPLINE("Spline", "Spline", ToolGroup.VECTOR, "Tocca per inserire punti di una curva spline morbida"),
    LINE("Linea", "Line", ToolGroup.VECTOR, "Trascina sulla tela per tracciare un segmento vettoriale"),
    RECTANGLE("Rettangolo", "Rectangle", ToolGroup.VECTOR, "Trascina sulla tela per creare un rettangolo procedurale"),
    ELLIPSE("Ellisse", "Ellipse", ToolGroup.VECTOR, "Trascina sulla tela per creare un'ellisse o cerchio procedurale"),
    POLYGON("Poligono", "Polygon", ToolGroup.VECTOR, "Trascina sulla tela per creare un poligono regolare parametrico"),
    STAR("Stella", "Star", ToolGroup.VECTOR, "Trascina sulla tela per creare una stella parametrica"),
    TEXT("Testo", "Text", ToolGroup.VECTOR, "Tocca sulla tela per inserire un livello di testo vettoriale"),

    // Raster
    BRUSH("Pennello", "Brush", ToolGroup.RASTER, "Dipingi tratti morbidi con controllo di dimensione e durezza")
}

enum class NodeDataType(val label: String, val color: Color) {
    VECTOR("Vector", DataVector),
    GRAPHIC("Graphic", DataGraphic),
    RASTER("Raster", DataRaster),
    NUMBER("Number", DataNumber),
    COLOR("Color", DataColor),
    ARTBOARD("Artboard", DataArtboard),
    GENERAL("Any", DataGeneral)
}

enum class NodeCategory(val labelIt: String, val dataType: NodeDataType) {
    VECTOR_MODIFIER("Modificatori Vettoriali", NodeDataType.VECTOR),
    REPEAT_SYMMETRY("Ripetizione & Simmetria", NodeDataType.GRAPHIC),
    APPEARANCE("Stile & Aspetto", NodeDataType.COLOR),
    RASTER_ADJUST("Filtri & Regolazioni", NodeDataType.RASTER)
}

data class NodeParamSpec(
    val key: String,
    val labelIt: String,
    val min: Float,
    val max: Float,
    val defaultVal: Float,
    val unit: String = ""
)

enum class ProceduralNodeType(
    val titleIt: String,
    val titleEn: String,
    val category: NodeCategory,
    val descriptionIt: String,
    val inputType: NodeDataType,
    val outputType: NodeDataType,
    val paramSpecs: List<NodeParamSpec>
) {
    TRANSFORM(
        titleIt = "Trasforma (Transform)",
        titleEn = "Transform",
        category = NodeCategory.VECTOR_MODIFIER,
        descriptionIt = "Trasla, scala e ruota la geometria in modo non distruttivo.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.VECTOR,
        paramSpecs = listOf(
            NodeParamSpec("offsetX", "Trasla X", -400f, 400f, 0f, "px"),
            NodeParamSpec("offsetY", "Trasla Y", -400f, 400f, 0f, "px"),
            NodeParamSpec("scaleX", "Scala X", 0.1f, 3.0f, 1.0f, "×"),
            NodeParamSpec("scaleY", "Scala Y", 0.1f, 3.0f, 1.0f, "×"),
            NodeParamSpec("rotate", "Rotazione", -180f, 180f, 0f, "°")
        )
    ),
    OFFSET_PATH(
        titleIt = "Espandi Tracciato (Offset Path)",
        titleEn = "Offset Path",
        category = NodeCategory.VECTOR_MODIFIER,
        descriptionIt = "Espande o contrae il contorno vettoriale e arrotonda gli angoli.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.VECTOR,
        paramSpecs = listOf(
            NodeParamSpec("offset", "Offset Contorno", -80f, 120f, 16f, "px"),
            NodeParamSpec("roundness", "Arrotondamento", 0f, 60f, 12f, "px")
        )
    ),
    JITTER_WAVE(
        titleIt = "Distorsione Onda / Jitter",
        titleEn = "Jitter & Wave",
        category = NodeCategory.VECTOR_MODIFIER,
        descriptionIt = "Applica una deformazione sinusoidale o rumore procedurale ai vertici.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.VECTOR,
        paramSpecs = listOf(
            NodeParamSpec("amplitude", "Ampiezza", 0f, 80f, 15f, "px"),
            NodeParamSpec("frequency", "Frequenza", 1f, 20f, 4f, "Hz"),
            NodeParamSpec("phase", "Fase", 0f, 360f, 0f, "°")
        )
    ),
    MIRROR_SYMMETRY(
        titleIt = "Simmetria Speculare (Mirror)",
        titleEn = "Mirror",
        category = NodeCategory.REPEAT_SYMMETRY,
        descriptionIt = "Riflette l'elemento lungo l'asse X, Y o entrambi gli assi.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.GRAPHIC,
        paramSpecs = listOf(
            NodeParamSpec("axisMode", "Asse (0=X, 1=Y, 2=XY)", 0f, 2f, 0f, ""),
            NodeParamSpec("gap", "Distanza dal Centro", -200f, 300f, 60f, "px")
        )
    ),
    RADIAL_REPEAT(
        titleIt = "Ripetizione Radiale (Circular Array)",
        titleEn = "Circular Repeat",
        category = NodeCategory.REPEAT_SYMMETRY,
        descriptionIt = "Clona il livello in una raggiera circolare parametrica.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.GRAPHIC,
        paramSpecs = listOf(
            NodeParamSpec("count", "Copie", 2f, 24f, 6f, "#"),
            NodeParamSpec("radius", "Raggio Raggiera", 0f, 320f, 90f, "px"),
            NodeParamSpec("arcDeg", "Arco Totale", 45f, 360f, 360f, "°"),
            NodeParamSpec("spinCopies", "Ruota Copie (0/1)", 0f, 1f, 1f, "")
        )
    ),
    GRID_ARRAY(
        titleIt = "Griglia Procedurale (Grid Repeat)",
        titleEn = "Grid Repeat",
        category = NodeCategory.REPEAT_SYMMETRY,
        descriptionIt = "Ripete l'elemento in una matrice di righe e colonne.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.GRAPHIC,
        paramSpecs = listOf(
            NodeParamSpec("columns", "Colonne", 1f, 8f, 3f, "#"),
            NodeParamSpec("rows", "Righe", 1f, 8f, 2f, "#"),
            NodeParamSpec("spacingX", "Passo Orizzontale", 20f, 350f, 110f, "px"),
            NodeParamSpec("spacingY", "Passo Verticale", 20f, 350f, 110f, "px"),
            NodeParamSpec("scaleStep", "Scala Progressiva", 0.5f, 1.3f, 1.0f, "×")
        )
    ),
    DROP_SHADOW(
        titleIt = "Ombra & Bagliore (Drop Shadow)",
        titleEn = "Drop Shadow",
        category = NodeCategory.APPEARANCE,
        descriptionIt = "Genera un'ombra proiettata o un bagliore neon procedurale.",
        inputType = NodeDataType.GRAPHIC,
        outputType = NodeDataType.GRAPHIC,
        paramSpecs = listOf(
            NodeParamSpec("offsetX", "Offset X", -80f, 80f, 10f, "px"),
            NodeParamSpec("offsetY", "Offset Y", -80f, 80f, 14f, "px"),
            NodeParamSpec("spread", "Espansione", 0f, 60f, 12f, "px"),
            NodeParamSpec("alpha", "Opacità Ombra", 0.05f, 1.0f, 0.55f, "")
        )
    ),
    COLOR_ADJUST(
        titleIt = "Tonalità / Luminosità (HSL Adjust)",
        titleEn = "HSL Adjust",
        category = NodeCategory.RASTER_ADJUST,
        descriptionIt = "Modifica tonalità, saturazione e luminosità dei colori generati.",
        inputType = NodeDataType.COLOR,
        outputType = NodeDataType.COLOR,
        paramSpecs = listOf(
            NodeParamSpec("hueShift", "Rotazione Tonalità", -180f, 180f, 35f, "°"),
            NodeParamSpec("saturation", "Saturazione", 0f, 2f, 1.15f, "×"),
            NodeParamSpec("lightness", "Luminosità", -0.5f, 0.5f, 0.0f, "")
        )
    ),
    STROKE_DASH(
        titleIt = "Tratteggio Procedurale (Dash)",
        titleEn = "Stroke Dash",
        category = NodeCategory.APPEARANCE,
        descriptionIt = "Converte il contorno in un pattern tratteggiato parametrico.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.VECTOR,
        paramSpecs = listOf(
            NodeParamSpec("dashLen", "Lunghezza Tratto", 4f, 80f, 18f, "px"),
            NodeParamSpec("gapLen", "Spazio Vuoto", 4f, 80f, 10f, "px"),
            NodeParamSpec("phase", "Scorrimento Fase", 0f, 100f, 0f, "px")
        )
    ),
    BOOLEAN_OP(
        titleIt = "Operazione Booleana (Skia Path.Op)",
        titleEn = "Boolean Operation",
        category = NodeCategory.VECTOR_MODIFIER,
        descriptionIt = "Sottrae, unisce, interseca o esclude (XOR) una sagoma secondaria con algebra vettoriale Skia.",
        inputType = NodeDataType.VECTOR,
        outputType = NodeDataType.VECTOR,
        paramSpecs = listOf(
            NodeParamSpec("opMode", "Modo (0=Sottrai, 1=Unisci, 2=Interseca, 3=XOR)", 0f, 3f, 0f, ""),
            NodeParamSpec("cutterShape", "Sagoma (0=Cerchio, 1=Rett, 2=Esagono)", 0f, 2f, 0f, ""),
            NodeParamSpec("offsetX", "Offset Taglio X", -180f, 180f, 0f, "px"),
            NodeParamSpec("offsetY", "Offset Taglio Y", -180f, 180f, 0f, "px"),
            NodeParamSpec("scaleRatio", "Scala Sagoma", 0.2f, 1.4f, 0.55f, "×")
        )
    )
}

enum class NodeGraphViewMode(val labelIt: String) {
    STACK("Stack Mobile"),
    CANVAS_2D("Lavagna 2D DAG")
}

data class ProceduralNode(
    val id: String,
    val type: ProceduralNodeType,
    val enabled: Boolean = true,
    val collapsed: Boolean = false,
    val params: Map<String, Float> = type.paramSpecs.associate { it.key to it.defaultVal },
    val shadowColorHex: Long = 0xFF000000L,
    val graphX: Float = 0f,
    val graphY: Float = 0f
)

enum class BlendModeType(val labelIt: String) {
    NORMAL("Normale"),
    MULTIPLY("Moltiplica"),
    SCREEN("Scolora (Screen)"),
    OVERLAY("Sovrapponi"),
    DARKEN("Scurisci"),
    LIGHTEN("Schiarisci"),
    COLOR_DODGE("Colore Scherma"),
    DIFFERENCE("Differenza")
}

enum class LayerGeometryKind(val labelIt: String, val dataType: NodeDataType) {
    RECTANGLE("Rettangolo", NodeDataType.VECTOR),
    ELLIPSE("Ellisse", NodeDataType.VECTOR),
    POLYGON("Poligono", NodeDataType.VECTOR),
    STAR("Stella", NodeDataType.VECTOR),
    LINE("Linea", NodeDataType.VECTOR),
    BEZIER_PATH("Tracciato Penna", NodeDataType.VECTOR),
    FREEHAND("Mano Libera", NodeDataType.VECTOR),
    SPLINE("Curva Spline", NodeDataType.VECTOR),
    TEXT("Testo Vettoriale", NodeDataType.VECTOR),
    BRUSH_STROKE("Tratto Pennello", NodeDataType.RASTER)
}

data class Vec2Point(
    val x: Float,
    val y: Float,
    val handleInX: Float = x,
    val handleInY: Float = y,
    val handleOutX: Float = x,
    val handleOutY: Float = y,
    val pressure: Float = 1.0f
)

data class GraphiteLayer(
    val id: String,
    val name: String,
    val kind: LayerGeometryKind,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val x: Float = 200f,
    val y: Float = 200f,
    val width: Float = 260f,
    val height: Float = 200f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val blendMode: BlendModeType = BlendModeType.NORMAL,
    val hasFill: Boolean = true,
    val fillColorHex: Long = 0xFF65BBE5L,
    val useGradient: Boolean = false,
    val gradientEndColorHex: Long = 0xFF68C587L,
    val gradientAngle: Float = 45f,
    val isRadialGradient: Boolean = false,
    val strokeColorHex: Long = 0xFFEEEFF2L,
    val strokeWidth: Float = 4f,
    val cornerRadius: Float = 16f,
    val polygonSides: Int = 6,
    val starInnerRatio: Float = 0.45f,
    val textContent: String = "Graphite",
    val fontSize: Float = 48f,
    val brushHardness: Float = 0.7f,
    val points: List<Vec2Point> = emptyList(),
    val nodes: List<ProceduralNode> = emptyList()
)

data class GraphiteDocumentModel(
    val id: String,
    val name: String,
    val artboardWidth: Int = 1080,
    val artboardHeight: Int = 1080,
    val artboardBgHex: Long = 0xFF1A1D24L,
    val layers: List<GraphiteLayer> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val demoId: String? = null
)

data class DemoArtworkInfo(
    val id: String,
    val title: String,
    val subtitleIt: String,
    val filename: String,
    @DrawableRes val thumbnailRes: Int,
    val tags: List<String>,
    val nodeCountBadge: String
)

val OFFICIAL_DEMO_ARTWORKS = listOf(
    DemoArtworkInfo(
        id = "isometric-fountain",
        title = "Isometric Fountain",
        subtitleIt = "Architettura isometrica procedurale con simmetrie e array",
        filename = "isometric-fountain.graphite",
        thumbnailRes = R.drawable.thumb_isometric_fountain,
        tags = listOf("Isometrico", "Vettoriale", "Array"),
        nodeCountBadge = "14 Nodi"
    ),
    DemoArtworkInfo(
        id = "changing-seasons",
        title = "Changing Seasons",
        subtitleIt = "Albero frattale e variazioni cromatiche stagionali non distruttive",
        filename = "changing-seasons.graphite",
        thumbnailRes = R.drawable.thumb_changing_seasons,
        tags = listOf("Natura", "HSL Adjust", "Radiale"),
        nodeCountBadge = "12 Nodi"
    ),
    DemoArtworkInfo(
        id = "painted-dreams",
        title = "Painted Dreams",
        subtitleIt = "Composizione pittorica con tratti pennello, bagliori e livelli",
        filename = "painted-dreams.graphite",
        thumbnailRes = R.drawable.thumb_painted_dreams,
        tags = listOf("Raster & Vector", "Pennello", "Sfumature"),
        nodeCountBadge = "11 Nodi"
    ),
    DemoArtworkInfo(
        id = "parametric-dunescape",
        title = "Parametric Dunescape",
        subtitleIt = "Dune desertiche generate da curve d'onda e gradienti solari",
        filename = "parametric-dunescape.graphite",
        thumbnailRes = R.drawable.thumb_parametric_dunescape,
        tags = listOf("Parametrico", "Onde", "Sfumature"),
        nodeCountBadge = "10 Nodi"
    ),
    DemoArtworkInfo(
        id = "procedural-string-lights",
        title = "Procedural String Lights",
        subtitleIt = "Catene luminose lungo spline con bagliore e ripetizione",
        filename = "procedural-string-lights.graphite",
        thumbnailRes = R.drawable.thumb_procedural_string_lights,
        tags = listOf("Spline", "Glow", "Procedural"),
        nodeCountBadge = "13 Nodi"
    ),
    DemoArtworkInfo(
        id = "red-dress",
        title = "Red Dress",
        subtitleIt = "Illustrazione editoriale vettoriale con tracciati Bézier e ombre",
        filename = "red-dress.graphite",
        thumbnailRes = R.drawable.thumb_red_dress,
        tags = listOf("Illustrazione", "Bézier", "Ombre"),
        nodeCountBadge = "15 Nodi"
    ),
    DemoArtworkInfo(
        id = "valley-of-spires",
        title = "Valley of Spires",
        subtitleIt = "Paesaggio atmosferico stratificato con profondità e foschia",
        filename = "valley-of-spires.graphite",
        thumbnailRes = R.drawable.thumb_valley_of_spires,
        tags = listOf("Paesaggio", "Livelli", "Atmosfera"),
        nodeCountBadge = "12 Nodi"
    )
)
