"""The 10 strategy personas.

For each strategy we store:
  - name        : human label
  - focus       : one-line description of its lens
  - system      : the persona "voice" used in the Claude prompt
  - weights     : how strongly it weighs each sub-score (technical, valuation,
                  quality, momentum). These let the deterministic fallback differ
                  meaningfully per persona, exactly as the real LLM would.

The four sub-scores (each 0..1) are computed in verdict.py from the deterministic
engine. weights need not sum to 1; they are normalized when combined.
"""
from __future__ import annotations

# Default balanced weighting, overridden per persona below.
_BAL = {"technical": 1.0, "valuation": 1.0, "quality": 1.0, "momentum": 1.0}

PERSONAS: dict[str, dict] = {
    "MORGAN_STANLEY": {
        "name": "Morgan Stanley — DCF Valuation",
        "focus": "Intrinsic value: is the stock cheap vs its earnings power?",
        "system": "You are a VP-level investment banker at Morgan Stanley who builds "
                  "DCF valuation models. You care most about valuation and earnings quality.",
        "weights": {"technical": 0.4, "valuation": 2.0, "quality": 1.6, "momentum": 0.5},
    },
    "BRIDGEWATER": {
        "name": "Bridgewater — Risk Lens",
        "focus": "Risk-adjusted view: quality and stability over hype.",
        "system": "You are a senior risk analyst at Bridgewater trained in Ray Dalio's "
                  "principles. You weigh balance-sheet quality and downside risk heavily.",
        "weights": {"technical": 0.6, "valuation": 1.2, "quality": 2.0, "momentum": 0.6},
    },
    "JPMORGAN": {
        "name": "JPMorgan — Earnings Momentum",
        "focus": "Earnings trajectory and momentum into results.",
        "system": "You are a senior equity research analyst at JPMorgan writing earnings "
                  "previews. You weigh profit growth and momentum.",
        "weights": {"technical": 1.0, "valuation": 0.8, "quality": 1.4, "momentum": 1.6},
    },
    "BLACKROCK": {
        "name": "BlackRock — Portfolio Fit",
        "focus": "Core-holding quality suitable for a diversified book.",
        "system": "You are a senior portfolio strategist at BlackRock. You favour durable, "
                  "high-quality compounders over speculative trades.",
        "weights": {"technical": 0.7, "valuation": 1.3, "quality": 1.8, "momentum": 0.7},
    },
    "CITADEL": {
        "name": "Citadel — Technical Timing",
        "focus": "Trend, momentum and precise entries/exits.",
        "system": "You are a senior quantitative trader at Citadel who times entries with "
                  "technical analysis. You weigh trend, momentum and chart structure heavily.",
        "weights": {"technical": 2.2, "valuation": 0.3, "quality": 0.5, "momentum": 1.8},
    },
    "HARVARD": {
        "name": "Harvard Endowment — Quality Income",
        "focus": "Steady, high-quality businesses (income tilt).",
        "system": "You are the chief investment strategist for Harvard's endowment, focused "
                  "on durable, cash-generative businesses.",
        "weights": {"technical": 0.5, "valuation": 1.4, "quality": 2.0, "momentum": 0.5},
    },
    "BAIN": {
        "name": "Bain — Competitive Quality",
        "focus": "Business quality and competitive strength.",
        "system": "You are a senior partner at Bain analysing competitive advantage. You "
                  "weigh returns on capital and profitability.",
        "weights": {"technical": 0.5, "valuation": 1.2, "quality": 2.0, "momentum": 0.7},
    },
    "RENAISSANCE": {
        "name": "Renaissance — Statistical Edge",
        "focus": "Quantitative momentum and mean-reversion signals.",
        "system": "You are a quantitative researcher at Renaissance Technologies hunting "
                  "statistical edges. You weigh momentum and technical signals.",
        "weights": {"technical": 1.8, "valuation": 0.5, "quality": 0.7, "momentum": 2.0},
    },
    "MCKINSEY": {
        "name": "McKinsey — Macro & Fundamentals",
        "focus": "Fundamentals through a macro/structural lens.",
        "system": "You are a senior partner at McKinsey Global Institute advising on how "
                  "fundamentals and macro trends affect the stock.",
        "weights": {"technical": 0.6, "valuation": 1.4, "quality": 1.5, "momentum": 0.8},
    },
    "BLACK_BOX": {
        "name": "Black Box — Proprietary Composite",
        "focus": "A balanced blend of every lens into one conviction score.",
        "system": "You are EaseTrading's proprietary Black Box model. You blend technical, "
                  "valuation, quality and momentum signals into one conviction score.",
        # Balanced by design — this is the tunable 'secret sauce'.
        "weights": dict(_BAL),
    },
}


def get_persona(strategy: str) -> dict:
    """Return a persona profile, defaulting to the Black Box if unknown."""
    return PERSONAS.get(strategy.upper(), PERSONAS["BLACK_BOX"])


def list_strategies() -> list[dict]:
    """Lightweight list for the frontend dropdown."""
    return [{"key": k, "name": v["name"], "focus": v["focus"]} for k, v in PERSONAS.items()]
