"""Orchestrates one stock analysis end-to-end.

Steps (matches the LLD):
  1. Compute indicators from the candles (deterministic).
  2. Build the grounded verdict (rating + entry/stop/target) from the numbers.
  3. Ask Claude (Agent SDK, Pro plan) to write the verdict/memo; if Claude returns
     a valid JSON verdict, use its rating/levels/memo. Otherwise fall back to the
     deterministic analyst's grounded memo.
  4. Return the verdict. The SIGNALS (numbers) are always the deterministic ones, so
     the result is grounded either way.
"""
from __future__ import annotations

from indicators import engine
from .personas import get_persona
from .verdict import compute_verdict, validate
from .memo import grounded_memo
from .prompt_builder import build_system_prompt, build_user_prompt
from .claude_runner import run_claude, ClaudeUnavailable


def analyze(symbol: str, strategy: str, candles: list[dict], fundamentals: dict) -> dict:
    indicators = engine.compute_all(candles)

    # Guard: need enough price history to form an opinion.
    if "error" in indicators:
        return {
            "symbol": symbol, "strategy": strategy, "rating": "NEUTRAL",
            "direction": "neutral", "confidence": 0.5,
            "entry": None, "stopLoss": None, "target": None, "rrRatio": None,
            "signals": {}, "memo": "Not enough price history to analyse this stock yet.",
            "source": "insufficient_data",
        }

    persona = get_persona(strategy)
    verdict = compute_verdict(symbol, strategy, persona, indicators, fundamentals)

    # The levels we computed act as an anchor for Claude.
    levels = {k: verdict[k] for k in ("entry", "stopLoss", "target", "rrRatio")}

    # --- Try Claude first; fall back to the grounded analyst. ---
    try:
        llm = run_claude(
            build_system_prompt(persona),
            build_user_prompt(symbol, indicators, fundamentals, levels),
        )
        # Trust Claude's narrative + (validated) levels; keep our deterministic signals.
        merged = {**verdict,
                  "rating": llm.get("rating", verdict["rating"]),
                  "entry": llm.get("entry", verdict["entry"]),
                  "stopLoss": llm.get("stopLoss", verdict["stopLoss"]),
                  "target": llm.get("target", verdict["target"]),
                  "rrRatio": llm.get("rrRatio", verdict["rrRatio"]),
                  "memo": llm.get("memo_md", ""),
                  "source": "claude"}
        if validate(merged) and merged["memo"]:
            return merged
        # If Claude's output is malformed, fall through to grounded.
    except ClaudeUnavailable as e:
        print(f"[analyze] Using grounded analyst ({e})")

    verdict["memo"] = grounded_memo(verdict, persona)
    return verdict
