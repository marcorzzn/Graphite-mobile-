package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ArtboardAlignment
import com.example.model.BlendModeType
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.ui.theme.DataColor
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
import kotlin.math.roundToInt

@Composable
fun LayersAndInspectorScreen(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel,
    modifier: Modifier = Modifier
) {
    val doc = uiState.document
    val selectedLayer = uiState.selectedLayer

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteNearBlack)
            .testTag("layers_inspector_screen")
    ) {
        // 1. Layer Stack Header + Quick Add Layer Row
        item {
            Surface(
                color = GraphitePanelBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteDimGray, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = DataVector)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pila Livelli (${doc.layers.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GraphiteNearWhite
                                )
                                Text(
                                    text = "Ordine di composizione dall'alto verso il basso",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GraphiteLightGray
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Quick Add Layer Geometry Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val quickKinds = listOf(
                            LayerGeometryKind.RECTANGLE,
                            LayerGeometryKind.ELLIPSE,
                            LayerGeometryKind.POLYGON,
                            LayerGeometryKind.STAR,
                            LayerGeometryKind.SPLINE,
                            LayerGeometryKind.TEXT
                        )
                        for (kind in quickKinds) {
                            AssistChip(
                                onClick = { viewModel.addQuickLayer(kind) },
                                label = { Text("+ ${kind.labelIt}", style = MaterialTheme.typography.labelLarge) },
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
                                    labelColor = GraphiteNearWhite
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Reversed Layer List (topmost layer first, matching Graphite Layers.svelte)
        items(
            items = doc.layers.asReversed(),
            key = { it.id }
        ) { layer ->
            val isSelected = layer.id == selectedLayer?.id
            LayerRowItem(
                layer = layer,
                isSelected = isSelected,
                onSelect = { viewModel.selectLayer(layer.id) },
                onToggleVisible = {
                    viewModel.updateLayer(layer.id) { it.copy(visible = !it.visible) }
                },
                onToggleLock = {
                    viewModel.updateLayer(layer.id) { it.copy(locked = !it.locked) }
                },
                onMoveUp = { viewModel.moveLayerOrder(layer.id, moveUpInVisualStack = true) },
                onMoveDown = { viewModel.moveLayerOrder(layer.id, moveUpInVisualStack = false) },
                onDuplicate = { viewModel.duplicateLayer(layer.id) },
                onDelete = { viewModel.deleteLayer(layer.id) },
                onOpenNodes = {
                    viewModel.selectLayer(layer.id)
                    viewModel.selectTab(MobileWorkspaceTab.NODE_GRAPH)
                }
            )
        }

        // 3. Detailed Property Inspector for Selected Layer
        if (selectedLayer != null) {
            item {
                LayerInspectorCard(
                    layer = selectedLayer,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun LayerRowItem(
    layer: GraphiteLayer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenNodes: () -> Unit
) {
    Surface(
        color = if (isSelected) GraphiteDarkGray else GraphitePanelBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) DataVector else GraphiteDimGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .testTag("layer_item_${layer.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Swatch preview
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(layer.fillColorHex.toInt()))
                            .border(1.dp, Color(layer.strokeColorHex.toInt()), RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = layer.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (layer.visible) GraphiteNearWhite else GraphiteLightGray
                        )
                        Text(
                            text = "${layer.kind.labelIt} • ${layer.blendMode.labelIt} • ${(layer.opacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = GraphiteLightGray
                        )
                    }
                }

                // Node Chain Badge (tap to jump to Node Graph)
                Surface(
                    color = GraphiteMildBlack,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, DataGraphic.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { onOpenNodes() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "Nodi",
                            tint = DataGraphic,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${layer.nodes.size} nodi",
                            style = MaterialTheme.typography.labelSmall,
                            color = DataGraphic
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Layer Action Controls Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onToggleVisible, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Visibilità",
                        tint = if (layer.visible) DataVector else GraphiteDimGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onToggleLock, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Blocca",
                        tint = if (layer.locked) DataRaster else GraphiteLightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Porta Avanti",
                        tint = GraphiteLightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Porta Indietro",
                        tint = GraphiteLightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplica",
                        tint = DataGraphic,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Elimina",
                        tint = GraphiteErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerInspectorCard(
    layer: GraphiteLayer,
    viewModel: GraphiteViewModel
) {
    Surface(
        color = GraphitePanelBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteDimGray, RoundedCornerShape(14.dp))
            .testTag("layer_inspector_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = DataRaster)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Ispettore Proprietà: ${layer.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = GraphiteNearWhite
                )
            }

            // Layer Name Input
            OutlinedTextField(
                value = layer.name,
                onValueChange = { newName ->
                    viewModel.updateLayer(layer.id, recordHistory = false) { it.copy(name = newName) }
                },
                label = { Text("Nome Livello") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // If Text Layer: Text Content & Font Size
            if (layer.kind == LayerGeometryKind.TEXT) {
                OutlinedTextField(
                    value = layer.textContent,
                    onValueChange = { txt ->
                        viewModel.updateLayer(layer.id, recordHistory = false) { it.copy(textContent = txt) }
                    },
                    label = { Text("Contenuto Testo Vettoriale") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                InspectorSliderRow(
                    label = "Dimensione Font",
                    value = layer.fontSize,
                    range = 16f..180f,
                    unit = "pt",
                    color = DataVector,
                    onValueChange = { v, commit ->
                        viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(fontSize = v) }
                    }
                )
            }

            // Blend Mode Selector Chips
            Text(
                text = "Metodo di Fusione (Blend Mode)",
                style = MaterialTheme.typography.labelLarge,
                color = GraphiteLightGray
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                for (bm in BlendModeType.entries) {
                    FilterChip(
                        selected = layer.blendMode == bm,
                        onClick = {
                            viewModel.updateLayer(layer.id) { it.copy(blendMode = bm) }
                        },
                        label = { Text(bm.labelIt, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DataVector.copy(alpha = 0.25f),
                            selectedLabelColor = GraphiteNearWhite
                        )
                    )
                }
            }

            // Opacity Slider
            InspectorSliderRow(
                label = "Opacità Livello",
                value = layer.opacity * 100f,
                range = 5f..100f,
                unit = "%",
                color = DataGraphic,
                onValueChange = { v, commit ->
                    viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(opacity = v / 100f) }
                }
            )

            HorizontalDivider(color = GraphiteDimGray)

            // Quick Artboard Alignment & Multi-Layer Distribution Bar (Fase 3 TP-B3 / TP-C3)
            Text(
                text = "Allineamento Rapido & Distribuzione su Tavola",
                style = MaterialTheme.typography.labelLarge,
                color = DataGraphic
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("artboard_alignment_bar")
            ) {
                for (align in ArtboardAlignment.entries) {
                    AssistChip(
                        onClick = { viewModel.alignSelectedLayer(align) },
                        label = {
                            Text(
                                text = align.shortLabelIt,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = GraphiteNearWhite
                        ),
                        modifier = Modifier.testTag("align_chip_${align.name.lowercase()}")
                    )
                }
            }

            // Patch v3.5: Pathfinder Multi-Layer Booleans & Bake to Editable Bézier Path
            Text(
                text = "Pathfinder Booleano & Conversione in Tracciato Bézier",
                style = MaterialTheme.typography.labelLarge,
                color = DataRaster
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("pathfinder_boolean_bar")
            ) {
                if (layer.kind != LayerGeometryKind.TEXT && layer.kind != LayerGeometryKind.BEZIER_PATH) {
                    AssistChip(
                        onClick = { viewModel.bakeSelectedLayerToBezierPath() },
                        label = { Text("✒️ Converti in Tracciato", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = DataRaster
                        ),
                        modifier = Modifier.testTag("inspector_bake_path_chip")
                    )
                }
                val boolActions = listOf(
                    0 to "⊖ Sottrai (Fora)",
                    1 to "⊕ Unisci",
                    2 to "⊗ Interseca",
                    3 to "⊘ Escludi XOR"
                )
                for ((opCode, opLabel) in boolActions) {
                    AssistChip(
                        onClick = { viewModel.combineSelectedWithBelowBoolean(opCode) },
                        label = { Text(opLabel, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GraphiteDarkGray,
                            labelColor = GraphiteNearWhite
                        ),
                        modifier = Modifier.testTag("pathfinder_op_$opCode")
                    )
                }
            }

            // Geometry & Transform Section
            Text(
                text = "Trasformazione & Geometria Parametrica",
                style = MaterialTheme.typography.labelLarge,
                color = DataVector
            )

            InspectorSliderRow(
                label = "Posizione X",
                value = layer.x,
                range = 0f..1200f,
                unit = "px",
                color = DataNumber,
                onValueChange = { v, commit ->
                    viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(x = v) }
                }
            )
            InspectorSliderRow(
                label = "Posizione Y",
                value = layer.y,
                range = 0f..1200f,
                unit = "px",
                color = DataNumber,
                onValueChange = { v, commit ->
                    viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(y = v) }
                }
            )
            InspectorSliderRow(
                label = "Larghezza (W)",
                value = layer.width,
                range = 20f..1000f,
                unit = "px",
                color = DataVector,
                onValueChange = { v, commit ->
                    viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(width = v) }
                }
            )
            InspectorSliderRow(
                label = "Altezza (H)",
                value = layer.height,
                range = 20f..1000f,
                unit = "px",
                color = DataVector,
                onValueChange = { v, commit ->
                    viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(height = v) }
                }
            )
            InspectorSliderRow(
                label = "Rotazione",
                value = layer.rotation,
                range = -180f..180f,
                unit = "°",
                color = DataRaster,
                onValueChange = { v, commit ->
                    viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(rotation = v) }
                }
            )

            if (layer.kind == LayerGeometryKind.RECTANGLE) {
                InspectorSliderRow(
                    label = "Raggio Angoli",
                    value = layer.cornerRadius,
                    range = 0f..120f,
                    unit = "px",
                    color = DataVector,
                    onValueChange = { v, commit ->
                        viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(cornerRadius = v) }
                    }
                )
            }

            if (layer.kind == LayerGeometryKind.POLYGON || layer.kind == LayerGeometryKind.STAR) {
                InspectorSliderRow(
                    label = "Numero Lati / Punte",
                    value = layer.polygonSides.toFloat(),
                    range = 3f..16f,
                    unit = "",
                    color = DataGraphic,
                    onValueChange = { v, commit ->
                        viewModel.updateLayer(layer.id, recordHistory = commit) {
                            it.copy(polygonSides = v.roundToInt().coerceIn(3, 16))
                        }
                    }
                )
            }

            if (layer.kind == LayerGeometryKind.STAR) {
                InspectorSliderRow(
                    label = "Raggio Interno Stella",
                    value = layer.starInnerRatio * 100f,
                    range = 15f..85f,
                    unit = "%",
                    color = DataRaster,
                    onValueChange = { v, commit ->
                        viewModel.updateLayer(layer.id, recordHistory = commit) {
                            it.copy(starInnerRatio = (v / 100f).coerceIn(0.15f, 0.85f))
                        }
                    }
                )
            }

            HorizontalDivider(color = GraphiteDimGray)

            // Fill, Gradient & Stroke Section
            Text(
                text = "Riempimento, Sfumatura & Traccia",
                style = MaterialTheme.typography.labelLarge,
                color = DataColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Riempimento Attivo", style = MaterialTheme.typography.bodyMedium, color = GraphiteNearWhite)
                Switch(
                    checked = layer.hasFill,
                    onCheckedChange = { enabled ->
                        viewModel.updateLayer(layer.id) { it.copy(hasFill = enabled) }
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sfumatura (Gradient)", style = MaterialTheme.typography.bodyMedium, color = GraphiteNearWhite)
                Switch(
                    checked = layer.useGradient,
                    onCheckedChange = { grad ->
                        viewModel.updateLayer(layer.id) { it.copy(useGradient = grad) }
                    }
                )
            }

            if (layer.useGradient) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sfumatura Radiale", style = MaterialTheme.typography.bodyMedium, color = GraphiteLightGray)
                    Switch(
                        checked = layer.isRadialGradient,
                        onCheckedChange = { radial ->
                            viewModel.updateLayer(layer.id) { it.copy(isRadialGradient = radial) }
                        }
                    )
                }
                if (!layer.isRadialGradient) {
                    InspectorSliderRow(
                        label = "Angolo Sfumatura",
                        value = layer.gradientAngle,
                        range = 0f..360f,
                        unit = "°",
                        color = DataColor,
                        onValueChange = { v, commit ->
                            viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(gradientAngle = v) }
                        }
                    )
                }
            }

            // Color pickers row for Fill & Stroke
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = GraphiteDarkGray,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.openColorPicker(isPrimary = true) }
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(layer.fillColorHex.toInt()))
                                .border(1.dp, GraphiteNearWhite, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Colore Riemp.", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Surface(
                    color = GraphiteDarkGray,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.openColorPicker(isPrimary = false) }
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(layer.strokeColorHex.toInt()))
                                .border(1.dp, GraphiteNearWhite, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Colore Traccia", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            InspectorSliderRow(
                label = "Spessore Contorno (Stroke)",
                value = layer.strokeWidth,
                range = 0f..48f,
                unit = "px",
                color = DataColor,
                onValueChange = { v, commit ->
                    viewModel.updateLayer(layer.id, recordHistory = commit) { it.copy(strokeWidth = v) }
                }
            )
        }
    }
}

@Composable
private fun InspectorSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    color: Color,
    onValueChange: (Float, Boolean) -> Unit
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(1f)
    val microStep = if (span <= 20f) 1f else if (span <= 180f) 2f else 5f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = GraphiteLightGray)

            // Phase 5 (TP-D5 — Gruppo D): Enterprise Precision Numeric Scrubber & Micro-Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = GraphiteDarkGray,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable {
                            val next = (value - microStep).coerceIn(range.start, range.endInclusive)
                            onValueChange(next, true)
                        }
                ) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.labelSmall,
                        color = GraphiteNearWhite,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = GraphiteNearBlack,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .pointerInput(range) {
                            var scrubVal = value
                            detectHorizontalDragGestures(
                                onDragStart = { scrubVal = value },
                                onDragEnd = { onValueChange(scrubVal, true) },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = (dragAmount / 180f) * span
                                    scrubVal = (scrubVal + delta).coerceIn(range.start, range.endInclusive)
                                    onValueChange(scrubVal, false)
                                }
                            )
                        }
                ) {
                    Text(
                        text = "⟷ ${value.roundToInt()}$unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = GraphiteDarkGray,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable {
                            val next = (value + microStep).coerceIn(range.start, range.endInclusive)
                            onValueChange(next, true)
                        }
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.labelSmall,
                        color = GraphiteNearWhite,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = { onValueChange(it, false) },
            onValueChangeFinished = { onValueChange(value, true) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = GraphiteDarkGray
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}
