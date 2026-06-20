"""Runs the screen across a list of instruments.

For each instrument we fetch fundamentals, evaluate the rules, then return the ones
that pass all mandatory rules — ranked best-first by composite score (and ROE as a
tie-breaker, because a higher return on equity is generally a better business).
"""
from __future__ import annotations

from fundamentals.service import get_fundamentals
from .rules import default_rules, evaluate


def run_screen(universe: list[dict], rules: list[dict] | None = None) -> dict:
    """
    universe : [{"symbol": "RELIANCE", "exchange": "NSE", "token": "2885"}, ...]
    rules    : optional custom rule list; falls back to default_rules().
    """
    rules = rules or default_rules()
    results = []

    for inst in universe:
        fundamentals = get_fundamentals(inst["symbol"], inst.get("exchange", "NSE"))
        verdict = evaluate(fundamentals, rules)
        results.append({
            "token": inst.get("token"),
            "symbol": inst["symbol"],
            "exchange": inst.get("exchange", "NSE"),
            "passedAll": verdict["mandatory_pass"],
            "score": verdict["score"],
            "passed": verdict["passed"],
            "failed": verdict["failed"],
            "values": verdict["values"],
        })

    # Candidates = those clearing every mandatory rule.
    matches = [r for r in results if r["passedAll"]]
    matches.sort(key=lambda r: (-r["score"], -(r["values"].get("roe_pct") or 0)))

    return {
        "rulesUsed": rules,
        "scanned": len(results),
        "matchCount": len(matches),
        "matches": matches,
        # Also return the full scan so the UI can show "why" a stock was excluded.
        "all": results,
    }
