package com.example.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EvaluatedShapeInstance
import com.example.engine.ProceduralEngine
import com.example.model.LayerGeometryKind
import com.example.model.ToolType
import com.example.model.Vec2Point
import com.example.ui.theme.DataGraphic
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
import com.example.viewmodel.GraphiteViewModel
import com.example.viewmodel.MobileWorkspaceTab
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
    val doc = uiState.document
    val selectedLayer = uiState.selectedLayer

    // Interactive gesture states for shape creation & freehand drawing
    var dragStartArtboard by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentArtboard by remember { mutableStateOf<Offset?>(null) }
    val activeStrokePoints = remember { mutableStateListOf<Vec2Point>() }
    var draggingVertexIndex by remember { mutableStateOf<Int?>(null) }

    // Canvas layout transform cache so pointer inputs map accurately to Artboard coordinates
    var currentScale by remember { mutableStateOf(1f) }
    var currentOriginX by remember { mutableStateOf(0f) }
    var currentOriginY by remember { mutableStateOf(0f) }

    fun screenToArtboard(screenOffset: Offset): Offset {
        val ax = (screenOffset.x - currentOriginX) / max(0.05f, currentScale)
        val ay = (screenOffset.y - currentOriginY) / max(0.05f, currentScale)
        return Offset(ax, ay)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteNearBlack)
    ) {
        // 1. Interactive 2D Viewport Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("graphite_2d_canvas")
                .pointerInput(uiState.activeTool, uiState.selectedLayerId, uiState.zoom, uiState.panX, uiState.panY) {
                    detectTapGestures(
                        onTap = { tapPos ->
                            val artPos = screenToArtboard(tapPos)
                            viewModel.handleCanvasTap(artPos.x, artPos.y)
                        },
                        onDoubleTap = {
                            viewModel.resetViewport()
                        }
                    )
                }
                .pointerInput(uiState.activeTool, uiState.selectedLayerId, uiState.zoom, uiState.panX, uiState.panY) {
                    detectDragGestures(
                        onDragStart = { startScreen ->
                            val artPos = screenToArtboard(startScreen)
                            when (uiState.activeTool) {
                                ToolType.PATH, ToolType.PEN, ToolType.SPLINE -> {
                                    val sel = uiState.selectedLayer
                                    if (sel != null && sel.points.isNotEmpty()) {
                                        val hitIdx = sel.points.indexOfFirst {
                                            hypot(it.x - artPos.x, it.y - artPos.y) <= 44f
                                        }
                                        draggingVertexIndex = if (hitIdx >= 0) hitIdx else null
                                    }
                                }
                                ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE -> {
                                    dragStartArtboard = artPos
                                    dragCurrentArtboard = artPos
                                }
                                ToolType.FREEHAND, ToolType.BRUSH -> {
                                    activeStrokePoints.clear()
                                    activeStrokePoints.add(Vec2Point(artPos.x, artPos.y))
                                }
                                else -> {}
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val artPos = screenToArtboard(change.position)
                            val artDeltaX = dragAmount.x / max(0.05f, currentScale)
                            val artDeltaY = dragAmount.y / max(0.05f, currentScale)

                            when (uiState.activeTool) {
                                ToolType.NAVIGATE -> {
                                    viewModel.updateViewport(
                                        zoom = uiState.zoom,
                                        panX = uiState.panX + dragAmount.x,
                                        panY = uiState.panY + dragAmount.y
                                    )
                                }
                                ToolType.SELECT -> {
                                    viewModel.moveSelectedLayerBy(artDeltaX, artDeltaY, isFinished = false)
                                }
                                ToolType.PATH, ToolType.PEN, ToolType.SPLINE -> {
                                    val vIdx = draggingVertexIndex
                                    if (vIdx != null) {
                                        viewModel.movePathVertex(vIdx, artPos.x, artPos.y, isFinished = false)
                                    } else {
                                        viewModel.moveSelectedLayerBy(artDeltaX, artDeltaY, isFinished = false)
                                    }
                                }
                                ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE -> {
                                    dragCurrentArtboard = artPos
                                }
                                ToolType.FREEHAND, ToolType.BRUSH -> {
                                    val lastPt = activeStrokePoints.lastOrNull()
                                    if (lastPt == null || hypot(artPos.x - lastPt.x, artPos.y - lastPt.y) >= 8f) {
                                        activeStrokePoints.add(Vec2Point(artPos.x, artPos.y))
                                    }
                                }
                                else -> {
                                    viewModel.moveSelectedLayerBy(artDeltaX, artDeltaY, isFinished = false)
                                }
                            }
                        },
                        onDragEnd = {
                            when (uiState.activeTool) {
                                ToolType.SELECT -> {
                                    viewModel.moveSelectedLayerBy(0f, 0f, isFinished = true)
                                }
                                ToolType.PATH, ToolType.PEN, ToolType.SPLINE -> {
                                    val vIdx = draggingVertexIndex
                                    val sel = uiState.selectedLayer
                                    if (vIdx != null && sel != null && vIdx in sel.points.indices) {
                                        val pt = sel.points[vIdx]
                                        viewModel.movePathVertex(vIdx, pt.x, pt.y, isFinished = true)
                                    }
                                    draggingVertexIndex = null
                                }
                                ToolType.RECTANGLE, ToolType.ELLIPSE, ToolType.POLYGON, ToolType.STAR, ToolType.LINE -> {
                                    val s = dragStartArtboard
                                    val e = dragCurrentArtboard
                                    if (s != null && e != null && hypot(e.x - s.x, e.y - s.y) > 16f) {
                                        viewModel.createShapeFromDrag(s.x, s.y, e.x, e.y)
                                    }
                                    dragStartArtboard = null
                                    dragCurrentArtboard = null
                                }
                                ToolType.FREEHAND, ToolType.BRUSH -> {
                                    if (activeStrokePoints.size >= 2) {
                                        viewModel.addFreehandOrBrushStroke(
                                            points = activeStrokePoints.toList(),
                                            isBrush = uiState.activeTool == ToolType.BRUSH
                                        )
                                    }
                                    activeStrokePoints.clear()
                                }
                                else -> {}
                            }
                        },
                        onDragCancel = {
                            dragStartArtboard = null
                            dragCurrentArtboard = null
                            activeStrokePoints.clear()
                            draggingVertexIndex = null
                        }
                    )
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
                for (layer in doc.layers) {
                    val evaluatedInstances = ProceduralEngine.evaluateLayer(layer)
                    for (inst in evaluatedInstances) {
                        drawEvaluatedInstance(
                            inst = inst,
                            wireframeOnly = uiState.wireframeOverlay,
                            invScale = 1f / totalScale
                        )
                    }
                }

                // Selection Cage & Node/Path Gizmos for the active layer
                selectedLayer?.let { sel ->
                    if (sel.visible) {
                        val halfW = sel.width / 2f
                        val halfH = sel.height / 2f
                        val topLeft = Offset(sel.x - halfW, sel.y - halfH)
                        val strokePx = 2f / totalScale

                        drawRect(
                            color = DataVector,
                            topLeft = topLeft,
                            size = Size(sel.width, sel.height),
                            style = Stroke(
                                width = strokePx,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / totalScale, 8f / totalScale))
                            )
                        )

                        // Corner resize/transform handles
                        val handleRadius = 7f / totalScale
                        val corners = listOf(
                            Offset(sel.x - halfW, sel.y - halfH),
                            Offset(sel.x + halfW, sel.y - halfH),
                            Offset(sel.x + halfW, sel.y + halfH),
                            Offset(sel.x - halfW, sel.y + halfH)
                        )
                        for (c in corners) {
                            drawCircle(color = GraphiteNearWhite, radius = handleRadius, center = c)
                            drawCircle(
                                color = DataVector,
                                radius = handleRadius,
                                center = c,
                                style = Stroke(width = 2f / totalScale)
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

                        // Path / Spline Anchor Handles
                        if (sel.points.isNotEmpty()) {
                            for (i in 0 until sel.points.size - 1) {
                                val p0 = sel.points[i]
                                val p1 = sel.points[i + 1]
                                drawLine(
                                    color = DataVector.copy(alpha = 0.7f),
                                    start = Offset(p0.x, p0.y),
                                    end = Offset(p1.x, p1.y),
                                    strokeWidth = 1.5f / totalScale
                                )
                            }
                            sel.points.forEachIndexed { idx, pt ->
                                val isDragged = idx == draggingVertexIndex
                                val r = (if (isDragged) 11f else 8f) / totalScale
                                drawCircle(
                                    color = if (isDragged) DataRaster else DataVector,
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
                    val w = max(8f, kotlin.math.abs(ePos.x - sPos.x))
                    val h = max(8f, kotlin.math.abs(ePos.y - sPos.y))
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

                // Live Freehand / Brush Stroke Preview
                if (activeStrokePoints.size >= 2) {
                    val strokePath = Path()
                    strokePath.moveTo(activeStrokePoints.first().x, activeStrokePoints.first().y)
                    for (i in 1 until activeStrokePoints.size) {
                        strokePath.lineTo(activeStrokePoints[i].x, activeStrokePoints[i].y)
                    }
                    drawPath(
                        path = strokePath,
                        color = Color(uiState.primaryColorHex.toInt()),
                        style = Stroke(
                            width = if (uiState.activeTool == ToolType.BRUSH) 18f else 5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Artboard border outline
                drawRect(
                    color = GraphiteDimGray,
                    topLeft = Offset.Zero,
                    size = Size(doc.artboardWidth.toFloat(), doc.artboardHeight.toFloat()),
                    style = Stroke(width = 1.5f / totalScale)
                )
            }

            // Optional Top & Left Rulers (Graphite RulerInput style)
            if (uiState.showRulers) {
                val rulerThick = 18.dp.toPx()
                drawRect(
                    color = GraphitePanelBg.copy(alpha = 0.9f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, rulerThick)
                )
                var rx = 0f
                while (rx < size.width) {
                    val isMajor = (rx.toInt() % 100) < 20
                    drawLine(
                        color = GraphiteLightGray.copy(alpha = if (isMajor) 0.5f else 0.25f),
                        start = Offset(rx, if (isMajor) 4f else 10f),
                        end = Offset(rx, rulerThick),
                        strokeWidth = 1f
                    )
                    rx += 20f
                }
            }
        }

        // 2. Top Floating Context Pill Bar (Document, Undo/Redo, Zoom, Snap, Grid, Export)
        TopCanvasControlPill(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 10.dp, end = 10.dp)
        )

        // 3. Toast / Hint Banner
        AnimatedVisibility(
            visible = uiState.statusToast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp, start = 16.dp, end = 16.dp)
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

        // 4. Bottom Dock: Selected Layer Node Strip + Working Colors + Scrollable Tool Groups
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Selected Layer & Working Colors Quick Bar
            QuickLayerAndColorsBar(
                uiState = uiState,
                viewModel = viewModel
            )

            // Grouped Mobile Tool Dock
            MobileToolDock(
                activeTool = uiState.activeTool,
                onSelectTool = { viewModel.selectTool(it) }
            )
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

            // Zoom - / + quick buttons
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

            // Wireframe Toggle
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

            // Export SVG / PNG / Graphite Button
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
                                text = selectedLayer.name,
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
                                imageVector = Icons.Default.AccountTree,
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
            LayerGeometryKind.BEZIER_PATH,
            LayerGeometryKind.FREEHAND,
            LayerGeometryKind.SPLINE,
            LayerGeometryKind.BRUSH_STROKE
        )

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
        LayerGeometryKind.LINE, LayerGeometryKind.BEZIER_PATH, LayerGeometryKind.FREEHAND, LayerGeometryKind.SPLINE, LayerGeometryKind.BRUSH_STROKE -> {
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
