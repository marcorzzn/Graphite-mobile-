package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GraphiteDatabase
import com.example.data.GraphiteRepository
import com.example.engine.DemoArtworkFactory
import com.example.engine.ProceduralEngine
import com.example.model.Vec2Point
import com.example.viewmodel.GizmoHandleType
import com.example.viewmodel.GraphiteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext_andRoundTripGraphiteJsonWithPressureAndDag() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Graphite Mobile", context.getString(R.string.app_name))

        val doc = DemoArtworkFactory.createIsometricFountain()
        val json = ProceduralEngine.toGraphiteJson(doc)
        assertTrue(json.contains("grapheneNetwork"))
        assertTrue(json.contains("graphene-procedural-dag"))

        val restored = ProceduralEngine.fromGraphiteJson(json)
        assertNotNull(restored)
        assertEquals(doc.name, restored?.name)
        assertEquals(doc.layers.size, restored?.layers?.size)
        val bmp = ProceduralEngine.renderToBitmap(doc, targetSide = 256)
        assertEquals(256, bmp.width)
    }

    @Test
    fun viewModel_handlesGizmoTransform_stylusPressure_andStressBenchmark() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = GraphiteDatabase.getInstance(context)
        val repo = GraphiteRepository(db.documentDao())
        val vm = GraphiteViewModel(repo)

        val initialLayer = vm.uiState.value.selectedLayer
        assertNotNull(initialLayer)
        val origWidth = initialLayer!!.width

        // Test direct on-canvas gizmo resize (RIGHT_EDGE +60px)
        vm.transformSelectedLayerViaGizmo(
            handle = GizmoHandleType.RIGHT_EDGE,
            artboardX = initialLayer.x + origWidth / 2f + 60f,
            artboardY = initialLayer.y,
            deltaX = 60f,
            deltaY = 0f,
            isFinished = true
        )
        val resizedLayer = vm.uiState.value.selectedLayer!!
        assertTrue(resizedLayer.width > origWidth)

        // Test 2-finger gesture Undo
        vm.undo(fromGesture = true)
        assertEquals(origWidth, vm.uiState.value.selectedLayer!!.width, 1f)

        // Test stylus pressure brush stroke
        vm.addFreehandOrBrushStroke(
            points = listOf(
                Vec2Point(100f, 100f, pressure = 0.5f),
                Vec2Point(200f, 180f, pressure = 1.2f),
                Vec2Point(300f, 240f, pressure = 0.9f)
            ),
            isBrush = true
        )
        assertEquals(1.2f, vm.uiState.value.selectedLayer!!.points[1].pressure, 0.01f)

        // Test 48x Stress Benchmark recipe
        vm.applyProceduralPresetRecipe("stress_benchmark")
        assertTrue(vm.uiState.value.showPerformanceHud)
        val benchInstances = ProceduralEngine.evaluateLayer(vm.uiState.value.selectedLayer!!)
        assertEquals(48, benchInstances.size)

        // Test Before/After bypassNodes vs full procedural evaluation & timeSec animation
        val bypassedInstances = ProceduralEngine.evaluateLayer(
            layer = vm.uiState.value.selectedLayer!!,
            timeSec = 1.25f,
            bypassNodes = true
        )
        assertEquals(1, bypassedInstances.size)

        // Test Boolean Gear preset (Skia Path.op Difference + Radial Repeat) & 2D DAG positioning
        vm.applyProceduralPresetRecipe("boolean_gear")
        val gearLayer = vm.uiState.value.selectedLayer!!
        val gearInstances = ProceduralEngine.evaluateLayer(gearLayer, timeSec = 0.5f)
        assertEquals(8, gearInstances.size)
        assertNotNull(gearInstances.first().booleanOp)

        val firstNodeId = gearLayer.nodes[0].id
        val secondNodeId = gearLayer.nodes[1].id
        // Drag the first node horizontally past the second node (e.g. graphX = 950f) to trigger spatial DAG rewiring
        vm.moveNodeIn2DGraph(gearLayer.id, firstNodeId, newX = 950f, newY = 310f, isFinished = true)
        val rewiredNodes = vm.uiState.value.selectedLayer!!.nodes
        assertEquals(secondNodeId, rewiredNodes.first().id)
        assertEquals(firstNodeId, rewiredNodes.last().id)

        // Verify SVG export preserves Boolean Path.op (fill-rule="evenodd")
        val gearSvg = ProceduralEngine.exportToSvg(vm.uiState.value.document)
        assertTrue(gearSvg.contains("fill-rule=\"evenodd\""))

        // Test Cubic Bézier Path creation, C1 symmetric tangent handle manipulation, and SVG <path d="M ... C ...">
        vm.addQuickLayer(com.example.model.LayerGeometryKind.BEZIER_PATH)
        val bezierLayer = vm.uiState.value.selectedLayer!!
        val origPt1 = bezierLayer.points[1]
        vm.moveBezierHandle(
            pointIndex = 1,
            isHandleIn = false,
            newX = origPt1.x + 80f,
            newY = origPt1.y - 50f,
            isFinished = true
        )
        val updatedPt1 = vm.uiState.value.selectedLayer!!.points[1]
        assertEquals(origPt1.x + 80f, updatedPt1.handleOutX, 0.5f)
        // C1 symmetric reflection check on opposite handleIn
        assertEquals(origPt1.x - 80f, updatedPt1.handleInX, 0.5f)

        val bezierSvg = ProceduralEngine.exportToSvg(vm.uiState.value.document)
        assertTrue(bezierSvg.contains(" C "))

        // Phase 2 (TP-A2 & TP-B2): Test C0 Independent Cusp Handles & Closed Bézier Path with SVG 'Z'
        vm.toggleBezierVerticesSmoothMode()
        assertTrue(vm.uiState.value.selectedLayer!!.points.all { !it.smooth })
        val cuspAnchor = vm.uiState.value.selectedLayer!!.points[1]
        val prevInX = cuspAnchor.handleInX
        vm.moveBezierHandle(
            pointIndex = 1,
            isHandleIn = false,
            newX = cuspAnchor.x + 110f,
            newY = cuspAnchor.y + 35f,
            isFinished = true
        )
        val afterCusp = vm.uiState.value.selectedLayer!!.points[1]
        assertEquals(cuspAnchor.x + 110f, afterCusp.handleOutX, 0.5f)
        // C0 cusp mode: opposite handleIn must remain unchanged!
        assertEquals(prevInX, afterCusp.handleInX, 0.5f)

        vm.toggleBezierClosedPath()
        assertTrue(vm.uiState.value.selectedLayer!!.closedPath)
        assertTrue(vm.uiState.value.selectedLayer!!.hasFill)
        val closedBezierSvg = ProceduralEngine.exportToSvg(vm.uiState.value.document)
        assertTrue(closedBezierSvg.contains(" Z\""))

        // Phase 2 (TP-C2): Test Live Split-Workspace DAG Drawer toggle
        vm.toggleLiveDagDrawer()
        assertTrue(vm.uiState.value.showLiveDagDrawer)

        // Test Dynamic Magnetic Smart Guides during layer drag near Artboard center
        val curSel = vm.uiState.value.selectedLayer!!
        val artCenterX = vm.uiState.value.document.artboardWidth / 2f
        val artCenterY = vm.uiState.value.document.artboardHeight / 2f
        vm.moveSelectedLayerBy(
            dx = (artCenterX - curSel.x) + 4f,
            dy = (artCenterY - curSel.y) - 3f,
            isFinished = false
        )
        assertTrue(vm.uiState.value.activeSmartGuides.isNotEmpty())
        assertEquals(artCenterX, vm.uiState.value.selectedLayer!!.x, 0.5f)
        vm.moveSelectedLayerBy(0f, 0f, isFinished = true)
        assertTrue(vm.uiState.value.activeSmartGuides.isEmpty())

        // Test Executive A/B Testing Lab Simulation & 100% Winning Variant Rollout
        val initialSamples = vm.uiState.value.abExperiments.first().sampleSize
        vm.runDepartmentABSimulation()
        assertTrue(vm.uiState.value.abExperiments.first().sampleSize > initialSamples)
        vm.applyWinningABRollout()
        assertTrue(vm.uiState.value.abRolloutApplied)
        assertTrue(vm.uiState.value.compactTopBarMode)
        assertEquals(com.example.model.NodeGraphViewMode.CANVAS_2D, vm.uiState.value.nodeGraphViewMode)

        // Render document with Boolean Path.op and Cubic Bézier to Bitmap
        val gearBmp = ProceduralEngine.renderToBitmap(vm.uiState.value.document, targetSide = 128)
        assertEquals(128, gearBmp.width)

        // Phase 3 (TP-B3): Quick Artboard Alignment & Multi-Layer Distribution
        vm.alignSelectedLayer(com.example.model.ArtboardAlignment.ALIGN_LEFT)
        val alignedLeft = vm.uiState.value.selectedLayer!!
        assertEquals(alignedLeft.width / 2f, alignedLeft.x, 0.5f)

        vm.alignSelectedLayer(com.example.model.ArtboardAlignment.ALIGN_BOTTOM)
        val alignedBottom = vm.uiState.value.selectedLayer!!
        assertEquals(
            vm.uiState.value.document.artboardHeight - alignedBottom.height / 2f,
            alignedBottom.y,
            0.5f
        )

        vm.alignSelectedLayer(com.example.model.ArtboardAlignment.DISTRIBUTE_H)
        assertTrue(vm.uiState.value.statusToast.orEmpty().contains("Distribuisci"))

        // Phase 3 (TP-A3 & TP-C3): Bidirectional SVG 1.1 Import & Cubic Bézier / Stroke-Dash / BlendMode Round-Trip
        val sampleSvg = """
            <svg viewBox="0 0 1080 1080" width="1080" height="1080">
              <rect width="100%" height="100%" fill="#161920" />
              <rect x="120" y="140" width="240" height="160" rx="18" fill="#68C587" stroke="#FBF9EB" stroke-width="4.0" />
              <path d="M 300.0 500.0 C 300.0 320.0, 780.0 320.0, 780.0 500.0 C 780.0 680.0, 300.0 680.0, 300.0 500.0 Z" fill="#65BBE5" stroke="#FBF9EB" stroke-width="6.0" stroke-dasharray="20,12" stroke-dashoffset="5" style="mix-blend-mode: screen;" />
            </svg>
        """.trimIndent()

        vm.importSvgDocument(sampleSvg, mergeIntoCurrent = false)
        val importedDoc = vm.uiState.value.document
        assertEquals(2, importedDoc.layers.size)
        val importedBezier = importedDoc.layers.last()
        assertEquals(com.example.model.LayerGeometryKind.BEZIER_PATH, importedBezier.kind)
        assertTrue(importedBezier.closedPath)
        assertEquals(2, importedBezier.points.size)
        assertEquals(320f, importedBezier.points[0].handleOutY, 0.5f)
        assertEquals(680f, importedBezier.points[0].handleInY, 0.5f)
        assertEquals(com.example.model.BlendModeType.SCREEN, importedBezier.blendMode)
        assertTrue(importedBezier.nodes.any { it.type == com.example.model.ProceduralNodeType.STROKE_DASH })

        // Verify Round-Trip SVG Live preserves layers and attributes
        val reExportedSvg = ProceduralEngine.exportToSvg(importedDoc)
        assertTrue(reExportedSvg.contains("mix-blend-mode: screen"))
        assertTrue(reExportedSvg.contains("stroke-dasharray=\"20,12\""))
        vm.roundTripCurrentDocumentViaSvg()
        assertEquals(2, vm.uiState.value.document.layers.size)
    }

    @Test
    fun enterpriseRoadmap_phases4To6_validatesFrustumCulling_edgeSmartSnap_commandPalette_andDevHandoffExporters() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = GraphiteDatabase.getInstance(context)
        val repo = GraphiteRepository(db.documentDao())
        val vm = GraphiteViewModel(repo)

        // Phase 4 (TP-A4 — Gruppo A): Viewport Frustum Culling & DAG Cache Hit-Rate Telemetry
        vm.applyProceduralPresetRecipe("stress_benchmark")
        val stressLayer = vm.uiState.value.selectedLayer!!
        val allInstances = ProceduralEngine.evaluateLayer(stressLayer)
        assertEquals(48, allInstances.size)

        // Second evaluation hits LRU cache
        val cachedInstances = ProceduralEngine.evaluateLayer(stressLayer)
        assertEquals(48, cachedInstances.size)
        assertTrue(ProceduralEngine.getCacheHitRatePercent() > 0)

        // Test Frustum Culler on a tight sub-quadrant viewport (-200..0, -200..0)
        val (visibleInCorner, culledCount) = ProceduralEngine.cullInstancesForViewport(
            instances = allInstances,
            minArtX = -500f,
            minArtY = -500f,
            maxArtX = -100f,
            maxArtY = -100f
        )
        assertEquals(48, visibleInCorner.size + culledCount)
        assertTrue(culledCount > 0)

        // Phase 4 (TP-B4 — Gruppo B): CAD Edge-to-Edge Magnetic Smart Guides
        val doc = vm.uiState.value.document
        val targetRefLayer = doc.layers.first { it.id != stressLayer.id && it.visible }
        val targetLeftEdge = targetRefLayer.x - targetRefLayer.width / 2f
        val desiredCenterForLeftSnap = targetLeftEdge + stressLayer.width / 2f
        vm.moveSelectedLayerBy(
            dx = (desiredCenterForLeftSnap - stressLayer.x) + 3f,
            dy = 0f,
            isFinished = false
        )
        assertTrue(vm.uiState.value.activeSmartGuides.isNotEmpty())
        assertEquals(desiredCenterForLeftSnap, vm.uiState.value.selectedLayer!!.x, 0.5f)
        vm.moveSelectedLayerBy(0f, 0f, isFinished = true)

        // Phase 5 (TP-C5 & TP-D5 — Gruppi C & D): Universal Command Palette, Node Bypass & Sticker Outline Macro
        vm.setCommandPaletteVisible(true)
        assertTrue(vm.uiState.value.showCommandPalette)
        vm.executeEnterpriseCommand("cmd_macro_sticker_outline")
        assertTrue(!vm.uiState.value.showCommandPalette)
        val stickerLayer = vm.uiState.value.selectedLayer!!
        assertEquals(3, stickerLayer.nodes.size)

        // Mute/Bypass first node (OFFSET_PATH) and verify non-destructive DAG re-evaluation
        val firstNodeId = stickerLayer.nodes.first().id
        vm.toggleNodeEnabled(stickerLayer.id, firstNodeId)
        val bypassedNodeLayer = vm.uiState.value.selectedLayer!!
        assertTrue(!bypassedNodeLayer.nodes.first().enabled)

        // Execute 1-tap Center Alignment via Enterprise Command Palette
        vm.executeEnterpriseCommand("cmd_align_center")
        assertEquals(doc.artboardWidth / 2f, vm.uiState.value.selectedLayer!!.x, 0.5f)
        assertEquals(doc.artboardHeight / 2f, vm.uiState.value.selectedLayer!!.y, 0.5f)

        // Phase 6 (TP-E6 — Gruppo E): Multi-Format Enterprise Developer Handoff Pipeline
        val currentDoc = vm.uiState.value.document
        val vectorDrawableXml = ProceduralEngine.exportToAndroidVectorDrawable(currentDoc)
        assertTrue(vectorDrawableXml.contains("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\""))
        assertTrue(vectorDrawableXml.contains("android:pathData=\"M "))

        val composeImageVectorKt = ProceduralEngine.exportToComposeImageVector(currentDoc)
        assertTrue(composeImageVectorKt.contains("ImageVector.Builder("))
        assertTrue(composeImageVectorKt.contains("moveTo("))

        val w3cDesignTokensJson = ProceduralEngine.exportToW3CDesignTokens(currentDoc)
        assertTrue(w3cDesignTokensJson.contains("https://tr.designtokens.org/format/"))
        assertTrue(w3cDesignTokensJson.contains("colorTokens"))
        assertTrue(w3cDesignTokensJson.contains("3.0-enterprise"))

        // Patch v3.5 (Zero-Defect Audit Fixes):
        // 1. Verify Rounded Rectangle (cornerRadius > 0) exports exact quadratic curves (`Q` in XML, `quadTo` in Compose .kt)
        vm.addQuickLayer(com.example.model.LayerGeometryKind.RECTANGLE)
        val rectLayer = vm.uiState.value.selectedLayer!!
        vm.updateLayer(rectLayer.id) { it.copy(cornerRadius = 24f) }
        val xmlWithRoundedRect = ProceduralEngine.exportToAndroidVectorDrawable(vm.uiState.value.document)
        assertTrue(xmlWithRoundedRect.contains(" Q "))
        val composeWithRoundedRect = ProceduralEngine.exportToComposeImageVector(vm.uiState.value.document)
        assertTrue(composeWithRoundedRect.contains("quadTo("))

        // 2. Verify selecting ToolType.PATH or baking converts any primitive (Ellipse/Rectangle) into an editable BEZIER_PATH
        vm.addQuickLayer(com.example.model.LayerGeometryKind.ELLIPSE)
        assertEquals(com.example.model.LayerGeometryKind.ELLIPSE, vm.uiState.value.selectedLayer!!.kind)
        vm.selectTool(com.example.model.ToolType.PATH)
        val bakedEllipse = vm.uiState.value.selectedLayer!!
        assertEquals(com.example.model.LayerGeometryKind.BEZIER_PATH, bakedEllipse.kind)
        assertEquals(4, bakedEllipse.points.size)
        assertTrue(bakedEllipse.closedPath)

        // 3. Verify Multi-Layer Pathfinder Boolean subtraction (`combineSelectedWithBelowBoolean`)
        val countBeforeBool = vm.uiState.value.document.layers.size
        vm.combineSelectedWithBelowBoolean(opMode = 0)
        assertEquals(countBeforeBool - 1, vm.uiState.value.document.layers.size)
        assertTrue(vm.uiState.value.selectedLayer!!.nodes.any { it.type == com.example.model.ProceduralNodeType.BOOLEAN_OP })

        // 4. Verify Adaptive Studio Workspace Mode cycling (PRO -> EASY -> ZEN -> PRO)
        assertEquals(com.example.model.StudioWorkspaceMode.PRO, vm.uiState.value.workspaceMode)
        vm.cycleWorkspaceMode()
        assertEquals(com.example.model.StudioWorkspaceMode.EASY, vm.uiState.value.workspaceMode)
        assertTrue(!vm.uiState.value.showRulers)
        vm.cycleWorkspaceMode()
        assertEquals(com.example.model.StudioWorkspaceMode.ZEN, vm.uiState.value.workspaceMode)
        vm.cycleWorkspaceMode()
        assertEquals(com.example.model.StudioWorkspaceMode.PRO, vm.uiState.value.workspaceMode)
        assertTrue(vm.uiState.value.showRulers)
    }

    @Test
    fun unifiedCreativeSuite_validatesPicsart_Canva_Figma_FigJam_andDeterministicGraphiteGuide() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = GraphiteDatabase.getInstance(context)
        val repo = GraphiteRepository(db.documentDao())
        val vm = GraphiteViewModel(repo)

        // 1. Test 3-State Graphite Guide Toggle (ACTIVE -> HIDDEN_PTT -> OFF -> ACTIVE)
        assertEquals(com.example.model.GraphiteGuideState.ACTIVE, vm.uiState.value.guideState)
        vm.cycleGraphiteGuideState()
        assertEquals(com.example.model.GraphiteGuideState.HIDDEN_PTT, vm.uiState.value.guideState)
        vm.cycleGraphiteGuideState()
        assertEquals(com.example.model.GraphiteGuideState.OFF, vm.uiState.value.guideState)
        vm.cycleGraphiteGuideState()
        assertEquals(com.example.model.GraphiteGuideState.ACTIVE, vm.uiState.value.guideState)

        // 2. Test Deterministic Rule-Based Command Parser (Verb + Object + Modifiers)
        vm.executeGuideCommand("come rimuovo lo sfondo?")
        val bgRes = vm.uiState.value.lastGuideResolution
        assertNotNull(bgRes)
        assertEquals(com.example.engine.GuideActionVerb.REMOVE_ERASE, bgRes!!.matchedVerb)
        assertEquals(com.example.engine.GuideTargetObject.BACKGROUND, bgRes.matchedObject)
        assertEquals(com.example.model.SuiteSubPanel.PHOTO_RETOUCH_BG, vm.uiState.value.activeSuiteSubPanel)

        vm.executeGuideCommand("crea un collage a cuore")
        val heartRes = vm.uiState.value.lastGuideResolution!!
        assertEquals(com.example.engine.GuideTargetObject.COLLAGE_STICKER, heartRes.matchedObject)

        vm.executeGuideCommand("unisci due forme")
        val boolRes = vm.uiState.value.lastGuideResolution!!
        assertEquals(com.example.engine.GuideTargetObject.VECTOR_PATH, boolRes.matchedObject)

        // 3. Test Picsart Engine: Crop Ratio 4:5, Tone Curve S-Contrast, Retouch & LUT Charcoal Sketch
        vm.applyCropRatioToArtboard(com.example.model.PhotoCropAspectRatio.RATIO_4_5, straightenDeg = 3.5f)
        assertEquals(3.5f, vm.uiState.value.photoAdjustments.horizonStraightenDeg, 0.01f)
        vm.applyToneCurvePreset("S_CONTRAST")
        assertEquals(0.16f, vm.uiState.value.photoAdjustments.rgbCurvePoints[1].outputY, 0.01f)

        vm.updateRetouchAndBgState(
            com.example.model.RetouchAndBgState(
                activeMode = com.example.model.RetouchAlgorithmMode.GRAPH_CUT_CONTOUR,
                tolerance = 35f,
                skinSmoothIntensity = 0.5f,
                teethWhitenIntensity = 0.4f,
                redEyeCorrected = true
            )
        )
        assertEquals(
            com.example.model.RetouchAlgorithmMode.GRAPH_CUT_CONTOUR,
            vm.uiState.value.retouchState.activeMode
        )

        vm.applyLutAndEffects(
            com.example.model.FilterAndEffectsState(
                activeLut = com.example.model.PhotoFilterLutPreset.CHARCOAL_SKETCH,
                lutIntensity = 0.9f
            )
        )
        assertEquals(
            com.example.model.PhotoFilterLutPreset.CHARCOAL_SKETCH,
            vm.uiState.value.filterEffectsState.activeLut
        )

        // Generate 4-cell Heart Collage & insert local sticker
        vm.generateCollageOnCanvas(
            com.example.model.CollageComposerState(
                cellCount = 4,
                shapeMask = com.example.model.CollageShapeMask.HEART
            )
        )
        assertEquals(4, vm.uiState.value.document.layers.size)
        vm.insertLocalStickerOnCanvas(com.example.engine.PhotoAndVideoEngine.PREINSTALLED_LOCAL_STICKERS.first())
        assertEquals(5, vm.uiState.value.document.layers.size)

        // Video Timeline split & MP4 export
        val initialClipsCount = vm.uiState.value.videoTimeline.clips.size
        vm.splitSelectedVideoClip()
        assertEquals(initialClipsCount + 1, vm.uiState.value.videoTimeline.clips.size)
        vm.exportLocalVideoMp4()
        assertTrue(vm.uiState.value.lastVideoExportSummary.orEmpty().contains("MP4"))

        // 4. Test Canva Engine: Templates, Brand Kit 1-Tap, Magic Resize, Presentations & Scheduler
        val firstTemplate = com.example.engine.PhotoAndVideoEngine.PREINSTALLED_CANVA_TEMPLATES.first()
        vm.applyCanvaTemplate(firstTemplate)
        assertEquals(firstTemplate.category.defaultW, vm.uiState.value.document.artboardWidth)

        vm.applyLocalBrandKitToDocument()
        vm.toggleBrandStyleLock()
        assertTrue(vm.uiState.value.brandKit.isStyleLocked)

        vm.applyMagicResize(1080, 1920, "Instagram Story 9:16")
        assertEquals(1080, vm.uiState.value.document.artboardWidth)
        assertEquals(1920, vm.uiState.value.document.artboardHeight)

        val slidesBefore = vm.uiState.value.presentationSlides.size
        vm.addPresentationSlide("Slide Roadmap Q4", "Note relatore offline")
        assertEquals(slidesBefore + 1, vm.uiState.value.presentationSlides.size)

        val postsBefore = vm.uiState.value.scheduledPosts.size
        vm.scheduleLocalSocialPost("Campagna Design", "Instagram", "2026-07-01 18:00", "Lancio Prodotto", true)
        assertEquals(postsBefore + 1, vm.uiState.value.scheduledPosts.size)

        // 5. Test Figma & FigJam Engine: Device Frame, Auto Layout, Components, Prototype, Comments, Version Diff, CSS3 & FigJam
        vm.applyDeviceFramePreset(com.example.model.DeviceFramePreset.IPHONE_16_PRO)
        assertEquals(786, vm.uiState.value.document.artboardWidth)

        vm.applyFigmaAutoLayout(
            com.example.model.FigmaAutoLayoutState(
                enabled = true,
                direction = com.example.model.AutoLayoutDirection.VERTICAL_COLUMN,
                spacingPx = 24f,
                paddingHorizontalPx = 32f,
                paddingVerticalPx = 32f
            )
        )
        assertTrue(vm.uiState.value.autoLayoutState.enabled)

        val masterComp = vm.uiState.value.masterComponents.first()
        val layersBeforeComp = vm.uiState.value.document.layers.size
        vm.instantiateFigmaComponent(masterComp, com.example.model.ComponentVariantState.HOVER)
        assertEquals(layersBeforeComp + 1, vm.uiState.value.document.layers.size)

        vm.addPrototypeLink(
            "Schermata Pagamento",
            com.example.model.PrototypeTriggerType.ON_TAP,
            com.example.model.PrototypeTransitionType.SMART_ANIMATE
        )
        vm.triggerPrototypeNavigation("Schermata Pagamento")
        assertEquals("Schermata Pagamento", vm.uiState.value.activePrototypeScreenName)

        vm.addOfflineComment("Marta B.", "Verificare spaziatura 24px")
        assertEquals("Verificare spaziatura 24px", vm.uiState.value.offlineComments.first().message)

        vm.saveVersionSnapshot("v2.0 — Suite Unificata")
        assertNotNull(vm.uiState.value.activeDiffReport)
        assertTrue(vm.uiState.value.activeDiffReport.orEmpty().contains("Visual Diff"))

        val cssHandoff = com.example.engine.PhotoAndVideoEngine.exportToCssHandoff(
            vm.uiState.value.document,
            vm.uiState.value.selectedLayer
        )
        assertTrue(cssHandoff.contains("background-color:"))

        val stickiesBefore = vm.uiState.value.figJamStickies.size
        vm.addFigJamStickyNote("Brainstorming Architettura Offline")
        assertEquals(stickiesBefore + 1, vm.uiState.value.figJamStickies.size)
        val newStickyId = vm.uiState.value.figJamStickies.first().id
        vm.voteFigJamSticky(newStickyId)
        assertEquals(2, vm.uiState.value.figJamStickies.first().votes)
    }
}





