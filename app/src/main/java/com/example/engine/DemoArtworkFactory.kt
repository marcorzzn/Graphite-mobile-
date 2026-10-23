package com.example.engine

import com.example.model.BlendModeType
import com.example.model.GraphiteDocumentModel
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.model.ProceduralNode
import com.example.model.ProceduralNodeType
import com.example.model.Vec2Point
import java.util.UUID

object DemoArtworkFactory {

    fun createDocumentForDemo(demoId: String): GraphiteDocumentModel {
        return when (demoId) {
            "isometric-fountain" -> createIsometricFountain()
            "changing-seasons" -> createChangingSeasons()
            "painted-dreams" -> createPaintedDreams()
            "parametric-dunescape" -> createParametricDunescape()
            "procedural-string-lights" -> createProceduralStringLights()
            "red-dress" -> createRedDress()
            "valley-of-spires" -> createValleyOfSpires()
            else -> createIsometricFountain()
        }
    }

    fun createIsometricFountain(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = "demo-isometric-fountain",
            name = "Isometric Fountain",
            artboardWidth = 1000,
            artboardHeight = 1000,
            artboardBgHex = 0xFF141E28L,
            demoId = "isometric-fountain",
            layers = listOf(
                GraphiteLayer(
                    id = "iso-plaza-grid",
                    name = "Pavimentazione Isometrica",
                    kind = LayerGeometryKind.POLYGON,
                    x = 340f,
                    y = 380f,
                    width = 140f,
                    height = 85f,
                    polygonSides = 4,
                    fillColorHex = 0xFF24384AL,
                    strokeColorHex = 0xFF417892L,
                    strokeWidth = 2.5f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-iso-grid",
                            type = ProceduralNodeType.GRID_ARRAY,
                            params = mapOf(
                                "columns" to 3f,
                                "rows" to 3f,
                                "spacingX" to 150f,
                                "spacingY" to 90f,
                                "scaleStep" to 1.0f
                            )
                        )
                    )
                ),
                GraphiteLayer(
                    id = "iso-basin-outer",
                    name = "Vasca Ottagonale Esterna",
                    kind = LayerGeometryKind.POLYGON,
                    x = 500f,
                    y = 560f,
                    width = 580f,
                    height = 360f,
                    polygonSides = 8,
                    fillColorHex = 0xFF2E4A62L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF1B2C3BL,
                    gradientAngle = 90f,
                    strokeColorHex = 0xFF8ABEDBL,
                    strokeWidth = 5f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-basin-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 0f, "offsetY" to 24f, "spread" to 18f, "alpha" to 0.65f)
                        ),
                        ProceduralNode(
                            id = "node-basin-offset",
                            type = ProceduralNodeType.OFFSET_PATH,
                            params = mapOf("offset" to 10f, "roundness" to 16f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "iso-water-pool",
                    name = "Acqua Cristallina & Onde",
                    kind = LayerGeometryKind.ELLIPSE,
                    x = 500f,
                    y = 555f,
                    width = 480f,
                    height = 285f,
                    fillColorHex = 0xFF39A9DBL,
                    useGradient = true,
                    gradientEndColorHex = 0xFF1C6E9CL,
                    isRadialGradient = true,
                    strokeColorHex = 0xFFA5E6FFL,
                    strokeWidth = 3f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-water-wave",
                            type = ProceduralNodeType.JITTER_WAVE,
                            params = mapOf("amplitude" to 6f, "frequency" to 8f, "phase" to 35f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "iso-pillars",
                    name = "Colonne Simmetriche",
                    kind = LayerGeometryKind.RECTANGLE,
                    x = 500f,
                    y = 445f,
                    width = 56f,
                    height = 200f,
                    cornerRadius = 12f,
                    fillColorHex = 0xFFE6DFD3L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF9EA8B3L,
                    gradientAngle = 0f,
                    strokeColorHex = 0xFFFBF9EBL,
                    strokeWidth = 2f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-pillar-mirror",
                            type = ProceduralNodeType.MIRROR_SYMMETRY,
                            params = mapOf("axisMode" to 0f, "gap" to 135f)
                        ),
                        ProceduralNode(
                            id = "node-pillar-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 8f, "offsetY" to 12f, "spread" to 10f, "alpha" to 0.45f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "iso-water-jets",
                    name = "Zampilli d'Acqua Radiali",
                    kind = LayerGeometryKind.ELLIPSE,
                    x = 500f,
                    y = 390f,
                    width = 48f,
                    height = 110f,
                    fillColorHex = 0xFF8BE8FFL,
                    useGradient = true,
                    gradientEndColorHex = 0xFF65BBE5L,
                    opacity = 0.88f,
                    blendMode = BlendModeType.SCREEN,
                    strokeColorHex = 0xFFFFFFFFL,
                    strokeWidth = 2f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-jets-radial",
                            type = ProceduralNodeType.RADIAL_REPEAT,
                            params = mapOf("count" to 8f, "radius" to 95f, "arcDeg" to 360f, "spinCopies" to 1f)
                        ),
                        ProceduralNode(
                            id = "node-jets-dash",
                            type = ProceduralNodeType.STROKE_DASH,
                            params = mapOf("dashLen" to 14f, "gapLen" to 8f, "phase" to 0f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "iso-crown-star",
                    name = "Rosone Centrale Fontana",
                    kind = LayerGeometryKind.STAR,
                    x = 500f,
                    y = 370f,
                    width = 150f,
                    height = 150f,
                    polygonSides = 8,
                    starInnerRatio = 0.55f,
                    fillColorHex = 0xFFFBF9EBL,
                    useGradient = true,
                    gradientEndColorHex = 0xFFE4BB72L,
                    isRadialGradient = true,
                    strokeColorHex = 0xFF65BBE5L,
                    strokeWidth = 3.5f
                )
            )
        )
    }

    fun createChangingSeasons(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = "demo-changing-seasons",
            name = "Changing Seasons",
            artboardWidth = 1000,
            artboardHeight = 1000,
            artboardBgHex = 0xFF1B1924L,
            demoId = "changing-seasons",
            layers = listOf(
                GraphiteLayer(
                    id = "season-sun-halo",
                    name = "Disco Solare Equinox",
                    kind = LayerGeometryKind.ELLIPSE,
                    x = 500f,
                    y = 460f,
                    width = 620f,
                    height = 620f,
                    fillColorHex = 0xFFE4BB72L,
                    useGradient = true,
                    gradientEndColorHex = 0xFFD6536EL,
                    isRadialGradient = true,
                    opacity = 0.45f,
                    strokeWidth = 0f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-sun-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 0f, "offsetY" to 0f, "spread" to 40f, "alpha" to 0.5f),
                            shadowColorHex = 0xFFE4BB72L
                        )
                    )
                ),
                GraphiteLayer(
                    id = "season-foliage-autumn",
                    name = "Chioma Autunnale Radiale",
                    kind = LayerGeometryKind.POLYGON,
                    x = 500f,
                    y = 430f,
                    width = 135f,
                    height = 135f,
                    polygonSides = 6,
                    fillColorHex = 0xFFE07A47L,
                    useGradient = true,
                    gradientEndColorHex = 0xFFD5AA43L,
                    strokeColorHex = 0xFFFBF9EBL,
                    strokeWidth = 2f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-autumn-radial",
                            type = ProceduralNodeType.RADIAL_REPEAT,
                            params = mapOf("count" to 10f, "radius" to 175f, "arcDeg" to 360f, "spinCopies" to 1f)
                        ),
                        ProceduralNode(
                            id = "node-autumn-hsl",
                            type = ProceduralNodeType.COLOR_ADJUST,
                            params = mapOf("hueShift" to 12f, "saturation" to 1.2f, "lightness" to 0.04f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "season-foliage-spring",
                    name = "Fioritura Primaverile",
                    kind = LayerGeometryKind.STAR,
                    x = 500f,
                    y = 430f,
                    width = 96f,
                    height = 96f,
                    polygonSides = 5,
                    starInnerRatio = 0.52f,
                    fillColorHex = 0xFF68C587L,
                    useGradient = true,
                    gradientEndColorHex = 0xFFCE6EA7L,
                    strokeColorHex = 0xFFFFFFFFL,
                    strokeWidth = 1.5f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-spring-radial",
                            type = ProceduralNodeType.RADIAL_REPEAT,
                            params = mapOf("count" to 8f, "radius" to 95f, "arcDeg" to 360f, "spinCopies" to 1f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "season-trunk",
                    name = "Tronco & Rami Simmetrici",
                    kind = LayerGeometryKind.BEZIER_PATH,
                    x = 500f,
                    y = 680f,
                    width = 220f,
                    height = 420f,
                    hasFill = false,
                    strokeColorHex = 0xFFC9A699L,
                    strokeWidth = 18f,
                    points = listOf(
                        Vec2Point(500f, 890f),
                        Vec2Point(500f, 670f),
                        Vec2Point(420f, 520f),
                        Vec2Point(350f, 430f)
                    ),
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-trunk-mirror",
                            type = ProceduralNodeType.MIRROR_SYMMETRY,
                            params = mapOf("axisMode" to 0f, "gap" to 0f)
                        )
                    )
                )
            )
        )
    }

    fun createPaintedDreams(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = "demo-painted-dreams",
            name = "Painted Dreams",
            artboardWidth = 1000,
            artboardHeight = 1000,
            artboardBgHex = 0xFF171326L,
            demoId = "painted-dreams",
            layers = listOf(
                GraphiteLayer(
                    id = "dream-nebula",
                    name = "Nebulosa Pittorica",
                    kind = LayerGeometryKind.ELLIPSE,
                    x = 500f,
                    y = 480f,
                    width = 680f,
                    height = 460f,
                    rotation = -18f,
                    fillColorHex = 0xFFCE6EA7L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF417892L,
                    isRadialGradient = true,
                    opacity = 0.75f,
                    strokeColorHex = 0xFFE4BB72L,
                    strokeWidth = 3f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-nebula-wave",
                            type = ProceduralNodeType.JITTER_WAVE,
                            params = mapOf("amplitude" to 24f, "frequency" to 6f, "phase" to 90f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "dream-brush-ribbons",
                    name = "Tratti Pennello Fluido",
                    kind = LayerGeometryKind.SPLINE,
                    x = 500f,
                    y = 500f,
                    width = 700f,
                    height = 400f,
                    hasFill = false,
                    strokeColorHex = 0xFF65BBE5L,
                    strokeWidth = 22f,
                    points = listOf(
                        Vec2Point(160f, 620f),
                        Vec2Point(340f, 360f),
                        Vec2Point(560f, 640f),
                        Vec2Point(760f, 340f),
                        Vec2Point(880f, 480f)
                    ),
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-ribbon-mirror",
                            type = ProceduralNodeType.MIRROR_SYMMETRY,
                            params = mapOf("axisMode" to 1f, "gap" to 45f)
                        ),
                        ProceduralNode(
                            id = "node-ribbon-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 0f, "offsetY" to 14f, "spread" to 22f, "alpha" to 0.6f),
                            shadowColorHex = 0xFFCE6EA7L
                        )
                    )
                ),
                GraphiteLayer(
                    id = "dream-stars",
                    name = "Costellazione Onirica",
                    kind = LayerGeometryKind.STAR,
                    x = 500f,
                    y = 480f,
                    width = 68f,
                    height = 68f,
                    polygonSides = 4,
                    starInnerRatio = 0.3f,
                    fillColorHex = 0xFFFBF9EBL,
                    strokeColorHex = 0xFFE4BB72L,
                    strokeWidth = 2f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-dream-stars-radial",
                            type = ProceduralNodeType.RADIAL_REPEAT,
                            params = mapOf("count" to 7f, "radius" to 250f, "arcDeg" to 360f, "spinCopies" to 1f)
                        )
                    )
                )
            )
        )
    }

    fun createParametricDunescape(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = "demo-parametric-dunescape",
            name = "Parametric Dunescape",
            artboardWidth = 1000,
            artboardHeight = 1000,
            artboardBgHex = 0xFF28191CL,
            demoId = "parametric-dunescape",
            layers = listOf(
                GraphiteLayer(
                    id = "dune-sun",
                    name = "Sole del Deserto",
                    kind = LayerGeometryKind.ELLIPSE,
                    x = 500f,
                    y = 330f,
                    width = 340f,
                    height = 340f,
                    fillColorHex = 0xFFFBF9EBL,
                    useGradient = true,
                    gradientEndColorHex = 0xFFE4BB72L,
                    isRadialGradient = true,
                    strokeColorHex = 0xFFD5AA43L,
                    strokeWidth = 4f
                ),
                GraphiteLayer(
                    id = "dune-ridge-back",
                    name = "Dune Orizzonte (Wave Node)",
                    kind = LayerGeometryKind.RECTANGLE,
                    x = 500f,
                    y = 650f,
                    width = 920f,
                    height = 240f,
                    cornerRadius = 48f,
                    fillColorHex = 0xFFB86B52L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF6B362FL,
                    gradientAngle = 90f,
                    strokeColorHex = 0xFFE4BB72L,
                    strokeWidth = 3f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-dune-wave-1",
                            type = ProceduralNodeType.JITTER_WAVE,
                            params = mapOf("amplitude" to 34f, "frequency" to 3f, "phase" to 20f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "dune-ridge-front",
                    name = "Dune Primo Piano Parametriche",
                    kind = LayerGeometryKind.RECTANGLE,
                    x = 500f,
                    y = 810f,
                    width = 960f,
                    height = 260f,
                    cornerRadius = 60f,
                    fillColorHex = 0xFFE4BB72L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF9A553BL,
                    gradientAngle = 90f,
                    strokeColorHex = 0xFFFBF9EBL,
                    strokeWidth = 2.5f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-dune-wave-2",
                            type = ProceduralNodeType.JITTER_WAVE,
                            params = mapOf("amplitude" to 42f, "frequency" to 2f, "phase" to 140f)
                        ),
                        ProceduralNode(
                            id = "node-dune-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 0f, "offsetY" to -15f, "spread" to 20f, "alpha" to 0.45f)
                        )
                    )
                )
            )
        )
    }

    fun createProceduralStringLights(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = "demo-procedural-string-lights",
            name = "Procedural String Lights",
            artboardWidth = 1000,
            artboardHeight = 1000,
            artboardBgHex = 0xFF0E131AL,
            demoId = "procedural-string-lights",
            layers = listOf(
                GraphiteLayer(
                    id = "lights-cable",
                    name = "Cavo Catenaria Spline",
                    kind = LayerGeometryKind.SPLINE,
                    x = 500f,
                    y = 420f,
                    width = 840f,
                    height = 300f,
                    hasFill = false,
                    strokeColorHex = 0xFF686F7DL,
                    strokeWidth = 6f,
                    points = listOf(
                        Vec2Point(80f, 260f),
                        Vec2Point(300f, 470f),
                        Vec2Point(520f, 340f),
                        Vec2Point(740f, 520f),
                        Vec2Point(920f, 290f)
                    ),
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-cable-mirror",
                            type = ProceduralNodeType.MIRROR_SYMMETRY,
                            params = mapOf("axisMode" to 1f, "gap" to 140f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "lights-bulbs-grid",
                    name = "Lampadine Filament Glow",
                    kind = LayerGeometryKind.ELLIPSE,
                    x = 220f,
                    y = 380f,
                    width = 74f,
                    height = 96f,
                    fillColorHex = 0xFFFBF9EBL,
                    useGradient = true,
                    gradientEndColorHex = 0xFFE4BB72L,
                    isRadialGradient = true,
                    strokeColorHex = 0xFFD5AA43L,
                    strokeWidth = 3f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-bulbs-grid",
                            type = ProceduralNodeType.GRID_ARRAY,
                            params = mapOf(
                                "columns" to 5f,
                                "rows" to 2f,
                                "spacingX" to 145f,
                                "spacingY" to 230f,
                                "scaleStep" to 0.96f
                            )
                        ),
                        ProceduralNode(
                            id = "node-bulbs-glow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 0f, "offsetY" to 8f, "spread" to 28f, "alpha" to 0.75f),
                            shadowColorHex = 0xFFE4BB72L
                        )
                    )
                )
            )
        )
    }

    fun createRedDress(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = "demo-red-dress",
            name = "Red Dress",
            artboardWidth = 1000,
            artboardHeight = 1000,
            artboardBgHex = 0xFF1F171BL,
            demoId = "red-dress",
            layers = listOf(
                GraphiteLayer(
                    id = "dress-spotlight",
                    name = "Riflettore Atelier",
                    kind = LayerGeometryKind.POLYGON,
                    x = 500f,
                    y = 500f,
                    width = 640f,
                    height = 820f,
                    polygonSides = 3,
                    fillColorHex = 0xFF3A242DL,
                    useGradient = true,
                    gradientEndColorHex = 0xFF1F171BL,
                    gradientAngle = 90f,
                    strokeWidth = 0f,
                    opacity = 0.7f
                ),
                GraphiteLayer(
                    id = "dress-pleats",
                    name = "Gonna Plissettata Radiale",
                    kind = LayerGeometryKind.POLYGON,
                    x = 500f,
                    y = 620f,
                    width = 160f,
                    height = 380f,
                    polygonSides = 3,
                    fillColorHex = 0xFFD6536EL,
                    useGradient = true,
                    gradientEndColorHex = 0xFF7D1D33L,
                    gradientAngle = 90f,
                    strokeColorHex = 0xFFFF9BB0L,
                    strokeWidth = 2f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-dress-fan",
                            type = ProceduralNodeType.RADIAL_REPEAT,
                            params = mapOf("count" to 7f, "radius" to 65f, "arcDeg" to 140f, "spinCopies" to 1f)
                        ),
                        ProceduralNode(
                            id = "node-dress-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 0f, "offsetY" to 18f, "spread" to 20f, "alpha" to 0.6f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "dress-bodice",
                    name = "Corpetto Simmetrico Bézier",
                    kind = LayerGeometryKind.POLYGON,
                    x = 500f,
                    y = 360f,
                    width = 180f,
                    height = 220f,
                    polygonSides = 5,
                    fillColorHex = 0xFFE84A67L,
                    strokeColorHex = 0xFFFBF9EBL,
                    strokeWidth = 3f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-bodice-offset",
                            type = ProceduralNodeType.OFFSET_PATH,
                            params = mapOf("offset" to 8f, "roundness" to 14f)
                        )
                    )
                )
            )
        )
    }

    fun createValleyOfSpires(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = "demo-valley-of-spires",
            name = "Valley of Spires",
            artboardWidth = 1000,
            artboardHeight = 1000,
            artboardBgHex = 0xFF131D24L,
            demoId = "valley-of-spires",
            layers = listOf(
                GraphiteLayer(
                    id = "spires-sky-glow",
                    name = "Aurora Boreale Vettoriale",
                    kind = LayerGeometryKind.ELLIPSE,
                    x = 500f,
                    y = 260f,
                    width = 860f,
                    height = 360f,
                    fillColorHex = 0xFF68C587L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF65BBE5L,
                    opacity = 0.55f,
                    strokeWidth = 0f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-aurora-wave",
                            type = ProceduralNodeType.JITTER_WAVE,
                            params = mapOf("amplitude" to 28f, "frequency" to 5f, "phase" to 60f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "spires-distant",
                    name = "Guglie Rocciose (Grid Array)",
                    kind = LayerGeometryKind.POLYGON,
                    x = 220f,
                    y = 600f,
                    width = 150f,
                    height = 460f,
                    polygonSides = 3,
                    fillColorHex = 0xFF2B4557L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF16242EL,
                    gradientAngle = 90f,
                    strokeColorHex = 0xFF65BBE5L,
                    strokeWidth = 2f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-spires-grid",
                            type = ProceduralNodeType.GRID_ARRAY,
                            params = mapOf(
                                "columns" to 5f,
                                "rows" to 1f,
                                "spacingX" to 140f,
                                "spacingY" to 100f,
                                "scaleStep" to 0.94f
                            )
                        ),
                        ProceduralNode(
                            id = "node-spires-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 12f, "offsetY" to 10f, "spread" to 16f, "alpha" to 0.5f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "spires-monolith",
                    name = "Monolite Centrale & Rune",
                    kind = LayerGeometryKind.POLYGON,
                    x = 500f,
                    y = 580f,
                    width = 190f,
                    height = 540f,
                    polygonSides = 6,
                    fillColorHex = 0xFF37754CL,
                    useGradient = true,
                    gradientEndColorHex = 0xFF182C25L,
                    gradientAngle = 90f,
                    strokeColorHex = 0xFF68C587L,
                    strokeWidth = 4f
                )
            )
        )
    }

    fun createStarterDocument(): GraphiteDocumentModel {
        return GraphiteDocumentModel(
            id = UUID.randomUUID().toString(),
            name = "Studio Procedurale Mobile",
            artboardWidth = 1080,
            artboardHeight = 1080,
            artboardBgHex = 0xFF161920L,
            layers = listOf(
                GraphiteLayer(
                    id = "layer-hex-ring",
                    name = "Anello Esagonale Radiale",
                    kind = LayerGeometryKind.POLYGON,
                    x = 540f,
                    y = 540f,
                    width = 150f,
                    height = 150f,
                    polygonSides = 6,
                    fillColorHex = 0xFF65BBE5L,
                    useGradient = true,
                    gradientEndColorHex = 0xFF68C587L,
                    gradientAngle = 45f,
                    strokeColorHex = 0xFFFBF9EBL,
                    strokeWidth = 3f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-starter-radial",
                            type = ProceduralNodeType.RADIAL_REPEAT,
                            params = mapOf("count" to 8f, "radius" to 220f, "arcDeg" to 360f, "spinCopies" to 1f)
                        ),
                        ProceduralNode(
                            id = "node-starter-shadow",
                            type = ProceduralNodeType.DROP_SHADOW,
                            params = mapOf("offsetX" to 0f, "offsetY" to 12f, "spread" to 16f, "alpha" to 0.55f)
                        )
                    )
                ),
                GraphiteLayer(
                    id = "layer-core-star",
                    name = "Nucleo Stella Graphite",
                    kind = LayerGeometryKind.STAR,
                    x = 540f,
                    y = 540f,
                    width = 230f,
                    height = 230f,
                    polygonSides = 6,
                    starInnerRatio = 0.48f,
                    fillColorHex = 0xFFE4BB72L,
                    useGradient = true,
                    gradientEndColorHex = 0xFFCE6EA7L,
                    isRadialGradient = true,
                    strokeColorHex = 0xFFFFFFFFL,
                    strokeWidth = 4f,
                    nodes = listOf(
                        ProceduralNode(
                            id = "node-starter-offset",
                            type = ProceduralNodeType.OFFSET_PATH,
                            params = mapOf("offset" to 10f, "roundness" to 12f)
                        )
                    )
                )
            )
        )
    }
}
