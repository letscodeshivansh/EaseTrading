"""Fibonacci retracement & extension — the platform's core requested technique.

Idea: after a strong move, price often retraces to a "Fibonacci ratio" of that move
(38.2%, 50%, 61.8%) before continuing. Those ratios become likely support (in an
uptrend) or resistance (in a downtrend). Extensions (127.2%, 161.8%) give profit
targets.

Algorithm (matches the LLD):
  1. Look at the last `window` bars.
  2. Find the swing high and swing low.
  3. Decide trend direction by which one came LAST.
  4. Project the standard ratios between them.
  5. Tag the level nearest current price as the "active" zone.
"""
from __future__ import annotations

import pandas as pd

RETRACEMENTS = [0.236, 0.382, 0.5, 0.618, 0.786]
EXTENSIONS = [1.272, 1.618]


def compute(high: pd.Series, low: pd.Series, close: pd.Series, window: int = 120) -> dict:
    h = high.tail(window)
    l = low.tail(window)
    if h.empty or l.empty:
        return {"trend": "unknown", "levels": {}, "extensions": {}, "active": None}

    swing_high = float(h.max())
    swing_low = float(l.min())
    diff = swing_high - swing_low
    if diff == 0:
        return {"trend": "flat", "levels": {}, "extensions": {}, "active": None}

    # Trend direction: did the high or the low occur more recently?
    idx_high = h.idxmax()
    idx_low = l.idxmin()
    uptrend = idx_low < idx_high  # low first, then high => up move

    levels = {}
    for r in RETRACEMENTS:
        # Uptrend: retraces DOWN from the high (support zones).
        # Downtrend: retraces UP from the low (resistance zones).
        levels[str(r)] = round(swing_high - diff * r if uptrend else swing_low + diff * r, 2)

    extensions = {}
    for e in EXTENSIONS:
        extensions[str(e)] = round(swing_high + diff * (e - 1) if uptrend
                                   else swing_low - diff * (e - 1), 2)

    # Which retracement level is price hugging right now?
    price = float(close.iloc[-1])
    active = min(levels.items(), key=lambda kv: abs(kv[1] - price))[0] if levels else None

    return {
        "trend": "up" if uptrend else "down",
        "swingHigh": round(swing_high, 2),
        "swingLow": round(swing_low, 2),
        "levels": levels,
        "extensions": extensions,
        "active": active,
    }
