package com.example.domain

import kotlin.math.roundToLong

/**
 * Exact mathematical parameter-count breakdown and 8 GB RAM memory estimator
 * for our proprietary PagePilot decoder-only Transformer configurations.
 *
 * Every configuration uses:
 * - Bias-free linear projections (`bias = false`)
 * - Grouped-Query Attention (W_q, W_k, W_v, W_o)
 * - SwiGLU Feed-Forward Network (W_gate, W_up, W_down) with explicit `dFfn`
 * - Pre-Attention & Pre-FFN RMSNorms (`2 * dModel` per layer) + Final RMSNorm (`dModel`)
 * - Explicit `tieWordEmbeddings` flag (`1 * vocabSize * dModel` vs. `2 * vocabSize * dModel`)
 */
data class PagePilotArchConfig(
    val configId: String,
    val displayName: String,
    val stageDescription: String,
    val vocabSize: Int,
    val tieWordEmbeddings: Boolean,
    val numLayers: Int,
    val dModel: Int,
    val numQueryHeads: Int,
    val numKvHeads: Int,
    val dFfn: Int,
    val maxContextLength: Int,
    val defaultQuantization: QuantizationType,
    val isVerificationOnly: Boolean,
    val expectedExactParams: Long
) {
    val headDim: Int
        get() = dModel / numQueryHeads

    val kvDim: Int
        get() = numKvHeads * headDim
}

enum class QuantizationType(val label: String, val bytesPerParam: Double) {
    INT4("INT4 (Block-32)", 0.55), // 0.5 bytes + block-32 FP16 scale overhead
    INT8("INT8 (Per-Channel)", 1.05),
    FP16("FP16 (Half Precision)", 2.0)
}

data class ParameterBreakdown(
    val config: PagePilotArchConfig,
    val embeddingAndLmHeadParams: Long,
    val perLayerAttentionParams: Long,
    val perLayerSwiGluFfnParams: Long,
    val perLayerRmsNormParams: Long,
    val perLayerTotalParams: Long,
    val allLayersTotalParams: Long,
    val finalRmsNormParams: Long,
    val exactTotalParams: Long,
    val matchesExpected: Boolean,
    val estimatedWeightMbInt4: Long,
    val estimatedWeightMbInt8: Long,
    val estimatedWeightMbFp16: Long,
    val estimatedKvCacheMbAtMaxCtx: Long,
    val estimatedTotalRuntimeRamMb: Long
)

object ModelParameterCalculator {

    val CANONICAL_CONFIGS: List<PagePilotArchConfig> = listOf(
        PagePilotArchConfig(
            configId = "PagePilot-Verify-Micro-918K",
            displayName = "PagePilot Verify Micro (918K • 4K Vocab)",
            stageDescription = "Stage 0: Sub-1M technical pipeline verification checkpoint (compact 4,096 BPE vocab, tied embeddings). Strictly for validating binary header, KV cache & token streaming.",
            vocabSize = 4096,
            tieWordEmbeddings = true,
            numLayers = 2,
            dModel = 128,
            numQueryHeads = 4,
            numKvHeads = 2,
            dFfn = 384,
            maxContextLength = 512,
            defaultQuantization = QuantizationType.INT8,
            isVerificationOnly = true,
            expectedExactParams = 918_144L
        ),
        PagePilotArchConfig(
            configId = "PagePilot-Verify-Micro-4.5M",
            displayName = "PagePilot Verify Micro (4.49M • 32K Vocab)",
            stageDescription = "Stage 0B: Full 32,000-token production vocabulary verification checkpoint with tied embeddings (4,096,000 embedding params + 393,728 layer params + 128 final norm).",
            vocabSize = 32000,
            tieWordEmbeddings = true,
            numLayers = 2,
            dModel = 128,
            numQueryHeads = 4,
            numKvHeads = 2,
            dFfn = 384,
            maxContextLength = 512,
            defaultQuantization = QuantizationType.INT8,
            isVerificationOnly = true,
            expectedExactParams = 4_489_856L
        ),
        PagePilotArchConfig(
            configId = "PagePilot-Exp-100M",
            displayName = "PagePilot Experimental (100.09M)",
            stageDescription = "Stage 1: First independently trained PyTorch model & LiteRT-LM export-validation gate checkpoint.",
            vocabSize = 32000,
            tieWordEmbeddings = true,
            numLayers = 12,
            dModel = 768,
            numQueryHeads = 12,
            numKvHeads = 4,
            dFfn = 2048,
            maxContextLength = 1024,
            defaultQuantization = QuantizationType.INT4,
            isVerificationOnly = false,
            expectedExactParams = 100_092_672L
        ),
        PagePilotArchConfig(
            configId = "PagePilot-Mobile-336M",
            displayName = "PagePilot Mobile (336.12M)",
            stageDescription = "Stage 2: Primary practical mobile model for fast, cool, low-battery offline inference on 8 GB RAM Android devices.",
            vocabSize = 32000,
            tieWordEmbeddings = false,
            numLayers = 24,
            dModel = 1024,
            numQueryHeads = 16,
            numKvHeads = 4,
            dFfn = 2816,
            maxContextLength = 2048,
            defaultQuantization = QuantizationType.INT4,
            isVerificationOnly = false,
            expectedExactParams = 336_118_784L
        ),
        PagePilotArchConfig(
            configId = "PagePilot-Edge-1.1B",
            displayName = "PagePilot Edge (1.10B)",
            stageDescription = "Stage 3A: Mid-tier scaling model after profiling 336M memory, thermal, and battery behavior on 8 GB phones.",
            vocabSize = 32000,
            tieWordEmbeddings = false,
            numLayers = 22,
            dModel = 2048,
            numQueryHeads = 32,
            numKvHeads = 4,
            dFfn = 5632,
            maxContextLength = 2048,
            defaultQuantization = QuantizationType.INT4,
            isVerificationOnly = false,
            expectedExactParams = 1_100_048_384L
        ),
        PagePilotArchConfig(
            configId = "PagePilot-3B",
            displayName = "PagePilot Flagship (3.02B)",
            stageDescription = "Stage 3B: Upper-bound 3B research target requiring strict 4-bit block quantization and bounded context on 8 GB hardware.",
            vocabSize = 32000,
            tieWordEmbeddings = false,
            numLayers = 28,
            dModel = 3072,
            numQueryHeads = 24,
            numKvHeads = 8,
            dFfn = 8192,
            maxContextLength = 2048,
            defaultQuantization = QuantizationType.INT4,
            isVerificationOnly = false,
            expectedExactParams = 3_015_355_392L
        )
    )

    fun calculate(
        config: PagePilotArchConfig,
        quantization: QuantizationType = config.defaultQuantization
    ): ParameterBreakdown {
        val v = config.vocabSize.toLong()
        val d = config.dModel.toLong()
        val l = config.numLayers.toLong()
        val kvDim = config.kvDim.toLong()
        val dFfn = config.dFfn.toLong()

        val embedAndHead = if (config.tieWordEmbeddings) {
            v * d
        } else {
            2L * v * d
        }

        // W_q (d*d) + W_k (d*kvDim) + W_v (d*kvDim) + W_o (d*d)
        val perLayerAttn = (2L * d * d) + (2L * d * kvDim)
        // SwiGLU: W_gate (d*dFfn) + W_up (d*dFfn) + W_down (dFfn*d)
        val perLayerFfn = 3L * d * dFfn
        // Pre-Attn RMSNorm + Pre-FFN RMSNorm
        val perLayerNorm = 2L * d

        val perLayerTotal = perLayerAttn + perLayerFfn + perLayerNorm
        val allLayersTotal = l * perLayerTotal
        val finalNorm = d

        val exactTotal = embedAndHead + allLayersTotal + finalNorm
        val mbDivisor = 1024.0 * 1024.0

        val weightMbInt4 = ((exactTotal * QuantizationType.INT4.bytesPerParam) / mbDivisor).roundToLong().coerceAtLeast(1L)
        val weightMbInt8 = ((exactTotal * QuantizationType.INT8.bytesPerParam) / mbDivisor).roundToLong().coerceAtLeast(1L)
        val weightMbFp16 = ((exactTotal * QuantizationType.FP16.bytesPerParam) / mbDivisor).roundToLong().coerceAtLeast(2L)

        // KV Cache in FP16 (2 bytes per element) for K and V across L layers:
        // 2 (K,V) * L * maxContextLength * kvDim * 2 bytes
        val kvBytes = 2L * l * config.maxContextLength.toLong() * kvDim * 2L
        val kvCacheMb = ((kvBytes.toDouble()) / mbDivisor).roundToLong().coerceAtLeast(1L)

        val selectedWeightMb = when (quantization) {
            QuantizationType.INT4 -> weightMbInt4
            QuantizationType.INT8 -> weightMbInt8
            QuantizationType.FP16 -> weightMbFp16
        }
        // Runtime buffer overhead (~45 MB for activation scratchpad + tokenizer tables, or 12 MB for micro)
        val activationOverheadMb = if (exactTotal < 10_000_000L) 12L else 64L

        return ParameterBreakdown(
            config = config,
            embeddingAndLmHeadParams = embedAndHead,
            perLayerAttentionParams = perLayerAttn,
            perLayerSwiGluFfnParams = perLayerFfn,
            perLayerRmsNormParams = perLayerNorm,
            perLayerTotalParams = perLayerTotal,
            allLayersTotalParams = allLayersTotal,
            finalRmsNormParams = finalNorm,
            exactTotalParams = exactTotal,
            matchesExpected = (exactTotal == config.expectedExactParams),
            estimatedWeightMbInt4 = weightMbInt4,
            estimatedWeightMbInt8 = weightMbInt8,
            estimatedWeightMbFp16 = weightMbFp16,
            estimatedKvCacheMbAtMaxCtx = kvCacheMb,
            estimatedTotalRuntimeRamMb = selectedWeightMb + kvCacheMb + activationOverheadMb
        )
    }

    fun validateAllCanonicalConfigs(): Boolean {
        return CANONICAL_CONFIGS.all { calculate(it).matchesExpected }
    }

    fun formatParameterCount(params: Long): String {
        return when {
            params >= 1_000_000_000L -> String.format("%.2fB (%,d)", params / 1_000_000_000.0, params)
            params >= 1_000_000L -> String.format("%.2fM (%,d)", params / 1_000_000.0, params)
            else -> String.format("%,d", params)
        }
    }
}
