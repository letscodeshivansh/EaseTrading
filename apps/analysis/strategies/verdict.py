"""The grounded verdict engine.

Turns the deterministic indicator + fundamental numbers into a trade verdict:
rating, entry / stop-loss / target, risk-to-reward, and a persona-flavoured memo.

This is the FALLBACK analyst (used when Claude isn't connected) AND the source of
the hard numbers the LLM must stick to. Keeping the math here means a verdict is
never "hallucinated" — every figure traces back to real data.
"""
from __future__ import annotations

RATINGS = ["STRONG_SELL", "SELL", "NEUTRAL", "BUY", "STRONG_BUY"]


def _clamp(x: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, x))


# ---------------------------------------------------------------------------
# Sub-scores (each 0..1). Higher = more bullish / higher quality.
# ---------------------------------------------------------------------------

def technical_score(ind: dict) -> float:
    s = 0.5
    if ind.get("trend") == "up": s += 0.15
    elif ind.get("trend") == "down": s -= 0.15

    cross = ind.get("cross_50_200")
    s += {"golden_cross": 0.10, "above": 0.05, "below": -0.05, "death_cross": -0.10}.get(cross, 0)

    sma200 = ind.get("sma", {}).get("200")
    last = ind.get("lastClose")
    if sma200 and last:
        s += 0.10 if last > sma200 else -0.10

    macd = ind.get("macd", {})
    if macd.get("cross") == "bullish": s += 0.10
    elif macd.get("cross") == "bearish": s -= 0.10
    if (macd.get("hist") or 0) > 0: s += 0.05

    rsi = ind.get("rsi14")
    if rsi is not None:
        if 45 <= rsi <= 65: s += 0.10        # healthy momentum
        elif rsi > 70: s -= 0.10             # overbought
        elif rsi < 30: s += 0.05             # oversold bounce potential
    return _clamp(s)


def momentum_score(ind: dict) -> float:
    s = 0.5
    if (ind.get("macd", {}).get("hist") or 0) > 0: s += 0.2
    else: s -= 0.2
    rsi = ind.get("rsi14")
    if rsi is not None:
        if rsi > 55: s += 0.15
        elif rsi < 45: s -= 0.15
    if ind.get("trend") == "up": s += 0.15
    elif ind.get("trend") == "down": s -= 0.15
    return _clamp(s)


def valuation_score(fund: dict) -> float:
    s = 0.5
    pe = fund.get("pe_ratio")
    if pe is not None:
        if pe < 15: s += 0.30
        elif pe < 30: s += 0.15
        elif pe >= 45: s -= 0.20
    growth = fund.get("profit_growth_pct")
    if growth is not None and growth > 15: s += 0.10
    return _clamp(s)


def quality_score(fund: dict) -> float:
    s = 0.3
    roe = fund.get("roe_pct")
    if roe is not None:
        if roe > 25: s += 0.25
        elif roe > 15: s += 0.10
    eps = fund.get("eps")
    if eps is not None:
        s += 0.15 if eps > 0 else -0.20
    growth = fund.get("profit_growth_pct")
    if growth is not None:
        if growth > 15: s += 0.20
        elif growth > 0: s += 0.10
    dte = fund.get("debt_to_equity")
    if dte is not None:
        if dte < 1: s += 0.10
        elif dte > 2: s -= 0.10
    return _clamp(s)


# ---------------------------------------------------------------------------
# Combine into a composite, rating, and trade levels.
# ---------------------------------------------------------------------------

def _rating_from(score: float) -> str:
    if score >= 0.75: return "STRONG_BUY"
    if score >= 0.60: return "BUY"
    if score >= 0.40: return "NEUTRAL"
    if score >= 0.25: return "SELL"
    return "STRONG_SELL"


def _nearest_below(levels: list[float], price: float):
    below = [x for x in levels if x is not None and x < price]
    return max(below) if below else None


def _nearest_above(levels: list[float], price: float):
    above = [x for x in levels if x is not None and x > price]
    return min(above) if above else None


def _trade_levels(ind: dict, direction: str) -> dict:
    """Pick entry/stop/target from real support, resistance and Fibonacci levels."""
    entry = ind.get("lastClose")
    if not entry:
        return {"entry": None, "stopLoss": None, "target": None, "rrRatio": None}

    supports = [s["price"] for s in ind.get("supports", [])]
    resistances = [r["price"] for r in ind.get("resistances", [])]
    fib_levels = list(ind.get("fibonacci", {}).get("levels", {}).values())
    fib_ext = list(ind.get("fibonacci", {}).get("extensions", {}).values())

    # Minimum reward we want for the trade to be worth taking (1.5x the risk).
    MIN_RR = 1.5

    if direction == "bearish":
        # Short setup: target below, stop above.
        stop = _nearest_above(resistances + fib_levels, entry) or round(entry * 1.05, 2)
        # Don't let the stop sit unrealistically tight (<1% away).
        if stop - entry < entry * 0.01:
            stop = round(entry * 1.05, 2)
        risk = stop - entry
        target = _nearest_below(supports + fib_levels, entry) or round(entry * 0.92, 2)
        # Ensure the target gives at least MIN_RR; otherwise extend it down.
        target = min(target, round(entry - MIN_RR * risk, 2))
    else:
        # Long setup (also used for neutral).
        stop = _nearest_below(supports + fib_levels, entry) or round(entry * 0.95, 2)
        if entry - stop < entry * 0.01:
            stop = round(entry * 0.95, 2)
        risk = entry - stop
        target = _nearest_above(resistances + fib_levels + fib_ext, entry) or round(entry * 1.10, 2)
        # Ensure the target gives at least MIN_RR; otherwise extend it up.
        target = max(target, round(entry + MIN_RR * risk, 2))

    rr = None
    reward = abs(target - entry)
    if risk > 0:
        rr = round(reward / risk, 2)

    return {"entry": round(entry, 2), "stopLoss": round(stop, 2),
            "target": round(target, 2), "rrRatio": rr}


def build_signals(ind: dict, fund: dict) -> dict:
    """The compact, auditable number set shown on the verdict card."""
    return {
        "trend": ind.get("trend"),
        "rsi14": ind.get("rsi14"),
        "macd": ind.get("macd"),
        "sma": ind.get("sma"),
        "cross_50_200": ind.get("cross_50_200"),
        "fibonacci": ind.get("fibonacci"),
        "fundamentals": {
            "peRatio": fund.get("pe_ratio"),
            "roePct": fund.get("roe_pct"),
            "eps": fund.get("eps"),
            "profitGrowthPct": fund.get("profit_growth_pct"),
            "fiiHoldingPct": fund.get("fii_holding_pct"),
            "diiHoldingPct": fund.get("dii_holding_pct"),
        },
    }


def compute_verdict(symbol: str, strategy: str, persona: dict, ind: dict, fund: dict) -> dict:
    """Produce the full grounded verdict for one stock under one persona."""
    sub = {
        "technical": round(technical_score(ind), 3),
        "valuation": round(valuation_score(fund), 3),
        "quality": round(quality_score(fund), 3),
        "momentum": round(momentum_score(ind), 3),
    }

    # Weighted average by the persona's emphasis.
    w = persona["weights"]
    wsum = sum(w.values()) or 1
    composite = sum(sub[k] * w[k] for k in sub) / wsum
    composite = round(_clamp(composite), 3)
    sub["composite"] = composite

    rating = _rating_from(composite)
    direction = "bullish" if rating in ("BUY", "STRONG_BUY") else \
                "bearish" if rating in ("SELL", "STRONG_SELL") else "neutral"

    levels = _trade_levels(ind, direction)

    return {
        "symbol": symbol,
        "strategy": strategy,
        "rating": rating,
        "direction": direction,
        "confidence": composite,
        **levels,
        "subScores": sub,
        "signals": build_signals(ind, fund),
        "source": "grounded",  # overwritten to "claude" when the LLM writes the memo
    }


def validate(verdict: dict) -> bool:
    """Guard the output contract before we trust/persist a verdict."""
    if verdict.get("rating") not in RATINGS:
        return False
    for key in ("entry", "stopLoss", "target"):
        if verdict.get(key) is None:
            return False
    return True
