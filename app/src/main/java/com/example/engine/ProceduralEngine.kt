package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.example.model.BlendModeType
import com.example.model.GraphiteDocumentModel
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.model.ProceduralNode
import com.example.model.ProceduralNodeType
import com.example.model.Vec2Point
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class EvaluatedShadow(
    val offsetX: Float,
    val offsetY: Float,
    val spread: Float,
    val alpha: Float,
    val colorHex: Long
)

data class EvaluatedShapeInstance(
    val layerId: String,
    val kind: LayerGeometryKind,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val rotationDeg: Float,
    val opacity: Float,
    val blendMode: BlendModeType,
    val hasFill: Boolean,
    val fillColorHex: Long,
    val useGradient: Boolean,
    val gradientEndColorHex: Long,
    val gradientAngle: Float,
    val isRadialGradient: Boolean,
    val strokeColorHex: Long,
    val strokeWidth: Float,
    val cornerRadius: Float,
    val polygonSides: Int,
    val starInnerRatio: Float,
    val textContent: String,
    val fontSize: Float,
    val points: List<Vec2Point>,
    val waveAmplitude: Float = 0f,
    val waveFrequency: Float = 0f,
    val wavePhaseDeg: Float = 0f,
    val dashLen: Float = 0f,
    val dashGap: Float = 0f,
    val dashPhase: Float = 0f,
    val shadow: EvaluatedShadow? = null
)

object ProceduralEngine {

    fun evaluateLayer(layer: GraphiteLayer): List<EvaluatedShapeInstance> {
        if (!layer.visible) return emptyList()

        var instances = listOf(
            EvaluatedShapeInstance(
                layerId = layer.id,
                kind = layer.kind,
                centerX = layer.x,
                centerY = layer.y,
                width = max(8f, layer.width),
                height = max(8f, layer.height),
                rotationDeg = layer.rotation,
                opacity = layer.opacity.coerceIn(0.05f, 1f),
                blendMode = layer.blendMode,
                hasFill = layer.hasFill,
                fillColorHex = layer.fillColorHex,
                useGradient = layer.useGradient,
                gradientEndColorHex = layer.gradientEndColorHex,
                gradientAngle = layer.gradientAngle,
                isRadialGradient = layer.isRadialGradient,
                strokeColorHex = layer.strokeColorHex,
                strokeWidth = layer.strokeWidth,
                cornerRadius = layer.cornerRadius,
                polygonSides = layer.polygonSides.coerceIn(3, 24),
                starInnerRatio = layer.starInnerRatio.coerceIn(0.15f, 0.9f),
                textContent = layer.textContent,
                fontSize = layer.fontSize,
                points = layer.points
            )
        )

        for (node in layer.nodes) {
            if (!node.enabled) continue
            instances = applyNode(node, instances)
        }

        return instances
    }

    private fun applyNode(
        node: ProceduralNode,
        input: List<EvaluatedShapeInstance>
    ): List<EvaluatedShapeInstance> {
        val p = node.params
        return when (node.type) {
            ProceduralNodeType.TRANSFORM -> {
                val dx = p["offsetX"] ?: 0f
                val dy = p["offsetY"] ?: 0f
                val sx = (p["scaleX"] ?: 1f).coerceIn(0.1f, 5f)
                val sy = (p["scaleY"] ?: 1f).coerceIn(0.1f, 5f)
                val rot = p["rotate"] ?: 0f
                input.map { inst ->
                    inst.copy(
                        centerX = inst.centerX + dx,
                        centerY = inst.centerY + dy,
                        width = inst.width * sx,
                        height = inst.height * sy,
                        rotationDeg = inst.rotationDeg + rot,
                        points = inst.points.map { pt ->
                            val relX = (pt.x - inst.centerX) * sx + inst.centerX + dx
                            val relY = (pt.y - inst.centerY) * sy + inst.centerY + dy
                            pt.copy(x = relX, y = relY)
                        }
                    )
                }
            }

            ProceduralNodeType.OFFSET_PATH -> {
                val offset = p["offset"] ?: 12f
                val roundness = p["roundness"] ?: 8f
                input.map { inst ->
                    inst.copy(
                        width = max(8f, inst.width + offset * 2f),
                        height = max(8f, inst.height + offset * 2f),
                        cornerRadius = max(0f, inst.cornerRadius + roundness),
                        strokeWidth = max(0f, inst.strokeWidth + (offset * 0.1f).coerceAtLeast(0f))
                    )
                }
            }

            ProceduralNodeType.JITTER_WAVE -> {
                val amp = p["amplitude"] ?: 15f
                val freq = p["frequency"] ?: 4f
                val phase = p["phase"] ?: 0f
                input.map { inst ->
                    inst.copy(
                        waveAmplitude = amp,
                        waveFrequency = freq,
                        wavePhaseDeg = phase
                    )
                }
            }

            ProceduralNodeType.MIRROR_SYMMETRY -> {
                val axisMode = (p["axisMode"] ?: 0f).toInt()
                val gap = p["gap"] ?: 60f
                val result = mutableListOf<EvaluatedShapeInstance>()
                for (inst in input) {
                    when (axisMode) {
                        0 -> { // Horizontal X mirror
                            result.add(
                                inst.copy(
                                    centerX = inst.centerX - gap,
                                    points = inst.points.map { it.copy(x = inst.centerX - gap - (it.x - inst.centerX)) }
                                )
                            )
                            result.add(
                                inst.copy(
                                    centerX = inst.centerX + gap,
                                    rotationDeg = -inst.rotationDeg,
                                    points = inst.points.map { it.copy(x = inst.centerX + gap + (it.x - inst.centerX)) }
                                )
                            )
                        }
                        1 -> { // Vertical Y mirror
                            result.add(
                                inst.copy(
                                    centerY = inst.centerY - gap,
                                    points = inst.points.map { it.copy(y = inst.centerY - gap + (it.y - inst.centerY)) }
                                )
                            )
                            result.add(
                                inst.copy(
                                    centerY = inst.centerY + gap,
                                    rotationDeg = -inst.rotationDeg,
                                    points = inst.points.map { it.copy(y = inst.centerY + gap - (it.y - inst.centerY)) }
                                )
                            )
                        }
                        else -> { // 4-way XY mirror
                            val signs = listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)
                            for ((sx, sy) in signs) {
                                result.add(
                                    inst.copy(
                                        centerX = inst.centerX + sx * gap,
                                        centerY = inst.centerY + sy * gap,
                                        rotationDeg = inst.rotationDeg * sx * sy
                                    )
                                )
                            }
                        }
                    }
                }
                result
            }

            ProceduralNodeType.RADIAL_REPEAT -> {
                val count = (p["count"] ?: 6f).toInt().coerceIn(2, 24)
                val radius = p["radius"] ?: 90f
                val arcDeg = (p["arcDeg"] ?: 360f).coerceIn(15f, 360f)
                val spin = (p["spinCopies"] ?: 1f) >= 0.5f
                val result = mutableListOf<EvaluatedShapeInstance>()
                for (inst in input) {
                    for (i in 0 until count) {
                        val stepDeg = if (arcDeg >= 359f) arcDeg / count else arcDeg / max(1, count - 1)
                        val angleDeg = -90f + i * stepDeg
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val cx = inst.centerX + (cos(rad) * radius).toFloat()
                        val cy = inst.centerY + (sin(rad) * radius).toFloat()
                        val rot = if (spin) inst.rotationDeg + angleDeg + 90f else inst.rotationDeg
                        val dx = cx - inst.centerX
                        val dy = cy - inst.centerY
                        result.add(
                            inst.copy(
                                centerX = cx,
                                centerY = cy,
                                rotationDeg = rot,
                                points = inst.points.map { pt -> pt.copy(x = pt.x + dx, y = pt.y + dy) }
                            )
                        )
                    }
                }
                result
            }

            ProceduralNodeType.GRID_ARRAY -> {
                val cols = (p["columns"] ?: 3f).toInt().coerceIn(1, 8)
                val rows = (p["rows"] ?: 2f).toInt().coerceIn(1, 8)
                val spacingX = p["spacingX"] ?: 110f
                val spacingY = p["spacingY"] ?: 110f
                val scaleStep = (p["scaleStep"] ?: 1.0f).coerceIn(0.4f, 1.4f)
                val result = mutableListOf<EvaluatedShapeInstance>()
                for (inst in input) {
                    var scaleAcc = 1f
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val dx = c * spacingX
                            val dy = r * spacingY
                            result.add(
                                inst.copy(
                                    centerX = inst.centerX + dx,
                                    centerY = inst.centerY + dy,
                                    width = max(8f, inst.width * scaleAcc),
                                    height = max(8f, inst.height * scaleAcc),
                                    points = inst.points.map { pt -> pt.copy(x = pt.x + dx, y = pt.y + dy) }
                                )
                            )
                            scaleAcc = (scaleAcc * scaleStep).coerceIn(0.25f, 2.5f)
                        }
                    }
                }
                result
            }

            ProceduralNodeType.DROP_SHADOW -> {
                val ox = p["offsetX"] ?: 10f
                val oy = p["offsetY"] ?: 14f
                val spread = p["spread"] ?: 12f
                val alpha = (p["alpha"] ?: 0.55f).coerceIn(0.05f, 1f)
                input.map { inst ->
                    inst.copy(
                        shadow = EvaluatedShadow(
                            offsetX = ox,
                            offsetY = oy,
                            spread = spread,
                            alpha = alpha,
                            colorHex = node.shadowColorHex
                        )
                    )
                }
            }

            ProceduralNodeType.COLOR_ADJUST -> {
                val hueShift = p["hueShift"] ?: 0f
                val satMult = p["saturation"] ?: 1f
                val lightDelta = p["lightness"] ?: 0f
                input.map { inst ->
                    inst.copy(
                        fillColorHex = shiftColorHsl(inst.fillColorHex, hueShift, satMult, lightDelta),
                        gradientEndColorHex = shiftColorHsl(inst.gradientEndColorHex, hueShift, satMult, lightDelta),
                        strokeColorHex = shiftColorHsl(inst.strokeColorHex, hueShift * 0.5f, satMult, lightDelta)
                    )
                }
            }

            ProceduralNodeType.STROKE_DASH -> {
                val dashLen = (p["dashLen"] ?: 18f).coerceAtLeast(2f)
                val gapLen = (p["gapLen"] ?: 10f).coerceAtLeast(2f)
                val phase = p["phase"] ?: 0f
                input.map { inst ->
                    inst.copy(
                        dashLen = dashLen,
                        dashGap = gapLen,
                        dashPhase = phase
                    )
                }
            }
        }
    }

    fun shiftColorHsl(colorHex: Long, hueShiftDeg: Float, satMult: Float, lightDelta: Float): Long {
        val argb = colorHex.toInt()
        val alpha = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        val hsl = FloatArray(3)
        ColorUtils.RGBToHSL(r, g, b, hsl)
        hsl[0] = ((hsl[0] + hueShiftDeg) % 360f + 360f) % 360f
        hsl[1] = (hsl[1] * satMult).coerceIn(0f, 1f)
        hsl[2] = (hsl[2] + lightDelta).coerceIn(0f, 1f)
        val outRgb = ColorUtils.HSLToColor(hsl)
        return ((alpha.toLong() shl 24) or (outRgb.toLong() and 0x00FFFFFFL))
    }

    /**
     * Evaluates mathematical expressions like "1080 / 2", "360 / 8", "sin(45) * 100", "pi * 12"
     * inspired by Graphite's `math-parser` crate.
     */
    fun evaluateMathExpression(expr: String, fallback: Float): Float {
        val cleaned = expr.trim()
            .lowercase(Locale.ROOT)
            .replace(",", ".")
            .replace("pi", PI.toString())
            .replace("tau", (PI * 2.0).toString())
        if (cleaned.isEmpty()) return fallback
        cleaned.toFloatOrNull()?.let { return it }
        return try {
            SimpleMathParser(cleaned).parse().toFloat().takeIf { it.isFinite() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private class SimpleMathParser(private val input: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) return x
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val denom = parseFactor()
                        if (abs(denom) > 1e-9) x /= denom
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code) {
                while (ch in 'a'.code..'z'.code) nextChar()
                val func = input.substring(startPos, pos)
                x = parseFactor()
                x = when (func) {
                    "sqrt" -> sqrt(max(0.0, x))
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "abs" -> abs(x)
                    else -> x
                }
            } else {
                x = 0.0
            }
            return x
        }
    }

    /**
     * Builds vertices for Polygon, Star, or Wave-deformed shapes in canvas coordinates.
     */
    fun buildShapeVertices(inst: EvaluatedShapeInstance): List<Pair<Float, Float>> {
        val rx = inst.width / 2f
        val ry = inst.height / 2f
        val basePoints = mutableListOf<Pair<Float, Float>>()

        when (inst.kind) {
            LayerGeometryKind.POLYGON -> {
                val n = inst.polygonSides.coerceIn(3, 24)
                for (i in 0 until n) {
                    val angle = -PI / 2.0 + (2.0 * PI * i) / n
                    val px = inst.centerX + (cos(angle) * rx).toFloat()
                    val py = inst.centerY + (sin(angle) * ry).toFloat()
                    basePoints.add(px to py)
                }
            }
            LayerGeometryKind.STAR -> {
                val pointsCount = inst.polygonSides.coerceIn(3, 24) * 2
                val innerRatio = inst.starInnerRatio.coerceIn(0.15f, 0.9f)
                for (i in 0 until pointsCount) {
                    val rFactor = if (i % 2 == 0) 1f else innerRatio
                    val angle = -PI / 2.0 + (PI * i) / inst.polygonSides.coerceIn(3, 24)
                    val px = inst.centerX + (cos(angle) * rx * rFactor).toFloat()
                    val py = inst.centerY + (sin(angle) * ry * rFactor).toFloat()
                    basePoints.add(px to py)
                }
            }
            LayerGeometryKind.ELLIPSE -> {
                val steps = 48
                for (i in 0 until steps) {
                    val angle = (2.0 * PI * i) / steps
                    var rMod = 1f
                    if (inst.waveAmplitude > 0.5f) {
                        val phaseRad = Math.toRadians(inst.wavePhaseDeg.toDouble())
                        rMod += (sin(angle * inst.waveFrequency + phaseRad) * (inst.waveAmplitude / max(20f, min(rx, ry)))).toFloat()
                    }
                    val px = inst.centerX + (cos(angle) * rx * rMod).toFloat()
                    val py = inst.centerY + (sin(angle) * ry * rMod).toFloat()
                    basePoints.add(px to py)
                }
            }
            else -> {
                // Rectangle or wave-deformed rectangle
                if (inst.waveAmplitude > 0.5f) {
                    val left = inst.centerX - rx
                    val right = inst.centerX + rx
                    val top = inst.centerY - ry
                    val bottom = inst.centerY + ry
                    val steps = 24
                    val phaseRad = Math.toRadians(inst.wavePhaseDeg.toDouble())
                    for (i in 0..steps) {
                        val t = i.toFloat() / steps
                        val x = left + (right - left) * t
                        val waveY = (sin(t * PI * 2.0 * inst.waveFrequency + phaseRad) * inst.waveAmplitude).toFloat()
                        basePoints.add(x to (top + waveY))
                    }
                    basePoints.add(right to bottom)
                    basePoints.add(left to bottom)
                } else {
                    basePoints.add((inst.centerX - rx) to (inst.centerY - ry))
                    basePoints.add((inst.centerX + rx) to (inst.centerY - ry))
                    basePoints.add((inst.centerX + rx) to (inst.centerY + ry))
                    basePoints.add((inst.centerX - rx) to (inst.centerY + ry))
                }
            }
        }
        return basePoints
    }

    /**
     * Generates clean SVG 1.1 markup for the entire GraphiteDocumentModel.
     */
    fun exportToSvg(doc: GraphiteDocumentModel): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${doc.artboardWidth} ${doc.artboardHeight}" width="${doc.artboardWidth}" height="${doc.artboardHeight}">""")
        sb.appendLine("""  <rect width="100%" height="100%" fill="${hexToSvgColor(doc.artboardBgHex)}" />""")

        var gradCounter = 0
        val defsBuilder = StringBuilder()
        val bodyBuilder = StringBuilder()

        for (layer in doc.layers) {
            val evaluated = evaluateLayer(layer)
            for (inst in evaluated) {
                val fillAttr = if (!inst.hasFill || inst.kind == LayerGeometryKind.LINE || inst.kind == LayerGeometryKind.FREEHAND || inst.kind == LayerGeometryKind.SPLINE) {
                    "none"
                } else if (inst.useGradient) {
                    gradCounter++
                    val gradId = "grad_$gradCounter"
                    if (inst.isRadialGradient) {
                        defsBuilder.appendLine("""    <radialGradient id="$gradId" cx="50%" cy="50%" r="50%">""")
                        defsBuilder.appendLine("""      <stop offset="0%" stop-color="${hexToSvgColor(inst.fillColorHex)}" />""")
                        defsBuilder.appendLine("""      <stop offset="100%" stop-color="${hexToSvgColor(inst.gradientEndColorHex)}" />""")
                        defsBuilder.appendLine("""    </radialGradient>""")
                    } else {
                        defsBuilder.appendLine("""    <linearGradient id="$gradId" gradientTransform="rotate(${inst.gradientAngle.toInt()})">""")
                        defsBuilder.appendLine("""      <stop offset="0%" stop-color="${hexToSvgColor(inst.fillColorHex)}" />""")
                        defsBuilder.appendLine("""      <stop offset="100%" stop-color="${hexToSvgColor(inst.gradientEndColorHex)}" />""")
                        defsBuilder.appendLine("""    </linearGradient>""")
                    }
                    "url(#$gradId)"
                } else {
                    hexToSvgColor(inst.fillColorHex)
                }

                val strokeAttr = if (inst.strokeWidth > 0.1f) hexToSvgColor(inst.strokeColorHex) else "none"
                val dashAttr = if (inst.dashLen > 0f && inst.dashGap > 0f) {
                    """ stroke-dasharray="${inst.dashLen.toInt()},${inst.dashGap.toInt()}" """
                } else ""
                val transformAttr = if (abs(inst.rotationDeg) > 0.1f) {
                    """ transform="rotate(${String.format(Locale.US, "%.1f", inst.rotationDeg)} ${inst.centerX.toInt()} ${inst.centerY.toInt()})" """
                } else ""
                val opacityAttr = if (inst.opacity < 0.99f) {
                    """ opacity="${String.format(Locale.US, "%.2f", inst.opacity)}" """
                } else ""

                when (inst.kind) {
                    LayerGeometryKind.RECTANGLE -> {
                        if (inst.waveAmplitude > 0.5f) {
                            val pts = buildShapeVertices(inst).joinToString(" ") { "${it.first.toInt()},${it.second.toInt()}" }
                            bodyBuilder.appendLine("""  <polygon points="$pts" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr />""")
                        } else {
                            val rx = (inst.centerX - inst.width / 2f).toInt()
                            val ry = (inst.centerY - inst.height / 2f).toInt()
                            val cr = inst.cornerRadius.toInt()
                            bodyBuilder.appendLine("""  <rect x="$rx" y="$ry" width="${inst.width.toInt()}" height="${inst.height.toInt()}" rx="$cr" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr />""")
                        }
                    }
                    LayerGeometryKind.ELLIPSE -> {
                        if (inst.waveAmplitude > 0.5f) {
                            val pts = buildShapeVertices(inst).joinToString(" ") { "${it.first.toInt()},${it.second.toInt()}" }
                            bodyBuilder.appendLine("""  <polygon points="$pts" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr />""")
                        } else {
                            bodyBuilder.appendLine("""  <ellipse cx="${inst.centerX.toInt()}" cy="${inst.centerY.toInt()}" rx="${(inst.width / 2f).toInt()}" ry="${(inst.height / 2f).toInt()}" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr />""")
                        }
                    }
                    LayerGeometryKind.POLYGON, LayerGeometryKind.STAR -> {
                        val pts = buildShapeVertices(inst).joinToString(" ") { "${it.first.toInt()},${it.second.toInt()}" }
                        bodyBuilder.appendLine("""  <polygon points="$pts" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}" stroke-linejoin="round"$dashAttr$transformAttr$opacityAttr />""")
                    }
                    LayerGeometryKind.LINE, LayerGeometryKind.BEZIER_PATH, LayerGeometryKind.FREEHAND, LayerGeometryKind.SPLINE, LayerGeometryKind.BRUSH_STROKE -> {
                        val pts = if (inst.points.size >= 2) {
                            inst.points.joinToString(" ") { "${it.x.toInt()},${it.y.toInt()}" }
                        } else {
                            "${(inst.centerX - inst.width / 2f).toInt()},${inst.centerY.toInt()} ${(inst.centerX + inst.width / 2f).toInt()},${inst.centerY.toInt()}"
                        }
                        bodyBuilder.appendLine("""  <polyline points="$pts" fill="none" stroke="${hexToSvgColor(inst.strokeColorHex)}" stroke-width="${max(2f, inst.strokeWidth)}" stroke-linecap="round" stroke-linejoin="round"$dashAttr$transformAttr$opacityAttr />""")
                    }
                    LayerGeometryKind.TEXT -> {
                        bodyBuilder.appendLine("""  <text x="${inst.centerX.toInt()}" y="${inst.centerY.toInt()}" fill="${hexToSvgColor(inst.fillColorHex)}" font-family="sans-serif" font-size="${inst.fontSize.toInt()}" text-anchor="middle" dominant-baseline="central"$transformAttr$opacityAttr>${inst.textContent}</text>""")
                    }
                }
            }
        }

        if (defsBuilder.isNotEmpty()) {
            sb.appendLine("  <defs>")
            sb.append(defsBuilder)
            sb.appendLine("  </defs>")
        }
        sb.append(bodyBuilder)
        sb.appendLine("</svg>")
        return sb.toString()
    }

    /**
     * Renders the GraphiteDocumentModel to an Android Bitmap for PNG export/preview.
     */
    fun renderToBitmap(doc: GraphiteDocumentModel, targetSide: Int = 1024): Bitmap {
        val scale = targetSide.toFloat() / max(1, max(doc.artboardWidth, doc.artboardHeight)).toFloat()
        val bmpW = max(64, (doc.artboardWidth * scale).toInt())
        val bmpH = max(64, (doc.artboardHeight * scale).toInt())
        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = doc.artboardBgHex.toInt()
        }
        canvas.drawRect(0f, 0f, doc.artboardWidth.toFloat(), doc.artboardHeight.toFloat(), bgPaint)

        for (layer in doc.layers) {
            val evaluated = evaluateLayer(layer)
            for (inst in evaluated) {
                canvas.save()
                canvas.rotate(inst.rotationDeg, inst.centerX, inst.centerY)

                val path = buildAndroidPath(inst)

                // Draw procedural shadow if present
                inst.shadow?.let { sh ->
                    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = applyAlpha(sh.colorHex.toInt(), sh.alpha * inst.opacity)
                    }
                    canvas.save()
                    canvas.translate(sh.offsetX, sh.offsetY)
                    canvas.drawPath(path, shadowPaint)
                    canvas.restore()
                }

                if (inst.hasFill && inst.kind != LayerGeometryKind.LINE && inst.kind != LayerGeometryKind.FREEHAND && inst.kind != LayerGeometryKind.SPLINE && inst.kind != LayerGeometryKind.BRUSH_STROKE) {
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        if (inst.useGradient) {
                            val c0 = applyAlpha(inst.fillColorHex.toInt(), inst.opacity)
                            val c1 = applyAlpha(inst.gradientEndColorHex.toInt(), inst.opacity)
                            shader = if (inst.isRadialGradient) {
                                RadialGradient(
                                    inst.centerX,
                                    inst.centerY,
                                    max(12f, max(inst.width, inst.height) * 0.55f),
                                    c0,
                                    c1,
                                    Shader.TileMode.CLAMP
                                )
                            } else {
                                val rad = Math.toRadians(inst.gradientAngle.toDouble())
                                val dx = (cos(rad) * inst.width * 0.5).toFloat()
                                val dy = (sin(rad) * inst.height * 0.5).toFloat()
                                LinearGradient(
                                    inst.centerX - dx,
                                    inst.centerY - dy,
                                    inst.centerX + dx,
                                    inst.centerY + dy,
                                    c0,
                                    c1,
                                    Shader.TileMode.CLAMP
                                )
                            }
                        } else {
                            color = applyAlpha(inst.fillColorHex.toInt(), inst.opacity)
                        }
                    }
                    if (inst.kind == LayerGeometryKind.TEXT) {
                        fillPaint.textSize = inst.fontSize
                        fillPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText(inst.textContent, inst.centerX, inst.centerY + inst.fontSize * 0.35f, fillPaint)
                    } else {
                        canvas.drawPath(path, fillPaint)
                    }
                }

                if (inst.strokeWidth > 0.2f && inst.kind != LayerGeometryKind.TEXT) {
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = inst.strokeWidth
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        color = applyAlpha(inst.strokeColorHex.toInt(), inst.opacity)
                        if (inst.dashLen > 0f && inst.dashGap > 0f) {
                            pathEffect = DashPathEffect(floatArrayOf(inst.dashLen, inst.dashGap), inst.dashPhase)
                        }
                    }
                    canvas.drawPath(path, strokePaint)
                }

                canvas.restore()
            }
        }
        return bitmap
    }

    private fun buildAndroidPath(inst: EvaluatedShapeInstance): Path {
        val path = Path()
        when (inst.kind) {
            LayerGeometryKind.RECTANGLE -> {
                if (inst.waveAmplitude > 0.5f) {
                    val pts = buildShapeVertices(inst)
                    pts.forEachIndexed { idx, (px, py) ->
                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                } else {
                    val rect = RectF(
                        inst.centerX - inst.width / 2f,
                        inst.centerY - inst.height / 2f,
                        inst.centerX + inst.width / 2f,
                        inst.centerY + inst.height / 2f
                    )
                    path.addRoundRect(rect, inst.cornerRadius, inst.cornerRadius, Path.Direction.CW)
                }
            }
            LayerGeometryKind.ELLIPSE -> {
                if (inst.waveAmplitude > 0.5f) {
                    val pts = buildShapeVertices(inst)
                    pts.forEachIndexed { idx, (px, py) ->
                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                } else {
                    val rect = RectF(
                        inst.centerX - inst.width / 2f,
                        inst.centerY - inst.height / 2f,
                        inst.centerX + inst.width / 2f,
                        inst.centerY + inst.height / 2f
                    )
                    path.addOval(rect, Path.Direction.CW)
                }
            }
            LayerGeometryKind.POLYGON, LayerGeometryKind.STAR -> {
                val pts = buildShapeVertices(inst)
                pts.forEachIndexed { idx, (px, py) ->
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            LayerGeometryKind.LINE, LayerGeometryKind.BEZIER_PATH, LayerGeometryKind.FREEHAND, LayerGeometryKind.SPLINE, LayerGeometryKind.BRUSH_STROKE -> {
                if (inst.points.size >= 2) {
                    path.moveTo(inst.points.first().x, inst.points.first().y)
                    for (i in 1 until inst.points.size) {
                        val prev = inst.points[i - 1]
                        val curr = inst.points[i]
                        if (inst.kind == LayerGeometryKind.SPLINE && i < inst.points.size) {
                            val midX = (prev.x + curr.x) / 2f
                            val midY = (prev.y + curr.y) / 2f
                            path.quadTo(prev.x, prev.y, midX, midY)
                        } else {
                            path.lineTo(curr.x, curr.y)
                        }
                    }
                    if (inst.kind == LayerGeometryKind.SPLINE) {
                        path.lineTo(inst.points.last().x, inst.points.last().y)
                    }
                } else {
                    path.moveTo(inst.centerX - inst.width / 2f, inst.centerY)
                    path.lineTo(inst.centerX + inst.width / 2f, inst.centerY)
                }
            }
            LayerGeometryKind.TEXT -> {}
        }
        return path
    }

    private fun applyAlpha(colorInt: Int, factor: Float): Int {
        val baseA = (colorInt ushr 24) and 0xFF
        val newA = (baseA * factor.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return (newA shl 24) or (colorInt and 0x00FFFFFF)
    }

    fun hexToSvgColor(colorHex: Long): String {
        val r = (colorHex ushr 16) and 0xFF
        val g = (colorHex ushr 8) and 0xFF
        val b = colorHex and 0xFF
        return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
    }

    /**
     * Serializes a GraphiteDocumentModel into `.graphite` JSON format for Room storage & file export.
     */
    fun toGraphiteJson(doc: GraphiteDocumentModel): String {
        val root = JSONObject()
        root.put("formatVersion", "1.0-mobile")
        root.put("id", doc.id)
        root.put("name", doc.name)
        root.put("artboardWidth", doc.artboardWidth)
        root.put("artboardHeight", doc.artboardHeight)
        root.put("artboardBgHex", doc.artboardBgHex)
        root.put("updatedAt", doc.updatedAt)
        doc.demoId?.let { root.put("demoId", it) }

        val layersArr = JSONArray()
        for (layer in doc.layers) {
            val lObj = JSONObject()
            lObj.put("id", layer.id)
            lObj.put("name", layer.name)
            lObj.put("kind", layer.kind.name)
            lObj.put("visible", layer.visible)
            lObj.put("locked", layer.locked)
            lObj.put("x", layer.x.toDouble())
            lObj.put("y", layer.y.toDouble())
            lObj.put("width", layer.width.toDouble())
            lObj.put("height", layer.height.toDouble())
            lObj.put("rotation", layer.rotation.toDouble())
            lObj.put("opacity", layer.opacity.toDouble())
            lObj.put("blendMode", layer.blendMode.name)
            lObj.put("hasFill", layer.hasFill)
            lObj.put("fillColorHex", layer.fillColorHex)
            lObj.put("useGradient", layer.useGradient)
            lObj.put("gradientEndColorHex", layer.gradientEndColorHex)
            lObj.put("gradientAngle", layer.gradientAngle.toDouble())
            lObj.put("isRadialGradient", layer.isRadialGradient)
            lObj.put("strokeColorHex", layer.strokeColorHex)
            lObj.put("strokeWidth", layer.strokeWidth.toDouble())
            lObj.put("cornerRadius", layer.cornerRadius.toDouble())
            lObj.put("polygonSides", layer.polygonSides)
            lObj.put("starInnerRatio", layer.starInnerRatio.toDouble())
            lObj.put("textContent", layer.textContent)
            lObj.put("fontSize", layer.fontSize.toDouble())

            val ptsArr = JSONArray()
            for (pt in layer.points) {
                val pObj = JSONObject()
                pObj.put("x", pt.x.toDouble())
                pObj.put("y", pt.y.toDouble())
                pObj.put("inX", pt.handleInX.toDouble())
                pObj.put("inY", pt.handleInY.toDouble())
                pObj.put("outX", pt.handleOutX.toDouble())
                pObj.put("outY", pt.handleOutY.toDouble())
                ptsArr.put(pObj)
            }
            lObj.put("points", ptsArr)

            val nodesArr = JSONArray()
            for (node in layer.nodes) {
                val nObj = JSONObject()
                nObj.put("id", node.id)
                nObj.put("type", node.type.name)
                nObj.put("enabled", node.enabled)
                nObj.put("collapsed", node.collapsed)
                nObj.put("shadowColorHex", node.shadowColorHex)
                val paramsObj = JSONObject()
                for ((k, v) in node.params) {
                    paramsObj.put(k, v.toDouble())
                }
                nObj.put("params", paramsObj)
                nodesArr.put(nObj)
            }
            lObj.put("nodes", nodesArr)
            layersArr.put(lObj)
        }
        root.put("layers", layersArr)
        return root.toString(2)
    }

    fun fromGraphiteJson(jsonStr: String): GraphiteDocumentModel? {
        return try {
            val root = JSONObject(jsonStr)
            val layersArr = root.optJSONArray("layers") ?: JSONArray()
            val layers = mutableListOf<GraphiteLayer>()
            for (i in 0 until layersArr.length()) {
                val lObj = layersArr.getJSONObject(i)
                val ptsArr = lObj.optJSONArray("points") ?: JSONArray()
                val points = mutableListOf<Vec2Point>()
                for (pIdx in 0 until ptsArr.length()) {
                    val pObj = ptsArr.getJSONObject(pIdx)
                    val px = pObj.optDouble("x", 0.0).toFloat()
                    val py = pObj.optDouble("y", 0.0).toFloat()
                    points.add(
                        Vec2Point(
                            x = px,
                            y = py,
                            handleInX = pObj.optDouble("inX", px.toDouble()).toFloat(),
                            handleInY = pObj.optDouble("inY", py.toDouble()).toFloat(),
                            handleOutX = pObj.optDouble("outX", px.toDouble()).toFloat(),
                            handleOutY = pObj.optDouble("outY", py.toDouble()).toFloat()
                        )
                    )
                }

                val nodesArr = lObj.optJSONArray("nodes") ?: JSONArray()
                val nodes = mutableListOf<ProceduralNode>()
                for (nIdx in 0 until nodesArr.length()) {
                    val nObj = nodesArr.getJSONObject(nIdx)
                    val typeName = nObj.optString("type", ProceduralNodeType.TRANSFORM.name)
                    val nodeType = ProceduralNodeType.entries.find { it.name == typeName } ?: ProceduralNodeType.TRANSFORM
                    val paramsObj = nObj.optJSONObject("params")
                    val paramsMap = mutableMapOf<String, Float>()
                    for (spec in nodeType.paramSpecs) {
                        val value = paramsObj?.optDouble(spec.key, spec.defaultVal.toDouble())?.toFloat() ?: spec.defaultVal
                        paramsMap[spec.key] = value
                    }
                    nodes.add(
                        ProceduralNode(
                            id = nObj.optString("id", "node_$nIdx"),
                            type = nodeType,
                            enabled = nObj.optBoolean("enabled", true),
                            collapsed = nObj.optBoolean("collapsed", false),
                            params = paramsMap,
                            shadowColorHex = nObj.optLong("shadowColorHex", 0xFF000000L)
                        )
                    )
                }

                val kindName = lObj.optString("kind", LayerGeometryKind.RECTANGLE.name)
                val kind = LayerGeometryKind.entries.find { it.name == kindName } ?: LayerGeometryKind.RECTANGLE
                val blendName = lObj.optString("blendMode", BlendModeType.NORMAL.name)
                val blend = BlendModeType.entries.find { it.name == blendName } ?: BlendModeType.NORMAL

                layers.add(
                    GraphiteLayer(
                        id = lObj.optString("id", "layer_$i"),
                        name = lObj.optString("name", "Livello ${i + 1}"),
                        kind = kind,
                        visible = lObj.optBoolean("visible", true),
                        locked = lObj.optBoolean("locked", false),
                        x = lObj.optDouble("x", 300.0).toFloat(),
                        y = lObj.optDouble("y", 300.0).toFloat(),
                        width = lObj.optDouble("width", 200.0).toFloat(),
                        height = lObj.optDouble("height", 200.0).toFloat(),
                        rotation = lObj.optDouble("rotation", 0.0).toFloat(),
                        opacity = lObj.optDouble("opacity", 1.0).toFloat(),
                        blendMode = blend,
                        hasFill = lObj.optBoolean("hasFill", true),
                        fillColorHex = lObj.optLong("fillColorHex", 0xFF65BBE5L),
                        useGradient = lObj.optBoolean("useGradient", false),
                        gradientEndColorHex = lObj.optLong("gradientEndColorHex", 0xFF68C587L),
                        gradientAngle = lObj.optDouble("gradientAngle", 45.0).toFloat(),
                        isRadialGradient = lObj.optBoolean("isRadialGradient", false),
                        strokeColorHex = lObj.optLong("strokeColorHex", 0xFFEEEFF2L),
                        strokeWidth = lObj.optDouble("strokeWidth", 3.0).toFloat(),
                        cornerRadius = lObj.optDouble("cornerRadius", 12.0).toFloat(),
                        polygonSides = lObj.optInt("polygonSides", 6),
                        starInnerRatio = lObj.optDouble("starInnerRatio", 0.45).toFloat(),
                        textContent = lObj.optString("textContent", "Graphite"),
                        fontSize = lObj.optDouble("fontSize", 48.0).toFloat(),
                        points = points,
                        nodes = nodes
                    )
                )
            }

            GraphiteDocumentModel(
                id = root.optString("id", "doc-default"),
                name = root.optString("name", "Progetto Graphite"),
                artboardWidth = root.optInt("artboardWidth", 1080),
                artboardHeight = root.optInt("artboardHeight", 1080),
                artboardBgHex = root.optLong("artboardBgHex", 0xFF161920L),
                layers = layers,
                updatedAt = root.optLong("updatedAt", System.currentTimeMillis()),
                demoId = root.optString("demoId", "").takeIf { it.isNotEmpty() }
            )
        } catch (_: Exception) {
            null
        }
    }
}
