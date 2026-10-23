package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.ProceduralEngine
import com.example.model.GraphiteLayer
import com.example.model.NodeDataType
import com.example.model.NodeParamSpec
import com.example.model.ProceduralNode
import com.example.ui.theme.DataArtboard
import com.example.ui.theme.DataGraphic
import com.example.ui.theme.DataNumber
import com.example.ui.theme.DataRaster
import com.example.ui.theme.DataVector
import com.example.ui.theme.GraphiteDarkGray
import com.example.ui.theme.GraphiteDimGray
import com.example.ui.theme.GraphiteErrorRed
import com.example.ui.theme.GraphiteLightGray
import com.example.ui.theme.GraphiteMildBlack
import com.example.ui.theme.GraphiteNearBlack
import com.example.ui.theme.GraphiteNearWhite
import com.example.ui.theme.GraphitePanelBg
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.GraphiteViewModel
import com.example.viewmodel.MobileWorkspaceTab
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun NodeGraphWorkspaceScreen(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel,
    modifier: Modifier = Modifier
) {
    val doc = uiState.document
    val selectedLayer = uiState.selectedLayer
    val evaluatedInstances = remember(selectedLayer) {
        selectedLayer?.let { ProceduralEngine.evaluateLayer(it) } ?: emptyList()
    }

    // State for Math Expression popup calculator (like Graphite's MathExpressionInput.svelte)
    var mathDialogTarget by remember { mutableStateOf<Triple<String, NodeParamSpec, Float>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteNearBlack)
    ) {
        // Dotted Node Graph Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            var x = step / 2f
            while (x < size.width) {
                var y = step / 2f
                while (y < size.height) {
                    drawCircle(
                        color = GraphiteDimGray.copy(alpha = 0.35f),
                        radius = 1.5f,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header: Layer Switcher + Live Mini Preview + Preset Recipes
            Surface(
                color = GraphitePanelBg.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = DataGraphic,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Grafo Nodi Procedurale",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GraphiteNearWhite
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Pipeline non distruttiva • ${evaluatedInstances.size} istanze generate sul livello attivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = GraphiteLightGray
                            )
                        }

                        // Live Mini Viewport PIP (tap to return to full 2D Canvas)
                        LiveNodeMiniPreview(
                            uiState = uiState,
                            onClick = { viewModel.selectTab(MobileWorkspaceTab.CANVAS) }
                        )
                    }

                    // Layer selector pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(doc.layers) { layer ->
                            val isSelected = layer.id == selectedLayer?.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectLayer(layer.id) },
                                label = {
                                    Text(
                                        text = "${layer.name} (${layer.nodes.size} nodi)",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DataVector.copy(alpha = 0.25f),
                                    selectedLabelColor = GraphiteNearWhite,
                                    selectedLeadingIconColor = DataVector
                                )
                            )
                        }
                    }

                    // Quick 1-Tap Procedural Recipes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Preset:",
                            style = MaterialTheme.typography.labelMedium,
                            color = GraphiteLightGray
                        )
                        AssistChip(
                            onClick = { viewModel.applyProceduralPresetRecipe("mandala") },
                            label = { Text("Mandala Radiale", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = DataGraphic,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = GraphiteDarkGray)
                        )
                        AssistChip(
                            onClick = { viewModel.applyProceduralPresetRecipe("isometric_grid") },
                            label = { Text("Matrice Griglia", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = DataVector,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = GraphiteDarkGray)
                        )
                        AssistChip(
                            onClick = { viewModel.applyProceduralPresetRecipe("wave_mirror") },
                            label = { Text("Onda Simmetrica", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = DataRaster,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = GraphiteDarkGray)
                        )
                    }
                }
            }

            // Main Scrollable Node Pipeline
            if (selectedLayer != null) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("node_graph_list")
                ) {
                    // 1. Source Geometry Generator Node
                    item(key = "source_${selectedLayer.id}") {
                        SourceGeometryNodeCard(
                            layer = selectedLayer,
                            onInspectGeometry = { viewModel.selectTab(MobileWorkspaceTab.LAYERS_INSPECTOR) }
                        )
                        NodeWireConnector(
                            fromType = selectedLayer.kind.dataType,
                            toType = selectedLayer.nodes.firstOrNull()?.type?.inputType ?: NodeDataType.ARTBOARD,
                            label = "Flusso ${selectedLayer.kind.dataType.label}"
                        )
                    }

                    // 2. Non-destructive Procedural Modifier Nodes
                    itemsIndexed(
                        items = selectedLayer.nodes,
                        key = { _, node -> node.id }
                    ) { index, node ->
                        ProceduralNodeCard(
                            node = node,
                            index = index,
                            totalNodes = selectedLayer.nodes.size,
                            onToggleEnabled = { viewModel.toggleNodeEnabled(selectedLayer.id, node.id) },
                            onMoveUp = { viewModel.moveNodeOrder(selectedLayer.id, node.id, -1) },
                            onMoveDown = { viewModel.moveNodeOrder(selectedLayer.id, node.id, 1) },
                            onDelete = { viewModel.removeNode(selectedLayer.id, node.id) },
                            onParamChange = { key, value, commit ->
                                viewModel.updateNodeParam(selectedLayer.id, node.id, key, value, recordHistory = commit)
                            },
                            onOpenMathInput = { spec, currentVal ->
                                mathDialogTarget = Triple(node.id, spec, currentVal)
                            }
                        )

                        val nextInputType = selectedLayer.nodes.getOrNull(index + 1)?.type?.inputType ?: NodeDataType.ARTBOARD
                        NodeWireConnector(
                            fromType = node.type.outputType,
                            toType = nextInputType,
                            label = "${node.type.outputType.label} → ${nextInputType.label}"
                        )
                    }

                    // 3. Output Artboard Compositor Node
                    item(key = "output_${selectedLayer.id}") {
                        OutputCompositorNodeCard(
                            layer = selectedLayer,
                            instanceCount = evaluatedInstances.size,
                            onAddNodeClick = { viewModel.setNodeCatalogVisible(true) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to open Node Catalog
        ExtendedFloatingActionButton(
            onClick = { viewModel.setNodeCatalogVisible(true) },
            containerColor = DataGraphic,
            contentColor = GraphiteNearBlack,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Aggiungi Nodo", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_node_fab")
        )
    }

    // Math Expression Input Dialog (Graphite MathParser feature)
    mathDialogTarget?.let { (nodeId, spec, currentVal) ->
        var expressionText by remember(nodeId, spec.key) {
            mutableStateOf(String.format(Locale.US, "%.2f", currentVal))
        }
        val previewVal = ProceduralEngine.evaluateMathExpression(expressionText, currentVal)
            .coerceIn(spec.min, spec.max)

        AlertDialog(
            onDismissRequest = { mathDialogTarget = null },
            containerColor = GraphitePanelBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Functions, contentDescription = null, tint = DataNumber)
                    Spacer(Modifier.width(8.dp))
                    Text("Espressione Matematica: ${spec.labelIt}", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Inserisci un numero o una formula (es. 360 / 8, 120 * 1.5, sin(45) * 80, pi * 15):",
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteLightGray
                    )
                    OutlinedTextField(
                        value = expressionText,
                        onValueChange = { expressionText = it },
                        singleLine = true,
                        label = { Text("Formula / Valore (${spec.min.toInt()} .. ${spec.max.toInt()})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Risultato calcolato: ${String.format(Locale.US, "%.2f", previewVal)}${spec.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DataGraphic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedLayer?.let { layer ->
                            viewModel.updateNodeParam(layer.id, nodeId, spec.key, previewVal, recordHistory = true)
                        }
                        mathDialogTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DataVector, contentColor = GraphiteNearBlack)
                ) {
                    Text("Applica")
                }
            },
            dismissButton = {
                TextButton(onClick = { mathDialogTarget = null }) {
                    Text("Annulla", color = GraphiteLightGray)
                }
            }
        )
    }
}

@Composable
private fun LiveNodeMiniPreview(
    uiState: EditorUiState,
    onClick: () -> Unit
) {
    val doc = uiState.document
    Surface(
        color = GraphiteNearBlack,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .size(width = 96.dp, height = 72.dp)
            .border(1.5.dp, DataVector, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("node_graph_mini_preview")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = min(
                    size.width / max(1f, doc.artboardWidth.toFloat()),
                    size.height / max(1f, doc.artboardHeight.toFloat())
                ) * 0.88f
                val ox = (size.width - doc.artboardWidth * scale) / 2f
                val oy = (size.height - doc.artboardHeight * scale) / 2f
                withTransform({
                    translate(ox, oy)
                    scale(scale, scale, Offset.Zero)
                }) {
                    drawRect(
                        color = Color(doc.artboardBgHex.toInt()),
                        size = Size(doc.artboardWidth.toFloat(), doc.artboardHeight.toFloat())
                    )
                    for (layer in doc.layers) {
                        val evaluated = ProceduralEngine.evaluateLayer(layer)
                        for (inst in evaluated) {
                            drawCircle(
                                color = Color(inst.fillColorHex.toInt()).copy(alpha = inst.opacity),
                                radius = max(16f, min(inst.width, inst.height) * 0.45f),
                                center = Offset(inst.centerX, inst.centerY)
                            )
                        }
                    }
                }
            }
            Text(
                text = "LIVE 2D",
                style = MaterialTheme.typography.labelSmall,
                color = DataVector,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(GraphiteNearBlack.copy(alpha = 0.75f), RoundedCornerShape(topStart = 4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun SourceGeometryNodeCard(
    layer: GraphiteLayer,
    onInspectGeometry: () -> Unit
) {
    Surface(
        color = GraphitePanelBg,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, DataVector, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(DataVector)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sorgente: ${layer.kind.labelIt}",
                            style = MaterialTheme.typography.titleSmall,
                            color = GraphiteNearWhite
                        )
                        Text(
                            text = "Livello '${layer.name}' • Centro (${layer.x.toInt()}, ${layer.y.toInt()}) • Dim ${layer.width.toInt()}×${layer.height.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GraphiteLightGray
                        )
                    }
                }
                TextButton(onClick = onInspectGeometry) {
                    Text("Modifica Base", color = DataVector, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ProceduralNodeCard(
    node: ProceduralNode,
    index: Int,
    totalNodes: Int,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onParamChange: (String, Float, Boolean) -> Unit,
    onOpenMathInput: (NodeParamSpec, Float) -> Unit
) {
    val borderColor = if (node.enabled) node.type.outputType.color else GraphiteDimGray
    Surface(
        color = if (node.enabled) GraphitePanelBg else GraphiteMildBlack.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .testTag("procedural_node_card_${node.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Node Header with Data Pin, Title, Enable/Bypass, Reorder & Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Input / Output Pin Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(node.type.outputType.color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = node.type.titleIt,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (node.enabled) GraphiteNearWhite else GraphiteLightGray
                        )
                        Text(
                            text = node.type.descriptionIt,
                            style = MaterialTheme.typography.bodySmall,
                            color = GraphiteLightGray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleEnabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (node.enabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Attiva/Disattiva Nodo",
                            tint = if (node.enabled) DataGraphic else GraphiteDimGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (index > 0) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Sposta Su",
                                tint = GraphiteLightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (index < totalNodes - 1) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Sposta Giù",
                                tint = GraphiteLightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Rimuovi Nodo",
                            tint = GraphiteErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (node.enabled) {
                Spacer(Modifier.height(8.dp))
                // Parameter Sliders + Math Expression Button
                for (spec in node.type.paramSpecs) {
                    val currentVal = node.params[spec.key] ?: spec.defaultVal
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(DataNumber)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = spec.labelIt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GraphiteLightGray
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GraphiteDarkGray)
                                    .clickable { onOpenMathInput(spec, currentVal) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", currentVal)}${spec.unit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DataNumber
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Functions,
                                    contentDescription = "Formula",
                                    tint = DataVector,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Slider(
                            value = currentVal.coerceIn(spec.min, spec.max),
                            onValueChange = { onParamChange(spec.key, it, false) },
                            onValueChangeFinished = { onParamChange(spec.key, currentVal, true) },
                            valueRange = spec.min..spec.max,
                            colors = SliderDefaults.colors(
                                thumbColor = node.type.outputType.color,
                                activeTrackColor = node.type.outputType.color,
                                inactiveTrackColor = GraphiteDarkGray
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeWireConnector(
    fromType: NodeDataType,
    toType: NodeDataType,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(start = 24.dp)
    ) {
        Canvas(modifier = Modifier.size(width = 24.dp, height = 38.dp)) {
            val path = Path().apply {
                moveTo(size.width / 2f, 0f)
                cubicTo(
                    size.width / 2f, size.height * 0.4f,
                    size.width / 2f, size.height * 0.6f,
                    size.width / 2f, size.height
                )
            }
            drawPath(
                path = path,
                color = fromType.color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawCircle(
                color = toType.color,
                radius = 4.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fromType.color.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun OutputCompositorNodeCard(
    layer: GraphiteLayer,
    instanceCount: Int,
    onAddNodeClick: () -> Unit
) {
    Surface(
        color = GraphitePanelBg,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, DataArtboard, RoundedCornerShape(12.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(DataArtboard)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Output Compositor (${layer.blendMode.labelIt})",
                        style = MaterialTheme.typography.titleSmall,
                        color = DataArtboard
                    )
                    Text(
                        text = "Renderizza $instanceCount elementi procedurali • Opacità ${(layer.opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteLightGray
                    )
                }
            }
            Button(
                onClick = onAddNodeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DataGraphic,
                    contentColor = GraphiteNearBlack
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Inserisci Nodo", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
