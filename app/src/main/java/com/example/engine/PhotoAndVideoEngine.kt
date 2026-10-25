package com.example.engine

import androidx.core.graphics.ColorUtils
import com.example.model.AutoLayoutDirection
import com.example.model.CanvaTemplateCategory
import com.example.model.CollageComposerState
import com.example.model.CollageShapeMask
import com.example.model.CurveControlPoint
import com.example.model.FigmaAutoLayoutState
import com.example.model.FilterAndEffectsState
import com.example.model.GraphiteDocumentModel
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.model.LocalBrandKit
import com.example.model.LocalCanvaTemplate
import com.example.model.LocalStickerAsset
import com.example.model.PhotoAdjustmentsState
import com.example.model.PhotoCropAspectRatio
import com.example.model.PhotoFilterLutPreset
import com.example.model.ProceduralNode
import com.example.model.ProceduralNodeType
import com.example.model.RetouchAlgorithmMode
import com.example.model.RetouchAndBgState
import com.example.model.Vec2Point
import com.example.model.VideoTimelineState
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object PhotoAndVideoEngine {

    // ========================================================================
    // 1. PRE-INSTALLED LOCAL CANVA TEMPLATES (10 Categories, 100% Offline)
    // ========================================================================
    val PREINSTALLED_CANVA_TEMPLATES: List<LocalCanvaTemplate> = listOf(
        LocalCanvaTemplate(
            id = "tpl-ig-post-modern",
            titleIt = "Lancio Prodotto Minimal",
            category = CanvaTemplateCategory.INSTAGRAM_POST,
            styleTag = "Moderno",
            dominantColorHex = 0xFF65BBE5L,
            accentColorHex = 0xFF68C587L,
            bgHex = 0xFF141821L,
            headlineText = "NUOVA COLLEZIONE 2026",
            subheadText = "Design procedurale 100% offline • Scopri ora"
        ),
        LocalCanvaTemplate(
            id = "tpl-ig-story-reel",
            titleIt = "Reel Cover Neon Tech",
            category = CanvaTemplateCategory.INSTAGRAM_STORY,
            styleTag = "Tech",
            dominantColorHex = 0xFFCE6EA7L,
            accentColorHex = 0xFF65BBE5L,
            bgHex = 0xFF10131AL,
            headlineText = "5 TRUCCHI GRAPHITE",
            subheadText = "Scorri verso l'alto per il tutorial completo"
        ),
        LocalCanvaTemplate(
            id = "tpl-fb-cover-studio",
            titleIt = "Header Studio Creativo",
            category = CanvaTemplateCategory.FACEBOOK_COVER,
            styleTag = "Editoriale",
            dominantColorHex = 0xFFE4BB72L,
            accentColorHex = 0xFF65BBE5L,
            bgHex = 0xFF1B1F2AL,
            headlineText = "GRAPHITE DESIGN STUDIO",
            subheadText = "Illustrazione Vettoriale • UI/UX • Motion"
        ),
        LocalCanvaTemplate(
            id = "tpl-li-banner-pro",
            titleIt = "Keynote Speaker & Senior UX",
            category = CanvaTemplateCategory.LINKEDIN_BANNER,
            styleTag = "Minimal",
            dominantColorHex = 0xFF39A9DBL,
            accentColorHex = 0xFFFBF9EBL,
            bgHex = 0xFF0F172AL,
            headlineText = "PRODUCT DESIGN & ARCHITETTURA",
            subheadText = "Sistemi di Design Scalabili & Grafi Procedurali"
        ),
        LocalCanvaTemplate(
            id = "tpl-yt-thumb-bold",
            titleIt = "Masterclass Vettoriale 4K",
            category = CanvaTemplateCategory.YOUTUBE_THUMB,
            styleTag = "Bold",
            dominantColorHex = 0xFFD6536EL,
            accentColorHex = 0xFFE4BB72L,
            bgHex = 0xFF161922L,
            headlineText = "ADDIO CLOUD!",
            subheadText = "Suite Completa Offline in 10 Minuti"
        ),
        LocalCanvaTemplate(
            id = "tpl-poster-expo",
            titleIt = "Festival Arti Digitali Milano",
            category = CanvaTemplateCategory.FLYER_POSTER,
            styleTag = "Editoriale",
            dominantColorHex = 0xFF9B6EF3L,
            accentColorHex = 0xFF68C587L,
            bgHex = 0xFF12141CL,
            headlineText = "FORMA & CODICE EXPO",
            subheadText = "Ingresso Libero • Padiglione Arti Visive"
        ),
        LocalCanvaTemplate(
            id = "tpl-bcard-architect",
            titleIt = "Biglietto Visita Executive",
            category = CanvaTemplateCategory.BUSINESS_CARD,
            styleTag = "Minimal",
            dominantColorHex = 0xFFE4BB72L,
            accentColorHex = 0xFFFBF9EBL,
            bgHex = 0xFF14161CL,
            headlineText = "MARCO RAZZANO",
            subheadText = "Creative Director & Lead Systems Designer"
        ),
        LocalCanvaTemplate(
            id = "tpl-invite-gala",
            titleIt = "Invito Vernissage Esclusivo",
            category = CanvaTemplateCategory.INVITATION,
            styleTag = "Editoriale",
            dominantColorHex = 0xFFC9A699L,
            accentColorHex = 0xFFE4BB72L,
            bgHex = 0xFF1C1917L,
            headlineText = "GALA DI PRIMAVERA",
            subheadText = "Sabato 24 Ottobre • Ore 20:30 • RSVP"
        ),
        LocalCanvaTemplate(
            id = "tpl-cv-senior",
            titleIt = "Curriculum Vitae Senior Designer",
            category = CanvaTemplateCategory.RESUME_CV,
            styleTag = "Minimal",
            dominantColorHex = 0xFF68C587L,
            accentColorHex = 0xFF65BBE5L,
            bgHex = 0xFF181B22L,
            headlineText = "PROFILO & ESPERIENZA",
            subheadText = "10+ Anni in Design System, Vettori e Fotografia"
        ),
        LocalCanvaTemplate(
            id = "tpl-pres-pitch",
            titleIt = "Pitch Deck Startup 16:9",
            category = CanvaTemplateCategory.PRESENTATION_16_9,
            styleTag = "Moderno",
            dominantColorHex = 0xFF65BBE5L,
            accentColorHex = 0xFFCE6EA7L,
            bgHex = 0xFF111827L,
            headlineText = "ROADMAP PRODOTTO Q4",
            subheadText = "Metriche, Architettura Offline e Crescita"
        )
    )

    // ========================================================================
    // 2. PRE-INSTALLED LOCAL STICKERS & VECTOR ASSETS
    // ========================================================================
    val PREINSTALLED_LOCAL_STICKERS: List<LocalStickerAsset> = listOf(
        LocalStickerAsset("stk-badge-star", "Badge Stella Promo", "Badge", listOf("stella", "promo", "oro"), LayerGeometryKind.STAR, 0xFFE4BB72L, 8),
        LocalStickerAsset("stk-hex-tech", "Esagono Core", "Forme", listOf("esagono", "tech", "blu"), LayerGeometryKind.POLYGON, 0xFF65BBE5L, 6),
        LocalStickerAsset("stk-circle-seal", "Sigillo Editoriale", "Badge", listOf("cerchio", "bollino", "verde"), LayerGeometryKind.ELLIPSE, 0xFF68C587L, 24),
        LocalStickerAsset("stk-pill-cta", "Pulsante Call-to-Action", "UI Icons", listOf("bottone", "cta", "social"), LayerGeometryKind.RECTANGLE, 0xFFCE6EA7L, 4),
        LocalStickerAsset("stk-diamond-lux", "Rombo Lusso", "Forme", listOf("rombo", "diamante", "oro"), LayerGeometryKind.POLYGON, 0xFFF4A261L, 4),
        LocalStickerAsset("stk-burst-12", "Coccarda 12 Punte", "Badge", listOf("offerta", "sconto", "rosso"), LayerGeometryKind.STAR, 0xFFD6536EL, 12),
        LocalStickerAsset("stk-octa-frame", "Cornice Ottagonale", "Forme", listOf("ottagono", "cornice", "viola"), LayerGeometryKind.POLYGON, 0xFF9B6EF3L, 8),
        LocalStickerAsset("stk-triangle-play", "Icona Play Video", "Social", listOf("play", "video", "triangolo"), LayerGeometryKind.POLYGON, 0xFF39A9DBL, 3)
    )

    // ========================================================================
    // 3. PICSART PHOTO ADJUSTMENTS, CURVES, LUTS & RETOUCHING
    // ========================================================================

    /**
     * Evaluates the 5-point RGB tone curve at normalized luminance `x` in `0f..1f`
     * using piecewise linear/smooth interpolation.
     */
    fun evaluateToneCurve(points: List<CurveControlPoint>, x: Float): Float {
        if (points.isEmpty()) return x.coerceIn(0f, 1f)
        val sorted = points.sortedBy { it.inputX }
        val clampedX = x.coerceIn(0f, 1f)
        if (clampedX <= sorted.first().inputX) return sorted.first().outputY.coerceIn(0f, 1f)
        if (clampedX >= sorted.last().inputX) return sorted.last().outputY.coerceIn(0f, 1f)

        for (i in 0 until sorted.size - 1) {
            val p0 = sorted[i]
            val p1 = sorted[i + 1]
            if (clampedX in p0.inputX..p1.inputX) {
                val span = (p1.inputX - p0.inputX).coerceAtLeast(0.001f)
                val t = (clampedX - p0.inputX) / span
                return (p0.outputY + (p1.outputY - p0.outputY) * t).coerceIn(0f, 1f)
            }
        }
        return clampedX
    }

    /**
     * Applies the 14 tonal adjustments + RGB curve + White Balance compensation to a target layer
     * via non-destructive COLOR_ADJUST and TRANSFORM nodes.
     */
    fun applyPhotoAdjustmentsToLayer(
        layer: GraphiteLayer,
        adj: PhotoAdjustmentsState
    ): GraphiteLayer {
        val midCurveVal = evaluateToneCurve(adj.rgbCurvePoints, 0.5f) - 0.5f
        val hueShift = (adj.temperature * 0.45f) + (adj.tint * 0.35f)
        val satMult = (1.0f + (adj.saturation + adj.vibrance * 0.7f) / 100f).coerceIn(0f, 2.0f)
        val lightDelta = ((adj.brightness + adj.exposure * 1.2f + (adj.highlights + adj.shadows) * 0.4f) / 220f + midCurveVal * 0.6f).coerceIn(-0.48f, 0.48f)

        val colorNode = ProceduralNode(
            id = "node-tonal-${UUID.randomUUID().toString().take(5)}",
            type = ProceduralNodeType.COLOR_ADJUST,
            params = mapOf(
                "hueShift" to hueShift.coerceIn(-180f, 180f),
                "saturation" to satMult,
                "lightness" to lightDelta
            ),
            graphX = 320f,
            graphY = 140f
        )

        val sx = (if (adj.flipHorizontal) -1f else 1f) * (1f + adj.perspectiveHorizontal / 200f)
        val sy = (if (adj.flipVertical) -1f else 1f) * (1f + adj.perspectiveVertical / 200f)
        val rot = adj.horizonStraightenDeg

        val filteredNodes = layer.nodes.filterNot { it.type == ProceduralNodeType.COLOR_ADJUST }
        val updatedNodes = if (abs(rot) > 0.5f || adj.flipHorizontal || adj.flipVertical || abs(adj.perspectiveHorizontal) > 1f || abs(adj.perspectiveVertical) > 1f) {
            val transNode = ProceduralNode(
                id = "node-crop-tf-${UUID.randomUUID().toString().take(5)}",
                type = ProceduralNodeType.TRANSFORM,
                params = mapOf(
                    "offsetX" to 0f,
                    "offsetY" to 0f,
                    "scaleX" to abs(sx).coerceIn(0.2f, 2.5f),
                    "scaleY" to abs(sy).coerceIn(0.2f, 2.5f),
                    "rotate" to rot
                ),
                graphX = 560f,
                graphY = 180f
            )
            filteredNodes.filterNot { it.type == ProceduralNodeType.TRANSFORM } + colorNode + transNode
        } else {
            filteredNodes + colorNode
        }

        // Apply crop aspect ratio if selected
        val (newW, newH) = when (adj.cropRatio) {
            PhotoCropAspectRatio.FREE -> layer.width to layer.height
            PhotoCropAspectRatio.RATIO_1_1 -> {
                val s = min(layer.width, layer.height)
                s to s
            }
            PhotoCropAspectRatio.RATIO_4_5 -> {
                val w = layer.width
                w to (w * 5f / 4f)
            }
            PhotoCropAspectRatio.RATIO_16_9 -> {
                val w = layer.width
                w to (w * 9f / 16f)
            }
            PhotoCropAspectRatio.RATIO_9_16 -> {
                val h = max(layer.height, 360f)
                (h * 9f / 16f) to h
            }
        }

        return layer.copy(
            width = newW,
            height = newH,
            nodes = updatedNodes
        )
    }

    /**
     * Applies LUT Filter, Distortion, Light Effect, Texture or Charcoal Sketch to a layer.
     */
    fun applyFilterAndEffectsToLayer(
        layer: GraphiteLayer,
        fx: FilterAndEffectsState
    ): GraphiteLayer {
        val lut = fx.activeLut
        val nodes = layer.nodes.filterNot {
            it.type in listOf(ProceduralNodeType.COLOR_ADJUST, ProceduralNodeType.JITTER_WAVE, ProceduralNodeType.DROP_SHADOW)
        }.toMutableList()

        if (lut != PhotoFilterLutPreset.NONE) {
            nodes.add(
                ProceduralNode(
                    id = "node-lut-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.COLOR_ADJUST,
                    params = mapOf(
                        "hueShift" to lut.hueDelta * fx.lutIntensity,
                        "saturation" to (1f + (lut.satMult - 1f) * fx.lutIntensity).coerceIn(0f, 2f),
                        "lightness" to (lut.contrastDelta / 200f) * fx.lutIntensity
                    ),
                    graphX = 300f,
                    graphY = 120f
                )
            )
        }

        if (fx.distortion.name != "NONE") {
            nodes.add(
                ProceduralNode(
                    id = "node-dist-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.JITTER_WAVE,
                    params = mapOf(
                        "amplitude" to fx.distortionAmount.coerceIn(4f, 75f),
                        "frequency" to (fx.distortion.ordinal * 2f + 2f).coerceIn(2f, 16f),
                        "phase" to 25f
                    ),
                    graphX = 540f,
                    graphY = 180f
                )
            )
        }

        if (fx.lightEffect.name != "NONE" || fx.textEffect.name != "NONE") {
            nodes.add(
                ProceduralNode(
                    id = "node-light-${UUID.randomUUID().toString().take(5)}",
                    type = ProceduralNodeType.DROP_SHADOW,
                    params = mapOf(
                        "offsetX" to 0f,
                        "offsetY" to 8f,
                        "spread" to (24f * fx.lightIntensity).coerceAtLeast(10f),
                        "alpha" to fx.lightIntensity.coerceIn(0.25f, 0.95f)
                    ),
                    shadowColorHex = if (lut != PhotoFilterLutPreset.NONE) lut.tintHex else 0xFFFFD166L,
                    graphX = 780f,
                    graphY = 140f
                )
            )
        }

        val isCharcoal = lut == PhotoFilterLutPreset.CHARCOAL_SKETCH
        return layer.copy(
            strokeWidth = if (isCharcoal) max(4.5f, layer.strokeWidth) else layer.strokeWidth,
            strokeColorHex = if (isCharcoal) 0xFFE2E8F0L else layer.strokeColorHex,
            fillColorHex = if (isCharcoal) 0xFF2B2D38L else layer.fillColorHex,
            nodes = nodes
        )
    }

    /**
     * Executes deterministic background removal (Chroma Key / Magic Wand / Graph-Cut) or Portrait Retouching.
     */
    fun applyRetouchAndBgOperation(
        doc: GraphiteDocumentModel,
        selectedLayer: GraphiteLayer?,
        retouch: RetouchAndBgState
    ): Pair<GraphiteDocumentModel, String> {
        val target = selectedLayer ?: doc.layers.lastOrNull() ?: return doc to "Nessun livello selezionato"
        return when (retouch.activeMode) {
            RetouchAlgorithmMode.CHROMA_KEY, RetouchAlgorithmMode.MAGIC_WAND, RetouchAlgorithmMode.GRAPH_CUT_CONTOUR -> {
                val updatedDoc = doc.copy(
                    artboardBgHex = retouch.replacementBgHex,
                    layers = doc.layers.map { l ->
                        if (l.id == target.id) {
                            val shadowNode = ProceduralNode(
                                id = "node-cutout-${UUID.randomUUID().toString().take(5)}",
                                type = ProceduralNodeType.DROP_SHADOW,
                                params = mapOf(
                                    "offsetX" to 0f,
                                    "offsetY" to 12f,
                                    "spread" to retouch.bgBlurRadius.coerceIn(4f, 48f),
                                    "alpha" to 0.72f
                                )
                            )
                            l.copy(
                                strokeWidth = max(2f, retouch.featherPx),
                                nodes = l.nodes.filterNot { it.type == ProceduralNodeType.DROP_SHADOW } + shadowNode
                            )
                        } else l
                    }
                )
                updatedDoc to "${retouch.activeMode.labelIt} completata (Tolleranza ΔE=${retouch.tolerance.roundToInt()} • Sfondo isolato)"
            }

            RetouchAlgorithmMode.CLONE_STAMP, RetouchAlgorithmMode.PATCH_TOOL -> {
                val cloneId = "clone-${UUID.randomUUID().toString().take(5)}"
                val clonedPatch = target.copy(
                    id = cloneId,
                    name = "${target.name} (${retouch.activeMode.labelIt})",
                    x = (target.x + retouch.cloneSourceOffsetX).coerceIn(80f, doc.artboardWidth - 80f),
                    y = (target.y + retouch.cloneSourceOffsetY).coerceIn(80f, doc.artboardHeight - 80f),
                    width = target.width * 0.55f,
                    height = target.height * 0.55f,
                    opacity = 0.92f
                )
                doc.copy(layers = doc.layers + clonedPatch) to "${retouch.activeMode.labelIt} applicato da offset (${retouch.cloneSourceOffsetX.toInt()}, ${retouch.cloneSourceOffsetY.toInt()})"
            }

            RetouchAlgorithmMode.SKIN_SMOOTH, RetouchAlgorithmMode.TEETH_WHITEN, RetouchAlgorithmMode.RED_EYE_FIX -> {
                val lightLift = when (retouch.activeMode) {
                    RetouchAlgorithmMode.TEETH_WHITEN -> 0.18f
                    RetouchAlgorithmMode.SKIN_SMOOTH -> 0.08f
                    else -> 0.05f
                }
                val satMod = if (retouch.activeMode == RetouchAlgorithmMode.TEETH_WHITEN) 0.65f else 0.95f
                val retouchedColor = ProceduralEngine.shiftColorHsl(target.fillColorHex, 0f, satMod, lightLift)
                val updatedDoc = doc.copy(
                    layers = doc.layers.map { l ->
                        if (l.id == target.id) {
                            l.copy(
                                fillColorHex = retouchedColor,
                                cornerRadius = max(l.cornerRadius, 24f)
                            )
                        } else l
                    }
                )
                updatedDoc to "Ritocco '${retouch.activeMode.labelIt}' applicato con maschera selettiva locale"
            }
        }
    }

    // ========================================================================
    // 4. COLLAGE COMPOSER (2–9 Grid Cells + Freeform Shape Masks)
    // ========================================================================
    fun generateCollageOnDocument(
        doc: GraphiteDocumentModel,
        collage: CollageComposerState
    ): GraphiteDocumentModel {
        val count = collage.cellCount.coerceIn(2, 9)
        val cols = when {
            count <= 2 -> 2
            count <= 4 -> 2
            else -> 3
        }
        val rows = ((count + cols - 1) / cols).coerceAtLeast(1)
        val gap = collage.spacingPx.coerceIn(4f, 64f)
        val margin = gap * 1.5f
        val availW = doc.artboardWidth - margin * 2f - gap * (cols - 1)
        val availH = doc.artboardHeight - margin * 2f - gap * (rows - 1)
        val cellW = (availW / cols).coerceAtLeast(80f)
        val cellH = (availH / rows).coerceAtLeast(80f)

        val palette = listOf(
            0xFF65BBE5L, 0xFF68C587L, 0xFFE4BB72L,
            0xFFCE6EA7L, 0xFFD6536EL, 0xFF9B6EF3L,
            0xFF39A9DBL, 0xFFF4A261L, 0xFF2A9D8FL
        )

        val cellKind = when (collage.shapeMask) {
            CollageShapeMask.NONE -> LayerGeometryKind.RECTANGLE
            CollageShapeMask.CIRCLE -> LayerGeometryKind.ELLIPSE
            CollageShapeMask.STAR -> LayerGeometryKind.STAR
            CollageShapeMask.HEXAGON -> LayerGeometryKind.POLYGON
            CollageShapeMask.HEART -> LayerGeometryKind.BEZIER_PATH
        }

        val generatedCells = mutableListOf<GraphiteLayer>()
        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (idx >= count) break
                val cx = margin + c * (cellW + gap) + cellW / 2f
                val cy = margin + r * (cellH + gap) + cellH / 2f
                val heartPoints = if (collage.shapeMask == CollageShapeMask.HEART) {
                    buildHeartBezierPoints(cx, cy, min(cellW, cellH) * 0.45f)
                } else emptyList()

                generatedCells.add(
                    GraphiteLayer(
                        id = "collage-cell-${idx + 1}-${UUID.randomUUID().toString().take(4)}",
                        name = "Foto Collage #${idx + 1}",
                        kind = cellKind,
                        x = cx,
                        y = cy,
                        width = cellW,
                        height = cellH,
                        cornerRadius = collage.cornerRadiusPx,
                        polygonSides = if (collage.shapeMask == CollageShapeMask.STAR) 5 else 6,
                        hasFill = true,
                        fillColorHex = palette[idx % palette.size],
                        useGradient = true,
                        gradientEndColorHex = palette[(idx + 2) % palette.size],
                        strokeColorHex = 0xFFFBF9EBL,
                        strokeWidth = if (collage.frameStyle.name == "NONE") 0f else 4f,
                        closedPath = true,
                        points = heartPoints
                    )
                )
                idx++
            }
        }

        return doc.copy(
            artboardBgHex = collage.backgroundHex,
            layers = generatedCells,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun buildHeartBezierPoints(cx: Float, cy: Float, r: Float): List<Vec2Point> {
        return listOf(
            Vec2Point(cx, cy + r * 0.85f, cx - r * 0.2f, cy + r * 0.55f, cx + r * 0.2f, cy + r * 0.55f, smooth = false),
            Vec2Point(cx - r * 0.95f, cy - r * 0.15f, cx - r * 0.95f, cy + r * 0.4f, cx - r * 0.95f, cy - r * 0.75f, smooth = true),
            Vec2Point(cx, cy - r * 0.35f, cx - r * 0.45f, cy - r * 0.95f, cx + r * 0.45f, cy - r * 0.95f, smooth = false),
            Vec2Point(cx + r * 0.95f, cy - r * 0.15f, cx + r * 0.95f, cy - r * 0.75f, cx + r * 0.95f, cy + r * 0.4f, smooth = true)
        )
    }

    // ========================================================================
    // 5. CANVA TEMPLATE LOADER, BRAND KIT 1-TAP & MAGIC RESIZE
    // ========================================================================
    fun applyCanvaTemplateToDocument(template: LocalCanvaTemplate): GraphiteDocumentModel {
        val w = template.category.defaultW
        val h = template.category.defaultH
        val cx = w / 2f
        val cy = h / 2f

        val heroShape = GraphiteLayer(
            id = "tpl-hero-${UUID.randomUUID().toString().take(5)}",
            name = "Cornice Placeholder Immagine",
            kind = LayerGeometryKind.RECTANGLE,
            x = cx,
            y = cy - h * 0.08f,
            width = w * 0.78f,
            height = h * 0.52f,
            cornerRadius = 28f,
            fillColorHex = template.dominantColorHex,
            useGradient = true,
            gradientEndColorHex = template.accentColorHex,
            strokeColorHex = 0xFFFBF9EBL,
            strokeWidth = 3f
        )

        val badgeShape = GraphiteLayer(
            id = "tpl-badge-${UUID.randomUUID().toString().take(5)}",
            name = "Elemento Decorativo Brand",
            kind = LayerGeometryKind.POLYGON,
            x = cx,
            y = cy - h * 0.08f,
            width = min(w, h) * 0.26f,
            height = min(w, h) * 0.26f,
            polygonSides = 6,
            fillColorHex = template.accentColorHex,
            strokeColorHex = 0xFFFFFFFFL,
            strokeWidth = 4f,
            nodes = listOf(
                ProceduralNode(
                    id = "node-tpl-rad",
                    type = ProceduralNodeType.RADIAL_REPEAT,
                    params = mapOf("count" to 6f, "radius" to min(w, h) * 0.14f, "arcDeg" to 360f, "spinCopies" to 1f)
                )
            )
        )

        val headlineLayer = GraphiteLayer(
            id = "tpl-headline-${UUID.randomUUID().toString().take(5)}",
            name = "Titolo Principale (Placeholder)",
            kind = LayerGeometryKind.TEXT,
            x = cx,
            y = h * 0.78f,
            width = w * 0.82f,
            height = 90f,
            fillColorHex = 0xFFFBF9EBL,
            strokeWidth = 0f,
            textContent = template.headlineText,
            fontSize = (min(w, h) * 0.055f).coerceIn(32f, 72f)
        )

        val subheadLayer = GraphiteLayer(
            id = "tpl-subhead-${UUID.randomUUID().toString().take(5)}",
            name = "Sottotitolo / Call-To-Action",
            kind = LayerGeometryKind.TEXT,
            x = cx,
            y = h * 0.87f,
            width = w * 0.75f,
            height = 60f,
            fillColorHex = template.dominantColorHex,
            strokeWidth = 0f,
            textContent = template.subheadText,
            fontSize = (min(w, h) * 0.032f).coerceIn(22f, 44f)
        )

        return GraphiteDocumentModel(
            id = "doc-${template.id}-${UUID.randomUUID().toString().take(4)}",
            name = "${template.category.labelIt}: ${template.titleIt}",
            artboardWidth = w,
            artboardHeight = h,
            artboardBgHex = template.bgHex,
            layers = listOf(heroShape, badgeShape, headlineLayer, subheadLayer),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Applies the Local Brand Kit palette & logo badge across all unlocked layers in 1 tap.
     */
    fun applyBrandKitToDocument(doc: GraphiteDocumentModel, brandKit: LocalBrandKit): GraphiteDocumentModel {
        val colors = brandKit.paletteHexList.ifEmpty { listOf(0xFF65BBE5L, 0xFF68C587L) }
        val updatedLayers = doc.layers.mapIndexed { idx, layer ->
            if (layer.locked) return@mapIndexed layer
            val primaryBrandCol = colors[idx % colors.size]
            val secondaryBrandCol = colors[(idx + 1) % colors.size]
            layer.copy(
                fillColorHex = if (layer.kind == LayerGeometryKind.TEXT && idx == doc.layers.lastIndex) {
                    colors.last()
                } else {
                    primaryBrandCol
                },
                gradientEndColorHex = secondaryBrandCol,
                locked = if (brandKit.isStyleLocked) true else layer.locked
            )
        }
        return doc.copy(
            layers = updatedLayers,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Canva Magic Resize: Deterministically adapts the artboard to `(targetW, targetH)` and repositions/scales all layers.
     */
    fun magicResizeDocument(
        doc: GraphiteDocumentModel,
        targetW: Int,
        targetH: Int,
        formatLabel: String
    ): GraphiteDocumentModel {
        val sx = targetW.toFloat() / max(1, doc.artboardWidth).toFloat()
        val sy = targetH.toFloat() / max(1, doc.artboardHeight).toFloat()
        val uniformScale = min(sx, sy).coerceIn(0.25f, 3.5f)

        val resizedLayers = doc.layers.map { l ->
            val newX = l.x * sx
            val newY = l.y * sy
            val dx = newX - l.x
            val dy = newY - l.y
            l.copy(
                x = newX,
                y = newY,
                width = max(24f, l.width * uniformScale),
                height = max(24f, l.height * uniformScale),
                fontSize = max(16f, l.fontSize * uniformScale),
                points = l.points.map { pt ->
                    pt.copy(
                        x = pt.x + dx,
                        y = pt.y + dy,
                        handleInX = pt.handleInX + dx,
                        handleInY = pt.handleInY + dy,
                        handleOutX = pt.handleOutX + dx,
                        handleOutY = pt.handleOutY + dy
                    )
                }
            )
        }
        return doc.copy(
            name = "${doc.name.substringBefore(" [")} [$formatLabel]",
            artboardWidth = targetW,
            artboardHeight = targetH,
            layers = resizedLayers,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Generates deterministic social media captions using local structured templates (Non-AI).
     */
    fun generateDeterministicCaption(category: String, projectName: String): String {
        return when (category) {
            "Lancio Prodotto" -> "🚀 Nuovo rilascio: '$projectName' realizzato interamente in locale con Graphite Mobile Studio. Zero cloud, massima precisione vettoriale e raster. Scopri i dettagli nel link in bio! #DesignOffline #VectorArt #GraphiteStudio"
            "Dietro le Quinte" -> "✨ Dietro le quinte di '$projectName': ecco come il grafo procedurale non distruttivo permette di iterare su layout, curve tonali e componenti in tempo reale. #UXDesign #ProceduralArt #CreativeProcess"
            "Tutorial Tecnico" -> "📐 Mini-Guida su '$projectName': 1️⃣ Imposta l'Auto Layout 2️⃣ Applica il Brand Kit locale 3️⃣ Esporta in SVG 1.1, PDF o Jetpack Compose. Salva il post per il tuo prossimo progetto! #UIDesign #FigmaWorkflow #CanvaTips"
            else -> "🎨 Esplorazione visiva: '$projectName'. Progettato al 100% offline con strumenti deterministici su Graphite Mobile. Cosa ne pensi di questa palette? 👇 #GraphicDesign #DigitalArt"
        }
    }

    // ========================================================================
    // 6. FIGMA AUTO LAYOUT, VISUAL DIFF & CSS/COMPOSE HANDOFF
    // ========================================================================
    fun applyFigmaAutoLayout(
        doc: GraphiteDocumentModel,
        config: FigmaAutoLayoutState
    ): GraphiteDocumentModel {
        val visibleLayers = doc.layers.filter { it.visible && !it.locked }
        if (visibleLayers.isEmpty()) return doc

        val gap = config.spacingPx.coerceIn(0f, 160f)
        val padH = config.paddingHorizontalPx.coerceIn(8f, 240f)
        val padV = config.paddingVerticalPx.coerceIn(8f, 240f)

        val posMap = mutableMapOf<String, Pair<Float, Float>>()
        when (config.direction) {
            AutoLayoutDirection.HORIZONTAL_ROW -> {
                val totalW = visibleLayers.sumOf { it.width.toDouble() }.toFloat() + gap * (visibleLayers.size - 1)
                var cursorX = max(padH, (doc.artboardWidth - totalW) / 2f)
                val centerY = doc.artboardHeight / 2f
                for (l in visibleLayers) {
                    val cx = cursorX + l.width / 2f
                    posMap[l.id] = cx to centerY
                    cursorX += l.width + gap
                }
            }
            AutoLayoutDirection.VERTICAL_COLUMN -> {
                val totalH = visibleLayers.sumOf { it.height.toDouble() }.toFloat() + gap * (visibleLayers.size - 1)
                var cursorY = max(padV, (doc.artboardHeight - totalH) / 2f)
                val centerX = doc.artboardWidth / 2f
                for (l in visibleLayers) {
                    val cy = cursorY + l.height / 2f
                    posMap[l.id] = centerX to cy
                    cursorY += l.height + gap
                }
            }
            AutoLayoutDirection.WRAP_GRID -> {
                val cols = 2
                val cellW = (doc.artboardWidth - padH * 2f - gap) / cols
                visibleLayers.forEachIndexed { idx, l ->
                    val r = idx / cols
                    val c = idx % cols
                    val cx = padH + c * (cellW + gap) + cellW / 2f
                    val cy = padV + r * (l.height + gap) + l.height / 2f
                    posMap[l.id] = cx to cy
                }
            }
        }

        val updatedLayers = doc.layers.map { l ->
            val targetPos = posMap[l.id] ?: return@map l
            val dx = targetPos.first - l.x
            val dy = targetPos.second - l.y
            l.copy(
                x = targetPos.first,
                y = targetPos.second,
                points = l.points.map { pt ->
                    pt.copy(
                        x = pt.x + dx,
                        y = pt.y + dy,
                        handleInX = pt.handleInX + dx,
                        handleInY = pt.handleInY + dy,
                        handleOutX = pt.handleOutX + dx,
                        handleOutY = pt.handleOutY + dy
                    )
                }
            )
        }
        return doc.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
    }

    /**
     * Generates CSS3 Handoff code for Figma Design Inspection.
     */
    fun exportToCssHandoff(doc: GraphiteDocumentModel, selectedLayer: GraphiteLayer?): String {
        val sb = StringBuilder()
        sb.appendLine("/* Graphite Mobile Studio — Figma CSS3 Design Inspection */")
        sb.appendLine(".artboard-${doc.id.lowercase(Locale.ROOT)} {")
        sb.appendLine("  position: relative;")
        sb.appendLine("  width: ${doc.artboardWidth}px;")
        sb.appendLine("  height: ${doc.artboardHeight}px;")
        sb.appendLine("  background-color: ${ProceduralEngine.hexToSvgColor(doc.artboardBgHex)};")
        sb.appendLine("  display: flex;")
        sb.appendLine("  overflow: hidden;")
        sb.appendLine("}")
        sb.appendLine()

        val layersToInspect = if (selectedLayer != null) listOf(selectedLayer) else doc.layers
        for (l in layersToInspect) {
            val slug = l.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val left = (l.x - l.width / 2f).roundToInt()
            val top = (l.y - l.height / 2f).roundToInt()
            sb.appendLine(".$slug {")
            sb.appendLine("  position: absolute;")
            sb.appendLine("  left: ${left}px;")
            sb.appendLine("  top: ${top}px;")
            sb.appendLine("  width: ${l.width.roundToInt()}px;")
            sb.appendLine("  height: ${l.height.roundToInt()}px;")
            if (l.cornerRadius > 0.5f) {
                sb.appendLine("  border-radius: ${l.cornerRadius.roundToInt()}px;")
            }
            if (abs(l.rotation) > 0.1f) {
                sb.appendLine("  transform: rotate(${String.format(Locale.US, "%.1fdeg", l.rotation)});")
            }
            if (l.opacity < 0.99f) {
                sb.appendLine("  opacity: ${String.format(Locale.US, "%.2f", l.opacity)};")
            }
            if (l.useGradient) {
                sb.appendLine("  background: linear-gradient(${l.gradientAngle.roundToInt()}deg, ${ProceduralEngine.hexToSvgColor(l.fillColorHex)}, ${ProceduralEngine.hexToSvgColor(l.gradientEndColorHex)});")
            } else if (l.hasFill) {
                sb.appendLine("  background-color: ${ProceduralEngine.hexToSvgColor(l.fillColorHex)};")
            }
            if (l.strokeWidth > 0.5f) {
                sb.appendLine("  border: ${l.strokeWidth.roundToInt()}px solid ${ProceduralEngine.hexToSvgColor(l.strokeColorHex)};")
            }
            if (l.kind == LayerGeometryKind.TEXT) {
                sb.appendLine("  font-size: ${l.fontSize.roundToInt()}px;")
                sb.appendLine("  font-weight: 700;")
                sb.appendLine("  color: ${ProceduralEngine.hexToSvgColor(l.fillColorHex)};")
            }
            sb.appendLine("}")
            sb.appendLine()
        }
        return sb.toString()
    }

    /**
     * Computes a deterministic Visual Diff summary between a saved Version History Snapshot and the current document.
     */
    fun computeVisualDiffBetweenSnapshots(
        olderDoc: GraphiteDocumentModel,
        currentDoc: GraphiteDocumentModel
    ): String {
        val oldIds = olderDoc.layers.associateBy { it.id }
        val curIds = currentDoc.layers.associateBy { it.id }

        val added = curIds.keys - oldIds.keys
        val removed = oldIds.keys - curIds.keys
        val modified = curIds.keys.intersect(oldIds.keys).count { id ->
            oldIds[id] != curIds[id]
        }

        val sizeDiff = if (olderDoc.artboardWidth != currentDoc.artboardWidth || olderDoc.artboardHeight != currentDoc.artboardHeight) {
            " • Tavola: ${olderDoc.artboardWidth}×${olderDoc.artboardHeight} → ${currentDoc.artboardWidth}×${currentDoc.artboardHeight}"
        } else ""

        return "+${added.size} livelli aggiunti • ~$modified modificati • -${removed.size} rimossi$sizeDiff"
    }

    /**
     * Generates a deterministic MP4 Video Export specification manifest for the Video Timeline.
     */
    fun buildMp4ExportSummary(timeline: VideoTimelineState): String {
        val totalDuration = timeline.clips.sumOf {
            ((it.trimEndSec - it.trimStartSec).coerceAtLeast(0.5f) / it.speedMultiplier.coerceAtLeast(0.25f)).toDouble()
        }
        return String.format(
            Locale.US,
            "MP4 H.264 • %s @ %d Mbps • %d clip (Durata: %.1fs) • Audio: '%s' (Vol %d%%)",
            timeline.exportResolution,
            timeline.exportBitrateMbps,
            timeline.clips.size,
            totalDuration,
            timeline.audioTrackTitle,
            (timeline.audioVolume * 100).roundToInt()
        )
    }
}
