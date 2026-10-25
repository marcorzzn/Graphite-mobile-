package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GraphiteGuideEngine
import com.example.model.CreativeDomainMode
import com.example.model.GraphiteGuideState
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.GraphiteViewModel

@Composable
fun ContextualModeAndGuideStrip(
    uiState: EditorUiState,
    viewModel: GraphiteViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicPills = remember(uiState.creativeDomainMode, uiState.document.layers.size, uiState.selectedLayer) {
        GraphiteGuideEngine.computeDynamicActionPills(
            domain = uiState.creativeDomainMode,
            doc = uiState.document,
            selectedLayer = uiState.selectedLayer
        )
    }
    val proactiveTip = remember(
        uiState.creativeDomainMode,
        uiState.selectedLayer,
        uiState.document.layers.size
    ) {
        GraphiteGuideEngine.computeProactiveTip(
            domain = uiState.creativeDomainMode,
            doc = uiState.document,
            selectedLayer = uiState.selectedLayer
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0F18))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: 4 Contextual Modes + Suite Launcher + Graphite Guide 3-State Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CreativeDomainMode.entries.forEach { mode ->
                val isSelected = uiState.creativeDomainMode == mode
                val accentColor = mode.accentColor
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF131C2B),
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accentColor else Color(0xFF233044),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.setCreativeDomainMode(mode) }
                        .testTag("domain_mode_${mode.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accentColor else Color(0xFF64748B))
                        )
                        Text(
                            text = mode.shortBadgeIt,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Open Unified Suite Sheet Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.toggleCreativeSuiteSheet(true) }
                    .testTag("open_creative_suite_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesomeMosaic,
                        contentDescription = "Apri Suite Creativa Unificata",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Suite (${uiState.activeSuiteSubPanel.labelIt})",
                        color = Color(0xFFE0F2FE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3-State Graphite Guide Toggle (ON / PTT / OFF)
            val guideColor = when (uiState.guideState) {
                GraphiteGuideState.ACTIVE -> Color(0xFF10B981)
                GraphiteGuideState.HIDDEN_PTT -> Color(0xFFF59E0B)
                GraphiteGuideState.OFF -> Color(0xFF64748B)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = guideColor.copy(alpha = 0.16f),
                modifier = Modifier
                    .border(1.dp, guideColor, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.cycleGraphiteGuideState() }
                    .testTag("graphite_guide_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Stato Graphite Guide Deterministico",
                        tint = guideColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = uiState.guideState.badgeIt,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // If in HIDDEN_PTT mode, show Push-to-Talk summon chip
            if (uiState.guideState == GraphiteGuideState.HIDDEN_PTT) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF292524),
                    modifier = Modifier
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.toggleGuidePttModal() }
                        .testTag("guide_ptt_summon_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Richiama Guida",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Chiedi a Guide",
                            color = Color(0xFFFEF3C7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Row 2: Active Graphite Guide Bar (or Push-to-Talk Modal when summoned)
        val showGuidePanel = uiState.guideState == GraphiteGuideState.ACTIVE ||
            (uiState.guideState == GraphiteGuideState.HIDDEN_PTT && uiState.showGuidePttModal)

        AnimatedVisibility(visible = showGuidePanel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111827))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Command Input + Quick Submit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.guideQueryText,
                        onValueChange = { viewModel.updateGuideQuery(it) },
                        placeholder = {
                            Text(
                                text = "Graphite Guide (No-IA): es. 'rimuovi sfondo', 'collage cuore', 'auto layout', 'esporta css'...",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = 12.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { viewModel.executeGuideCommand() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("graphite_guide_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    FilledTonalButton(
                        onClick = { viewModel.executeGuideCommand() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF052E16)
                        ),
                        modifier = Modifier.testTag("graphite_guide_execute_button")
                    ) {
                        Text(
                            text = "Guida",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dynamic 1-Tap Contextual Action Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Azioni 1-Tap:",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    dynamicPills.forEach { pill ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    pill.targetSubPanel?.let { viewModel.openSuiteSubPanel(it) }
                                }
                                .testTag("guide_pill_${pill.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = pill.labelIt,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = pill.labelIt,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Proactive Deterministic Rule Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Suggerimento Deterministico",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = proactiveTip,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Resolution Card when a query is parsed
                val resolution = uiState.lastGuideResolution
                if (resolution != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                            .testTag("guide_resolution_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Regola Deterministica: ${resolution.titleIt}",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissGuideSpotlight() },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Chiudi",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Parser: Verbo [${resolution.matchedVerb}] + Oggetto [${resolution.matchedObject}] • Modificatori: ${
                                    if (resolution.modifiers.isEmpty()) "Standard" else resolution.modifiers.joinToString { it.labelIt }
                                }",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = resolution.stepByStepIt,
                                color = Color.White,
                                fontSize = 11.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spotlight UI: #${resolution.spotlightTag}",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Button(
                                    onClick = { viewModel.triggerGuideInstantAction(resolution.instantActionId) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color(0xFF052E16)
                                    ),
                                    modifier = Modifier.testTag("guide_instant_action_button")
                                ) {
                                    Text(
                                        text = resolution.instantActionLabelIt,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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
