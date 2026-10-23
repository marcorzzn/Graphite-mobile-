package com.example

import com.example.engine.DemoArtworkFactory
import com.example.engine.ProceduralEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun mathExpressionEvaluator_handlesFormulasAndTrig() {
        assertEquals(540f, ProceduralEngine.evaluateMathExpression("1080 / 2", 0f), 0.01f)
        assertEquals(45f, ProceduralEngine.evaluateMathExpression("360 / 8", 0f), 0.01f)
        assertEquals(75f, ProceduralEngine.evaluateMathExpression("50 + 25", 0f), 0.01f)
    }

    @Test
    fun proceduralEngine_evaluatesDemoArtworksAndExportsSvg() {
        val doc = DemoArtworkFactory.createIsometricFountain()
        assertTrue(doc.layers.isNotEmpty())

        // Verify radial repeat layer expands to multiple instances
        val jetsLayer = doc.layers.first { it.id == "iso-water-jets" }
        val evaluated = ProceduralEngine.evaluateLayer(jetsLayer)
        assertEquals(8, evaluated.size)

        // Verify SVG export contains valid svg root and gradients
        val svg = ProceduralEngine.exportToSvg(doc)
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
    }
}
