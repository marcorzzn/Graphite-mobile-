package com.example.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.EvaluatedShapeInstance
import com.example.engine.ProceduralEngine
import com.example.model.ABVariantChoice
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.model.StudioWorkspaceMode
import com.example.model.ToolType
import com.example.model.Vec2Point
import com.example.ui.theme.DataArtboard
import com.example.ui.theme.DataGraphic
import com.example.ui.theme.DataNumber
import com.example.ui.theme.DataRaster
import com.example.ui.theme.DataVector
import com.example.ui.theme.GraphiteDarkGray
import com.example.ui.theme.GraphiteDimGray
import com.example.ui.theme.GraphiteLightGray
import com.example.ui.theme.GraphiteMildBlack
import com.example.ui.theme.GraphiteNearBlack
import com.example.ui.theme.GraphiteNearWhite
import com.example.ui.theme.GraphitePanelBg
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.GizmoHandleType
import com.example.viewmodel.GraphiteViewModel
import com.example.viewmodel.MobileWorkspaceTab
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CanvasWorkspaceScreen(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel,
    modifier: Modifier = Modifier
) {
    val currentUiState by rememberUpdatedState(uiState)
    val doc = uiState.document
    val selectedLayer = uiState.selectedLayer

    // Interactive gesture states for shape creation, freehand drawing & gizmo handles
    var dragStartArtboard by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentArtboard by remember { mutableStateOf<Offset?>(null) }
    val activeStrokePoints = remember { mutableStateListOf<Vec2Point>() }
    var draggingVertexIndex by remember { mutableStateOf<Int?>(null) }
    var draggingBezierHandle by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var activeGizmoHandle by remember { mutableStateOf<GizmoHandleType?>(null) }
    var currentStylusPressure by remember { mutableFloatStateOf(1.0f) }

    // Real-time Performance Telemetry metrics & Procedural Animation Clock (Phase 4 TP-A4)
    var lastEvalTimeMs by remember { mutableFloatStateOf(0.8f) }
    var totalEvaluatedInstances by remember { mutableIntStateOf(0) }
    var culledInstancesCount by remember { mutableIntStateOf(0) }
    var animTimeSec by remember { mutableFloatStateOf(0f) }
    var draggingSplitCurtain by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isPlayingAnimation) {
        if (uiState.isPlayingAnimation) {
            var lastNanos = withFrameNanos { it }
            while (uiState.isPlayingAnimation) {
                withFrameNanos { now ->
                    val dt = ((now - lastNanos) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
                    lastNanos = now
                    animTimeSec += dt
                }
            }
        }
    }

    // Canvas layout transform cache so pointer inputs map accurately to Artboard coordinates
    var currentScale by remember { mutableFloatStateOf(1f) }
    var currentOriginX by remember { mutableFloatStateOf(0f) }
    var currentOriginY by remember { mutableFloatStateOf(0f) }

    fun screenToArtboard(screenOffset: Offset): Offset {
        val ax = (screenOffset.x - currentOriginX) / max(0.05f, currentScale)
        val ay = (screenOffset.y - currentOriginY) / max(0.05f, currentScale)
        return Offset(ax, ay)
    }

    fun hitTestGizmoHandle(layer: GraphiteLayer, artPos: Offset, hitRadiusArtboard: Float): GizmoHandleType? {
        if (!layer.visible || layer.locked) return null
        // Inverse-rotate pointer into the layer's local Oriented Bounding Box (OBB) space
        val dx = artPos.x - layer.x
        val dy = artPos.y - layer.y
        val rad = -Math.toRadians(layer.rotation.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val localPos = Offset(
            x = layer.x + dx * cosA - dy * sinA,
            y = layer.y + dx * sinA + dy * cosA
        )

        val halfW = layer.width / 2f
        val halfH = layer.height / 2f
        val left = layer.x - halfW
        val right = layer.x + halfW
        val top = layer.y - halfH
        val bottom = layer.y + halfH
        val rotStemY = top - 42f / max(0.25f, currentScale / 0.5f).coerceIn(0.4f, 2.5f)

        val candidates = listOf(
            GizmoHandleType.ROTATE to Offset(layer.x, rotStemY),
            GizmoHandleType.TOP_LEFT to Offset(left, top),
            GizmoHandleType.TOP_RIGHT to Offset(right, top),
            GizmoHandleType.BOTTOM_RIGHT to Offset(right, bottom),
            GizmoHandleType.BOTTOM_LEFT to Offset(left, bottom),
            GizmoHandleType.TOP_EDGE to Offset(layer.x, top),
            GizmoHandleType.BOTTOM_EDGE to Offset(layer.x, bottom),
            GizmoHandleType.LEFT_EDGE to Offset(left, layer.y),
            GizmoHandleType.RIGHT_EDGE to Offset(right, layer.y)
        )
        return candidates.firstOrNull { (_, pos) ->
            hypot(pos.x - localPos.x, pos.y - localPos.y) <= hitRadiusArtboard
        }?.first
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteNearBlack)
    ) {
        // 1. Unified Multi-Touch (2-Finger Pinch/Pan + 2/3-Finger Undo/Redo) & 1-Finger/Stylus Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("graphite_2d_canvas")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val startScreenPos = firstDown.position
                        val startArtPos = screenToArtboard(startScreenPos)
                        currentStylusPressure = firstDown.pressure.coerceIn(0.15f, 1.5f)

                        var maxPointersSeen = 1
                        var totalPanMovement = 0f
                        var isSingleFingerDragging = false

                        val stateAtStart = currentUiState
                        val selLayerAtStart = stateAtStart.selectedLayer
                        val hitRadiusArt = 34f / max(0.15f, currentScale)

                        // Check if 1st finger grabbed the Before/After Split Curtain Divider
                        if (stateAtStart.compareSplitMode) {
                            val splitArtX = stateAtStart.document.artboardWidth * stateAtStart.compareSplitFraction
                            if (abs(startArtPos.x - splitArtX) <= hitRadiusArt * 1.4f) {
                                draggingSplitCurtain = true
                            }
                        }

                        // Check if 1st finger grabbed a Transform Cage Gizmo Handle, Cubic Bézier Tangent Handle, or Path Vertex
                        if (!draggingSplitCurtain && stateAtStart.activeTool in listOf(ToolType.SELECT, ToolType.PATH, ToolType.PEN, ToolType.SPLINE) && selLayerAtStart != null) {
                            if (selLayerAtStart.points.isNotEmpty()) {
                                // 1. Hit-test Cubic Bézier Tangent Handles (handleIn / handleOut) first when visible
                                if (selLayerAtStart.kind == LayerGeometryKind.BEZIER_PATH) {
                                    var foundHandle: Pair<Int, Boolean>? = null
                                    for (idx in selLayerAtStart.points.indices) {
                                        val pt = selLayerAtStart.points[idx]
                                        if (hypot(pt.handleOutX - startArtPos.x, pt.handleOutY - startArtPos.y) <= hitRadiusArt) {
                                            foundHandle = idx to false
                                            break
                                        }
                                        if (hypot(pt.handleInX - startArtPos.x, pt.handleInY - startArtPos.y) <= hitRadiusArt) {
                                            foundHandle = idx to true
                                            break
                                        }
                                    }
                                    draggingBezierHandle = foundHandle
                                }
                                // 2. Hit-test Path Anchor Vertices
                                if (draggingBezierHandle == null && (stateAtStart.activeTool in listOf(ToolType.PATH, ToolType.PEN, ToolType.SPLINE) || selLayerAtStart.kind == LayerGeometryKind.BEZIER_PATH)) {
                                    val vIdx = selLayerAtStart.points.indexOfFirst {
                                        hypot(it.x - startArtPos.x, it.y - startArtPos.y) <= hitRadiusArt
                                    }
                                    draggingVertexIndex = if (vIdx >= 0) vIdx else null
                                }
                            }
                            if (draggingBezierHandle == null && draggingVertexIndex == null) {
                                activeGizmoHandle = hitTestGizmoHandle(selLayerAtStart, startArtPos, hitRadiusArt)
                            }
                        }

                        if (!draggingSplitCurtain && activeGizmoHandle == null && draggingVertexIndex == null && draggingBezierHandle == null) {
                            when (stateAtStart.activeTool) {
                                ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE -> {
                                    dragStartArtboard = startArtPos
                                    dragCurrentArtboard = startArtPos
                                }
                                ToolType.FREEHAND, ToolType.BRUSH -> {
                                    // Patch v3.5: Defer seeding activeStrokePoints until single-finger drag is confirmed
                                    // so 2-finger pinch-to-zoom never leaves an accidental ghost dot on canvas.
                                    activeStrokePoints.clear()
                                }
                                else -> {}
                            }
                        }

                        var lastArtPos = startArtPos

                        do {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { it.pressed }
                            val pointerCount = pressedChanges.size
                            if (pointerCount > maxPointersSeen) {
                                maxPointersSeen = pointerCount
                            }

                            if (pointerCount >= 2) {
                                // Cancel any ongoing 1-finger draw/gizmo if a 2nd finger touches down
                                dragStartArtboard = null
                                dragCurrentArtboard = null
                                activeStrokePoints.clear()
                                activeGizmoHandle = null
                                draggingVertexIndex = null
                                draggingBezierHandle = null

                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                totalPanMovement += hypot(panChange.x, panChange.y)

                                if (abs(zoomChange - 1f) > 0.005f || hypot(panChange.x, panChange.y) > 1.5f) {
                                    val st = currentUiState
                                    viewModel.updateViewport(
                                        zoom = st.zoom * zoomChange,
                                        panX = st.panX + panChange.x,
                                        panY = st.panY + panChange.y
                                    )
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pointerCount == 1 && maxPointersSeen == 1) {
                                val change = pressedChanges.first()
                                currentStylusPressure = change.pressure.coerceIn(0.15f, 1.5f)
                                val screenDelta = change.position - change.previousPosition
                                val screenDist = hypot(change.position.x - startScreenPos.x, change.position.y - startScreenPos.y)
                                if (screenDist > 10f) {
                                    isSingleFingerDragging = true
                                }

                                if (isSingleFingerDragging) {
                                    val artPos = screenToArtboard(change.position)
                                    val artDeltaX = artPos.x - lastArtPos.x
                                    val artDeltaY = artPos.y - lastArtPos.y
                                    lastArtPos = artPos
                                    change.consume()

                                    val gHandle = activeGizmoHandle
                                    val bHandle = draggingBezierHandle
                                    val vIdx = draggingVertexIndex

                                    if (draggingSplitCurtain) {
                                        val frac = artPos.x / max(1f, currentUiState.document.artboardWidth.toFloat())
                                        viewModel.updateCompareSplitFraction(frac)
                                    } else if (bHandle != null) {
                                        viewModel.moveBezierHandle(
                                            pointIndex = bHandle.first,
                                            isHandleIn = bHandle.second,
                                            newX = artPos.x,
                                            newY = artPos.y,
                                            isFinished = false
                                        )
                                    } else if (gHandle != null) {
                                        viewModel.transformSelectedLayerViaGizmo(
                                            handle = gHandle,
                                            artboardX = artPos.x,
                                            artboardY = artPos.y,
                                            deltaX = artDeltaX,
                                            deltaY = artDeltaY,
                                            isFinished = false
                                        )
                                    } else if (vIdx != null) {
                                        viewModel.movePathVertex(vIdx, artPos.x, artPos.y, isFinished = false)
                                    } else {
                                        val st = currentUiState
                                        when (st.activeTool) {
                                            ToolType.NAVIGATE -> {
                                                viewModel.updateViewport(
                                                    zoom = st.zoom,
                                                    panX = st.panX + screenDelta.x,
                                                    panY = st.panY + screenDelta.y
                                                )
                                            }
                                            ToolType.SELECT, ToolType.PATH -> {
                                                viewModel.moveSelectedLayerBy(artDeltaX, artDeltaY, isFinished = false)
                                            }
                                            ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE -> {
                                                dragCurrentArtboard = artPos
                                            }
                                            ToolType.FREEHAND, ToolType.BRUSH -> {
                                                if (activeStrokePoints.isEmpty()) {
                                                    activeStrokePoints.add(
                                                        Vec2Point(
                                                            x = startArtPos.x,
                                                            y = startArtPos.y,
                                                            pressure = currentStylusPressure
                                                        )
                                                    )
                                                }
                                                val lastPt = activeStrokePoints.lastOrNull()
                                                if (lastPt == null || hypot(artPos.x - lastPt.x, artPos.y - lastPt.y) >= 7f) {
                                                    activeStrokePoints.add(
                                                        Vec2Point(
                                                            x = artPos.x,
                                                            y = artPos.y,
                                                            pressure = currentStylusPressure
                                                        )
                                                    )
                                                }
                                            }
                                            else -> {
                                                viewModel.moveSelectedLayerBy(artDeltaX, artDeltaY, isFinished = false)
                                            }
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        val elapsedMs = System.currentTimeMillis() - downTime

                        // Handle 2-Finger Tap (Undo) and 3-Finger Tap (Redo)
                        if (maxPointersSeen == 2 && elapsedMs < 280L && totalPanMovement < 28f) {
                            viewModel.undo(fromGesture = true)
                        } else if (maxPointersSeen >= 3 && elapsedMs < 320L && totalPanMovement < 36f) {
                            viewModel.redo(fromGesture = true)
                        } else if (maxPointersSeen == 1) {
                            val gHandle = activeGizmoHandle
                            val bHandle = draggingBezierHandle
                            val vIdx = draggingVertexIndex

                            if (!isSingleFingerDragging) {
                                // Clean 1-finger tap on canvas
                                viewModel.handleCanvasTap(startArtPos.x, startArtPos.y)
                            } else if (bHandle != null) {
                                viewModel.moveBezierHandle(
                                    pointIndex = bHandle.first,
                                    isHandleIn = bHandle.second,
                                    newX = lastArtPos.x,
                                    newY = lastArtPos.y,
                                    isFinished = true
                                )
                            } else if (gHandle != null) {
                                viewModel.transformSelectedLayerViaGizmo(
                                    handle = gHandle,
                                    artboardX = lastArtPos.x,
                                    artboardY = lastArtPos.y,
                                    deltaX = 0f,
                                    deltaY = 0f,
                                    isFinished = true
                                )
                            } else if (vIdx != null) {
                                val sel = currentUiState.selectedLayer
                                if (sel != null && vIdx in sel.points.indices) {
                                    val pt = sel.points[vIdx]
                                    viewModel.movePathVertex(vIdx, pt.x, pt.y, isFinished = true)
                                }
                            } else {
                                val st = currentUiState
                                when (st.activeTool) {
                                    ToolType.SELECT, ToolType.PATH -> {
                                        viewModel.moveSelectedLayerBy(0f, 0f, isFinished = true)
                                    }
                                    ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE -> {
                                        val s = dragStartArtboard
                                        val e = dragCurrentArtboard
                                        if (s != null && e != null && hypot(e.x - s.x, e.y - s.y) > 16f) {
                                            viewModel.createShapeFromDrag(s.x, s.y, e.x, e.y)
                                        }
                                    }
                                    ToolType.FREEHAND, ToolType.BRUSH -> {
                                        if (activeStrokePoints.size >= 2) {
                                            viewModel.addFreehandOrBrushStroke(
                                                points = activeStrokePoints.toList(),
                                                isBrush = st.activeTool == ToolType.BRUSH
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }

                        dragStartArtboard = null
                        dragCurrentArtboard = null
                        activeStrokePoints.clear()
                        activeGizmoHandle = null
                        draggingVertexIndex = null
                        draggingBezierHandle = null
                        draggingSplitCurtain = false
                    }
                }
        ) {
            val availW = size.width - 32.dp.toPx()
            val availH = size.height - 230.dp.toPx()
            val fitScale = min(
                availW / max(1f, doc.artboardWidth.toFloat()),
                availH / max(1f, doc.artboardHeight.toFloat())
            ).coerceAtLeast(0.1f)
            val totalScale = fitScale * uiState.zoom
            val originX = (size.width - doc.artboardWidth * totalScale) / 2f + uiState.panX
            val originY = (size.height - 80.dp.toPx() - doc.artboardHeight * totalScale) / 2f + uiState.panY

            currentScale = totalScale
            currentOriginX = originX
            currentOriginY = originY

            val t0 = System.nanoTime()
            var instanceCounter = 0
            var culledCounter = 0
            val viewMinArtX = (-originX) / max(0.05f, totalScale)
            val viewMinArtY = (-originY) / max(0.05f, totalScale)
            val viewMaxArtX = (size.width - originX) / max(0.05f, totalScale)
            val viewMaxArtY = (size.height - originY) / max(0.05f, totalScale)

            withTransform({
                translate(left = originX, top = originY)
                scale(scaleX = totalScale, scaleY = totalScale, pivot = Offset.Zero)
            }) {
                // Artboard shadow & background
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(12f, 18f),
                    size = Size(doc.artboardWidth.toFloat(), doc.artboardHeight.toFloat()),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRect(
                    color = Color(doc.artboardBgHex.toInt()),
                    topLeft = Offset.Zero,
                    size = Size(doc.artboardWidth.toFloat(), doc.artboardHeight.toFloat())
                )

                // Optional Procedural Grid
                if (uiState.showGrid) {
                    val gridStep = 100f
                    var gx = gridStep
                    while (gx < doc.artboardWidth) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, doc.artboardHeight.toFloat()),
                            strokeWidth = 1f / totalScale
                        )
                        gx += gridStep
                    }
                    var gy = gridStep
                    while (gy < doc.artboardHeight) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, gy),
                            end = Offset(doc.artboardWidth.toFloat(), gy),
                            strokeWidth = 1f / totalScale
                        )
                        gy += gridStep
                    }
                }

                // Render all visible layers through the non-destructive ProceduralEngine
                val currentAnimSec = if (uiState.isPlayingAnimation) animTimeSec else 0f

                if (uiState.compareSplitMode) {
                    val splitX = doc.artboardWidth * uiState.compareSplitFraction
                    // Left Side: PRIMA (Raw Base Geometry without Procedural Nodes)
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = splitX,
                        bottom = doc.artboardHeight.toFloat()
                    ) {
                        for (layer in doc.layers) {
                            val baseInstances = ProceduralEngine.evaluateLayer(
                                layer = layer,
                                timeSec = currentAnimSec,
                                bypassNodes = true
                            )
                            for (inst in baseInstances) {
                                drawEvaluatedInstance(
                                    inst = inst,
                                    wireframeOnly = uiState.wireframeOverlay,
                                    invScale = 1f / totalScale
                                )
                            }
                        }
                    }

                    // Right Side: DOPO (Full Procedural Node Graph Evaluation)
                    clipRect(
                        left = splitX,
                        top = 0f,
                        right = doc.artboardWidth.toFloat(),
                        bottom = doc.artboardHeight.toFloat()
                    ) {
                        for (layer in doc.layers) {
                            val evaluatedInstances = ProceduralEngine.evaluateLayer(
                                layer = layer,
                                timeSec = currentAnimSec,
                                bypassNodes = false
                            )
                            instanceCounter += evaluatedInstances.size
                            for (inst in evaluatedInstances) {
                                drawEvaluatedInstance(
                                    inst = inst,
                                    wireframeOnly = uiState.wireframeOverlay,
                                    invScale = 1f / totalScale
                                )
                            }
                        }
                    }

                    // Vertical Split Curtain Line + Knob
                    val lineStroke = 3f / totalScale
                    drawLine(
                        color = DataGraphic,
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, doc.artboardHeight.toFloat()),
                        strokeWidth = lineStroke
                    )
                    val handleCenter = Offset(splitX, doc.artboardHeight * 0.5f)
                    val knobR = 18f / totalScale
                    drawCircle(
                        color = DataGraphic,
                        radius = knobR,
                        center = handleCenter
                    )
                    drawCircle(
                        color = GraphiteNearBlack,
                        radius = knobR * 0.55f,
                        center = handleCenter
                    )
                } else {
                    for (layer in doc.layers) {
                        val evaluatedInstances = ProceduralEngine.evaluateLayer(
                            layer = layer,
                            timeSec = currentAnimSec,
                            bypassNodes = false
                        )
                        val (visibleInstances, culled) = ProceduralEngine.cullInstancesForViewport(
                            instances = evaluatedInstances,
                            minArtX = viewMinArtX,
                            minArtY = viewMinArtY,
                            maxArtX = viewMaxArtX,
                            maxArtY = viewMaxArtY
                        )
                        instanceCounter += visibleInstances.size
                        culledCounter += culled
                        for (inst in visibleInstances) {
                            drawEvaluatedInstance(
                                inst = inst,
                                wireframeOnly = uiState.wireframeOverlay,
                                invScale = 1f / totalScale
                            )
                        }
                    }
                }

                // Oriented Bounding Box (OBB) Selection Cage & Interactive 9-Handle Transform Gizmo
                selectedLayer?.let { sel ->
                    if (sel.visible) {
                        val halfW = sel.width / 2f
                        val halfH = sel.height / 2f
                        val left = sel.x - halfW
                        val right = sel.x + halfW
                        val top = sel.y - halfH
                        val bottom = sel.y + halfH
                        val strokePx = 2f / totalScale

                        withTransform({
                            if (abs(sel.rotation) > 0.01f) {
                                rotate(degrees = sel.rotation, pivot = Offset(sel.x, sel.y))
                            }
                        }) {
                            drawRect(
                                color = DataVector,
                                topLeft = Offset(left, top),
                                size = Size(sel.width, sel.height),
                                style = Stroke(
                                    width = strokePx,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / totalScale, 8f / totalScale))
                                )
                            )

                            // Top Rotation Handle Stem & Knob
                            val stemOffset = 42f / max(0.4f, min(2.5f, totalScale / 0.5f))
                            val rotKnobPos = Offset(sel.x, top - stemOffset)
                            drawLine(
                                color = DataGraphic,
                                start = Offset(sel.x, top),
                                end = rotKnobPos,
                                strokeWidth = 2.2f / totalScale
                            )
                            val isRotating = activeGizmoHandle == GizmoHandleType.ROTATE
                            val rotKnobR = (if (isRotating) 12f else 9f) / totalScale
                            drawCircle(
                                color = if (isRotating) DataRaster else DataGraphic,
                                radius = rotKnobR,
                                center = rotKnobPos
                            )
                            drawCircle(
                                color = GraphiteNearBlack,
                                radius = rotKnobR * 0.4f,
                                center = rotKnobPos
                            )

                            // 4 Corner Resize Handles
                            val cornerRadius = 8.5f / totalScale
                            val corners = listOf(
                                GizmoHandleType.TOP_LEFT to Offset(left, top),
                                GizmoHandleType.TOP_RIGHT to Offset(right, top),
                                GizmoHandleType.BOTTOM_RIGHT to Offset(right, bottom),
                                GizmoHandleType.BOTTOM_LEFT to Offset(left, bottom)
                            )
                            for ((hType, cPos) in corners) {
                                val isActive = activeGizmoHandle == hType
                                val r = if (isActive) cornerRadius * 1.35f else cornerRadius
                                drawCircle(
                                    color = if (isActive) DataRaster else GraphiteNearWhite,
                                    radius = r,
                                    center = cPos
                                )
                                drawCircle(
                                    color = DataVector,
                                    radius = r,
                                    center = cPos,
                                    style = Stroke(width = 2.2f / totalScale)
                                )
                            }

                            // 4 Edge Pill Handles
                            val edgeRadius = 6.5f / totalScale
                            val edges = listOf(
                                GizmoHandleType.TOP_EDGE to Offset(sel.x, top),
                                GizmoHandleType.BOTTOM_EDGE to Offset(sel.x, bottom),
                                GizmoHandleType.LEFT_EDGE to Offset(left, sel.y),
                                GizmoHandleType.RIGHT_EDGE to Offset(right, sel.y)
                            )
                            for ((hType, ePos) in edges) {
                                val isActive = activeGizmoHandle == hType
                                drawCircle(
                                    color = if (isActive) DataRaster else DataVector,
                                    radius = if (isActive) edgeRadius * 1.3f else edgeRadius,
                                    center = ePos
                                )
                            }

                            // Center Pivot Cross
                            val crossR = 9f / totalScale
                            drawLine(
                                color = DataGraphic,
                                start = Offset(sel.x - crossR, sel.y),
                                end = Offset(sel.x + crossR, sel.y),
                                strokeWidth = 2f / totalScale
                            )
                            drawLine(
                                color = DataGraphic,
                                start = Offset(sel.x, sel.y - crossR),
                                end = Offset(sel.x, sel.y + crossR),
                                strokeWidth = 2f / totalScale
                            )
                        }

                        // Path / Cubic Bézier / Spline Anchor Handles & C1/C0 Tangent Antennae
                        if (sel.points.isNotEmpty()) {
                            for (i in 0 until sel.points.size - 1) {
                                val p0 = sel.points[i]
                                val p1 = sel.points[i + 1]
                                drawLine(
                                    color = DataVector.copy(alpha = 0.45f),
                                    start = Offset(p0.x, p0.y),
                                    end = Offset(p1.x, p1.y),
                                    strokeWidth = 1.2f / totalScale,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f / totalScale, 6f / totalScale))
                                )
                            }
                            if (sel.closedPath && sel.points.size >= 3) {
                                val pLast = sel.points.last()
                                val pFirst = sel.points.first()
                                drawLine(
                                    color = DataGraphic.copy(alpha = 0.55f),
                                    start = Offset(pLast.x, pLast.y),
                                    end = Offset(pFirst.x, pFirst.y),
                                    strokeWidth = 1.4f / totalScale,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f / totalScale, 6f / totalScale))
                                )
                            }
                            sel.points.forEachIndexed { idx, pt ->
                                // Draw Cubic Bézier Tangent Antennae (handleIn & handleOut) when BEZIER_PATH
                                if (sel.kind == LayerGeometryKind.BEZIER_PATH) {
                                    val tR = 6.5f / totalScale
                                    val isDraggingIn = draggingBezierHandle == (idx to true)
                                    val isDraggingOut = draggingBezierHandle == (idx to false)
                                    val handleCol = if (pt.smooth) DataGraphic else DataRaster

                                    if (hypot(pt.handleInX - pt.x, pt.handleInY - pt.y) > 1f) {
                                        drawLine(
                                            color = handleCol.copy(alpha = 0.85f),
                                            start = Offset(pt.x, pt.y),
                                            end = Offset(pt.handleInX, pt.handleInY),
                                            strokeWidth = 1.8f / totalScale
                                        )
                                        drawCircle(
                                            color = if (isDraggingIn) DataRaster else handleCol,
                                            radius = if (isDraggingIn) tR * 1.35f else tR,
                                            center = Offset(pt.handleInX, pt.handleInY)
                                        )
                                    }
                                    if (hypot(pt.handleOutX - pt.x, pt.handleOutY - pt.y) > 1f) {
                                        drawLine(
                                            color = handleCol.copy(alpha = 0.85f),
                                            start = Offset(pt.x, pt.y),
                                            end = Offset(pt.handleOutX, pt.handleOutY),
                                            strokeWidth = 1.8f / totalScale
                                        )
                                        drawCircle(
                                            color = if (isDraggingOut) DataRaster else handleCol,
                                            radius = if (isDraggingOut) tR * 1.35f else tR,
                                            center = Offset(pt.handleOutX, pt.handleOutY)
                                        )
                                    }
                                }

                                val isDragged = idx == draggingVertexIndex
                                val r = (if (isDragged) 11f else 8f) / totalScale
                                drawCircle(
                                    color = if (isDragged) DataRaster else if (pt.smooth) DataVector else DataRaster,
                                    radius = r,
                                    center = Offset(pt.x, pt.y)
                                )
                                drawCircle(
                                    color = GraphiteNearBlack,
                                    radius = r * 0.45f,
                                    center = Offset(pt.x, pt.y)
                                )
                            }
                        }
                    }
                }

                // Live Rubber-band Shape Preview during Drag
                val sPos = dragStartArtboard
                val ePos = dragCurrentArtboard
                if (sPos != null && ePos != null) {
                    val left = min(sPos.x, ePos.x)
                    val top = min(sPos.y, ePos.y)
                    val w = max(8f, abs(ePos.x - sPos.x))
                    val h = max(8f, abs(ePos.y - sPos.y))
                    drawRect(
                        color = Color(uiState.primaryColorHex.toInt()).copy(alpha = 0.35f),
                        topLeft = Offset(left, top),
                        size = Size(w, h)
                    )
                    drawRect(
                        color = DataVector,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        style = Stroke(width = 2.5f / totalScale)
                    )
                }

                // Live Freehand / Variable-Width Brush Stroke Preview with Per-Segment Stylus Pressure
                if (activeStrokePoints.size >= 2) {
                    val baseW = if (uiState.activeTool == ToolType.BRUSH) 16f else 5f
                    val strokeCol = Color(uiState.primaryColorHex.toInt())
                    for (i in 1 until activeStrokePoints.size) {
                        val p0 = activeStrokePoints[i - 1]
                        val p1 = activeStrokePoints[i]
                        val segPressure = ((p0.pressure + p1.pressure) * 0.5f).coerceIn(0.2f, 1.8f)
                        drawLine(
                            color = strokeCol,
                            start = Offset(p0.x, p0.y),
                            end = Offset(p1.x, p1.y),
                            strokeWidth = baseW * segPressure,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Dynamic Magnetic Smart Guides (Laser Alignment Lines from EXP-02)
                if (uiState.activeSmartGuides.isNotEmpty()) {
                    for (guide in uiState.activeSmartGuides) {
                        if (guide.isVertical) {
                            drawLine(
                                color = Color(0xFFFF4081),
                                start = Offset(guide.positionArtboard, 0f),
                                end = Offset(guide.positionArtboard, doc.artboardHeight.toFloat()),
                                strokeWidth = 2.2f / totalScale,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f / totalScale, 6f / totalScale))
                            )
                        } else {
                            drawLine(
                                color = Color(0xFFFF4081),
                                start = Offset(0f, guide.positionArtboard),
                                end = Offset(doc.artboardWidth.toFloat(), guide.positionArtboard),
                                strokeWidth = 2.2f / totalScale,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f / totalScale, 6f / totalScale))
                            )
                        }
                    }
                }

                // Artboard border outline
                drawRect(
                    color = GraphiteDimGray,
                    topLeft = Offset.Zero,
                    size = Size(doc.artboardWidth.toFloat(), doc.artboardHeight.toFloat()),
                    style = Stroke(width = 1.5f / totalScale)
                )
            }

            val elapsedMs = ((System.nanoTime() - t0) / 1_000_000.0).toFloat()
            lastEvalTimeMs = (lastEvalTimeMs * 0.7f + elapsedMs * 0.3f).coerceAtLeast(0.1f)
            totalEvaluatedInstances = instanceCounter
            culledInstancesCount = culledCounter

            // Phase 4 (TP-B4 — Gruppo B): Precision Calibrated Top (X) & Left (Y) Rulers + Selected Layer Projection Markers
            if (uiState.showRulers && uiState.workspaceMode == StudioWorkspaceMode.PRO) {
                val rulerThick = 18.dp.toPx()
                // Top X Ruler background
                drawRect(
                    color = GraphitePanelBg.copy(alpha = 0.92f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, rulerThick)
                )
                // Left Y Ruler background
                drawRect(
                    color = GraphitePanelBg.copy(alpha = 0.92f),
                    topLeft = Offset(0f, rulerThick),
                    size = Size(rulerThick, size.height - rulerThick)
                )
                // Selected layer projection highlight on X and Y rulers
                selectedLayer?.let { sel ->
                    if (sel.visible) {
                        val projLeft = (originX + (sel.x - sel.width / 2f) * totalScale).coerceIn(rulerThick, size.width)
                        val projRight = (originX + (sel.x + sel.width / 2f) * totalScale).coerceIn(rulerThick, size.width)
                        if (projRight > projLeft) {
                            drawRect(
                                color = DataVector.copy(alpha = 0.32f),
                                topLeft = Offset(projLeft, 0f),
                                size = Size(projRight - projLeft, rulerThick)
                            )
                        }
                        val projTop = (originY + (sel.y - sel.height / 2f) * totalScale).coerceIn(rulerThick, size.height)
                        val projBottom = (originY + (sel.y + sel.height / 2f) * totalScale).coerceIn(rulerThick, size.height)
                        if (projBottom > projTop) {
                            drawRect(
                                color = DataVector.copy(alpha = 0.32f),
                                topLeft = Offset(0f, projTop),
                                size = Size(rulerThick, projBottom - projTop)
                            )
                        }
                    }
                }
                var rx = rulerThick
                while (rx < size.width) {
                    val isMajor = (rx.toInt() % 100) < 20
                    drawLine(
                        color = GraphiteLightGray.copy(alpha = if (isMajor) 0.55f else 0.25f),
                        start = Offset(rx, if (isMajor) 4f else 10f),
                        end = Offset(rx, rulerThick),
                        strokeWidth = 1f
                    )
                    rx += 20f
                }
                var ry = rulerThick
                while (ry < size.height) {
                    val isMajor = (ry.toInt() % 100) < 20
                    drawLine(
                        color = GraphiteLightGray.copy(alpha = if (isMajor) 0.55f else 0.25f),
                        start = Offset(if (isMajor) 4f else 10f, ry),
                        end = Offset(rulerThick, ry),
                        strokeWidth = 1f
                    )
                    ry += 20f
                }
            }
        }

        // 2. Top Floating Context Pill Bar (Document, Undo/Redo, Zoom, Snap, Grid, Perf HUD, Command Palette, Export)
        TopCanvasControlPill(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 10.dp, end = 10.dp)
        )

        // 3. Real-Time Graphene Performance & Telemetry HUD Overlay (Phase 4 TP-A4)
        AnimatedVisibility(
            visible = uiState.showPerformanceHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 72.dp, start = 12.dp)
        ) {
            PerformanceTelemetryHudCard(
                evalTimeMs = lastEvalTimeMs,
                instanceCount = totalEvaluatedInstances,
                culledCount = culledInstancesCount,
                cacheHitPercent = ProceduralEngine.getCacheHitRatePercent(),
                layerCount = doc.layers.size,
                nodeCount = doc.layers.sumOf { it.nodes.count { n -> n.enabled } },
                stylusPressure = currentStylusPressure,
                onRunStressTest = { viewModel.applyProceduralPresetRecipe("stress_benchmark") },
                onClose = { viewModel.togglePerformanceHud() }
            )
        }

        // 4. Toast / Hint Banner
        AnimatedVisibility(
            visible = uiState.statusToast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (uiState.showPerformanceHud) 172.dp else 68.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = GraphiteMildBlack.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .border(1.dp, DataVector.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable { viewModel.clearToast() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = DataVector,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.statusToast.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteNearWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 5. Bottom Dock: Split Comparison Slider (if active) + Selected Layer Node Strip + Working Colors + Tool Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (uiState.compareSplitMode) {
                Surface(
                    color = GraphiteNearBlack.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DataGraphic, RoundedCornerShape(12.dp))
                        .testTag("compare_split_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PRIMA (Base)",
                            style = MaterialTheme.typography.labelSmall,
                            color = DataVector
                        )
                        Slider(
                            value = uiState.compareSplitFraction,
                            onValueChange = { viewModel.updateCompareSplitFraction(it) },
                            valueRange = 0.08f..0.92f,
                            colors = SliderDefaults.colors(
                                thumbColor = DataGraphic,
                                activeTrackColor = DataVector,
                                inactiveTrackColor = DataGraphic
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(28.dp)
                        )
                        Text(
                            text = "DOPO (Grafo)",
                            style = MaterialTheme.typography.labelSmall,
                            color = DataGraphic
                        )
                    }
                }
            }

            if (uiState.workspaceMode != StudioWorkspaceMode.ZEN) {
                if (uiState.showLiveDagDrawer && selectedLayer != null && uiState.workspaceMode == StudioWorkspaceMode.PRO) {
                    LiveDagOverlayDrawer(
                        layer = selectedLayer,
                        viewModel = viewModel
                    )
                }

                ContextualModeAndGuideStrip(
                    uiState = uiState,
                    viewModel = viewModel
                )

                QuickLayerAndColorsBar(
                    uiState = uiState,
                    viewModel = viewModel
                )

                MobileToolDock(
                    activeTool = uiState.activeTool,
                    onSelectTool = { viewModel.selectTool(it) }
                )
            }
        }

        // 7. Executive A/B Testing & Department Command Center Lab Modal
        if (uiState.showABLabSheet) {
            ABExperimentLabDialog(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { viewModel.setABLabSheetVisible(false) }
            )
        }

        // 8. Phase 5 (TP-D5 — Gruppo D): Universal Enterprise Command Palette Modal
        if (uiState.showCommandPalette) {
            EnterpriseCommandPaletteDialog(
                onDismiss = { viewModel.setCommandPaletteVisible(false) },
                onExecuteCommand = { cmdId -> viewModel.executeEnterpriseCommand(cmdId) }
            )
        }

        // 9. Unified Creative Suite Sheet (Picsart, Canva, Figma, FigJam & Graphite Guide)
        UnifiedCreativeSuiteModal(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@Composable
private fun PerformanceTelemetryHudCard(
    evalTimeMs: Float,
    instanceCount: Int,
    culledCount: Int,
    cacheHitPercent: Int,
    layerCount: Int,
    nodeCount: Int,
    stylusPressure: Float,
    onRunStressTest: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = GraphiteNearBlack.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(1.dp, DataGraphic.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .testTag("performance_telemetry_hud")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (evalTimeMs < 8.3f) DataGraphic else DataRaster)
                )
                Text(
                    text = "GRAPHENE ULTRA-ENGINE TELEMETRY (120Hz • v3.0)",
                    style = MaterialTheme.typography.labelSmall,
                    color = DataGraphic
                )
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.labelSmall,
                    color = GraphiteLightGray,
                    modifier = Modifier.clickable { onClose() }
                )
            }
            Text(
                text = String.format(
                    Locale.US,
                    "DAG Eval: %.2f ms/frame • Visibili: %d • Scartate Frustum: %d • Cache Hit: %d%%",
                    evalTimeMs,
                    instanceCount,
                    culledCount,
                    cacheHitPercent
                ),
                style = MaterialTheme.typography.labelMedium,
                color = GraphiteNearWhite
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Livelli: $layerCount • Nodi: $nodeCount • Pressione Stilo: ${(stylusPressure * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = DataArtboard
                )
                Surface(
                    color = DataVector.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .border(1.dp, DataVector, RoundedCornerShape(6.dp))
                        .clickable { onRunStressTest() }
                        .testTag("run_stress_test_button")
                ) {
                    Text(
                        text = "Stress Benchmark 48×",
                        style = MaterialTheme.typography.labelSmall,
                        color = DataVector,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCanvasControlPill(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        color = GraphitePanelBg.copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteDimGray, RoundedCornerShape(14.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            // Graphite Hex Cube Badge + Document Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.selectTab(MobileWorkspaceTab.PORTFOLIO) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("document_title_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Hexagon,
                    contentDescription = "Graphite",
                    tint = DataVector,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = uiState.document.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = GraphiteNearWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "${uiState.document.artboardWidth}×${uiState.document.artboardHeight} • ${uiState.document.layers.size} liv.",
                        style = MaterialTheme.typography.labelSmall,
                        color = GraphiteLightGray
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            // Undo & Redo
            IconButton(
                onClick = { viewModel.undo() },
                enabled = uiState.canUndo,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Annulla",
                    tint = if (uiState.canUndo) GraphiteNearWhite else GraphiteDimGray
                )
            }
            IconButton(
                onClick = { viewModel.redo() },
                enabled = uiState.canRedo,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Ripeti",
                    tint = if (uiState.canRedo) GraphiteNearWhite else GraphiteDimGray
                )
            }

            // Zoom Pill (tap to reset to 100%)
            Surface(
                color = GraphiteDarkGray,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable { viewModel.resetViewport() }
                    .padding(horizontal = 2.dp)
                    .testTag("zoom_reset_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Centra",
                        tint = DataVector,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${(uiState.zoom * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = GraphiteNearWhite
                    )
                }
            }

            // Patch v3.5: Adaptive Studio Workspace Mode Switch (Easy / Pro CAD / Zen)
            val modeColor = when (uiState.workspaceMode) {
                StudioWorkspaceMode.EASY -> DataGraphic
                StudioWorkspaceMode.PRO -> DataVector
                StudioWorkspaceMode.ZEN -> DataRaster
            }
            Surface(
                color = modeColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .border(1.dp, modeColor, RoundedCornerShape(8.dp))
                    .clickable { viewModel.cycleWorkspaceMode() }
                    .testTag("workspace_mode_chip")
            ) {
                Text(
                    text = uiState.workspaceMode.badgeIt,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = modeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            if (!uiState.compactTopBarMode && uiState.workspaceMode == StudioWorkspaceMode.PRO) {
                // Zoom - / + quick buttons (hidden in Easy/Zen or Compact mode to reduce mis-taps)
                IconButton(
                    onClick = { viewModel.updateViewport(uiState.zoom * 0.8f, uiState.panX, uiState.panY) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold, color = GraphiteNearWhite)
                }
                IconButton(
                    onClick = { viewModel.updateViewport(uiState.zoom * 1.25f, uiState.panX, uiState.panY) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", fontWeight = FontWeight.Bold, color = GraphiteNearWhite)
                }
            }

            // Snap Toggle
            IconButton(
                onClick = { viewModel.toggleSnap() },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("snap_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = "Calamita Griglia",
                    tint = if (uiState.snapToGrid) DataGraphic else GraphiteLightGray
                )
            }

            // Grid Toggle
            IconButton(
                onClick = { viewModel.toggleGrid() },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("grid_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Grid4x4,
                    contentDescription = "Griglia",
                    tint = if (uiState.showGrid) DataVector else GraphiteLightGray
                )
            }

            // Live Procedural Animation Play / Pause Toggle (60fps)
            IconButton(
                onClick = { viewModel.togglePlayAnimation() },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("play_animation_button")
            ) {
                Icon(
                    imageVector = if (uiState.isPlayingAnimation) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Animazione Procedurale Live",
                    tint = if (uiState.isPlayingAnimation) DataGraphic else GraphiteNearWhite
                )
            }

            if (uiState.workspaceMode == StudioWorkspaceMode.PRO) {
                // Wireframe Toggle (Pro Mode only)
                IconButton(
                    onClick = { viewModel.toggleWireframe() },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("wireframe_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Contorni Wireframe",
                        tint = if (uiState.wireframeOverlay) DataRaster else GraphiteLightGray
                    )
                }

                // Before / After Split-Screen Comparison Curtain Toggle (Pro Mode only)
                IconButton(
                    onClick = { viewModel.toggleCompareSplitMode() },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("compare_split_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Confronto Prima/Dopo",
                        tint = if (uiState.compareSplitMode) DataGraphic else GraphiteLightGray
                    )
                }

                // Performance / Benchmark Telemetry HUD Toggle (Pro Mode only)
                IconButton(
                    onClick = { viewModel.togglePerformanceHud() },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("perf_hud_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Telemetria Performance",
                        tint = if (uiState.showPerformanceHud) DataGraphic else GraphiteLightGray
                    )
                }
            }

            // Phase 5 (TP-D5): Universal Enterprise Command Palette Button
            Surface(
                color = DataRaster.copy(alpha = 0.20f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .border(1.dp, DataRaster, RoundedCornerShape(8.dp))
                    .clickable { viewModel.setCommandPaletteVisible(true) }
                    .testTag("command_palette_button")
            ) {
                Text(
                    text = "⚡ Comandi",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DataRaster,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            if (uiState.workspaceMode == StudioWorkspaceMode.PRO) {
                // Executive A/B Testing & Department Command Center Badge
                Surface(
                    color = if (uiState.abRolloutApplied) DataGraphic.copy(alpha = 0.24f) else DataVector.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .border(
                            width = 1.dp,
                            color = if (uiState.abRolloutApplied) DataGraphic else DataVector,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setABLabSheetVisible(true) }
                        .testTag("ab_lab_button")
                ) {
                    Text(
                        text = if (uiState.abRolloutApplied) "A/B ✓" else "A/B Lab",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.abRolloutApplied) DataGraphic else DataVector,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            // Export SVG / PNG / XML / Compose / Tokens Button
            IconButton(
                onClick = { viewModel.setExportDialogVisible(true) },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = "Esporta",
                    tint = DataRaster
                )
            }
        }
    }
}

@Composable
private fun EnterpriseCommandPaletteDialog(
    onDismiss: () -> Unit,
    onExecuteCommand: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) com.example.model.ENTERPRISE_COMMAND_CATALOG
        else com.example.model.ENTERPRISE_COMMAND_CATALOG.filter {
            it.titleIt.lowercase(Locale.ROOT).contains(q) ||
                it.subtitleIt.lowercase(Locale.ROOT).contains(q) ||
                it.divisionBadge.lowercase(Locale.ROOT).contains(q)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = GraphitePanelBg,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, DataRaster.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                .testTag("enterprise_command_palette_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GRUPPO D • ENTERPRISE COMMAND PALETTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = DataRaster,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Accesso Rapido Azioni, Macro & Handoff",
                            style = MaterialTheme.typography.titleMedium,
                            color = GraphiteNearWhite
                        )
                    }
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleMedium,
                        color = GraphiteLightGray,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(6.dp)
                    )
                }

                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cerca comando, macro o divisione (es. Mandala, Centra, XML)...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("command_palette_search_input")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (cmd in filtered.take(6)) {
                        Surface(
                            color = GraphiteDarkGray,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GraphiteDimGray, RoundedCornerShape(10.dp))
                                .clickable { onExecuteCommand(cmd.id) }
                                .testTag("command_item_${cmd.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cmd.divisionBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DataGraphic
                                    )
                                    Text(
                                        text = cmd.titleIt,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = GraphiteNearWhite,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = cmd.subtitleIt,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GraphiteLightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = GraphiteNearBlack,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = cmd.shortcutHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DataVector,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ABExperimentLabDialog(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = GraphitePanelBg,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, DataVector.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                .testTag("ab_experiment_lab_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DIREZIONE 5 DIVISIONI ENTERPRISE • A/B LAB",
                            style = MaterialTheme.typography.labelSmall,
                            color = DataGraphic,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Telemetria & Decisioni Esecutive v3.0",
                            style = MaterialTheme.typography.titleMedium,
                            color = GraphiteNearWhite
                        )
                    }
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleMedium,
                        color = GraphiteLightGray,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(6.dp)
                    )
                }

                // Live Telemetry Summary Banner across 5 Enterprise Divisions
                Surface(
                    color = GraphiteNearBlack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ROADMAP ENTERPRISE: Fasi 1-3 ✓ • Fase 4 v2.5 Ultra-Engine ✓ • Fase 5 v2.8 UX ✓ • Fase 6 v3.0 Handoff ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = DataGraphic,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Divisioni Attive: Gruppo A (120Hz) • Gruppo B (CAD) • Gruppo C (DAG) • Gruppo D (UX) • Gruppo E (Dev Handoff)",
                            style = MaterialTheme.typography.labelSmall,
                            color = DataVector
                        )
                        Text(
                            text = "Undo Gesture: ${uiState.gestureUndoCount} • Snap Laser Bordi/Centri: ${uiState.smartGuideSnapsCount} • Ricablaggi 2D DAG: ${uiState.dagSpatialRewiresCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GraphiteNearWhite
                        )
                    }
                }

                // 4 Department Experiments List
                for (exp in uiState.abExperiments) {
                    Surface(
                        color = GraphiteDarkGray.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = exp.departmentIt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DataNumber
                                )
                                Text(
                                    text = "N=${exp.sampleSize} • Conf. ${exp.confidencePercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DataGraphic
                                )
                            }
                            Text(
                                text = exp.titleIt,
                                style = MaterialTheme.typography.labelMedium,
                                color = GraphiteNearWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Risultato: ${exp.metricDeltaIt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GraphiteLightGray
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val isA = exp.activeVariant == ABVariantChoice.VARIANT_A
                                Surface(
                                    color = if (isA) DataVector.copy(alpha = 0.28f) else GraphiteNearBlack,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (isA) DataVector else GraphiteDimGray,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            viewModel.setABExperimentVariant(exp.id, ABVariantChoice.VARIANT_A)
                                        }
                                ) {
                                    Text(
                                        text = exp.variantALabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isA) GraphiteNearWhite else GraphiteLightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    color = if (!isA) DataGraphic.copy(alpha = 0.28f) else GraphiteNearBlack,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (!isA) DataGraphic else GraphiteDimGray,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            viewModel.setABExperimentVariant(exp.id, ABVariantChoice.VARIANT_B)
                                        }
                                ) {
                                    Text(
                                        text = "${exp.variantBLabel} ★",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (!isA) DataGraphic else GraphiteLightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Executive Actions: Simulate Cohort + Rollout 100% Winners
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = GraphiteDarkGray,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, DataVector, RoundedCornerShape(10.dp))
                            .clickable { viewModel.runDepartmentABSimulation() }
                            .testTag("simulate_ab_sessions_button")
                    ) {
                        Text(
                            text = "Simula +1.200 Sessioni",
                            style = MaterialTheme.typography.labelMedium,
                            color = DataVector,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                    Surface(
                        color = DataGraphic,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable {
                                viewModel.applyWinningABRollout()
                                onDismiss()
                            }
                            .testTag("apply_ab_winners_button")
                    ) {
                        Text(
                            text = "Rollout 100% Vincenti",
                            style = MaterialTheme.typography.labelMedium,
                            color = GraphiteNearBlack,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLayerAndColorsBar(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel
) {
    val selectedLayer = uiState.selectedLayer
    Surface(
        color = GraphitePanelBg.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteDimGray, RoundedCornerShape(12.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Working Colors Swatches (Primary + Secondary + Swap + Reset)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(uiState.primaryColorHex.toInt()))
                        .border(2.dp, GraphiteNearWhite, CircleShape)
                        .clickable { viewModel.openColorPicker(isPrimary = true) }
                        .testTag("primary_color_swatch")
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(uiState.secondaryColorHex.toInt()))
                        .border(1.5.dp, GraphiteLightGray, CircleShape)
                        .clickable { viewModel.openColorPicker(isPrimary = false) }
                        .testTag("secondary_color_swatch")
                )
                IconButton(
                    onClick = { viewModel.swapWorkingColors() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Inverti Colori",
                        tint = GraphiteLightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.resetWorkingColors() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Ripristina Colori",
                        tint = GraphiteLightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Active Layer Pill + Node Count Badge
            if (selectedLayer != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.selectTab(MobileWorkspaceTab.LAYERS_INSPECTOR) },
                        label = {
                            Text(
                                text = "${selectedLayer.name} (${selectedLayer.width.roundToInt()}×${selectedLayer.height.roundToInt()} • ${selectedLayer.rotation.roundToInt()}°)",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = DataVector,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = GraphiteNearWhite
                        )
                    )

                    if (selectedLayer.kind == LayerGeometryKind.BEZIER_PATH || selectedLayer.kind == LayerGeometryKind.SPLINE) {
                        AssistChip(
                            onClick = { viewModel.toggleBezierClosedPath() },
                            label = {
                                Text(
                                    text = if (selectedLayer.closedPath) "Tracciato Chiuso" else "Chiudi Tracciato",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedLayer.closedPath) DataGraphic else GraphiteNearWhite
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = GraphiteDarkGray,
                                labelColor = GraphiteNearWhite
                            ),
                            modifier = Modifier.testTag("toggle_close_path_chip")
                        )

                        if (selectedLayer.kind == LayerGeometryKind.BEZIER_PATH) {
                            val allSmooth = selectedLayer.points.all { it.smooth }
                            AssistChip(
                                onClick = { viewModel.toggleBezierVerticesSmoothMode() },
                                label = {
                                    Text(
                                        text = if (allSmooth) "Vertici C¹" else "Cuspidi C⁰",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (allSmooth) DataVector else DataRaster
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = GraphiteDarkGray,
                                    labelColor = GraphiteNearWhite
                                ),
                                modifier = Modifier.testTag("toggle_cusp_mode_chip")
                            )
                        }
                    } else if (selectedLayer.kind != LayerGeometryKind.TEXT) {
                        // Patch v3.5: 1-Tap Bake Primitive / DAG to Editable Cubic Bézier Path
                        AssistChip(
                            onClick = { viewModel.bakeSelectedLayerToBezierPath() },
                            label = {
                                Text(
                                    text = "✒️ Converti in Tracciato",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DataRaster
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = GraphiteDarkGray,
                                labelColor = DataRaster
                            ),
                            modifier = Modifier.testTag("hud_bake_to_path_chip")
                        )
                    }

                    AssistChip(
                        onClick = { viewModel.toggleLiveDagDrawer() },
                        label = {
                            Text(
                                text = "Split DAG",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.showLiveDagDrawer) DataGraphic else DataVector
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = if (uiState.showLiveDagDrawer) DataGraphic else DataVector,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = GraphiteNearWhite
                        ),
                        modifier = Modifier.testTag("live_dag_split_button")
                    )

                    AssistChip(
                        onClick = { viewModel.setNodeCatalogVisible(true) },
                        label = {
                            Text(
                                text = "+ Nodo (${selectedLayer.nodes.count { it.enabled }})",
                                style = MaterialTheme.typography.labelLarge,
                                color = DataGraphic
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = DataGraphic,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = DataGraphic
                        ),
                        modifier = Modifier.testTag("quick_add_node_chip")
                    )

                    AssistChip(
                        onClick = {
                            viewModel.alignSelectedLayer(com.example.model.ArtboardAlignment.ALIGN_CENTER_H)
                            viewModel.alignSelectedLayer(com.example.model.ArtboardAlignment.ALIGN_CENTER_V)
                        },
                        label = { Text("🎯 Centra", style = MaterialTheme.typography.labelLarge) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = DataVector
                        ),
                        modifier = Modifier.testTag("hud_quick_center_chip")
                    )

                    AssistChip(
                        onClick = { viewModel.duplicateLayer(selectedLayer.id) },
                        label = { Text("⧉ Duplica", style = MaterialTheme.typography.labelLarge) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = DataGraphic
                        ),
                        modifier = Modifier.testTag("hud_quick_duplicate_chip")
                    )

                    AssistChip(
                        onClick = { viewModel.addQuickLayer(LayerGeometryKind.POLYGON) },
                        label = { Text("Nuovo", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = DataRaster,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = GraphiteNearWhite
                        ),
                        modifier = Modifier.testTag("quick_add_layer_chip")
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveDagOverlayDrawer(
    layer: com.example.model.GraphiteLayer,
    viewModel: GraphiteViewModel
) {
    Surface(
        color = GraphiteNearBlack.copy(alpha = 0.94f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DataVector.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .testTag("live_dag_overlay_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SPLIT-WORKSPACE DAG LIVE • ${layer.name} (${layer.nodes.size} nodi)",
                    style = MaterialTheme.typography.labelSmall,
                    color = DataGraphic,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "+ Nodo",
                        style = MaterialTheme.typography.labelSmall,
                        color = DataVector,
                        modifier = Modifier.clickable { viewModel.setNodeCatalogVisible(true) }
                    )
                    Text(
                        text = "Chiudi ✕",
                        style = MaterialTheme.typography.labelSmall,
                        color = GraphiteLightGray,
                        modifier = Modifier.clickable { viewModel.toggleLiveDagDrawer() }
                    )
                }
            }

            if (layer.nodes.isEmpty()) {
                Text(
                    text = "Nessun nodo procedurale attivo su questo livello. Tocca '+ Nodo' per comporre il grafo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GraphiteLightGray
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(layer.nodes, key = { it.id }) { node ->
                        val firstSpec = node.type.paramSpecs.firstOrNull()
                        val currentVal = if (firstSpec != null) {
                            node.params[firstSpec.key] ?: firstSpec.defaultVal
                        } else 0f

                        Surface(
                            color = GraphiteDarkGray,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .width(205.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (node.enabled) DataGraphic.copy(alpha = 0.6f) else GraphiteDimGray,
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = node.type.titleIt,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (node.enabled) GraphiteNearWhite else GraphiteLightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (node.enabled) "ON" else "OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (node.enabled) DataGraphic else GraphiteLightGray,
                                        modifier = Modifier.clickable {
                                            viewModel.toggleNodeEnabled(layer.id, node.id)
                                        }
                                    )
                                }
                                if (firstSpec != null) {
                                    Text(
                                        text = "${firstSpec.labelIt}: ${currentVal.roundToInt()}${firstSpec.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DataNumber
                                    )
                                    Slider(
                                        value = currentVal.coerceIn(firstSpec.min, firstSpec.max),
                                        onValueChange = { v ->
                                            viewModel.updateNodeParam(
                                                layerId = layer.id,
                                                nodeId = node.id,
                                                paramKey = firstSpec.key,
                                                newValue = v,
                                                recordHistory = false
                                            )
                                        },
                                        onValueChangeFinished = {
                                            viewModel.updateNodeParam(
                                                layerId = layer.id,
                                                nodeId = node.id,
                                                paramKey = firstSpec.key,
                                                newValue = currentVal,
                                                recordHistory = true
                                            )
                                        },
                                        valueRange = firstSpec.min..firstSpec.max,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileToolDock(
    activeTool: ToolType,
    onSelectTool: (ToolType) -> Unit
) {
    Surface(
        color = GraphitePanelBg.copy(alpha = 0.96f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteDimGray, RoundedCornerShape(14.dp))
            .testTag("mobile_tool_dock")
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ToolType.entries) { tool ->
                val isSelected = tool == activeTool
                val accentColor = when (tool.group.name) {
                    "GENERAL" -> DataVector
                    "VECTOR" -> DataGraphic
                    else -> DataRaster
                }
                Surface(
                    color = if (isSelected) accentColor.copy(alpha = 0.22f) else GraphiteDarkGray,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) accentColor else GraphiteDimGray,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectTool(tool) }
                        .padding(horizontal = 10.dp)
                        .testTag("tool_${tool.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = iconForTool(tool),
                            contentDescription = tool.labelIt,
                            tint = if (isSelected) accentColor else GraphiteLightGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tool.labelIt,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) GraphiteNearWhite else GraphiteLightGray
                        )
                    }
                }
            }
        }
    }
}

fun iconForTool(tool: ToolType): ImageVector {
    return when (tool) {
        ToolType.SELECT -> Icons.Default.NearMe
        ToolType.ARTBOARD -> Icons.Default.CropFree
        ToolType.NAVIGATE -> Icons.Default.OpenWith
        ToolType.EYEDROPPER -> Icons.Default.Colorize
        ToolType.FILL -> Icons.Default.FormatColorFill
        ToolType.GRADIENT -> Icons.Default.Gradient
        ToolType.PATH -> Icons.Default.Polyline
        ToolType.PEN -> Icons.Default.Edit
        ToolType.FREEHAND -> Icons.Default.Gesture
        ToolType.SPLINE -> Icons.Default.Timeline
        ToolType.LINE -> Icons.Default.LinearScale
        ToolType.RECTANGLE -> Icons.Default.Rectangle
        ToolType.ELLIPSE -> Icons.Default.Circle
        ToolType.POLYGON -> Icons.Default.ChangeHistory
        ToolType.STAR -> Icons.Default.Star
        ToolType.TEXT -> Icons.Default.TextFields
        ToolType.BRUSH -> Icons.Default.Brush
    }
}

private fun DrawScope.drawEvaluatedInstance(
    inst: EvaluatedShapeInstance,
    wireframeOnly: Boolean,
    invScale: Float
) {
    withTransform({
        rotate(degrees = inst.rotationDeg, pivot = Offset(inst.centerX, inst.centerY))
    }) {
        val path = buildComposePath(inst)

        // Draw procedural Drop Shadow / Glow if enabled and not in wireframe mode
        if (!wireframeOnly) {
            inst.shadow?.let { sh ->
                withTransform({
                    translate(left = sh.offsetX, top = sh.offsetY)
                }) {
                    drawPath(
                        path = path,
                        color = Color(sh.colorHex.toInt()).copy(alpha = (sh.alpha * inst.opacity).coerceIn(0f, 1f))
                    )
                }
            }
        }

        // Fill
        val isOpenStrokeKind = inst.kind in listOf(
            LayerGeometryKind.LINE,
            LayerGeometryKind.FREEHAND,
            LayerGeometryKind.BRUSH_STROKE
        ) || ((inst.kind == LayerGeometryKind.BEZIER_PATH || inst.kind == LayerGeometryKind.SPLINE) && !inst.closedPath)

        if (!wireframeOnly && inst.hasFill && !isOpenStrokeKind) {
            if (inst.kind == LayerGeometryKind.TEXT) {
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = inst.fillColorHex.toInt()
                        alpha = (inst.opacity * 255).toInt().coerceIn(0, 255)
                        textSize = inst.fontSize
                        textAlign = AndroidPaint.Align.CENTER
                        isFakeBoldText = true
                    }
                    drawText(
                        inst.textContent,
                        inst.centerX,
                        inst.centerY + inst.fontSize * 0.35f,
                        textPaint
                    )
                }
            } else if (inst.useGradient) {
                val c0 = Color(inst.fillColorHex.toInt()).copy(alpha = inst.opacity)
                val c1 = Color(inst.gradientEndColorHex.toInt()).copy(alpha = inst.opacity)
                val brush = if (inst.isRadialGradient) {
                    Brush.radialGradient(
                        colors = listOf(c0, c1),
                        center = Offset(inst.centerX, inst.centerY),
                        radius = max(16f, max(inst.width, inst.height) * 0.55f)
                    )
                } else {
                    val rad = Math.toRadians(inst.gradientAngle.toDouble())
                    val dx = (cos(rad) * inst.width * 0.5).toFloat()
                    val dy = (sin(rad) * inst.height * 0.5).toFloat()
                    Brush.linearGradient(
                        colors = listOf(c0, c1),
                        start = Offset(inst.centerX - dx, inst.centerY - dy),
                        end = Offset(inst.centerX + dx, inst.centerY + dy)
                    )
                }
                drawPath(path = path, brush = brush)
            } else {
                drawPath(
                    path = path,
                    color = Color(inst.fillColorHex.toInt()).copy(alpha = inst.opacity)
                )
            }
        }

        // Stroke (or Wireframe outline)
        val strokeW = if (wireframeOnly) max(2f * invScale, 1.5f) else inst.strokeWidth
        if (strokeW > 0.1f && inst.kind != LayerGeometryKind.TEXT) {
            if (!wireframeOnly && inst.kind == LayerGeometryKind.BRUSH_STROKE && inst.points.size >= 2 && inst.dashLen <= 0f) {
                // Variable-width stylus pressure brush rendering per segment
                val strokeCol = Color(inst.strokeColorHex.toInt()).copy(alpha = inst.opacity)
                for (i in 1 until inst.points.size) {
                    val p0 = inst.points[i - 1]
                    val p1 = inst.points[i]
                    val segPressure = ((p0.pressure + p1.pressure) * 0.5f).coerceIn(0.2f, 1.8f)
                    drawLine(
                        color = strokeCol,
                        start = Offset(p0.x, p0.y),
                        end = Offset(p1.x, p1.y),
                        strokeWidth = strokeW * segPressure,
                        cap = StrokeCap.Round
                    )
                }
            } else {
                val dashEffect = if (inst.dashLen > 0f && inst.dashGap > 0f) {
                    PathEffect.dashPathEffect(floatArrayOf(inst.dashLen, inst.dashGap), inst.dashPhase)
                } else null

                drawPath(
                    path = path,
                    color = if (wireframeOnly) DataVector else Color(inst.strokeColorHex.toInt()).copy(alpha = inst.opacity),
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = dashEffect
                    )
                )
            }
        }
    }
}

private fun buildComposePath(inst: EvaluatedShapeInstance): Path {
    val path = Path()
    when (inst.kind) {
        LayerGeometryKind.RECTANGLE -> {
            if (inst.waveAmplitude > 0.5f) {
                val pts = ProceduralEngine.buildShapeVertices(inst)
                pts.forEachIndexed { idx, (px, py) ->
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            } else {
                path.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = inst.centerX - inst.width / 2f,
                        top = inst.centerY - inst.height / 2f,
                        right = inst.centerX + inst.width / 2f,
                        bottom = inst.centerY + inst.height / 2f,
                        cornerRadius = CornerRadius(inst.cornerRadius, inst.cornerRadius)
                    )
                )
            }
        }
        LayerGeometryKind.ELLIPSE -> {
            if (inst.waveAmplitude > 0.5f) {
                val pts = ProceduralEngine.buildShapeVertices(inst)
                pts.forEachIndexed { idx, (px, py) ->
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            } else {
                path.addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = inst.centerX - inst.width / 2f,
                        top = inst.centerY - inst.height / 2f,
                        right = inst.centerX + inst.width / 2f,
                        bottom = inst.centerY + inst.height / 2f
                    )
                )
            }
        }
        LayerGeometryKind.POLYGON, LayerGeometryKind.STAR -> {
            val pts = ProceduralEngine.buildShapeVertices(inst)
            pts.forEachIndexed { idx, (px, py) ->
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }
        LayerGeometryKind.BEZIER_PATH -> {
            if (inst.points.size >= 2) {
                path.moveTo(inst.points.first().x, inst.points.first().y)
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
                if (inst.closedPath && inst.points.size >= 3) {
                    val last = inst.points.last()
                    val first = inst.points.first()
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
                    if (inst.kind == LayerGeometryKind.SPLINE) {
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        path.quadraticTo(prev.x, prev.y, midX, midY)
                    } else {
                        path.lineTo(curr.x, curr.y)
                    }
                }
                if (inst.kind == LayerGeometryKind.SPLINE) {
                    path.lineTo(inst.points.last().x, inst.points.last().y)
                    if (inst.closedPath && inst.points.size >= 3) {
                        path.close()
                    }
                }
            } else {
                path.moveTo(inst.centerX - inst.width / 2f, inst.centerY)
                path.lineTo(inst.centerX + inst.width / 2f, inst.centerY)
            }
        }
        LayerGeometryKind.TEXT -> {}
    }

    // Apply non-destructive Skia Boolean Path Operation in Compose if present
    val boolOp = inst.booleanOp
    if (boolOp != null) {
        val isClosedShape = inst.kind in listOf(
            LayerGeometryKind.RECTANGLE,
            LayerGeometryKind.ELLIPSE,
            LayerGeometryKind.POLYGON,
            LayerGeometryKind.STAR
        ) || (inst.kind == LayerGeometryKind.BEZIER_PATH && inst.closedPath)
        if (isClosedShape) {
            val cutterPath = Path()
            val cx = inst.centerX + boolOp.offsetX
            val cy = inst.centerY + boolOp.offsetY
            val cw = max(12f, inst.width * boolOp.scaleRatio)
            val ch = max(12f, inst.height * boolOp.scaleRatio)
            when (boolOp.cutterShape) {
                0 -> {
                    cutterPath.addOval(
                        androidx.compose.ui.geometry.Rect(
                            left = cx - cw / 2f,
                            top = cy - ch / 2f,
                            right = cx + cw / 2f,
                            bottom = cy + ch / 2f
                        )
                    )
                }
                1 -> {
                    cutterPath.addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = cx - cw / 2f,
                            top = cy - ch / 2f,
                            right = cx + cw / 2f,
                            bottom = cy + ch / 2f,
                            cornerRadius = CornerRadius(cw * 0.15f, ch * 0.15f)
                        )
                    )
                }
                else -> {
                    cutterPath.moveTo(cx, cy - ch / 2f)
                    cutterPath.lineTo(cx + cw / 2f, cy)
                    cutterPath.lineTo(cx, cy + ch / 2f)
                    cutterPath.lineTo(cx - cw / 2f, cy)
                    cutterPath.close()
                }
            }
            val op = when (boolOp.opMode) {
                0 -> PathOperation.Difference
                1 -> PathOperation.Union
                2 -> PathOperation.Intersect
                else -> PathOperation.Xor
            }
            val combined = Path()
            combined.op(path, cutterPath, op)
            return combined
        }
    }

    return path
}
