package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PagePilotDatabase
import com.example.data.PagePilotRepository
import com.example.domain.ModelParameterCalculator
import com.example.domain.QuantizationType
import com.example.engine.DeviceHardwareMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun verifyAppNameAndHeaderRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PagePilot", appName)

        val db = PagePilotDatabase.getInstance(context)
        val repo = PagePilotRepository(context, db.pagePilotDao())
        val microConfig = ModelParameterCalculator.CANONICAL_CONFIGS[0]
        val breakdown = ModelParameterCalculator.calculate(microConfig, QuantizationType.INT8)

        val headerBytes = repo.encodePagePilotHeader(
            config = microConfig,
            quantization = QuantizationType.INT8,
            exactParams = breakdown.exactTotalParams,
            isRandomVerification = true
        )
        val parsed = repo.parseCheckpointHeader(headerBytes, headerBytes.size)
        assertNotNull(parsed)
        assertTrue(parsed!!.magicValid)
        assertTrue(parsed.parameterMathMatches)
        assertEquals(918_144L, parsed.calculatedExactParams)
        assertEquals(4096, parsed.vocabSize)
        assertTrue(parsed.tieWordEmbeddings)
        assertEquals(384, parsed.dFfn)

        val hw = DeviceHardwareMonitor(context).captureSnapshot()
        assertTrue(hw.totalDeviceRamMb > 0L)
        assertTrue(hw.batteryCurrentReport.isNotBlank())
    }
}
