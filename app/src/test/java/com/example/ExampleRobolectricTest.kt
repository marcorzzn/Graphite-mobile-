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

        // Render document with Boolean Path.op and Cubic Bézier to Bitmap
        val gearBmp = ProceduralEngine.renderToBitmap(vm.uiState.value.document, targetSide = 128)
        assertEquals(128, gearBmp.width)
    }
}

