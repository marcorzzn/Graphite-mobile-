package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.engine.ProceduralEngine
import com.example.model.DemoArtworkInfo
import com.example.model.GraphiteDocumentModel
import com.example.model.NodeCategory
import com.example.model.OFFICIAL_DEMO_ARTWORKS
import com.example.model.ProceduralNodeType
import com.example.ui.theme.DataArtboard
import com.example.ui.theme.DataColor
import com.example.ui.theme.DataGraphic
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioAndGalleryScreen(
    uiState: EditorUiState,
    savedDocuments: List<GraphiteDocumentModel>,
    viewModel: GraphiteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

    // Native Storage Access Framework (SAF) Launcher to import a .graphite file from device
    val importGraphiteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    viewModel.importGraphiteJsonDocument(content)
                } else {
                    viewModel.showToast("File vuoto o illeggibile")
                }
            } catch (e: Exception) {
                viewModel.showToast("Errore apertura file: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteNearBlack)
            .testTag("portfolio_screen")
    ) {
        // 1. Graphite Welcome & Active Artboard Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DataVector.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GraphitePanelBg, Color(0xFF1A2836), GraphiteMildBlack)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hexagon,
                                    contentDescription = null,
                                    tint = DataVector,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Graphite Mobile Studio",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = GraphiteNearWhite
                                    )
                                    Text(
                                        text = "Motore grafico procedurale 2D vettoriale & raster",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GraphiteLightGray
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.setNewDocDialogVisible(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DataVector,
                                    contentColor = GraphiteNearBlack
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_document_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Nuovo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    importGraphiteLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_graphite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = null,
                                    tint = DataRaster,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Importa", color = GraphiteNearWhite)
                            }

                            OutlinedButton(
                                onClick = { viewModel.setExportDialogVisible(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("portfolio_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = DataGraphic,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Salva/Esporta", color = GraphiteNearWhite)
                            }
                        }
                    }
                }
            }
        }

        // 2. Official Graphite Demo Artwork Gallery (with real extracted thumbnails!)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DataRaster)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Galleria Demo Ufficiale Graphite",
                                style = MaterialTheme.typography.titleMedium,
                                color = GraphiteNearWhite
                            )
                            Text(
                                text = "Tocca un'opera per aprirla ed esplorare i suoi nodi procedurali",
                                style = MaterialTheme.typography.bodySmall,
                                color = GraphiteLightGray
                            )
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.testTag("demo_artwork_gallery_row")
                ) {
                    items(OFFICIAL_DEMO_ARTWORKS, key = { it.id }) { demo ->
                        DemoArtworkCard(
                            demo = demo,
                            isCurrent = uiState.document.demoId == demo.id,
                            onOpen = { viewModel.openDemoArtwork(demo.id) }
                        )
                    }
                }
            }
        }

        // 3. Saved Local Projects in Room Database
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = DataGraphic)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Progetti Salvati sul Dispositivo (${savedDocuments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = GraphiteNearWhite
                )
            }
        }

        items(
            items = savedDocuments,
            key = { it.id }
        ) { doc ->
            val isCurrent = doc.id == uiState.document.id
            val totalNodes = doc.layers.sumOf { it.nodes.size }
            Surface(
                color = if (isCurrent) GraphiteDarkGray else GraphitePanelBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) DataGraphic else GraphiteDimGray,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.openSavedDocument(doc) }
                    .testTag("saved_doc_${doc.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(doc.artboardBgHex.toInt()))
                                .border(1.dp, DataVector, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hexagon,
                                contentDescription = null,
                                tint = DataVector,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = doc.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GraphiteNearWhite
                                )
                                if (isCurrent) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "ATTIVO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DataGraphic
                                    )
                                }
                            }
                            Text(
                                text = "${doc.artboardWidth}×${doc.artboardHeight} • ${doc.layers.size} livelli • $totalNodes nodi • ${dateFormat.format(Date(doc.updatedAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GraphiteLightGray
                            )
                        }
                    }

                    if (savedDocuments.size > 1) {
                        IconButton(onClick = { viewModel.deleteSavedDocument(doc.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Elimina Progetto",
                                tint = GraphiteErrorRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoArtworkCard(
    demo: DemoArtworkInfo,
    isCurrent: Boolean,
    onOpen: () -> Unit
) {
    Surface(
        color = GraphitePanelBg,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 6.dp,
        modifier = Modifier
            .width(230.dp)
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) DataRaster else GraphiteDimGray,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onOpen() }
            .testTag("demo_card_${demo.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                Image(
                    painter = painterResource(id = demo.thumbnailRes),
                    contentDescription = demo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = GraphiteNearBlack.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = demo.nodeCountBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = DataGraphic,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = demo.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = GraphiteNearWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = demo.subtitleIt,
                    style = MaterialTheme.typography.bodySmall,
                    color = GraphiteLightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (tag in demo.tags.take(2)) {
                        Surface(
                            color = GraphiteDarkGray,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = DataVector,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeCatalogBottomSheet(
    onDismiss: () -> Unit,
    onSelectNodeType: (ProceduralNodeType) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<NodeCategory?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GraphitePanelBg,
        contentColor = GraphiteNearWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = DataGraphic)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Catalogo Nodi Procedurali Graphite",
                        style = MaterialTheme.typography.titleMedium,
                        color = GraphiteNearWhite
                    )
                    Text(
                        text = "Aggiungi un modificatore non distruttivo alla pipeline del livello attivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteLightGray
                    )
                }
            }

            // Category Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Tutti (${ProceduralNodeType.entries.size})") }
                )
                for (cat in NodeCategory.entries) {
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.labelIt) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.dataType.color.copy(alpha = 0.25f)
                        )
                    )
                }
            }

            val filteredNodes = ProceduralNodeType.entries.filter {
                selectedCategory == null || it.category == selectedCategory
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                modifier = Modifier.height(360.dp)
            ) {
                items(filteredNodes) { nodeType ->
                    Surface(
                        color = GraphiteDarkGray,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, nodeType.outputType.color.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .clickable { onSelectNodeType(nodeType) }
                            .testTag("catalog_node_${nodeType.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(nodeType.outputType.color)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = nodeType.titleIt,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GraphiteNearWhite
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = nodeType.descriptionIt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GraphiteLightGray
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = GraphiteMildBlack,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${nodeType.inputType.label} → ${nodeType.outputType.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = nodeType.outputType.color,
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

@Composable
fun GraphiteColorPickerDialog(
    initialColorHex: Long,
    isPrimary: Boolean,
    onDismiss: () -> Unit,
    onSelectColor: (Long) -> Unit
) {
    val hsl = remember(initialColorHex) {
        val out = FloatArray(3)
        val c = initialColorHex.toInt()
        ColorUtils.RGBToHSL((c ushr 16) and 0xFF, (c ushr 8) and 0xFF, c and 0xFF, out)
        out
    }
    var hue by remember { mutableFloatStateOf(hsl[0]) }
    var sat by remember { mutableFloatStateOf(hsl[1].coerceIn(0.1f, 1f)) }
    var light by remember { mutableFloatStateOf(hsl[2].coerceIn(0.1f, 0.95f)) }

    val currentRgbInt = ColorUtils.HSLToColor(floatArrayOf(hue, sat, light))
    val currentHexLong = 0xFF000000L or (currentRgbInt.toLong() and 0x00FFFFFFL)

    val studioSwatches = listOf(
        0xFF65BBE5L, // Vector Blue
        0xFF68C587L, // Graphic Green
        0xFFE4BB72L, // Raster Gold
        0xFFCE6EA7L, // Color Magenta
        0xFFD6536EL, // Crimson Red
        0xFFFBF9EBL, // Artboard Cream
        0xFFC9A699L, // Number Rose
        0xFF39A9DBL, // Cyan Ocean
        0xFFE07A47L, // Autumn Amber
        0xFF9B6EF3L, // Procedural Violet
        0xFF22252AL, // Mild Black
        0xFF141E28L  // Deep Slate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphitePanelBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = DataColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isPrimary) "Colore Primario / Riempimento" else "Colore Secondario / Traccia",
                    style = MaterialTheme.typography.titleMedium,
                    color = GraphiteNearWhite
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Preview Banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(currentHexLong.toInt()))
                        .border(1.dp, GraphiteNearWhite, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = ProceduralEngine.hexToSvgColor(currentHexLong),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (light > 0.55f) GraphiteNearBlack else GraphiteNearWhite
                    )
                    Text(
                        text = "H:${hue.toInt()}° S:${(sat * 100).toInt()}% L:${(light * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (light > 0.55f) GraphiteNearBlack else GraphiteNearWhite
                    )
                }

                // Hue, Saturation, Lightness Sliders
                Text("Tonalità (Hue): ${hue.toInt()}°", style = MaterialTheme.typography.bodySmall, color = GraphiteLightGray)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = DataColor, activeTrackColor = DataColor)
                )

                Text("Saturazione: ${(sat * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = GraphiteLightGray)
                Slider(
                    value = sat,
                    onValueChange = { sat = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = DataVector, activeTrackColor = DataVector)
                )

                Text("Luminosità: ${(light * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = GraphiteLightGray)
                Slider(
                    value = light,
                    onValueChange = { light = it },
                    valueRange = 0.05f..0.98f,
                    colors = SliderDefaults.colors(thumbColor = DataArtboard, activeTrackColor = DataArtboard)
                )

                Text("Palette Ufficiale Graphite:", style = MaterialTheme.typography.labelMedium, color = GraphiteLightGray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (swatch in studioSwatches) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(swatch.toInt()))
                                .border(1.5.dp, GraphiteNearWhite, CircleShape)
                                .clickable {
                                    onSelectColor(swatch)
                                    onDismiss()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectColor(currentHexLong)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DataVector, contentColor = GraphiteNearBlack)
            ) {
                Text("Applica Colore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi", color = GraphiteLightGray)
            }
        }
    )
}

@Composable
fun ExportDocumentDialog(
    doc: GraphiteDocumentModel,
    onDismiss: () -> Unit,
    onCopiedToast: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableIntStateOf(0) } // 0 = SVG, 1 = PNG HD, 2 = .graphite JSON
    val svgCode = remember(doc) { ProceduralEngine.exportToSvg(doc) }
    val graphiteJson = remember(doc) { ProceduralEngine.toGraphiteJson(doc) }
    val previewBitmap = remember(doc) { ProceduralEngine.renderToBitmap(doc, targetSide = 640) }

    val activeText = when (selectedFormat) {
        0 -> svgCode
        1 -> "Immagine Raster PNG Lossless (${doc.artboardWidth}×${doc.artboardHeight}px) pronta per il salvataggio su file system."
        else -> graphiteJson
    }

    // Native Storage Access Framework (SAF) Launchers for direct file save on device
    val createSvgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/svg+xml")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(svgCode.toByteArray(Charsets.UTF_8))
                }
                onCopiedToast("File SVG salvato correttamente sul dispositivo!")
            } catch (e: Exception) {
                onCopiedToast("Errore salvataggio SVG: ${e.localizedMessage}")
            }
        }
    }

    val createPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            try {
                val hiResBmp = ProceduralEngine.renderToBitmap(doc, targetSide = 2048)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    hiResBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                onCopiedToast("Immagine PNG HD (2048px) salvata sul dispositivo!")
            } catch (e: Exception) {
                onCopiedToast("Errore salvataggio PNG: ${e.localizedMessage}")
            }
        }
    }

    val createGraphiteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(graphiteJson.toByteArray(Charsets.UTF_8))
                }
                onCopiedToast("Progetto .graphite salvato sul dispositivo!")
            } catch (e: Exception) {
                onCopiedToast("Errore salvataggio .graphite: ${e.localizedMessage}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphitePanelBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = DataGraphic)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Salva ed Esporta '${doc.name}'",
                    style = MaterialTheme.typography.titleMedium,
                    color = GraphiteNearWhite
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // High-res Rendered Bitmap Preview
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GraphiteNearBlack)
                        .border(1.dp, GraphiteDimGray, RoundedCornerShape(10.dp))
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Anteprima Render",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedFormat == 0,
                        onClick = { selectedFormat = 0 },
                        label = { Text("SVG 1.1") }
                    )
                    FilterChip(
                        selected = selectedFormat == 1,
                        onClick = { selectedFormat = 1 },
                        label = { Text("PNG HD (2048px)") }
                    )
                    FilterChip(
                        selected = selectedFormat == 2,
                        onClick = { selectedFormat = 2 },
                        label = { Text(".graphite DAG") }
                    )
                }

                Surface(
                    color = GraphiteNearBlack,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(1.dp, GraphiteDimGray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = activeText.take(600),
                        style = MaterialTheme.typography.labelSmall,
                        color = DataGraphic,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedFormat != 1) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    if (selectedFormat == 0) "${doc.name}.svg" else "${doc.name}.graphite",
                                    activeText
                                )
                            )
                            onCopiedToast("Codice ${if (selectedFormat == 0) "SVG" else ".graphite"} copiato negli appunti!")
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copia")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (selectedFormat == 0) "image/svg+xml" else "application/json"
                            putExtra(Intent.EXTRA_TITLE, doc.name)
                            putExtra(Intent.EXTRA_TEXT, if (selectedFormat == 0) svgCode else graphiteJson)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Condividi ${doc.name}"))
                    }
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                Button(
                    onClick = {
                        val cleanBaseName = doc.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_-]"), "_")
                        when (selectedFormat) {
                            0 -> createSvgLauncher.launch("$cleanBaseName.svg")
                            1 -> createPngLauncher.launch("$cleanBaseName.png")
                            else -> createGraphiteLauncher.launch("$cleanBaseName.graphite")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DataGraphic, contentColor = GraphiteNearBlack),
                    modifier = Modifier.testTag("save_file_saf_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Salva File", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi", color = GraphiteLightGray)
            }
        }
    )
}

@Composable
fun NewDocumentDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, width: Int, height: Int, bgHex: Long) -> Unit
) {
    var docName by remember { mutableStateOf("Nuova Composizione") }
    var widthStr by remember { mutableStateOf("1080") }
    var heightStr by remember { mutableStateOf("1080") }
    var selectedBg by remember { mutableStateOf(0xFF161920L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphitePanelBg,
        title = {
            Text("Nuovo Documento Graphite", style = MaterialTheme.typography.titleMedium, color = GraphiteNearWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = { Text("Nome Documento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Preset Dimensioni Tavola:", style = MaterialTheme.typography.labelMedium, color = GraphiteLightGray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = { widthStr = "1080"; heightStr = "1080" },
                        label = { Text("1080×1080 Quadrato") }
                    )
                    AssistChip(
                        onClick = { widthStr = "1920"; heightStr = "1080" },
                        label = { Text("1920×1080 Full HD") }
                    )
                    AssistChip(
                        onClick = { widthStr = "1080"; heightStr = "1920" },
                        label = { Text("1080×1920 Mobile") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it },
                        label = { Text("Larghezza (px)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it },
                        label = { Text("Altezza (px)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Sfondo Tavola:", style = MaterialTheme.typography.labelMedium, color = GraphiteLightGray)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val bgs = listOf(0xFF161920L, 0xFF141E28L, 0xFF1F171BL, 0xFFFBF9EBL, 0xFFFFFFFFL)
                    for (bg in bgs) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(bg.toInt()))
                                .border(
                                    width = if (selectedBg == bg) 2.5.dp else 1.dp,
                                    color = if (selectedBg == bg) DataVector else GraphiteDimGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedBg = bg }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = widthStr.toIntOrNull() ?: 1080
                    val h = heightStr.toIntOrNull() ?: 1080
                    onCreate(docName, w, h, selectedBg)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DataVector, contentColor = GraphiteNearBlack)
            ) {
                Text("Crea Tavola")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = GraphiteLightGray)
            }
        }
    )
}
