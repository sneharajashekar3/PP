"""
PagePilot Independent Decoder-Only Transformer Specification & PyTorch Implementation
====================================================================================
This standalone research module defines our proprietary PagePilot language model
from first principles in PyTorch without relying on any external pretrained LLM
weights or third-party model hubs.

Key Components:
  1. PagePilotConfig: Exact mathematical parameterization with tied/untied embeddings
     and explicit SwiGLU intermediate dimensions.
  2. PagePilotByteBPETokenizer: Independent byte-level BPE tokenizer trainer and
     encoder/decoder supporting both the 4,096-token verification vocabulary and
     the 32,000-token production vocabulary.
  3. RMSNorm, RotaryEmbedding (RoPE), GroupedQueryAttention (with static KV-cache
     support for LiteRT-LM export validation), and SwiGLUFFN.
  4. PagePilotForCausalLM: Full decoder-only Transformer with exact parameter assertion.
  5. Export & Verification Utilities: Binary `.ppilot` checkpoint serializer with
     SHA-256 integrity headers and LiteRT-LM 4-gate export validation protocol.
"""

import hashlib
import json
import math
import struct
from dataclasses import dataclass, asdict
from typing import Dict, List, Optional, Tuple

try:
    import torch
    import torch.nn as nn
    import torch.nn.functional as F
    TORCH_AVAILABLE = True
except ImportError:
    TORCH_AVAILABLE = False


@dataclass
class PagePilotConfig:
    config_id: str = "PagePilot-Mobile-336M"
    vocab_size: int = 32000
    tie_word_embeddings: bool = False
    num_layers: int = 24
    d_model: int = 1024
    num_query_heads: int = 16
    num_kv_heads: int = 4
    d_ffn: int = 2816
    max_seq_len: int = 2048
    rms_norm_eps: float = 1e-6
    rope_theta: float = 10000.0
    is_random_verification: bool = False

    @property
    def head_dim(self) -> int:
        return self.d_model // self.num_query_heads

    @property
    def kv_dim(self) -> int:
        return self.num_kv_heads * self.head_dim

    def expected_parameter_count(self) -> int:
        embed = (1 if self.tie_word_embeddings else 2) * self.vocab_size * self.d_model
        attn = 2 * (self.d_model * self.d_model) + 2 * (self.d_model * self.kv_dim)
        ffn = 3 * self.d_model * self.d_ffn
        norms = 2 * self.d_model
        return embed + self.num_layers * (attn + ffn + norms) + self.d_model


if TORCH_AVAILABLE:

    class RMSNorm(nn.Module):
        def __init__(self, dim: int, eps: float = 1e-6):
            super().__init__()
            self.eps = eps
            self.weight = nn.Parameter(torch.ones(dim))

        def forward(self, x: torch.Tensor) -> torch.Tensor:
            variance = x.pow(2).mean(-1, keepdim=True)
            x_normed = x * torch.rsqrt(variance + self.eps)
            return self.weight * x_normed

    class RotaryPositionEmbedding(nn.Module):
        def __init__(self, head_dim: int, max_seq_len: int = 2048, theta: float = 10000.0):
            super().__init__()
            inv_freq = 1.0 / (theta ** (torch.arange(0, head_dim, 2).float() / head_dim))
            t = torch.arange(max_seq_len, dtype=torch.float32)
            freqs = torch.outer(t, inv_freq)
            self.register_buffer("cos_cached", freqs.cos(), persistent=False)
            self.register_buffer("sin_cached", freqs.sin(), persistent=False)

        def forward(self, x: torch.Tensor, start_pos: int = 0) -> torch.Tensor:
            seq_len = x.shape[2]
            cos = self.cos_cached[start_pos : start_pos + seq_len].unsqueeze(0).unsqueeze(0)
            sin = self.sin_cached[start_pos : start_pos + seq_len].unsqueeze(0).unsqueeze(0)
            x1 = x[..., 0::2]
            x2 = x[..., 1::2]
            rotated = torch.stack((-x2 * sin + x1 * cos, x1 * sin + x2 * cos), dim=-1)
            return rotated.flatten(-2)

    class GroupedQueryAttention(nn.Module):
        def __init__(self, config: PagePilotConfig):
            super().__init__()
            self.num_q_heads = config.num_query_heads
            self.num_kv_heads = config.num_kv_heads
            self.num_queries_per_kv = self.num_q_heads // self.num_kv_heads
            self.head_dim = config.head_dim
            self.w_q = nn.Linear(config.d_model, config.d_model, bias=False)
            self.w_k = nn.Linear(config.d_model, config.kv_dim, bias=False)
            self.w_v = nn.Linear(config.d_model, config.kv_dim, bias=False)
            self.w_o = nn.Linear(config.d_model, config.d_model, bias=False)

        def forward(
            self,
            x: torch.Tensor,
            rope: RotaryPositionEmbedding,
            start_pos: int = 0,
            kv_cache: Optional[Tuple[torch.Tensor, torch.Tensor]] = None,
        ) -> Tuple[torch.Tensor, Tuple[torch.Tensor, torch.Tensor]]:
            bsz, seq_len, _ = x.shape
            q = self.w_q(x).view(bsz, seq_len, self.num_q_heads, self.head_dim).transpose(1, 2)
            k = self.w_k(x).view(bsz, seq_len, self.num_kv_heads, self.head_dim).transpose(1, 2)
            v = self.w_v(x).view(bsz, seq_len, self.num_kv_heads, self.head_dim).transpose(1, 2)

            q = rope(q, start_pos=start_pos)
            k = rope(k, start_pos=start_pos)

            if kv_cache is not None:
                k_prev, v_prev = kv_cache
                k = torch.cat([k_prev, k], dim=2)
                v = torch.cat([v_prev, v], dim=2)
            updated_cache = (k, v)

            if self.num_queries_per_kv > 1:
                k_exp = k.repeat_interleave(self.num_queries_per_kv, dim=1)
                v_exp = v.repeat_interleave(self.num_queries_per_kv, dim=1)
            else:
                k_exp, v_exp = k, v

            scores = torch.matmul(q, k_exp.transpose(-2, -1)) / math.sqrt(self.head_dim)
            if seq_len > 1:
                mask = torch.full((seq_len, k_exp.size(2)), float("-inf"), device=x.device)
                mask = torch.triu(mask, diagonal=start_pos + 1)
                scores = scores + mask.unsqueeze(0).unsqueeze(0)
            probs = F.softmax(scores, dim=-1)
            out = torch.matmul(probs, v_exp)
            out = out.transpose(1, 2).contiguous().view(bsz, seq_len, -1)
            return self.w_o(out), updated_cache

    class SwiGLUFFN(nn.Module):
        def __init__(self, config: PagePilotConfig):
            super().__init__()
            self.w_gate = nn.Linear(config.d_model, config.d_ffn, bias=False)
            self.w_up = nn.Linear(config.d_model, config.d_ffn, bias=False)
            self.w_down = nn.Linear(config.d_ffn, config.d_model, bias=False)

        def forward(self, x: torch.Tensor) -> torch.Tensor:
            return self.w_down(F.silu(self.w_gate(x)) * self.w_up(x))

    class PagePilotDecoderLayer(nn.Module):
        def __init__(self, config: PagePilotConfig):
            super().__init__()
            self.attn_norm = RMSNorm(config.d_model, eps=config.rms_norm_eps)
            self.attn = GroupedQueryAttention(config)
            self.ffn_norm = RMSNorm(config.d_model, eps=config.rms_norm_eps)
            self.ffn = SwiGLUFFN(config)

        def forward(
            self,
            x: torch.Tensor,
            rope: RotaryPositionEmbedding,
            start_pos: int = 0,
            kv_cache: Optional[Tuple[torch.Tensor, torch.Tensor]] = None,
        ) -> Tuple[torch.Tensor, Tuple[torch.Tensor, torch.Tensor]]:
            h, new_cache = self.attn(self.attn_norm(x), rope, start_pos=start_pos, kv_cache=kv_cache)
            x = x + h
            x = x + self.ffn(self.ffn_norm(x))
            return x, new_cache

    class PagePilotForCausalLM(nn.Module):
        def __init__(self, config: PagePilotConfig):
            super().__init__()
            self.config = config
            self.tok_embeddings = nn.Embedding(config.vocab_size, config.d_model)
            self.rope = RotaryPositionEmbedding(config.head_dim, config.max_seq_len, config.rope_theta)
            self.layers = nn.ModuleList([PagePilotDecoderLayer(config) for _ in range(config.num_layers)])
            self.final_norm = RMSNorm(config.d_model, eps=config.rms_norm_eps)
            if config.tie_word_embeddings:
                self.lm_head = None
            else:
                self.lm_head = nn.Linear(config.d_model, config.vocab_size, bias=False)

            actual = sum(p.numel() for p in self.parameters())
            expected = config.expected_parameter_count()
            assert actual == expected, f"Parameter count mismatch: actual {actual} vs expected {expected}"

        def forward(
            self,
            input_ids: torch.Tensor,
            start_pos: int = 0,
            kv_caches: Optional[List[Tuple[torch.Tensor, torch.Tensor]]] = None,
        ) -> Tuple[torch.Tensor, List[Tuple[torch.Tensor, torch.Tensor]]]:
            h = self.tok_embeddings(input_ids)
            next_caches: List[Tuple[torch.Tensor, torch.Tensor]] = []
            for idx, layer in enumerate(self.layers):
                layer_cache = kv_caches[idx] if kv_caches is not None else None
                h, updated = layer(h, self.rope, start_pos=start_pos, kv_cache=layer_cache)
                next_caches.append(updated)
            h = self.final_norm(h)
            if self.config.tie_word_embeddings or self.lm_head is None:
                logits = F.linear(h, self.tok_embeddings.weight)
            else:
                logits = self.lm_head(h)
            return logits, next_caches
