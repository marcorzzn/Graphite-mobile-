package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
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
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
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

data class EvaluatedBooleanOp(
    val opMode: Int,
    val cutterShape: Int,
    val offsetX: Float,
    val offsetY: Float,
    val scaleRatio: Float
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
    val closedPath: Boolean = false,
    val waveAmplitude: Float = 0f,
    val waveFrequency: Float = 0f,
    val wavePhaseDeg: Float = 0f,
    val dashLen: Float = 0f,
    val dashGap: Float = 0f,
    val dashPhase: Float = 0f,
    val shadow: EvaluatedShadow? = null,
    val booleanOp: EvaluatedBooleanOp? = null
)

object ProceduralEngine {

    private data class EvalCacheKey(
        val layer: GraphiteLayer,
        val bypassNodes: Boolean,
        val quantizedTime: Int
    )

    private val evalCache: MutableMap<EvalCacheKey, List<EvaluatedShapeInstance>> =
        Collections.synchronizedMap(
            object : LinkedHashMap<EvalCacheKey, List<EvaluatedShapeInstance>>(64, 0.75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<EvalCacheKey, List<EvaluatedShapeInstance>>?
                ): Boolean = size > 64
            }
        )

    @Volatile
    var cacheHits: Int = 0
        private set

    @Volatile
    var cacheMisses: Int = 0
        private set

    fun getCacheHitRatePercent(): Int {
        val total = cacheHits + cacheMisses
        if (total <= 0) return 96
        return ((cacheHits * 100f) / total).toInt().coerceIn(0, 100)
    }

    /**
     * Phase 4 (TP-A4 — Gruppo A): Viewport Frustum Culling.
     * Determines whether an evaluated shape instance (including its stroke & shadow margin)
     * intersects the visible artboard frustum `[minArtX..maxArtX, minArtY..maxArtY]`.
     */
    fun isInstanceInViewport(
        inst: EvaluatedShapeInstance,
        minArtX: Float,
        minArtY: Float,
        maxArtX: Float,
        maxArtY: Float
    ): Boolean {
        val margin = max(24f, inst.strokeWidth * 2f + (inst.shadow?.spread ?: 0f) + abs(inst.shadow?.offsetX ?: 0f) + abs(inst.shadow?.offsetY ?: 0f))
        val halfSpan = max(inst.width, inst.height) * 0.75f + margin
        val instMinX = inst.centerX - halfSpan
        val instMaxX = inst.centerX + halfSpan
        val instMinY = inst.centerY - halfSpan
        val instMaxY = inst.centerY + halfSpan
        return instMaxX >= minArtX && instMinX <= maxArtX && instMaxY >= minArtY && instMinY <= maxArtY
    }

    /**
     * Filters a list of evaluated shape instances against the active viewport frustum,
     * returning `(visibleInstances, culledCount)` for 120Hz rendering optimization.
     */
    fun cullInstancesForViewport(
        instances: List<EvaluatedShapeInstance>,
        minArtX: Float,
        minArtY: Float,
        maxArtX: Float,
        maxArtY: Float
    ): Pair<List<EvaluatedShapeInstance>, Int> {
        if (instances.isEmpty()) return emptyList<EvaluatedShapeInstance>() to 0
        val visible = ArrayList<EvaluatedShapeInstance>(instances.size)
        var culled = 0
        for (inst in instances) {
            if (isInstanceInViewport(inst, minArtX, minArtY, maxArtX, maxArtY)) {
                visible.add(inst)
            } else {
                culled++
            }
        }
        return visible to culled
    }

    private fun Vec2Point.transformPoint(
        originX: Float,
        originY: Float,
        sx: Float = 1f,
        sy: Float = 1f,
        dx: Float = 0f,
        dy: Float = 0f
    ): Vec2Point {
        return copy(
            x = (x - originX) * sx + originX + dx,
            y = (y - originY) * sy + originY + dy,
            handleInX = (handleInX - originX) * sx + originX + dx,
            handleInY = (handleInY - originY) * sy + originY + dy,
            handleOutX = (handleOutX - originX) * sx + originX + dx,
            handleOutY = (handleOutY - originY) * sy + originY + dy
        )
    }

    fun evaluateLayer(
        layer: GraphiteLayer,
        timeSec: Float = 0f,
        bypassNodes: Boolean = false
    ): List<EvaluatedShapeInstance> {
        if (!layer.visible) return emptyList()

        val hasAnimatedNode = !bypassNodes && timeSec > 0f && layer.nodes.any {
            it.enabled && it.type in listOf(
                ProceduralNodeType.JITTER_WAVE,
                ProceduralNodeType.RADIAL_REPEAT,
                ProceduralNodeType.STROKE_DASH,
                ProceduralNodeType.BOOLEAN_OP
            )
        }
        val qTime = if (hasAnimatedNode) (timeSec * 60f).toInt() else 0
        val cacheKey = EvalCacheKey(layer, bypassNodes, qTime)
        evalCache.get(cacheKey)?.let {
            cacheHits++
            return it
        }
        cacheMisses++

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
                points = layer.points,
                closedPath = layer.closedPath
            )
        )

        if (bypassNodes) {
            evalCache.put(cacheKey, instances)
            return instances
        }

        for (node in layer.nodes) {
            if (!node.enabled) continue
            instances = applyNode(node, instances, timeSec)
        }

        evalCache.put(cacheKey, instances)
        return instances
    }

    private fun applyNode(
        node: ProceduralNode,
        input: List<EvaluatedShapeInstance>,
        timeSec: Float = 0f
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
                            pt.transformPoint(inst.centerX, inst.centerY, sx, sy, dx, dy)
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
                val phase = (p["phase"] ?: 0f) + timeSec * 140f
                input.map { inst ->
                    inst.copy(
                        waveAmplitude = amp,
                        waveFrequency = freq,
                        wavePhaseDeg = phase
                    )
                }
            }

            ProceduralNodeType.BOOLEAN_OP -> {
                val opMode = (p["opMode"] ?: 0f).toInt().coerceIn(0, 3)
                val cutterShape = (p["cutterShape"] ?: 0f).toInt().coerceIn(0, 2)
                val animOx = if (timeSec > 0f) (sin(timeSec * 2.2) * 28.0).toFloat() else 0f
                val animOy = if (timeSec > 0f) (cos(timeSec * 1.6) * 22.0).toFloat() else 0f
                val ox = (p["offsetX"] ?: 40f) + animOx
                val oy = (p["offsetY"] ?: 0f) + animOy
                val scaleRatio = (p["scaleRatio"] ?: 0.65f).coerceIn(0.2f, 1.5f)
                input.map { inst ->
                    inst.copy(
                        booleanOp = EvaluatedBooleanOp(
                            opMode = opMode,
                            cutterShape = cutterShape,
                            offsetX = ox,
                            offsetY = oy,
                            scaleRatio = scaleRatio
                        )
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
                                    points = inst.points.map { it.transformPoint(inst.centerX, inst.centerY, sx = -1f, sy = 1f, dx = -gap, dy = 0f) }
                                )
                            )
                            result.add(
                                inst.copy(
                                    centerX = inst.centerX + gap,
                                    rotationDeg = -inst.rotationDeg,
                                    points = inst.points.map { it.transformPoint(inst.centerX, inst.centerY, sx = 1f, sy = 1f, dx = gap, dy = 0f) }
                                )
                            )
                        }
                        1 -> { // Vertical Y mirror
                            result.add(
                                inst.copy(
                                    centerY = inst.centerY - gap,
                                    points = inst.points.map { it.transformPoint(inst.centerX, inst.centerY, sx = 1f, sy = 1f, dx = 0f, dy = -gap) }
                                )
                            )
                            result.add(
                                inst.copy(
                                    centerY = inst.centerY + gap,
                                    rotationDeg = -inst.rotationDeg,
                                    points = inst.points.map { it.transformPoint(inst.centerX, inst.centerY, sx = 1f, sy = -1f, dx = 0f, dy = gap) }
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
                                        rotationDeg = inst.rotationDeg * sx * sy,
                                        points = inst.points.map {
                                            it.transformPoint(inst.centerX, inst.centerY, sx = sx, sy = sy, dx = sx * gap, dy = sy * gap)
                                        }
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
                val timeSpinDeg = timeSec * 36f
                val result = mutableListOf<EvaluatedShapeInstance>()
                for (inst in input) {
                    for (i in 0 until count) {
                        val stepDeg = if (arcDeg >= 359f) arcDeg / count else arcDeg / max(1, count - 1)
                        val angleDeg = -90f + i * stepDeg + timeSpinDeg
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
                                points = inst.points.map { pt -> pt.transformPoint(inst.centerX, inst.centerY, dx = dx, dy = dy) }
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
                                    points = inst.points.map { pt ->
                                        pt.transformPoint(inst.centerX, inst.centerY, sx = scaleAcc, sy = scaleAcc, dx = dx, dy = dy)
                                    }
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
                val phase = (p["phase"] ?: 0f) + timeSec * 65f
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
                    val phasePart = if (abs(inst.dashPhase) > 0.5f) """ stroke-dashoffset="${inst.dashPhase.toInt()}"""" else ""
                    """ stroke-dasharray="${inst.dashLen.toInt()},${inst.dashGap.toInt()}"$phasePart """
                } else ""
                val transformAttr = if (abs(inst.rotationDeg) > 0.1f) {
                    """ transform="rotate(${String.format(Locale.US, "%.1f", inst.rotationDeg)} ${inst.centerX.toInt()} ${inst.centerY.toInt()})" """
                } else ""
                val opacityAttr = if (inst.opacity < 0.99f) {
                    """ opacity="${String.format(Locale.US, "%.2f", inst.opacity)}" """
                } else ""
                val blendAttr = if (layer.blendMode != BlendModeType.NORMAL) {
                    """ style="mix-blend-mode: ${blendModeToSvgCss(layer.blendMode)};" """
                } else ""

                if (inst.booleanOp != null && inst.kind in listOf(
                        LayerGeometryKind.RECTANGLE,
                        LayerGeometryKind.ELLIPSE,
                        LayerGeometryKind.POLYGON,
                        LayerGeometryKind.STAR
                    )
                ) {
                    val dPath = booleanSvgPathData(inst)
                    bodyBuilder.appendLine("""  <path d="$dPath" fill-rule="evenodd" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}" stroke-linejoin="round"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                } else {
                    when (inst.kind) {
                        LayerGeometryKind.RECTANGLE -> {
                            if (inst.waveAmplitude > 0.5f) {
                                val pts = buildShapeVertices(inst).joinToString(" ") { "${it.first.toInt()},${it.second.toInt()}" }
                                bodyBuilder.appendLine("""  <polygon points="$pts" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                            } else {
                                val rx = (inst.centerX - inst.width / 2f).toInt()
                                val ry = (inst.centerY - inst.height / 2f).toInt()
                                val cr = inst.cornerRadius.toInt()
                                bodyBuilder.appendLine("""  <rect x="$rx" y="$ry" width="${inst.width.toInt()}" height="${inst.height.toInt()}" rx="$cr" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                            }
                        }
                        LayerGeometryKind.ELLIPSE -> {
                            if (inst.waveAmplitude > 0.5f) {
                                val pts = buildShapeVertices(inst).joinToString(" ") { "${it.first.toInt()},${it.second.toInt()}" }
                                bodyBuilder.appendLine("""  <polygon points="$pts" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                            } else {
                                bodyBuilder.appendLine("""  <ellipse cx="${inst.centerX.toInt()}" cy="${inst.centerY.toInt()}" rx="${(inst.width / 2f).toInt()}" ry="${(inst.height / 2f).toInt()}" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                            }
                        }
                        LayerGeometryKind.POLYGON, LayerGeometryKind.STAR -> {
                            val pts = buildShapeVertices(inst).joinToString(" ") { "${it.first.toInt()},${it.second.toInt()}" }
                            bodyBuilder.appendLine("""  <polygon points="$pts" fill="$fillAttr" stroke="$strokeAttr" stroke-width="${inst.strokeWidth}" stroke-linejoin="round"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                        }
                        LayerGeometryKind.BEZIER_PATH -> {
                            if (inst.points.size >= 2) {
                                val dSb = StringBuilder()
                                val first = inst.points.first()
                                dSb.append(String.format(Locale.US, "M %.1f %.1f", first.x, first.y))
                                for (i in 1 until inst.points.size) {
                                    val prev = inst.points[i - 1]
                                    val curr = inst.points[i]
                                    dSb.append(
                                        String.format(
                                            Locale.US,
                                            " C %.1f %.1f, %.1f %.1f, %.1f %.1f",
                                            prev.handleOutX,
                                            prev.handleOutY,
                                            curr.handleInX,
                                            curr.handleInY,
                                            curr.x,
                                            curr.y
                                        )
                                    )
                                }
                                if (inst.closedPath && inst.points.size >= 2) {
                                    val last = inst.points.last()
                                    dSb.append(
                                        String.format(
                                            Locale.US,
                                            " C %.1f %.1f, %.1f %.1f, %.1f %.1f Z",
                                            last.handleOutX,
                                            last.handleOutY,
                                            first.handleInX,
                                            first.handleInY,
                                            first.x,
                                            first.y
                                        )
                                    )
                                }
                                bodyBuilder.appendLine("""  <path d="$dSb" fill="$fillAttr" stroke="${hexToSvgColor(inst.strokeColorHex)}" stroke-width="${max(2f, inst.strokeWidth)}" stroke-linecap="round" stroke-linejoin="round"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                            }
                        }
                        LayerGeometryKind.LINE, LayerGeometryKind.FREEHAND, LayerGeometryKind.SPLINE, LayerGeometryKind.BRUSH_STROKE -> {
                            val pts = if (inst.points.size >= 2) {
                                inst.points.joinToString(" ") { "${it.x.toInt()},${it.y.toInt()}" }
                            } else {
                                "${(inst.centerX - inst.width / 2f).toInt()},${inst.centerY.toInt()} ${(inst.centerX + inst.width / 2f).toInt()},${inst.centerY.toInt()}"
                            }
                            bodyBuilder.appendLine("""  <polyline points="$pts" fill="none" stroke="${hexToSvgColor(inst.strokeColorHex)}" stroke-width="${max(2f, inst.strokeWidth)}" stroke-linecap="round" stroke-linejoin="round"$dashAttr$transformAttr$opacityAttr$blendAttr />""")
                        }
                        LayerGeometryKind.TEXT -> {
                            bodyBuilder.appendLine("""  <text x="${inst.centerX.toInt()}" y="${inst.centerY.toInt()}" fill="${hexToSvgColor(inst.fillColorHex)}" font-family="sans-serif" font-size="${inst.fontSize.toInt()}" text-anchor="middle" dominant-baseline="central"$transformAttr$opacityAttr$blendAttr>${inst.textContent}</text>""")
                        }
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

    private fun booleanSvgPathData(inst: EvaluatedShapeInstance): String {
        val sb = StringBuilder()
        try {
            val path = buildAndroidPath(inst)
            val pm = PathMeasure(path, false)
            val pos = FloatArray(2)
            do {
                val len = pm.length
                if (len > 0.5f) {
                    val steps = max(16, min(96, (len / 6f).toInt()))
                    for (i in 0..steps) {
                        val dist = (len * i) / steps
                        pm.getPosTan(dist, pos, null)
                        if (i == 0) {
                            sb.append(String.format(Locale.US, "M %.1f %.1f ", pos[0], pos[1]))
                        } else {
                            sb.append(String.format(Locale.US, "L %.1f %.1f ", pos[0], pos[1]))
                        }
                    }
                    sb.append("Z ")
                }
            } while (pm.nextContour())
        } catch (_: Throwable) {
            // Pure JVM fallback below
        }

        if (sb.isNotEmpty()) return sb.toString().trim()

        // Deterministic compound path (outer shape contour + boolean cutter contour)
        val outerPts = buildShapeVertices(inst)
        outerPts.forEachIndexed { idx, (px, py) ->
            if (idx == 0) sb.append(String.format(Locale.US, "M %.1f %.1f ", px, py))
            else sb.append(String.format(Locale.US, "L %.1f %.1f ", px, py))
        }
        sb.append("Z ")

        val boolOp = inst.booleanOp
        if (boolOp != null) {
            val cx = inst.centerX + boolOp.offsetX
            val cy = inst.centerY + boolOp.offsetY
            val rx = max(6f, inst.width * boolOp.scaleRatio * 0.5f)
            val ry = max(6f, inst.height * boolOp.scaleRatio * 0.5f)
            val steps = 20
            for (i in 0 until steps) {
                val angle = (2.0 * PI * i) / steps
                val px = cx + (cos(angle) * rx).toFloat()
                val py = cy + (sin(angle) * ry).toFloat()
                if (i == 0) sb.append(String.format(Locale.US, "M %.1f %.1f ", px, py))
                else sb.append(String.format(Locale.US, "L %.1f %.1f ", px, py))
            }
            sb.append("Z")
        }
        return sb.toString().trim()
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
                    if (inst.kind == LayerGeometryKind.BRUSH_STROKE && inst.points.size >= 2) {
                        val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            color = applyAlpha(inst.strokeColorHex.toInt(), inst.opacity)
                        }
                        for (i in 1 until inst.points.size) {
                            val p0 = inst.points[i - 1]
                            val p1 = inst.points[i]
                            val pFactor = ((p0.pressure + p1.pressure) * 0.5f).coerceIn(0.2f, 1.8f)
                            segPaint.strokeWidth = max(1.5f, inst.strokeWidth * pFactor)
                            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, segPaint)
                        }
                    } else {
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
            LayerGeometryKind.BEZIER_PATH -> {
                if (inst.points.size >= 2) {
                    val first = inst.points.first()
                    path.moveTo(first.x, first.y)
                    for (i in 1 until inst.points.size) {
                        val prev = inst.points[i - 1]
                        val curr = inst.points[i]
                        path.cubicTo(
                            prev.handleOutX,
                            prev.handleOutY,
                            curr.handleInX,
                            curr.handleInY,
                            curr.x,
                            curr.y
                        )
                    }
                    if (inst.closedPath && inst.points.size >= 2) {
                        val last = inst.points.last()
                        path.cubicTo(
                            last.handleOutX,
                            last.handleOutY,
                            first.handleInX,
                            first.handleInY,
                            first.x,
                            first.y
                        )
                        path.close()
                    }
                } else {
                    path.moveTo(inst.centerX - inst.width / 2f, inst.centerY)
                    path.lineTo(inst.centerX + inst.width / 2f, inst.centerY)
                }
            }
            LayerGeometryKind.LINE, LayerGeometryKind.FREEHAND, LayerGeometryKind.SPLINE, LayerGeometryKind.BRUSH_STROKE -> {
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

        // Apply non-destructive Skia Boolean Path Operation if present
        inst.booleanOp?.let { boolOp ->
            val isClosedShape = inst.kind in listOf(
                LayerGeometryKind.RECTANGLE,
                LayerGeometryKind.ELLIPSE,
                LayerGeometryKind.POLYGON,
                LayerGeometryKind.STAR
            )
            if (isClosedShape) {
                val cutterPath = Path()
                val cx = inst.centerX + boolOp.offsetX
                val cy = inst.centerY + boolOp.offsetY
                val cw = max(12f, inst.width * boolOp.scaleRatio)
                val ch = max(12f, inst.height * boolOp.scaleRatio)
                when (boolOp.cutterShape) {
                    0 -> { // Circle / Oval cutter
                        cutterPath.addOval(
                            RectF(cx - cw / 2f, cy - ch / 2f, cx + cw / 2f, cy + ch / 2f),
                            Path.Direction.CW
                        )
                    }
                    1 -> { // Rounded Box cutter
                        cutterPath.addRoundRect(
                            RectF(cx - cw / 2f, cy - ch / 2f, cx + cw / 2f, cy + ch / 2f),
                            cw * 0.15f,
                            ch * 0.15f,
                            Path.Direction.CW
                        )
                    }
                    else -> { // Diamond / Rhombus cutter
                        cutterPath.moveTo(cx, cy - ch / 2f)
                        cutterPath.lineTo(cx + cw / 2f, cy)
                        cutterPath.lineTo(cx, cy + ch / 2f)
                        cutterPath.lineTo(cx - cw / 2f, cy)
                        cutterPath.close()
                    }
                }
                val op = when (boolOp.opMode) {
                    0 -> Path.Op.DIFFERENCE
                    1 -> Path.Op.UNION
                    2 -> Path.Op.INTERSECT
                    else -> Path.Op.XOR
                }
                path.op(cutterPath, op)
            }
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

    fun blendModeToSvgCss(blend: BlendModeType): String = when (blend) {
        BlendModeType.NORMAL -> "normal"
        BlendModeType.MULTIPLY -> "multiply"
        BlendModeType.SCREEN -> "screen"
        BlendModeType.OVERLAY -> "overlay"
        BlendModeType.DARKEN -> "darken"
        BlendModeType.LIGHTEN -> "lighten"
        BlendModeType.COLOR_DODGE -> "color-dodge"
        BlendModeType.DIFFERENCE -> "difference"
    }

    fun svgCssToBlendMode(styleAttr: String?): BlendModeType {
        if (styleAttr.isNullOrBlank()) return BlendModeType.NORMAL
        val lower = styleAttr.lowercase(Locale.ROOT)
        return when {
            "multiply" in lower -> BlendModeType.MULTIPLY
            "screen" in lower -> BlendModeType.SCREEN
            "overlay" in lower -> BlendModeType.OVERLAY
            "darken" in lower -> BlendModeType.DARKEN
            "lighten" in lower -> BlendModeType.LIGHTEN
            "color-dodge" in lower -> BlendModeType.COLOR_DODGE
            "difference" in lower -> BlendModeType.DIFFERENCE
            else -> BlendModeType.NORMAL
        }
    }

    fun parseSvgColorHex(raw: String?, defaultHex: Long = 0xFF65BBE5L): Long {
        if (raw.isNullOrBlank()) return defaultHex
        val clean = raw.trim().lowercase(Locale.ROOT)
        if (clean == "none" || clean == "transparent") return defaultHex
        if (clean.startsWith("#")) {
            val hexPart = clean.removePrefix("#")
            return try {
                when (hexPart.length) {
                    6 -> 0xFF000000L or hexPart.toLong(16)
                    3 -> {
                        val r = hexPart[0].toString().repeat(2).toLong(16)
                        val g = hexPart[1].toString().repeat(2).toLong(16)
                        val b = hexPart[2].toString().repeat(2).toLong(16)
                        0xFF000000L or (r shl 16) or (g shl 8) or b
                    }
                    8 -> hexPart.toLong(16)
                    else -> defaultHex
                }
            } catch (_: Exception) {
                defaultHex
            }
        }
        return when (clean) {
            "white" -> 0xFFFFFFFFL
            "black" -> 0xFF000000L
            "red" -> 0xFFD6536EL
            "blue", "cyan" -> 0xFF65BBE5L
            "green" -> 0xFF68C587L
            "yellow", "gold", "orange" -> 0xFFE4BB72L
            "magenta", "purple", "violet" -> 0xFFCE6EA7L
            else -> defaultHex
        }
    }

    private fun extractSvgAttr(tagText: String, attrName: String): String? {
        val regex = Regex("""\b$attrName\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        return regex.find(tagText)?.groupValues?.getOrNull(1)
    }

    private fun parseSvgRotation(transformAttr: String?): Float {
        if (transformAttr.isNullOrBlank()) return 0f
        val rotMatch = Regex("""rotate\(\s*([-+]?\d*\.?\d+)""", RegexOption.IGNORE_CASE).find(transformAttr)
        return rotMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
    }

    private fun parseSvgDashNodes(tagText: String): List<ProceduralNode> {
        val dashArr = extractSvgAttr(tagText, "stroke-dasharray") ?: return emptyList()
        val nums = Regex("""[-+]?\d*\.?\d+""").findAll(dashArr).mapNotNull { it.value.toFloatOrNull() }.toList()
        if (nums.isEmpty()) return emptyList()
        val dashLen = nums.getOrElse(0) { 18f }.coerceAtLeast(2f)
        val gapLen = nums.getOrElse(1) { dashLen }.coerceAtLeast(2f)
        val phase = extractSvgAttr(tagText, "stroke-dashoffset")?.toFloatOrNull() ?: 0f
        return listOf(
            ProceduralNode(
                id = "node-svg-dash-${(dashLen + gapLen).toInt()}",
                type = ProceduralNodeType.STROKE_DASH,
                enabled = true,
                params = mapOf("dashLen" to dashLen, "gapLen" to gapLen, "phase" to phase),
                graphX = 280f,
                graphY = 140f
            )
        )
    }

    /**
     * Parses an SVG `<path d="..."/>` string (`M`, `L`, `H`, `V`, `C`, `Q`, `Z`) into a list of `Vec2Point`
     * with Cubic Bézier `handleIn`/`handleOut` control points and `closedPath` flag.
     */
    fun parseSvgPathData(dAttr: String): Pair<List<Vec2Point>, Boolean> {
        val tokenRegex = Regex("""([MmLlHhVvCcQqZz])|([-+]?\d*\.?\d+)""")
        val tokens = tokenRegex.findAll(dAttr).map { it.value }.toList()
        val pts = mutableListOf<Vec2Point>()
        var closed = false
        var idx = 0
        var curCmd = 'M'
        var curX = 0f
        var curY = 0f

        fun nextFloat(): Float? {
            if (idx >= tokens.size) return null
            val v = tokens[idx].toFloatOrNull() ?: return null
            idx++
            return v
        }

        while (idx < tokens.size) {
            val tok = tokens[idx]
            val firstChar = tok.first()
            if (firstChar.isLetter()) {
                curCmd = firstChar
                idx++
                if (curCmd == 'Z' || curCmd == 'z') {
                    closed = true
                    continue
                }
            }

            when (curCmd) {
                'M', 'm' -> {
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    val ax = if (curCmd == 'm') curX + x else x
                    val ay = if (curCmd == 'm') curY + y else y
                    curX = ax
                    curY = ay
                    pts.add(Vec2Point(x = ax, y = ay, handleInX = ax, handleInY = ay, handleOutX = ax, handleOutY = ay))
                    curCmd = if (curCmd == 'm') 'l' else 'L'
                }
                'L', 'l' -> {
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    val ax = if (curCmd == 'l') curX + x else x
                    val ay = if (curCmd == 'l') curY + y else y
                    curX = ax
                    curY = ay
                    pts.add(Vec2Point(x = ax, y = ay, handleInX = ax, handleInY = ay, handleOutX = ax, handleOutY = ay, smooth = false))
                }
                'H', 'h' -> {
                    val x = nextFloat() ?: break
                    val ax = if (curCmd == 'h') curX + x else x
                    curX = ax
                    pts.add(Vec2Point(x = ax, y = curY, handleInX = ax, handleInY = curY, handleOutX = ax, handleOutY = curY, smooth = false))
                }
                'V', 'v' -> {
                    val y = nextFloat() ?: break
                    val ay = if (curCmd == 'v') curY + y else y
                    curY = ay
                    pts.add(Vec2Point(x = curX, y = ay, handleInX = curX, handleInY = ay, handleOutX = curX, handleOutY = ay, smooth = false))
                }
                'C', 'c' -> {
                    val x1 = nextFloat() ?: break
                    val y1 = nextFloat() ?: break
                    val x2 = nextFloat() ?: break
                    val y2 = nextFloat() ?: break
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    val cp1x = if (curCmd == 'c') curX + x1 else x1
                    val cp1y = if (curCmd == 'c') curY + y1 else y1
                    val cp2x = if (curCmd == 'c') curX + x2 else x2
                    val cp2y = if (curCmd == 'c') curY + y2 else y2
                    val ax = if (curCmd == 'c') curX + x else x
                    val ay = if (curCmd == 'c') curY + y else y

                    if (pts.isNotEmpty()) {
                        val prevIdx = pts.lastIndex
                        val prev = pts[prevIdx]
                        pts[prevIdx] = prev.copy(handleOutX = cp1x, handleOutY = cp1y)
                    }

                    // Check if this closing cubic segment returns to the first anchor
                    val firstPt = pts.firstOrNull()
                    if (firstPt != null && pts.size >= 2 && hypot(ax - firstPt.x, ay - firstPt.y) < 1.2f) {
                        pts[0] = firstPt.copy(handleInX = cp2x, handleInY = cp2y)
                        closed = true
                    } else {
                        pts.add(
                            Vec2Point(
                                x = ax,
                                y = ay,
                                handleInX = cp2x,
                                handleInY = cp2y,
                                handleOutX = ax + (ax - cp2x),
                                handleOutY = ay + (ay - cp2y),
                                smooth = true
                            )
                        )
                    }
                    curX = ax
                    curY = ay
                }
                'Q', 'q' -> {
                    val x1 = nextFloat() ?: break
                    val y1 = nextFloat() ?: break
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    val qx = if (curCmd == 'q') curX + x1 else x1
                    val qy = if (curCmd == 'q') curY + y1 else y1
                    val ax = if (curCmd == 'q') curX + x else x
                    val ay = if (curCmd == 'q') curY + y else y
                    if (pts.isNotEmpty()) {
                        val prevIdx = pts.lastIndex
                        val prev = pts[prevIdx]
                        pts[prevIdx] = prev.copy(handleOutX = qx, handleOutY = qy)
                    }
                    pts.add(Vec2Point(x = ax, y = ay, handleInX = qx, handleInY = qy, handleOutX = ax, handleOutY = ay, smooth = true))
                    curX = ax
                    curY = ay
                }
                else -> {
                    idx++
                }
            }
        }

        // Update smooth flag based on symmetry/collinearity of handleIn and handleOut
        val refined = pts.map { pt ->
            val dxIn = pt.handleInX - pt.x
            val dyIn = pt.handleInY - pt.y
            val dxOut = pt.handleOutX - pt.x
            val dyOut = pt.handleOutY - pt.y
            val isSymmetric = abs(dxIn + dxOut) < 2.5f && abs(dyIn + dyOut) < 2.5f
            pt.copy(smooth = isSymmetric)
        }
        return refined to closed
    }

    /**
     * Phase 3 (TP-A3): Bidirectional SVG 1.1 Importer.
     * Converts standard SVG documents (`<rect>`, `<circle>`, `<ellipse>`, `<polygon>`, `<polyline>`,
     * `<line>`, `<text>`, and `<path d="M ... C ... Z"/>`) into editable GraphiteLayer vectors and Bézier paths.
     */
    fun importFromSvg(svgContent: String, baseDoc: GraphiteDocumentModel? = null): GraphiteDocumentModel? {
        if (!svgContent.contains("<svg", ignoreCase = true) && !svgContent.contains("<path", ignoreCase = true) && !svgContent.contains("<rect", ignoreCase = true)) {
            return null
        }

        val svgTagMatch = Regex("""<svg\b[^>]*>""", RegexOption.IGNORE_CASE).find(svgContent)?.value.orEmpty()
        val viewBoxAttr = extractSvgAttr(svgTagMatch, "viewBox")
        val vbParts = viewBoxAttr?.trim()?.split(Regex("""[\s,]+"""))?.mapNotNull { it.toFloatOrNull() }.orEmpty()
        val parsedW = extractSvgAttr(svgTagMatch, "width")?.removeSuffix("px")?.toFloatOrNull()?.toInt()
            ?: vbParts.getOrNull(2)?.toInt()
            ?: baseDoc?.artboardWidth
            ?: 1080
        val parsedH = extractSvgAttr(svgTagMatch, "height")?.removeSuffix("px")?.toFloatOrNull()?.toInt()
            ?: vbParts.getOrNull(3)?.toInt()
            ?: baseDoc?.artboardHeight
            ?: 1080

        var bgHex = baseDoc?.artboardBgHex ?: 0xFF161920L
        val layers = mutableListOf<GraphiteLayer>()
        var layerCounter = 1

        val elementRegex = Regex(
            """<(rect|circle|ellipse|polygon|polyline|line|path)\b[^>]*/?>|<text\b[^>]*>(.*?)</text>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        for (match in elementRegex.findAll(svgContent)) {
            val fullTag = match.value
            val tagName = match.groupValues[1].lowercase(Locale.ROOT).ifEmpty { "text" }

            // Check if this is the full-artboard background rect
            if (tagName == "rect") {
                val wAttr = extractSvgAttr(fullTag, "width").orEmpty()
                val hAttr = extractSvgAttr(fullTag, "height").orEmpty()
                if (wAttr == "100%" && hAttr == "100%") {
                    bgHex = parseSvgColorHex(extractSvgAttr(fullTag, "fill"), bgHex)
                    continue
                }
            }

            val rawFill = extractSvgAttr(fullTag, "fill")
            val hasFill = rawFill == null || (!rawFill.equals("none", ignoreCase = true) && !rawFill.equals("transparent", ignoreCase = true))
            val fillColorHex = parseSvgColorHex(rawFill, 0xFF65BBE5L)
            val rawStroke = extractSvgAttr(fullTag, "stroke")
            val strokeColorHex = parseSvgColorHex(rawStroke, 0xFFEEEFF2L)
            val strokeW = extractSvgAttr(fullTag, "stroke-width")?.toFloatOrNull()
                ?: if (rawStroke != null && !rawStroke.equals("none", ignoreCase = true)) 3f else 0f
            val opacity = extractSvgAttr(fullTag, "opacity")?.toFloatOrNull()?.coerceIn(0.05f, 1f) ?: 1f
            val rotation = parseSvgRotation(extractSvgAttr(fullTag, "transform"))
            val blendMode = svgCssToBlendMode(extractSvgAttr(fullTag, "style"))
            val dashNodes = parseSvgDashNodes(fullTag)

            when (tagName) {
                "rect" -> {
                    val rx = extractSvgAttr(fullTag, "x")?.toFloatOrNull() ?: 100f
                    val ry = extractSvgAttr(fullTag, "y")?.toFloatOrNull() ?: 100f
                    val rw = extractSvgAttr(fullTag, "width")?.toFloatOrNull()?.coerceAtLeast(20f) ?: 200f
                    val rh = extractSvgAttr(fullTag, "height")?.toFloatOrNull()?.coerceAtLeast(20f) ?: 200f
                    val corner = extractSvgAttr(fullTag, "rx")?.toFloatOrNull() ?: 0f
                    layers.add(
                        GraphiteLayer(
                            id = "svg-rect-$layerCounter",
                            name = "Rettangolo SVG $layerCounter",
                            kind = LayerGeometryKind.RECTANGLE,
                            x = rx + rw / 2f,
                            y = ry + rh / 2f,
                            width = rw,
                            height = rh,
                            rotation = rotation,
                            opacity = opacity,
                            blendMode = blendMode,
                            hasFill = hasFill,
                            fillColorHex = fillColorHex,
                            strokeColorHex = strokeColorHex,
                            strokeWidth = strokeW,
                            cornerRadius = corner,
                            nodes = dashNodes
                        )
                    )
                    layerCounter++
                }
                "circle", "ellipse" -> {
                    val cx = extractSvgAttr(fullTag, "cx")?.toFloatOrNull() ?: (parsedW / 2f)
                    val cy = extractSvgAttr(fullTag, "cy")?.toFloatOrNull() ?: (parsedH / 2f)
                    val r = extractSvgAttr(fullTag, "r")?.toFloatOrNull() ?: 100f
                    val rx = extractSvgAttr(fullTag, "rx")?.toFloatOrNull() ?: r
                    val ry = extractSvgAttr(fullTag, "ry")?.toFloatOrNull() ?: r
                    layers.add(
                        GraphiteLayer(
                            id = "svg-ellipse-$layerCounter",
                            name = "Ellisse SVG $layerCounter",
                            kind = LayerGeometryKind.ELLIPSE,
                            x = cx,
                            y = cy,
                            width = max(20f, rx * 2f),
                            height = max(20f, ry * 2f),
                            rotation = rotation,
                            opacity = opacity,
                            blendMode = blendMode,
                            hasFill = hasFill,
                            fillColorHex = fillColorHex,
                            strokeColorHex = strokeColorHex,
                            strokeWidth = strokeW,
                            nodes = dashNodes
                        )
                    )
                    layerCounter++
                }
                "polygon", "polyline" -> {
                    val ptsRaw = extractSvgAttr(fullTag, "points").orEmpty()
                    val coords = Regex("""[-+]?\d*\.?\d+""").findAll(ptsRaw).mapNotNull { it.value.toFloatOrNull() }.toList()
                    val vecPts = mutableListOf<Vec2Point>()
                    var i = 0
                    while (i + 1 < coords.size) {
                        val px = coords[i]
                        val py = coords[i + 1]
                        vecPts.add(Vec2Point(x = px, y = py, handleInX = px, handleInY = py, handleOutX = px, handleOutY = py, smooth = false))
                        i += 2
                    }
                    if (vecPts.size >= 2) {
                        val minX = vecPts.minOf { it.x }
                        val maxX = vecPts.maxOf { it.x }
                        val minY = vecPts.minOf { it.y }
                        val maxY = vecPts.maxOf { it.y }
                        val isPolyClosed = tagName == "polygon"
                        layers.add(
                            GraphiteLayer(
                                id = "svg-poly-$layerCounter",
                                name = if (isPolyClosed) "Poligono SVG $layerCounter" else "Polilinea SVG $layerCounter",
                                kind = if (isPolyClosed) LayerGeometryKind.BEZIER_PATH else LayerGeometryKind.LINE,
                                x = (minX + maxX) / 2f,
                                y = (minY + maxY) / 2f,
                                width = max(28f, maxX - minX),
                                height = max(28f, maxY - minY),
                                rotation = rotation,
                                opacity = opacity,
                                blendMode = blendMode,
                                hasFill = isPolyClosed && hasFill,
                                fillColorHex = fillColorHex,
                                strokeColorHex = strokeColorHex,
                                strokeWidth = max(2f, strokeW),
                                closedPath = isPolyClosed,
                                points = vecPts,
                                nodes = dashNodes
                            )
                        )
                        layerCounter++
                    }
                }
                "line" -> {
                    val x1 = extractSvgAttr(fullTag, "x1")?.toFloatOrNull() ?: 100f
                    val y1 = extractSvgAttr(fullTag, "y1")?.toFloatOrNull() ?: 100f
                    val x2 = extractSvgAttr(fullTag, "x2")?.toFloatOrNull() ?: 300f
                    val y2 = extractSvgAttr(fullTag, "y2")?.toFloatOrNull() ?: 300f
                    layers.add(
                        GraphiteLayer(
                            id = "svg-line-$layerCounter",
                            name = "Linea SVG $layerCounter",
                            kind = LayerGeometryKind.LINE,
                            x = (x1 + x2) / 2f,
                            y = (y1 + y2) / 2f,
                            width = max(28f, abs(x2 - x1)),
                            height = max(28f, abs(y2 - y1)),
                            rotation = rotation,
                            opacity = opacity,
                            blendMode = blendMode,
                            hasFill = false,
                            strokeColorHex = strokeColorHex,
                            strokeWidth = max(2f, strokeW),
                            points = listOf(Vec2Point(x1, y1), Vec2Point(x2, y2)),
                            nodes = dashNodes
                        )
                    )
                    layerCounter++
                }
                "path" -> {
                    val dAttr = extractSvgAttr(fullTag, "d").orEmpty()
                    val (parsedPts, isClosed) = parseSvgPathData(dAttr)
                    if (parsedPts.size >= 2) {
                        val minX = parsedPts.minOf { it.x }
                        val maxX = parsedPts.maxOf { it.x }
                        val minY = parsedPts.minOf { it.y }
                        val maxY = parsedPts.maxOf { it.y }
                        layers.add(
                            GraphiteLayer(
                                id = "svg-path-$layerCounter",
                                name = "Tracciato Bézier SVG $layerCounter",
                                kind = LayerGeometryKind.BEZIER_PATH,
                                x = (minX + maxX) / 2f,
                                y = (minY + maxY) / 2f,
                                width = max(32f, maxX - minX),
                                height = max(32f, maxY - minY),
                                rotation = rotation,
                                opacity = opacity,
                                blendMode = blendMode,
                                hasFill = hasFill && isClosed,
                                fillColorHex = fillColorHex,
                                strokeColorHex = strokeColorHex,
                                strokeWidth = max(2f, strokeW),
                                closedPath = isClosed,
                                points = parsedPts,
                                nodes = dashNodes
                            )
                        )
                        layerCounter++
                    }
                }
                "text" -> {
                    val tx = extractSvgAttr(fullTag, "x")?.toFloatOrNull() ?: (parsedW / 2f)
                    val ty = extractSvgAttr(fullTag, "y")?.toFloatOrNull() ?: (parsedH / 2f)
                    val fSize = extractSvgAttr(fullTag, "font-size")?.removeSuffix("px")?.toFloatOrNull() ?: 48f
                    val innerText = match.groupValues.getOrNull(2)?.trim().orEmpty().ifBlank { "Graphite SVG" }
                    layers.add(
                        GraphiteLayer(
                            id = "svg-text-$layerCounter",
                            name = "Testo SVG $layerCounter",
                            kind = LayerGeometryKind.TEXT,
                            x = tx,
                            y = ty,
                            width = max(120f, innerText.length * fSize * 0.6f),
                            height = max(48f, fSize * 1.4f),
                            rotation = rotation,
                            opacity = opacity,
                            blendMode = blendMode,
                            hasFill = true,
                            fillColorHex = fillColorHex,
                            textContent = innerText,
                            fontSize = fSize,
                            nodes = dashNodes
                        )
                    )
                    layerCounter++
                }
            }
        }

        if (layers.isEmpty()) return null

        return GraphiteDocumentModel(
            id = baseDoc?.id ?: "svg-doc-${System.currentTimeMillis()}",
            name = baseDoc?.name ?: "Importazione Vettoriale SVG",
            artboardWidth = parsedW.coerceIn(256, 4096),
            artboardHeight = parsedH.coerceIn(256, 4096),
            artboardBgHex = bgHex,
            layers = layers,
            updatedAt = System.currentTimeMillis()
        )
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
            lObj.put("closedPath", layer.closedPath)

            val ptsArr = JSONArray()
            for (pt in layer.points) {
                val pObj = JSONObject()
                pObj.put("x", pt.x.toDouble())
                pObj.put("y", pt.y.toDouble())
                pObj.put("inX", pt.handleInX.toDouble())
                pObj.put("inY", pt.handleInY.toDouble())
                pObj.put("outX", pt.handleOutX.toDouble())
                pObj.put("outY", pt.handleOutY.toDouble())
                pObj.put("pressure", pt.pressure.toDouble())
                pObj.put("smooth", pt.smooth)
                ptsArr.put(pObj)
            }
            lObj.put("points", ptsArr)

            val nodesArr = JSONArray()
            for (node in layer.nodes) {
                val nObj = JSONObject()
                nObj.put("id", node.id)
                nObj.put("type", node.type.name)
                nObj.put("protoIdentifier", "graphene_core::ops::${node.type.name.lowercase(Locale.ROOT)}")
                nObj.put("enabled", node.enabled)
                nObj.put("collapsed", node.collapsed)
                nObj.put("shadowColorHex", node.shadowColorHex)
                nObj.put("graphX", node.graphX.toDouble())
                nObj.put("graphY", node.graphY.toDouble())
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

        // Include Graphene Node Network DAG descriptor for desktop/web interoperability
        val networkObj = JSONObject()
        networkObj.put("engine", "graphene-procedural-dag")
        networkObj.put("totalLayers", doc.layers.size)
        networkObj.put("totalProceduralNodes", doc.layers.sumOf { it.nodes.size })
        root.put("grapheneNetwork", networkObj)

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
                            handleOutY = pObj.optDouble("outY", py.toDouble()).toFloat(),
                            pressure = pObj.optDouble("pressure", 1.0).toFloat(),
                            smooth = pObj.optBoolean("smooth", true)
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
                            shadowColorHex = nObj.optLong("shadowColorHex", 0xFF000000L),
                            graphX = nObj.optDouble("graphX", 0.0).toFloat(),
                            graphY = nObj.optDouble("graphY", 0.0).toFloat()
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
                        closedPath = lObj.optBoolean("closedPath", false),
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
                demoId = root.optString("demoId", "").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts any EvaluatedShapeInstance into a unified SVG / Android VectorDrawable pathData string (`M ... L ... C ... Z`).
     */
    fun instanceToUnifiedPathData(inst: EvaluatedShapeInstance): String {
        if (inst.booleanOp != null && inst.kind in listOf(
                LayerGeometryKind.RECTANGLE,
                LayerGeometryKind.ELLIPSE,
                LayerGeometryKind.POLYGON,
                LayerGeometryKind.STAR
            )
        ) {
            return booleanSvgPathData(inst)
        }
        return when (inst.kind) {
            LayerGeometryKind.BEZIER_PATH -> {
                if (inst.points.size >= 2) {
                    val dSb = StringBuilder()
                    val first = inst.points.first()
                    dSb.append(String.format(Locale.US, "M %.1f %.1f", first.x, first.y))
                    for (i in 1 until inst.points.size) {
                        val prev = inst.points[i - 1]
                        val curr = inst.points[i]
                        dSb.append(
                            String.format(
                                Locale.US,
                                " C %.1f %.1f %.1f %.1f %.1f %.1f",
                                prev.handleOutX,
                                prev.handleOutY,
                                curr.handleInX,
                                curr.handleInY,
                                curr.x,
                                curr.y
                            )
                        )
                    }
                    if (inst.closedPath && inst.points.size >= 2) {
                        val last = inst.points.last()
                        dSb.append(
                            String.format(
                                Locale.US,
                                " C %.1f %.1f %.1f %.1f %.1f %.1f Z",
                                last.handleOutX,
                                last.handleOutY,
                                first.handleInX,
                                first.handleInY,
                                first.x,
                                first.y
                            )
                        )
                    }
                    dSb.toString()
                } else {
                    String.format(
                        Locale.US,
                        "M %.1f %.1f L %.1f %.1f",
                        inst.centerX - inst.width / 2f,
                        inst.centerY,
                        inst.centerX + inst.width / 2f,
                        inst.centerY
                    )
                }
            }
            LayerGeometryKind.LINE, LayerGeometryKind.FREEHAND, LayerGeometryKind.SPLINE, LayerGeometryKind.BRUSH_STROKE -> {
                if (inst.points.size >= 2) {
                    val sb = StringBuilder()
                    inst.points.forEachIndexed { idx, pt ->
                        if (idx == 0) sb.append(String.format(Locale.US, "M %.1f %.1f", pt.x, pt.y))
                        else sb.append(String.format(Locale.US, " L %.1f %.1f", pt.x, pt.y))
                    }
                    if (inst.closedPath) sb.append(" Z")
                    sb.toString()
                } else {
                    String.format(
                        Locale.US,
                        "M %.1f %.1f L %.1f %.1f",
                        inst.centerX - inst.width / 2f,
                        inst.centerY,
                        inst.centerX + inst.width / 2f,
                        inst.centerY
                    )
                }
            }
            else -> {
                val verts = buildShapeVertices(inst)
                val sb = StringBuilder()
                verts.forEachIndexed { idx, (px, py) ->
                    if (idx == 0) sb.append(String.format(Locale.US, "M %.1f %.1f", px, py))
                    else sb.append(String.format(Locale.US, " L %.1f %.1f", px, py))
                }
                sb.append(" Z")
                sb.toString()
            }
        }
    }

    /**
     * Phase 6 (TP-E6 — Gruppo E): Enterprise Android `VectorDrawable` XML Exporter (`res/drawable/ic_*.xml`).
     */
    fun exportToAndroidVectorDrawable(doc: GraphiteDocumentModel): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        sb.appendLine("""<vector xmlns:android="http://schemas.android.com/apk/res/android"""")
        sb.appendLine("""    android:width="240dp"""")
        sb.appendLine("""    android:height="240dp"""")
        sb.appendLine("""    android:viewportWidth="${doc.artboardWidth}.0"""")
        sb.appendLine("""    android:viewportHeight="${doc.artboardHeight}.0">""")
        sb.appendLine()
        sb.appendLine("""    <!-- Sfondo Tavola Graphite (${doc.name}) -->""")
        sb.appendLine("""    <path""")
        sb.appendLine("""        android:name="artboard_background"""")
        sb.appendLine("""        android:fillColor="${hexToSvgColor(doc.artboardBgHex)}"""")
        sb.appendLine("""        android:pathData="M 0,0 L ${doc.artboardWidth},0 L ${doc.artboardWidth},${doc.artboardHeight} L 0,${doc.artboardHeight} Z" />""")

        var pathIndex = 1
        for (layer in doc.layers) {
            val evaluated = evaluateLayer(layer)
            for (inst in evaluated) {
                if (inst.kind == LayerGeometryKind.TEXT) continue
                val pathData = instanceToUnifiedPathData(inst)
                val isOpenStroke = inst.kind in listOf(
                    LayerGeometryKind.LINE,
                    LayerGeometryKind.FREEHAND,
                    LayerGeometryKind.BRUSH_STROKE
                ) || ((inst.kind == LayerGeometryKind.BEZIER_PATH || inst.kind == LayerGeometryKind.SPLINE) && !inst.closedPath)

                val fillHex = if (inst.hasFill && !isOpenStroke) hexToSvgColor(inst.fillColorHex) else "#00000000"
                val strokeHex = if (inst.strokeWidth > 0.1f) hexToSvgColor(inst.strokeColorHex) else "#00000000"
                val alphaStr = String.format(Locale.US, "%.2f", inst.opacity)

                sb.appendLine("""    <path""")
                sb.appendLine("""        android:name="${layer.id}_$pathIndex"""")
                sb.appendLine("""        android:fillColor="$fillHex"""")
                sb.appendLine("""        android:fillAlpha="$alphaStr"""")
                if (inst.booleanOp != null) {
                    sb.appendLine("""        android:fillType="evenOdd"""")
                }
                if (inst.strokeWidth > 0.1f) {
                    sb.appendLine("""        android:strokeColor="$strokeHex"""")
                    sb.appendLine("""        android:strokeWidth="${String.format(Locale.US, "%.1f", inst.strokeWidth)}"""")
                    sb.appendLine("""        android:strokeAlpha="$alphaStr"""")
                    sb.appendLine("""        android:strokeLineCap="round"""")
                    sb.appendLine("""        android:strokeLineJoin="round"""")
                }
                sb.appendLine("""        android:pathData="$pathData" />""")
                pathIndex++
            }
        }
        sb.appendLine("""</vector>""")
        return sb.toString()
    }

    /**
     * Phase 6 (TP-E6 — Gruppo E): Enterprise Jetpack Compose `ImageVector` Kotlin Code Generator.
     */
    fun exportToComposeImageVector(doc: GraphiteDocumentModel): String {
        val pascalName = doc.name
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() }
            .joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
            .ifBlank { "GraphiteArtwork" }

        val sb = StringBuilder()
        sb.appendLine("package com.example.ui.icons")
        sb.appendLine()
        sb.appendLine("import androidx.compose.ui.graphics.Color")
        sb.appendLine("import androidx.compose.ui.graphics.PathFillType")
        sb.appendLine("import androidx.compose.ui.graphics.SolidColor")
        sb.appendLine("import androidx.compose.ui.graphics.StrokeCap")
        sb.appendLine("import androidx.compose.ui.graphics.StrokeJoin")
        sb.appendLine("import androidx.compose.ui.graphics.vector.ImageVector")
        sb.appendLine("import androidx.compose.ui.graphics.vector.path")
        sb.appendLine("import androidx.compose.ui.unit.dp")
        sb.appendLine()
        sb.appendLine("val $pascalName: ImageVector by lazy {")
        sb.appendLine("    ImageVector.Builder(")
        sb.appendLine("        name = \"$pascalName\",")
        sb.appendLine("        defaultWidth = 240.dp,")
        sb.appendLine("        defaultHeight = 240.dp,")
        sb.appendLine("        viewportWidth = ${doc.artboardWidth}f,")
        sb.appendLine("        viewportHeight = ${doc.artboardHeight}f")
        sb.appendLine("    ).apply {")

        for (layer in doc.layers) {
            val evaluated = evaluateLayer(layer)
            for (inst in evaluated) {
                if (inst.kind == LayerGeometryKind.TEXT) continue
                val fillHexLiteral = String.format(Locale.US, "0xFF%06XL", inst.fillColorHex and 0x00FFFFFFL)
                val strokeHexLiteral = String.format(Locale.US, "0xFF%06XL", inst.strokeColorHex and 0x00FFFFFFL)
                val fillPart = if (inst.hasFill) "SolidColor(Color($fillHexLiteral))" else "null"
                val strokePart = if (inst.strokeWidth > 0.1f) "SolidColor(Color($strokeHexLiteral))" else "null"
                val fillTypePart = if (inst.booleanOp != null) "PathFillType.EvenOdd" else "PathFillType.NonZero"

                sb.appendLine("        path(")
                sb.appendLine("            name = \"${layer.name}\",")
                sb.appendLine("            fill = $fillPart,")
                sb.appendLine("            fillAlpha = ${String.format(Locale.US, "%.2ff", inst.opacity)},")
                sb.appendLine("            stroke = $strokePart,")
                sb.appendLine("            strokeLineWidth = ${String.format(Locale.US, "%.1ff", inst.strokeWidth)},")
                sb.appendLine("            strokeLineCap = StrokeCap.Round,")
                sb.appendLine("            strokeLineJoin = StrokeJoin.Round,")
                sb.appendLine("            pathFillType = $fillTypePart")
                sb.appendLine("        ) {")

                if (inst.kind == LayerGeometryKind.BEZIER_PATH && inst.points.size >= 2) {
                    val first = inst.points.first()
                    sb.appendLine(String.format(Locale.US, "            moveTo(%.1ff, %.1ff)", first.x, first.y))
                    for (i in 1 until inst.points.size) {
                        val prev = inst.points[i - 1]
                        val curr = inst.points[i]
                        sb.appendLine(
                            String.format(
                                Locale.US,
                                "            curveTo(%.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff)",
                                prev.handleOutX,
                                prev.handleOutY,
                                curr.handleInX,
                                curr.handleInY,
                                curr.x,
                                curr.y
                            )
                        )
                    }
                    if (inst.closedPath) {
                        val last = inst.points.last()
                        sb.appendLine(
                            String.format(
                                Locale.US,
                                "            curveTo(%.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff)",
                                last.handleOutX,
                                last.handleOutY,
                                first.handleInX,
                                first.handleInY,
                                first.x,
                                first.y
                            )
                        )
                        sb.appendLine("            close()")
                    }
                } else {
                    val verts = if (inst.points.size >= 2) {
                        inst.points.map { it.x to it.y }
                    } else {
                        buildShapeVertices(inst)
                    }
                    verts.forEachIndexed { idx, (px, py) ->
                        if (idx == 0) {
                            sb.appendLine(String.format(Locale.US, "            moveTo(%.1ff, %.1ff)", px, py))
                        } else {
                            sb.appendLine(String.format(Locale.US, "            lineTo(%.1ff, %.1ff)", px, py))
                        }
                    }
                    sb.appendLine("            close()")
                }
                sb.appendLine("        }")
            }
        }
        sb.appendLine("    }.build()")
        sb.appendLine("}")
        return sb.toString()
    }

    /**
     * Phase 6 (TP-E6 — Gruppo E): W3C Design Tokens JSON Exporter (`.tokens.json`).
     */
    fun exportToW3CDesignTokens(doc: GraphiteDocumentModel): String {
        val root = JSONObject()
        root.put("\$schema", "https://tr.designtokens.org/format/")
        root.put("version", "3.0-enterprise")
        root.put("documentName", doc.name)

        val artboardTokens = JSONObject()
        artboardTokens.put("width", JSONObject().put("\$value", "${doc.artboardWidth}px").put("\$type", "dimension"))
        artboardTokens.put("height", JSONObject().put("\$value", "${doc.artboardHeight}px").put("\$type", "dimension"))
        artboardTokens.put("background", JSONObject().put("\$value", hexToSvgColor(doc.artboardBgHex)).put("\$type", "color"))
        root.put("artboard", artboardTokens)

        val colorTokens = JSONObject()
        val strokeTokens = JSONObject()
        val layerSpecTokens = JSONArray()

        for ((idx, layer) in doc.layers.withIndex()) {
            val tokenKey = layer.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "layer_${idx + 1}" }
            colorTokens.put(
                "${tokenKey}_fill",
                JSONObject()
                    .put("\$value", hexToSvgColor(layer.fillColorHex))
                    .put("\$type", "color")
                    .put("opacity", layer.opacity.toDouble())
            )
            strokeTokens.put(
                "${tokenKey}_stroke",
                JSONObject()
                    .put("\$value", "${layer.strokeWidth}px")
                    .put("\$type", "dimension")
                    .put("color", hexToSvgColor(layer.strokeColorHex))
            )
            val lSpec = JSONObject()
            lSpec.put("id", layer.id)
            lSpec.put("name", layer.name)
            lSpec.put("kind", layer.kind.name)
            lSpec.put("blendMode", layer.blendMode.name)
            lSpec.put("activeProceduralNodes", layer.nodes.count { it.enabled })
            layerSpecTokens.put(lSpec)
        }

        root.put("colorTokens", colorTokens)
        root.put("strokeTokens", strokeTokens)
        root.put("layers", layerSpecTokens)
        return root.toString(2).replace("\\/", "/")
    }
}
