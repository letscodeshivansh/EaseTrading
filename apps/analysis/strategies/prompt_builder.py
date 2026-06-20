"""Builds the prompt Claude receives.

The golden rule encoded here: Claude must use ONLY the numbers we give it (or fetch
via the read-only MCP tools) and must return strict JSON. That keeps the narrative
grounded in the deterministic figures.
"""
from __future__ import annotations

import json


def build_system_prompt(persona: dict) -> str:
    return (
        f"{persona['system']}\n\n"
        "You write a concise, professional analysis of one Indian stock (NSE/BSE).\n"
        "RULES:\n"
        "1. Use ONLY the numbers provided in the message or returned by the tools. "
        "Never invent figures.\n"
        "2. Return STRICT JSON only, matching this schema:\n"
        '   {"rating": one of '
        '["STRONG_BUY","BUY","NEUTRAL","SELL","STRONG_SELL"],\n'
        '    "entry": number, "stopLoss": number, "target": number, "rrRatio": number,\n'
        '    "memo_md": markdown string in your professional voice}\n'
        "3. Keep entry/stop/target consistent with the support, resistance and "
        "Fibonacci levels provided.\n"
        "4. The memo must cite the actual numbers."
    )


def build_user_prompt(symbol: str, indicators: dict, fundamentals: dict, levels: dict) -> str:
    """The data payload. Claude may also call MCP tools to pull more if needed."""
    payload = {
        "symbol": symbol,
        "indicators": indicators,
        "fundamentals": fundamentals,
        "suggestedLevels": levels,  # from our deterministic engine, as a sanity anchor
    }
    return (
        "Analyse this stock and return the JSON verdict.\n\n"
        + json.dumps(payload, indent=2)
    )
