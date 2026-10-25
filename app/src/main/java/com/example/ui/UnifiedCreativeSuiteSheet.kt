package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.PhotoAndVideoEngine
import com.example.engine.ProceduralEngine
import com.example.model.*
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.GraphiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCreativeSuiteModal(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel
) {
    if (uiState.showCreativeSuiteSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleCreativeSuiteSheet(false) },
            containerColor = Color(0xFF0B111E),
            contentColor = Color.White,
            modifier = Modifier.testTag("unified_creative_suite_sheet")
        ) {
            UnifiedCreativeSuiteContent(
                uiState = uiState,
                viewModel = viewModel,
                onClose = { viewModel.toggleCreativeSuiteSheet(false) }
            )
        }
    }

    if (uiState.isPlayingPresentation) {
        FullscreenPresentationOverlay(
            uiState = uiState,
            viewModel = viewModel
        )
    }

    if (uiState.isPlayingPrototype) {
        FullscreenPrototypePlayerOverlay(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@Composable
private fun UnifiedCreativeSuiteContent(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel,
    onClose: () -> Unit
) {
    val domainSubPanels = remember(uiState.creativeDomainMode) {
        SuiteSubPanel.entries.filter { it.domain == uiState.creativeDomainMode }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Graphite Unified Suite • 100% Offline",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.creativeDomainMode.labelIt} — ${uiState.activeSuiteSubPanel.labelIt}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("close_creative_suite_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Chiudi Suite",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        // 4 Domain Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CreativeDomainMode.entries.forEach { mode ->
                val selected = uiState.creativeDomainMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setCreativeDomainMode(mode) },
                    label = { Text(mode.labelIt, fontSize = 12.sp) },
                    modifier = Modifier.testTag("suite_domain_chip_${mode.name.lowercase()}")
                )
            }
        }

        // Sub-panels for current domain
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            domainSubPanels.forEach { panel ->
                val isSelected = uiState.activeSuiteSubPanel == panel
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.openSuiteSubPanel(panel) }
                        .testTag("suite_subpanel_${panel.name.lowercase()}")
                ) {
                    Text(
                        text = panel.labelIt,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B))

        // Active Sub-Panel Body
        when (uiState.activeSuiteSubPanel) {
            SuiteSubPanel.PHOTO_CROP_TRANSFORM -> PicsartCropAndTonalPanel(uiState, viewModel)
            SuiteSubPanel.PHOTO_TONAL_CURVES -> PicsartCurvesAndPerspectivePanel(uiState, viewModel)
            SuiteSubPanel.PHOTO_RETOUCH_BG -> PicsartRetouchAndBgPanel(uiState, viewModel)
            SuiteSubPanel.PHOTO_FILTERS_EFFECTS -> PicsartLutAndEffectsPanel(uiState, viewModel)
            SuiteSubPanel.PHOTO_COLLAGE -> PicsartCollageAndStickersPanel(uiState, viewModel)
            SuiteSubPanel.VIDEO_TIMELINE -> PicsartVideoTimelinePanel(uiState, viewModel)

            SuiteSubPanel.DESIGN_TEMPLATES -> CanvaTemplatesPanel(uiState, viewModel)
            SuiteSubPanel.DESIGN_BRAND_KIT -> CanvaBrandKitPanel(uiState, viewModel)
            SuiteSubPanel.DESIGN_PRESENTATION -> CanvaPresentationsPanel(uiState, viewModel)
            SuiteSubPanel.DESIGN_SOCIAL_PLANNER -> CanvaSocialSchedulerPanel(uiState, viewModel)
            SuiteSubPanel.DESIGN_STICKERS_ASSETS -> CanvaAssetLibraryPanel(uiState, viewModel)

            SuiteSubPanel.UIUX_AUTO_LAYOUT -> FigmaAutoLayoutAndFramesPanel(uiState, viewModel)
            SuiteSubPanel.UIUX_COMPONENTS -> FigmaComponentsPanel(uiState, viewModel)
            SuiteSubPanel.UIUX_PROTOTYPE -> FigmaPrototypePanel(uiState, viewModel)
            SuiteSubPanel.UIUX_COLLAB_HISTORY -> FigmaCommentsAndHistoryPanel(uiState, viewModel)
            SuiteSubPanel.UIUX_HANDOFF_INSPECT -> FigmaInspectHandoffPanel(uiState, viewModel)
            SuiteSubPanel.UIUX_FIGJAM_BOARD -> FigJamBoardPanel(uiState, viewModel)

            SuiteSubPanel.DRAW_BRUSH_STUDIO,
            SuiteSubPanel.DRAW_VECTOR_BOOLEAN -> DrawBrushesAndVectorPanel(uiState, viewModel)
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

// =============================================================================
// 1. PICSART PANELS
// =============================================================================

@Composable
private fun PicsartCropAndTonalPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val adj = uiState.photoAdjustments
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Crop Proporzioni, Raddrizza Orizzonte & Riempimento Bordi Intelligente",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFF43F5E)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PhotoCropAspectRatio.entries.forEach { ratio ->
                FilterChip(
                    selected = adj.cropRatio == ratio,
                    onClick = { viewModel.applyCropRatioToArtboard(ratio) },
                    label = { Text(ratio.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("crop_ratio_${ratio.name.lowercase()}")
                )
            }
            AssistChip(
                onClick = { viewModel.rotateArtboard90Degrees() },
                label = { Text("Ruota 90°", fontSize = 11.sp) },
                modifier = Modifier.testTag("rotate_90_button")
            )
            AssistChip(
                onClick = { viewModel.flipSelectedLayerAxes(horizontal = true) },
                label = { Text("Flip Oriz.", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { viewModel.flipSelectedLayerAxes(horizontal = false) },
                label = { Text("Flip Vert.", fontSize = 11.sp) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riempimento Bordi (Mirror/Clone Locale senza IA)",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1)
            )
            Switch(
                checked = adj.smartBorderMirrorFill,
                onCheckedChange = {
                    viewModel.applyPhotoAdjustmentsToLayer(adj.copy(smartBorderMirrorFill = it))
                }
            )
        }

        SuiteSliderRow(
            label = "Raddrizza Orizzonte (${adj.horizonStraightenDeg.toInt()}°)",
            value = adj.horizonStraightenDeg,
            range = -45f..45f,
            onValueChange = { viewModel.applyPhotoAdjustmentsToLayer(adj.copy(horizonStraightenDeg = it)) }
        )

        HorizontalDivider(color = Color(0xFF1E293B))
        Text(
            text = "14 Regolazioni Tonali Deterministiche",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF38BDF8)
        )

        SuiteSliderRow("Luminosità", adj.brightness, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(brightness = it))
        }
        SuiteSliderRow("Contrasto", adj.contrast, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(contrast = it))
        }
        SuiteSliderRow("Esposizione", adj.exposure, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(exposure = it))
        }
        SuiteSliderRow("Recupero Alte Luci", adj.highlights, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(highlights = it))
        }
        SuiteSliderRow("Recupero Ombre", adj.shadows, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(shadows = it))
        }
        SuiteSliderRow("Saturazione", adj.saturation, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(saturation = it))
        }
        SuiteSliderRow("Vibrance", adj.vibrance, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(vibrance = it))
        }
        SuiteSliderRow("Temperatura (Caldo/Freddo)", adj.temperature, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(temperature = it))
        }
        SuiteSliderRow("Tinta (Verde/Magenta)", adj.tint, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(tint = it))
        }
        SuiteSliderRow("Nitidezza (Sharpen)", adj.sharpness, 0f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(sharpness = it))
        }
        SuiteSliderRow("Riduzione Rumore", adj.noiseReduction, 0f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(noiseReduction = it))
        }
        SuiteSliderRow("Vignettatura", adj.vignette, 0f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(vignette = it))
        }
        SuiteSliderRow("Grana Pellicola", adj.filmGrain, 0f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(filmGrain = it))
        }
    }
}

@Composable
private fun PicsartCurvesAndPerspectivePanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val adj = uiState.photoAdjustments
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Curve Tonali RGB & Singoli Canali (5 Punti di Controllo)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF38BDF8)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToneCurveChannel.entries.forEach { ch ->
                FilterChip(
                    selected = adj.activeCurveChannel == ch,
                    onClick = { viewModel.applyPhotoAdjustmentsToLayer(adj.copy(activeCurveChannel = ch)) },
                    label = { Text(ch.labelIt, fontSize = 11.sp) }
                )
            }
        }

        // Interactive Curve Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, h),
                    end = Offset(w, 0f),
                    strokeWidth = 2f
                )
                val path = Path()
                adj.rgbCurvePoints.forEachIndexed { idx, pt ->
                    val px = pt.inputX * w
                    val py = (1f - pt.outputY) * h
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 6f,
                        center = Offset(px, py)
                    )
                }
                drawPath(path = path, color = Color(0xFF38BDF8), style = Stroke(width = 3f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.applyToneCurvePreset("LINEAR") },
                modifier = Modifier.weight(1f)
            ) { Text("Lineare", fontSize = 11.sp) }
            OutlinedButton(
                onClick = { viewModel.applyToneCurvePreset("S_CONTRAST") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("curve_preset_s_contrast")
            ) { Text("Curva a S", fontSize = 11.sp) }
            OutlinedButton(
                onClick = { viewModel.applyToneCurvePreset("MATTE_FADE") },
                modifier = Modifier.weight(1f)
            ) { Text("Matte Fade", fontSize = 11.sp) }
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Text(
            text = "Prospettiva, Distorsione Lente & Ridimensionamento Bicubico/Lanczos",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFF43F5E)
        )

        SuiteSliderRow("Prospettiva Orizzontale", adj.perspectiveHorizontal, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(perspectiveHorizontal = it))
        }
        SuiteSliderRow("Prospettiva Verticale", adj.perspectiveVertical, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(perspectiveVertical = it))
        }
        SuiteSliderRow("Correzione Distorsione Lente", adj.lensDistortion, -100f..100f) {
            viewModel.applyPhotoAdjustmentsToLayer(adj.copy(lensDistortion = it))
        }
    }
}

@Composable
private fun PicsartRetouchAndBgPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val ret = uiState.retouchState
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Rimozione Sfondo & Ritocco Deterministico (Senza IA)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF10B981)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RetouchAlgorithmMode.entries.forEach { alg ->
                FilterChip(
                    selected = ret.activeMode == alg,
                    onClick = { viewModel.updateRetouchAndBgState(ret.copy(activeMode = alg)) },
                    label = { Text(alg.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("bg_algo_${alg.name.lowercase()}")
                )
            }
        }

        SuiteSliderRow(
            label = "Tolleranza Colore / Soglia Contorni ΔE (${ret.tolerance.toInt()})",
            value = ret.tolerance,
            range = 1f..100f,
            onValueChange = { viewModel.updateRetouchAndBgState(ret.copy(tolerance = it)) }
        )

        Text(
            text = "Sfocatura Profondità Manuale (${ret.bgBlurMode.labelIt})",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackgroundBlurMode.entries.forEach { blur ->
                FilterChip(
                    selected = ret.bgBlurMode == blur,
                    onClick = { viewModel.updateRetouchAndBgState(ret.copy(bgBlurMode = blur)) },
                    label = { Text(blur.labelIt, fontSize = 11.sp) }
                )
            }
        }
        SuiteSliderRow("Raggio Sfocatura Sfondo", ret.bgBlurRadius, 0f..50f) {
            viewModel.updateRetouchAndBgState(ret.copy(bgBlurRadius = it))
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Text(
            text = "Ritocco Viso & Rimozione Oggetti (Clone Stamp & Patch Tool)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFF59E0B)
        )
        SuiteSliderRow("Offset Sorgente Clone X (${ret.cloneSourceOffsetX.toInt()}px)", ret.cloneSourceOffsetX, -200f..200f) {
            viewModel.updateRetouchAndBgState(ret.copy(cloneSourceOffsetX = it))
        }
        SuiteSliderRow("Levigatura Pelle (Filtro Passa-Basso Locale)", ret.skinSmoothIntensity, 0f..1f) {
            viewModel.updateRetouchAndBgState(ret.copy(skinSmoothIntensity = it))
        }
        SuiteSliderRow("Sbiancamento Denti (Desaturazione Gialli)", ret.teethWhitenIntensity, 0f..1f) {
            viewModel.updateRetouchAndBgState(ret.copy(teethWhitenIntensity = it))
        }
        SuiteSliderRow("Schiarimento Occhi (Curve Locali)", ret.eyeTeethBrighten, 0f..1f) {
            viewModel.updateRetouchAndBgState(ret.copy(eyeTeethBrighten = it))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Correzione Occhi Rossi (Sostituzione Canale R→G/B)", fontSize = 12.sp)
            Switch(
                checked = ret.redEyeCorrected,
                onCheckedChange = { viewModel.updateRetouchAndBgState(ret.copy(redEyeCorrected = it)) }
            )
        }
    }
}

@Composable
private fun PicsartLutAndEffectsPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val eff = uiState.filterEffectsState
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Filtri Artistici Preimpostati (LUT Locali + Matita/Carboncino Graphite)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFA855F7)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PhotoFilterLutPreset.entries.forEach { lut ->
                FilterChip(
                    selected = eff.activeLut == lut,
                    onClick = { viewModel.applyLutAndEffects(eff.copy(activeLut = lut)) },
                    label = { Text(lut.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("lut_preset_${lut.name.lowercase()}")
                )
            }
        }
        SuiteSliderRow("Intensità LUT (${(eff.lutIntensity * 100).toInt()}%)", eff.lutIntensity, 0f..1f) {
            viewModel.applyLutAndEffects(eff.copy(lutIntensity = it))
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Text("Effetti di Distorsione Geometrica", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DistortionEffectType.entries.forEach { dist ->
                FilterChip(
                    selected = eff.distortion == dist,
                    onClick = { viewModel.applyLutAndEffects(eff.copy(distortion = dist)) },
                    label = { Text(dist.labelIt, fontSize = 11.sp) }
                )
            }
        }

        Text("Effetti di Luce & Atmosfera", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LightEffectType.entries.forEach { light ->
                FilterChip(
                    selected = eff.lightEffect == light,
                    onClick = { viewModel.applyLutAndEffects(eff.copy(lightEffect = light)) },
                    label = { Text(light.labelIt, fontSize = 11.sp) }
                )
            }
        }

        Text("Texture Fisiche Sovrapposte", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextureOverlayType.entries.forEach { tex ->
                FilterChip(
                    selected = eff.textureOverlay == tex,
                    onClick = { viewModel.applyLutAndEffects(eff.copy(textureOverlay = tex)) },
                    label = { Text(tex.labelIt, fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
private fun PicsartCollageAndStickersPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val col = uiState.collageState
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Generatore Collage (Griglie 2–9 & Ritaglio in Forme)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFEC4899)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(2, 3, 4, 6, 9).forEach { count ->
                FilterChip(
                    selected = col.cellCount == count,
                    onClick = {
                        viewModel.generateCollageOnCanvas(col.copy(cellCount = count))
                    },
                    label = { Text("$count Foto", fontSize = 11.sp) },
                    modifier = Modifier.testTag("collage_grid_$count")
                )
            }
        }

        Text("Maschera Forma Cella (Cerchio, Cuore, Stella, Poligono)", fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CollageShapeMask.entries.forEach { mask ->
                FilterChip(
                    selected = col.shapeMask == mask,
                    onClick = {
                        viewModel.generateCollageOnCanvas(col.copy(shapeMask = mask))
                    },
                    label = { Text(mask.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("collage_mask_${mask.name.lowercase()}")
                )
            }
        }

        SuiteSliderRow("Spaziatura Celle (${col.spacingPx.toInt()}px)", col.spacingPx, 4f..48f) {
            viewModel.generateCollageOnCanvas(col.copy(spacingPx = it))
        }
        SuiteSliderRow("Raggio Bordi Arrotondati (${col.cornerRadiusPx.toInt()}px)", col.cornerRadiusPx, 0f..64f) {
            viewModel.generateCollageOnCanvas(col.copy(cornerRadiusPx = it))
        }

        Button(
            onClick = { viewModel.generateCollageOnCanvas(col) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_collage_button")
        ) {
            Text("Genera Collage sulla Tavola")
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Text(
            text = "Libreria Sticker Vettoriali Locali",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF38BDF8)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoAndVideoEngine.PREINSTALLED_LOCAL_STICKERS.forEach { sticker ->
                OutlinedButton(
                    onClick = { viewModel.insertLocalStickerOnCanvas(sticker) },
                    modifier = Modifier.testTag("insert_sticker_${sticker.id}")
                ) {
                    Text("${sticker.titleIt} (${sticker.categoryIt})", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PicsartVideoTimelinePanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val tl = uiState.videoTimeline
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Video Editor Multi-Clip Locale (Trim, Split, Velocità 0.25x–4x, LUT, MP4)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFF43F5E)
        )

        tl.clips.forEachIndexed { idx, clip ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF162032),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${idx + 1}. ${clip.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${clip.trimStartSec}s – ${clip.trimEndSec}s • ${clip.speedMultiplier}x",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    SuiteSliderRow(
                        label = "Velocità Clip (${String.format("%.2f", clip.speedMultiplier)}x)",
                        value = clip.speedMultiplier,
                        range = 0.25f..4f,
                        onValueChange = { newSpeed ->
                            val updated = tl.clips.toMutableList()
                            updated[idx] = clip.copy(speedMultiplier = newSpeed)
                            viewModel.updateVideoTimeline(tl.copy(clips = updated))
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.splitSelectedVideoClip() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("split_video_clip_button")
            ) {
                Text("Split Clip a Metà", fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.exportLocalVideoMp4() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_video_mp4_button")
            ) {
                Text("Esporta MP4 (${tl.exportResolution})", fontSize = 12.sp)
            }
        }

        if (uiState.lastVideoExportSummary != null) {
            Text(
                text = uiState.lastVideoExportSummary,
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// =============================================================================
// 2. CANVA PANELS
// =============================================================================

@Composable
private fun CanvaTemplatesPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Libreria Template Preinstallati Locali (10 Categorie Social, Print & Deck)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF8B5CF6)
        )

        PhotoAndVideoEngine.PREINSTALLED_CANVA_TEMPLATES.forEach { tpl ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF151F30),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF283548), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.applyCanvaTemplate(tpl) }
                    .testTag("canva_template_${tpl.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${tpl.titleIt} • ${tpl.category.labelIt}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${tpl.category.defaultW}×${tpl.category.defaultH}px • Stile ${tpl.styleTag} • Placeholder: '${tpl.headlineText}'",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(tpl.dominantColorHex, tpl.accentColorHex, tpl.bgHex).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvaBrandKitPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val kit = uiState.brandKit
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Brand Kit Locale (${kit.brandName}) — Palette Colori, Font & Blocco Stili",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF8B5CF6)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            kit.paletteHexList.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        Text(
            text = "Font Brand Locali: ${kit.headingFontName} • ${kit.bodyFontName} • ${kit.monospaceFontName}",
            fontSize = 12.sp,
            color = Color(0xFFCBD5E1)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Blocca Stili Brand (Impedisce modifiche fuori palette)", fontSize = 12.sp)
            Switch(
                checked = kit.isStyleLocked,
                onCheckedChange = { viewModel.toggleBrandStyleLock() },
                modifier = Modifier.testTag("brand_kit_lock_switch")
            )
        }

        Button(
            onClick = { viewModel.applyLocalBrandKitToDocument() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apply_brand_kit_1tap_button")
        ) {
            Text("Applica Brand Kit all'Intero Progetto con 1-Tap")
        }
    }
}

@Composable
private fun CanvaPresentationsPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    var newTitle by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Presentazioni & Storyboard Multi-Slide (Transizioni, Note Relatore & Timer)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF38BDF8)
        )

        uiState.presentationSlides.forEachIndexed { idx, slide ->
            val isActive = idx == uiState.activeSlideIndex
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isActive) Color(0xFF1E3A8A) else Color(0xFF151F30),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectPresentationSlide(idx) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Slide ${idx + 1}: ${slide.title} (${slide.transition.labelIt} • ${slide.entranceAnim.labelIt})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Note Relatore: ${slide.speakerNotes}",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("Titolo Nuova Slide") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newNotes,
            onValueChange = { newNotes = it },
            label = { Text("Note del Relatore (Offline)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.addPresentationSlide(newTitle, newNotes)
                    newTitle = ""
                    newNotes = ""
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_presentation_slide_button")
            ) {
                Text("+ Aggiungi Slide")
            }
            Button(
                onClick = { viewModel.togglePresentationPlayer(true) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("start_presentation_fullscreen_button")
            ) {
                Text("Avvia Presentazione")
            }
        }
    }
}

@Composable
private fun CanvaSocialSchedulerPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    var postTitle by remember { mutableStateOf("Nuova Campagna Visual") }
    var selectedTone by remember { mutableStateOf("Lancio Prodotto") }
    var includeWatermark by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Magic Resize (Adattamento Automatico Multi-Formato)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF10B981)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { viewModel.applyMagicResize(1080, 1080, "Instagram Post 1:1") },
                label = { Text("IG Post 1:1 (1080×1080)") },
                modifier = Modifier.testTag("magic_resize_ig_post")
            )
            AssistChip(
                onClick = { viewModel.applyMagicResize(1080, 1920, "Instagram Story 9:16") },
                label = { Text("Story 9:16 (1080×1920)") },
                modifier = Modifier.testTag("magic_resize_story")
            )
            AssistChip(
                onClick = { viewModel.applyMagicResize(1280, 720, "YouTube Thumbnail 16:9") },
                label = { Text("YT 16:9 (1280×720)") }
            )
            AssistChip(
                onClick = { viewModel.applyMagicResize(1584, 396, "LinkedIn Banner") },
                label = { Text("LinkedIn (1584×396)") }
            )
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Text(
            text = "Pianificatore Post Locale & Generatore Didascalie Deterministico (No-IA)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFF59E0B)
        )

        OutlinedTextField(
            value = postTitle,
            onValueChange = { postTitle = it },
            label = { Text("Titolo Post / Campagna") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Lancio Prodotto", "Dietro le Quinte", "Tutorial Tecnico").forEach { tone ->
                FilterChip(
                    selected = selectedTone == tone,
                    onClick = { selectedTone = tone },
                    label = { Text(tone, fontSize = 11.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Applica Watermark Locale all'Esportazione", fontSize = 12.sp)
            Switch(checked = includeWatermark, onCheckedChange = { includeWatermark = it })
        }

        Button(
            onClick = {
                viewModel.scheduleLocalSocialPost(
                    title = postTitle,
                    channel = "Instagram & LinkedIn",
                    dateText = "2026-06-10 18:00",
                    categoryTone = selectedTone,
                    includeWatermark = includeWatermark
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("schedule_social_post_button")
        ) {
            Text("Genera Didascalia & Pianifica nel Calendario Locale")
        }

        uiState.scheduledPosts.forEach { post ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF151F30),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "📅 ${post.dateTimeLabel} • ${post.channel} — ${post.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = post.captionText,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvaAssetLibraryPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Libreria Asset Vettoriali, Badge & Audio Royalty-Free Locali",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF38BDF8)
        )
        PhotoAndVideoEngine.PREINSTALLED_LOCAL_STICKERS.forEach { asset ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF162032),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.insertLocalStickerOnCanvas(asset) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(asset.titleIt, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "Categoria: ${asset.categoryIt} • Tag: ${asset.tags.joinToString()}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Text("+ Inserisci", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// =============================================================================
// 3. FIGMA & FIGJAM PANELS
// =============================================================================

@Composable
private fun FigmaAutoLayoutAndFramesPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val al = uiState.autoLayoutState
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Preset Frame Dispositivi (iPhone, Android, iPad, Desktop)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF0EA5E9)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DeviceFramePreset.entries.forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.applyDeviceFramePreset(preset) },
                    label = { Text(preset.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("figma_frame_${preset.name.lowercase()}")
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Text(
            text = "Figma Auto Layout & Constraints",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF10B981)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AutoLayoutDirection.entries.forEach { dir ->
                FilterChip(
                    selected = al.direction == dir,
                    onClick = {
                        viewModel.applyFigmaAutoLayout(al.copy(enabled = true, direction = dir))
                    },
                    label = { Text(dir.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("autolayout_dir_${dir.name.lowercase()}")
                )
            }
        }

        SuiteSliderRow("Spaziatura Elementi (${al.spacingPx.toInt()}px)", al.spacingPx, 0f..96f) {
            viewModel.applyFigmaAutoLayout(al.copy(enabled = true, spacingPx = it))
        }
        SuiteSliderRow("Padding Orizzontale (${al.paddingHorizontalPx.toInt()}px)", al.paddingHorizontalPx, 8f..120f) {
            viewModel.applyFigmaAutoLayout(al.copy(enabled = true, paddingHorizontalPx = it))
        }
        SuiteSliderRow("Padding Verticale (${al.paddingVerticalPx.toInt()}px)", al.paddingVerticalPx, 8f..120f) {
            viewModel.applyFigmaAutoLayout(al.copy(enabled = true, paddingVerticalPx = it))
        }

        Button(
            onClick = { viewModel.applyFigmaAutoLayout(al.copy(enabled = true)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apply_figma_autolayout_button")
        ) {
            Text("Applica Auto Layout ai Livelli Visibili")
        }
    }
}

@Composable
private fun FigmaComponentsPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    var selectedVariant by remember { mutableStateOf(ComponentVariantState.DEFAULT) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Componenti Master, Varianti di Stato & Libreria Design System",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF0EA5E9)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ComponentVariantState.entries.forEach { v ->
                FilterChip(
                    selected = selectedVariant == v,
                    onClick = { selectedVariant = v },
                    label = { Text(v.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("figma_variant_${v.name.lowercase()}")
                )
            }
        }

        uiState.masterComponents.forEach { comp ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF151F30),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("❖ ${comp.name}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "Testo Override: '${comp.overrideLabel}' • Sorgente: ${comp.sourceLayerId}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Button(
                        onClick = { viewModel.instantiateFigmaComponent(comp, selectedVariant) },
                        modifier = Modifier.testTag("insert_component_${comp.id}")
                    ) {
                        Text("Inserisci (${selectedVariant.labelIt})", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FigmaPrototypePanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    var targetScreen by remember { mutableStateOf("Schermata Checkout") }
    var trigger by remember { mutableStateOf(PrototypeTriggerType.ON_TAP) }
    var anim by remember { mutableStateOf(PrototypeTransitionType.SMART_ANIMATE) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Prototipazione Interattiva (Hotspot, Trigger, Smart Animate & Player)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF38BDF8)
        )

        OutlinedTextField(
            value = targetScreen,
            onValueChange = { targetScreen = it },
            label = { Text("Frame / Schermata Destinazione") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrototypeTriggerType.entries.forEach { t ->
                FilterChip(
                    selected = trigger == t,
                    onClick = { trigger = t },
                    label = { Text(t.labelIt, fontSize = 11.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrototypeTransitionType.entries.forEach { a ->
                FilterChip(
                    selected = anim == a,
                    onClick = { anim = a },
                    label = { Text(a.labelIt, fontSize = 11.sp) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.addPrototypeLink(targetScreen, trigger, anim) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_prototype_link_button")
            ) {
                Text("+ Collega Hotspot", fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.togglePrototypePlayer(true) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("launch_prototype_player_button")
            ) {
                Text("Avvia Player Prototipo", fontSize = 12.sp)
            }
        }

        uiState.prototypeLinks.forEach { link ->
            Text(
                text = "🔗 [${link.sourceLayerName}] —(${link.trigger.labelIt} / ${link.transition.labelIt})→ ${link.targetSlideTitle}",
                fontSize = 11.sp,
                color = Color(0xFF6EE7B7),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun FigmaCommentsAndHistoryPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    var commentText by remember { mutableStateOf("") }
    var snapshotName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Commenti Offline Ancorati alla Tela & Version History con Visual Diff",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFF59E0B)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Nuovo Commento sul Livello") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.addOfflineComment("Lead Designer", commentText)
                    commentText = ""
                },
                modifier = Modifier.testTag("add_offline_comment_button")
            ) {
                Text("Ancora")
            }
        }

        uiState.offlineComments.forEach { c ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF162032),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleCommentResolved(c.id) }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${c.author} • (${c.artboardX.toInt()}, ${c.artboardY.toInt()}) • ${c.createdAtLabel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Text(c.message, fontSize = 12.sp, color = Color.White)
                    }
                    Text(
                        text = if (c.isResolved) "✓ Risolto" else "Aperto",
                        color = if (c.isResolved) Color(0xFF10B981) else Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = snapshotName,
                onValueChange = { snapshotName = it },
                label = { Text("Etichetta Versione (es. v1.2 Release)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.saveVersionSnapshot(snapshotName)
                    snapshotName = ""
                },
                modifier = Modifier.testTag("save_version_snapshot_button")
            ) {
                Text("Salva Snapshot")
            }
        }

        if (uiState.activeDiffReport != null) {
            Text(
                text = uiState.activeDiffReport,
                color = Color(0xFF6EE7B7),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        uiState.versionSnapshots.forEach { snap ->
            Text(
                text = "⏱ ${snap.versionTag} — ${snap.titleIt} (${snap.timestampLabel}): ${snap.diffSummaryIt}",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun FigmaInspectHandoffPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    val selected = uiState.selectedLayer ?: uiState.document.layers.firstOrNull()
    val cssCode = remember(uiState.document, selected) {
        PhotoAndVideoEngine.exportToCssHandoff(uiState.document, selected)
    }
    val composeCode = remember(uiState.document) {
        ProceduralEngine.exportToComposeImageVector(uiState.document)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Design Inspection & Code Handoff (CSS3, Jetpack Compose, SVG)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF10B981)
        )

        Text("Proprietà CSS3 Livello Selezionato:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF090D16),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .testTag("css_handoff_code_box")
        ) {
            Text(
                text = cssCode,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF6EE7B7),
                modifier = Modifier.padding(12.dp)
            )
        }

        Text("Codice Nativo Jetpack Compose ImageVector:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF090D16),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
        ) {
            Text(
                text = composeCode.lines().take(12).joinToString("\n"),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun FigJamBoardPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    var stickyText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "FigJam Lavagna Infinita — Sticky Notes, Connettori, Votazioni & Timer",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFFFBBF24)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stickyText,
                onValueChange = { stickyText = it },
                label = { Text("Testo Nota Adesiva (Sticky Note)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.addFigJamStickyNote(stickyText)
                    stickyText = ""
                },
                modifier = Modifier.testTag("add_figjam_sticky_button")
            ) {
                Text("+ Nota")
            }
        }

        uiState.figJamStickies.forEach { sticky ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(sticky.colorHex),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sticky.text,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Autore: ${sticky.author}",
                            color = Color(0xFF334155),
                            fontSize = 11.sp
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.voteFigJamSticky(sticky.id) },
                        modifier = Modifier.testTag("vote_sticky_${sticky.id}")
                    ) {
                        Text("👍 ${sticky.votes}")
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. DRAWING & VECTOR NETWORK PANEL
// =============================================================================

@Composable
private fun DrawBrushesAndVectorPanel(uiState: EditorUiState, viewModel: GraphiteViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "6 Pennelli Raster & Illustrazione (Matita, Penna, Pennarello, Acquerello, Aerografo, Gessetto)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF10B981)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RasterBrushPreset.entries.forEach { preset ->
                FilterChip(
                    selected = uiState.activeBrushPreset == preset,
                    onClick = { viewModel.applyRasterBrushPreset(preset) },
                    label = { Text(preset.labelIt, fontSize = 11.sp) },
                    modifier = Modifier.testTag("brush_preset_${preset.name.lowercase()}")
                )
            }
        }

        SuiteSliderRow("Dimensione Pennello (${uiState.brushSizePx.toInt()}px)", uiState.brushSizePx, 2f..120f) {
            viewModel.applyRasterBrushPreset(uiState.activeBrushPreset, sizePx = it)
        }
        SuiteSliderRow("Durezza Tratto (${(uiState.brushHardness * 100).toInt()}%)", uiState.brushHardness, 0.05f..1f) {
            viewModel.applyRasterBrushPreset(uiState.activeBrushPreset, hardness = it)
        }
        SuiteSliderRow("Opacità Tratto (${(uiState.brushOpacity * 100).toInt()}%)", uiState.brushOpacity, 0.05f..1f) {
            viewModel.applyRasterBrushPreset(uiState.activeBrushPreset, opacity = it)
        }

        HorizontalDivider(color = Color(0xFF1E293B))
        Text(
            text = "Vector Network, Operazioni Booleane & Nodi Procedurali DAG",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF38BDF8)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { viewModel.addProceduralNodeToSelected(ProceduralNodeType.BOOLEAN_OP) },
                label = { Text("+ Nodo Booleano (Unione/Sottrazione)") }
            )
            AssistChip(
                onClick = { viewModel.addProceduralNodeToSelected(ProceduralNodeType.OFFSET_PATH) },
                label = { Text("+ Espandi/Arrotonda Bézier") }
            )
            AssistChip(
                onClick = { viewModel.addProceduralNodeToSelected(ProceduralNodeType.RADIAL_REPEAT) },
                label = { Text("+ Raggiera Radiale") }
            )
        }
    }
}

// =============================================================================
// FULLSCREEN OVERLAYS (PRESENTATION MODE & FIGMA PROTOTYPE PLAYER)
// =============================================================================

@Composable
private fun FullscreenPresentationOverlay(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel
) {
    val slides = uiState.presentationSlides
    val currentSlide = slides.getOrNull(uiState.activeSlideIndex) ?: slides.first()

    Dialog(
        onDismissRequest = { viewModel.togglePresentationPlayer(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050811))
                .padding(24.dp)
                .testTag("fullscreen_presentation_player"),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MODALITÀ PRESENTAZIONE CANVA • Slide ${uiState.activeSlideIndex + 1}/${slides.size}",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                OutlinedButton(
                    onClick = { viewModel.togglePresentationPlayer(false) },
                    modifier = Modifier.testTag("exit_presentation_button")
                ) {
                    Text("Esci")
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSlide.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Transizione: ${currentSlide.transition.labelIt} • Animazione: ${currentSlide.entranceAnim.labelIt}",
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Note del Relatore (Visibili solo in locale):\n${currentSlide.speakerNotes}",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.selectPresentationSlide(uiState.activeSlideIndex - 1) },
                    enabled = uiState.activeSlideIndex > 0
                ) {
                    Text("← Slide Precedente")
                }
                Button(
                    onClick = { viewModel.selectPresentationSlide(uiState.activeSlideIndex + 1) },
                    enabled = uiState.activeSlideIndex < slides.lastIndex
                ) {
                    Text("Slide Successiva →")
                }
            }
        }
    }
}

@Composable
private fun FullscreenPrototypePlayerOverlay(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel
) {
    Dialog(
        onDismissRequest = { viewModel.togglePrototypePlayer(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060B14))
                .padding(24.dp)
                .testTag("fullscreen_prototype_player"),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FIGMA INTERACTIVE PLAYER • Frame: ${uiState.activePrototypeScreenName}",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                OutlinedButton(
                    onClick = { viewModel.togglePrototypePlayer(false) },
                    modifier = Modifier.testTag("exit_prototype_player_button")
                ) {
                    Text("Chiudi Player")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.activePrototypeScreenName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                uiState.prototypeLinks.forEach { link ->
                    Button(
                        onClick = { viewModel.triggerPrototypeNavigation(link.targetSlideTitle) },
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .testTag("prototype_hotspot_${link.id}")
                    ) {
                        Text("Hotspot (${link.trigger.labelIt} • ${link.transition.labelIt}) → ${link.targetSlideTitle}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SuiteSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = Color(0xFFCBD5E1))
            Text(
                text = String.format("%.2f", value),
                fontSize = 11.sp,
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(28.dp)
        )
    }
}
