#!/usr/bin/env python3
"""
PagePilot — Automated Model Parameter Count Validation Script
=============================================================
Mathematically verifies the exact parameter count of every PagePilot
decoder-only Transformer configuration, breaking down:
  1. Token Embedding & Output LM Head (Tied vs. Untied)
  2. Grouped-Query Attention (W_q, W_k, W_v, W_o, bias=False)
  3. SwiGLU Feed-Forward Network (W_gate, W_up, W_down, bias=False)
  4. Pre-Attention, Pre-FFN, and Final RMSNorm weights
  5. Estimated weight storage & runtime RAM across FP16, INT8, and INT4
"""

from dataclasses import dataclass
from typing import List


@dataclass(frozen=True)
class PagePilotConfigSpec:
    config_id: str
    stage_label: str
    vocab_size: int
    tie_word_embeddings: bool
    num_layers: int
    d_model: int
    num_query_heads: int
    num_kv_heads: int
    d_ffn: int
    max_seq_len: int
    expected_exact_params: int

    @property
    def head_dim(self) -> int:
        assert self.d_model % self.num_query_heads == 0, "d_model must be divisible by num_query_heads"
        return self.d_model // self.num_query_heads

    @property
    def kv_dim(self) -> int:
        return self.num_kv_heads * self.head_dim

    def embedding_and_head_params(self) -> int:
        if self.tie_word_embeddings:
            return self.vocab_size * self.d_model
        return 2 * self.vocab_size * self.d_model

    def per_layer_attention_params(self) -> int:
        # W_q: (d_model, d_model), W_k: (d_model, kv_dim), W_v: (d_model, kv_dim), W_o: (d_model, d_model)
        w_q = self.d_model * self.d_model
        w_k = self.d_model * self.kv_dim
        w_v = self.d_model * self.kv_dim
        w_o = self.d_model * self.d_model
        return w_q + w_k + w_v + w_o

    def per_layer_ffn_params(self) -> int:
        # SwiGLU: W_gate (d_model, d_ffn), W_up (d_model, d_ffn), W_down (d_ffn, d_model)
        return 3 * self.d_model * self.d_ffn

    def per_layer_norm_params(self) -> int:
        # Pre-attention RMSNorm + Pre-FFN RMSNorm
        return 2 * self.d_model

    def per_layer_total_params(self) -> int:
        return (
            self.per_layer_attention_params()
            + self.per_layer_ffn_params()
            + self.per_layer_norm_params()
        )

    def final_norm_params(self) -> int:
        return self.d_model

    def total_params(self) -> int:
        return (
            self.embedding_and_head_params()
            + self.num_layers * self.per_layer_total_params()
            + self.final_norm_params()
        )


CANONICAL_CONFIGS: List[PagePilotConfigSpec] = [
    PagePilotConfigSpec(
        config_id="PagePilot-Verify-Micro-918K",
        stage_label="Stage 0 (Pipeline Verify - Compact 4K Vocab)",
        vocab_size=4096,
        tie_word_embeddings=True,
        num_layers=2,
        d_model=128,
        num_query_heads=4,
        num_kv_heads=2,
        d_ffn=384,
        max_seq_len=512,
        expected_exact_params=918_144,
    ),
    PagePilotConfigSpec(
        config_id="PagePilot-Verify-Micro-4.5M",
        stage_label="Stage 0B (Pipeline Verify - Full 32K Vocab)",
        vocab_size=32000,
        tie_word_embeddings=True,
        num_layers=2,
        d_model=128,
        num_query_heads=4,
        num_kv_heads=2,
        d_ffn=384,
        max_seq_len=512,
        expected_exact_params=4_489_856,
    ),
    PagePilotConfigSpec(
        config_id="PagePilot-Exp-100M",
        stage_label="Stage 1 (Experimental Trained & LiteRT Validation)",
        vocab_size=32000,
        tie_word_embeddings=True,
        num_layers=12,
        d_model=768,
        num_query_heads=12,
        num_kv_heads=4,
        d_ffn=2048,
        max_seq_len=1024,
        expected_exact_params=100_092_672,
    ),
    PagePilotConfigSpec(
        config_id="PagePilot-Mobile-336M",
        stage_label="Stage 2 (First Practical 8GB Mobile Target)",
        vocab_size=32000,
        tie_word_embeddings=False,
        num_layers=24,
        d_model=1024,
        num_query_heads=16,
        num_kv_heads=4,
        d_ffn=2816,
        max_seq_len=2048,
        expected_exact_params=336_118_784,
    ),
    PagePilotConfigSpec(
        config_id="PagePilot-Edge-1.1B",
        stage_label="Stage 3A (Secondary Mobile Scaling Target)",
        vocab_size=32000,
        tie_word_embeddings=False,
        num_layers=22,
        d_model=2048,
        num_query_heads=32,
        num_kv_heads=4,
        d_ffn=5632,
        max_seq_len=2048,
        expected_exact_params=1_100_048_384,
    ),
    PagePilotConfigSpec(
        config_id="PagePilot-3B",
        stage_label="Stage 3B (Flagship 3B Research Target)",
        vocab_size=32000,
        tie_word_embeddings=False,
        num_layers=28,
        d_model=3072,
        num_query_heads=24,
        num_kv_heads=8,
        d_ffn=8192,
        max_seq_len=2048,
        expected_exact_params=3_015_355_392,
    ),
]


def validate_all() -> None:
    print("=" * 92)
    print("PAGEPILOT TRANSFORMER — AUTOMATED PARAMETER COUNT VALIDATION")
    print("=" * 92)
    for spec in CANONICAL_CONFIGS:
        computed = spec.total_params()
        assert computed == spec.expected_exact_params, (
            f"Mismatch for {spec.config_id}: computed {computed:,}, "
            f"expected {spec.expected_exact_params:,}"
        )
        int4_mb = (computed * 0.55) / (1024 * 1024)
        fp16_mb = (computed * 2.0) / (1024 * 1024)
        print(f"\n[{spec.config_id}] — {spec.stage_label}")
        print(
            f"  Vocab={spec.vocab_size:,} | TiedEmbeds={spec.tie_word_embeddings} | "
            f"L={spec.num_layers} | d_model={spec.d_model} | "
            f"Heads={spec.num_query_heads}:{spec.num_kv_heads} (d_head={spec.head_dim}) | "
            f"SwiGLU d_ffn={spec.d_ffn}"
        )
        print(f"  • Embedding + LM Head : {spec.embedding_and_head_params():>14,}")
        print(f"  • Per-Layer Attention : {spec.per_layer_attention_params():>14,}")
        print(f"  • Per-Layer SwiGLU FFN: {spec.per_layer_ffn_params():>14,}")
        print(f"  • Per-Layer RMSNorms  : {spec.per_layer_norm_params():>14,}")
        print(f"  • All {spec.num_layers:>2} Layers Total : {spec.num_layers * spec.per_layer_total_params():>14,}")
        print(f"  • Final RMSNorm       : {spec.final_norm_params():>14,}")
        print(f"  => EXACT TOTAL PARAMS : {computed:>14,}  [VERIFIED]")
        print(f"  => Est. Weight Size   : INT4 ~{int4_mb:.1f} MB | FP16 ~{fp16_mb:.1f} MB")
    print("\n" + "=" * 92)
    print("ALL 6 PAGEPILOT CONFIGURATIONS PASSED EXACT MATHEMATICAL VALIDATION.")
    print("=" * 92)


if __name__ == "__main__":
    validate_all()
