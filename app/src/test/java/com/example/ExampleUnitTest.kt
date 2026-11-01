package com.example

import com.example.domain.ModelParameterCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun allSixCanonicalPagePilotConfigs_matchExactMathematicalParameterCounts() {
        assertTrue(ModelParameterCalculator.validateAllCanonicalConfigs())

        val configs = ModelParameterCalculator.CANONICAL_CONFIGS
        assertEquals(6, configs.size)

        // 1. PagePilot-Verify-Micro-918K (V=4096, tied=true, L=2, d_model=128, H=4:2, d_ffn=384)
        val c0 = ModelParameterCalculator.calculate(configs[0])
        assertEquals(524_288L, c0.embeddingAndLmHeadParams)
        assertEquals(49_152L, c0.perLayerAttentionParams)
        assertEquals(147_456L, c0.perLayerSwiGluFfnParams)
        assertEquals(256L, c0.perLayerRmsNormParams)
        assertEquals(393_728L, c0.allLayersTotalParams)
        assertEquals(128L, c0.finalRmsNormParams)
        assertEquals(918_144L, c0.exactTotalParams)

        // 2. PagePilot-Verify-Micro-4.5M (V=32000, tied=true, L=2, d_model=128, H=4:2, d_ffn=384)
        val c1 = ModelParameterCalculator.calculate(configs[1])
        assertEquals(4_096_000L, c1.embeddingAndLmHeadParams)
        assertEquals(4_489_856L, c1.exactTotalParams)

        // 3. PagePilot-Exp-100M (V=32000, tied=true, L=12, d_model=768, H=12:4, d_ffn=2048)
        val c2 = ModelParameterCalculator.calculate(configs[2])
        assertEquals(100_092_672L, c2.exactTotalParams)

        // 4. PagePilot-Mobile-336M (V=32000, tied=false, L=24, d_model=1024, H=16:4, d_ffn=2816)
        val c3 = ModelParameterCalculator.calculate(configs[3])
        assertEquals(336_118_784L, c3.exactTotalParams)

        // 5. PagePilot-Edge-1.1B (V=32000, tied=false, L=22, d_model=2048, H=32:4, d_ffn=5632)
        val c4 = ModelParameterCalculator.calculate(configs[4])
        assertEquals(1_100_048_384L, c4.exactTotalParams)

        // 6. PagePilot-3B (V=32000, tied=false, L=28, d_model=3072, H=24:8, d_ffn=8192)
        val c5 = ModelParameterCalculator.calculate(configs[5])
        assertEquals(3_015_355_392L, c5.exactTotalParams)
    }
}
