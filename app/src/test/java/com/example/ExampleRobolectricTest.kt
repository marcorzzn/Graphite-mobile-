package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.DemoArtworkFactory
import com.example.engine.ProceduralEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext_andRoundTripGraphiteJson() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Graphite Mobile", context.getString(R.string.app_name))

        val doc = DemoArtworkFactory.createIsometricFountain()
        val json = ProceduralEngine.toGraphiteJson(doc)
        val restored = ProceduralEngine.fromGraphiteJson(json)
        assertNotNull(restored)
        assertEquals(doc.name, restored?.name)
        assertEquals(doc.layers.size, restored?.layers?.size)
        val bmp = ProceduralEngine.renderToBitmap(doc, targetSide = 256)
        assertEquals(256, bmp.width)
    }
}
