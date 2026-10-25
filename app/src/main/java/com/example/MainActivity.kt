package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GraphiteDatabase
import com.example.data.GraphiteRepository
import com.example.ui.CanvasWorkspaceScreen
import com.example.ui.ExportDocumentDialog
import com.example.ui.GraphiteColorPickerDialog
import com.example.ui.LayersAndInspectorScreen
import com.example.ui.NewDocumentDialog
import com.example.ui.NodeCatalogBottomSheet
import com.example.ui.NodeGraphWorkspaceScreen
import com.example.ui.PortfolioAndGalleryScreen
import com.example.ui.theme.DataGraphic
import com.example.ui.theme.DataVector
import com.example.ui.theme.GraphiteDarkGray
import com.example.ui.theme.GraphiteDimGray
import com.example.ui.theme.GraphiteLightGray
import com.example.ui.theme.GraphiteMobileTheme
import com.example.ui.theme.GraphiteNearBlack
import com.example.ui.theme.GraphiteNearWhite
import com.example.ui.theme.GraphitePanelBg
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.GraphiteViewModel
import com.example.viewmodel.MobileWorkspaceTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GraphiteMobileTheme {
                GraphiteMobileApp()
            }
        }
    }
}

@Composable
fun GraphiteMobileApp() {
    val context = LocalContext.current
    val repository = remember(context) {
        val db = GraphiteDatabase.getInstance(context)
        GraphiteRepository(db.documentDao())
    }
    val viewModel: GraphiteViewModel = viewModel(
        factory = GraphiteViewModel.provideFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedDocuments by viewModel.savedDocuments.collectAsStateWithLifecycle()

    // BackHandler on secondary tabs returns to the primary 2D Canvas
    BackHandler(enabled = uiState.activeTab != MobileWorkspaceTab.CANVAS) {
        viewModel.selectTab(MobileWorkspaceTab.CANVAS)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphiteNearBlack)
    ) {
        val isExpandedTablet = maxWidth >= 840.dp

        if (isExpandedTablet) {
            // Canonical Tablet / Foldable Split Workspace: NavigationRail + 2D Canvas + Side Supporting Pane
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = GraphiteNearBlack
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    GraphiteSideNavigationRail(
                        uiState = uiState,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                    VerticalDivider(color = GraphiteDimGray)
                    Box(modifier = Modifier.weight(1.35f).fillMaxHeight()) {
                        CanvasWorkspaceScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                    VerticalDivider(color = GraphiteDimGray)
                    Box(
                        modifier = Modifier
                            .width(390.dp)
                            .fillMaxHeight()
                    ) {
                        when (uiState.activeTab) {
                            MobileWorkspaceTab.CANVAS, MobileWorkspaceTab.NODE_GRAPH -> {
                                NodeGraphWorkspaceScreen(uiState = uiState, viewModel = viewModel)
                            }
                            MobileWorkspaceTab.LAYERS_INSPECTOR -> {
                                LayersAndInspectorScreen(uiState = uiState, viewModel = viewModel)
                            }
                            MobileWorkspaceTab.PORTFOLIO -> {
                                PortfolioAndGalleryScreen(
                                    uiState = uiState,
                                    savedDocuments = savedDocuments,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Mobile Handheld Workspace with Bottom NavigationBar
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = GraphiteNearBlack,
                bottomBar = {
                    GraphiteBottomNavigationBar(
                        uiState = uiState,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (uiState.activeTab) {
                        MobileWorkspaceTab.CANVAS -> {
                            CanvasWorkspaceScreen(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                        MobileWorkspaceTab.NODE_GRAPH -> {
                            NodeGraphWorkspaceScreen(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                        MobileWorkspaceTab.LAYERS_INSPECTOR -> {
                            LayersAndInspectorScreen(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                        MobileWorkspaceTab.PORTFOLIO -> {
                            PortfolioAndGalleryScreen(
                                uiState = uiState,
                                savedDocuments = savedDocuments,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        // Global Modals & Bottom Sheets
        if (uiState.showNodeCatalogSheet) {
            NodeCatalogBottomSheet(
                onDismiss = { viewModel.setNodeCatalogVisible(false) },
                onSelectNodeType = { nodeType ->
                    viewModel.addProceduralNodeToSelected(nodeType)
                }
            )
        }

        if (uiState.showColorPickerDialog) {
            val initialColor = if (uiState.editingPrimaryColor) uiState.primaryColorHex else uiState.secondaryColorHex
            GraphiteColorPickerDialog(
                initialColorHex = initialColor,
                isPrimary = uiState.editingPrimaryColor,
                onDismiss = { viewModel.closeColorPicker() },
                onSelectColor = { hex ->
                    viewModel.setWorkingColor(hex, applyToSelectedLayer = true)
                }
            )
        }

        if (uiState.showExportDialog) {
            ExportDocumentDialog(
                doc = uiState.document,
                onDismiss = { viewModel.setExportDialogVisible(false) },
                onCopiedToast = { msg ->
                    viewModel.showToast(msg)
                    viewModel.setExportDialogVisible(false)
                },
                onImportSvg = { svgStr, merge ->
                    viewModel.importSvgDocument(svgStr, mergeIntoCurrent = merge)
                },
                onRoundTripSvg = {
                    viewModel.roundTripCurrentDocumentViaSvg()
                    viewModel.setExportDialogVisible(false)
                }
            )
        }

        if (uiState.showNewDocDialog) {
            NewDocumentDialog(
                onDismiss = { viewModel.setNewDocDialogVisible(false) },
                onCreate = { name, w, h, bg ->
                    viewModel.createNewDocument(name, w, h, bg)
                }
            )
        }
    }
}

@Composable
private fun GraphiteBottomNavigationBar(
    uiState: EditorUiState,
    onSelectTab: (MobileWorkspaceTab) -> Unit
) {
    val activeNodeCount = uiState.selectedLayer?.nodes?.count { it.enabled } ?: 0
    val layerCount = uiState.document.layers.size

    NavigationBar(
        containerColor = GraphitePanelBg,
        contentColor = GraphiteNearWhite,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("graphite_bottom_nav")
    ) {
        for (tab in MobileWorkspaceTab.entries) {
            val selected = uiState.activeTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    val iconVec = tabIcon(tab, selected)
                    when (tab) {
                        MobileWorkspaceTab.NODE_GRAPH -> {
                            BadgedBox(
                                badge = {
                                    if (activeNodeCount > 0) {
                                        Badge(containerColor = DataGraphic, contentColor = GraphiteNearBlack) {
                                            Text("$activeNodeCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(iconVec, contentDescription = tab.labelIt)
                            }
                        }
                        MobileWorkspaceTab.LAYERS_INSPECTOR -> {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = DataVector, contentColor = GraphiteNearBlack) {
                                        Text("$layerCount")
                                    }
                                }
                            ) {
                                Icon(iconVec, contentDescription = tab.labelIt)
                            }
                        }
                        else -> {
                            Icon(iconVec, contentDescription = tab.labelIt)
                        }
                    }
                },
                label = { Text(tab.labelIt) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DataVector,
                    selectedTextColor = GraphiteNearWhite,
                    indicatorColor = GraphiteDarkGray,
                    unselectedIconColor = GraphiteLightGray,
                    unselectedTextColor = GraphiteLightGray
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun GraphiteSideNavigationRail(
    uiState: EditorUiState,
    onSelectTab: (MobileWorkspaceTab) -> Unit
) {
    NavigationRail(
        containerColor = GraphitePanelBg,
        contentColor = GraphiteNearWhite
    ) {
        for (tab in MobileWorkspaceTab.entries) {
            val selected = uiState.activeTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(tabIcon(tab, selected), contentDescription = tab.labelIt)
                },
                label = { Text(tab.labelIt) }
            )
        }
    }
}

private fun tabIcon(tab: MobileWorkspaceTab, selected: Boolean): ImageVector {
    return when (tab) {
        MobileWorkspaceTab.CANVAS -> if (selected) Icons.Filled.Brush else Icons.Outlined.Brush
        MobileWorkspaceTab.NODE_GRAPH -> if (selected) Icons.Filled.AccountTree else Icons.Outlined.AccountTree
        MobileWorkspaceTab.LAYERS_INSPECTOR -> if (selected) Icons.Filled.Layers else Icons.Outlined.Layers
        MobileWorkspaceTab.PORTFOLIO -> if (selected) Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial
    }
}
