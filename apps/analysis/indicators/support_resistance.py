"""Support & resistance levels, plus a simple volume read.

Support/resistance are price zones where the stock has repeatedly turned around.
We find them as "swing pivots" (a bar higher/lower than its neighbours) and cluster
nearby pivots together; the more touches, the stronger the level.
"""
from __future__ import annotations

import pandas as pd


def _pivots(series: pd.Series, k: int, kind: str) -> list[float]:
    """Find local maxima (kind='high') or minima (kind='low') over a +/-k window."""
    values = series.values
    out = []
    for i in range(k, len(values) - k):
        window = values[i - k : i + k + 1]
        if kind == "high" and values[i] == window.max():
            out.append(float(values[i]))
        elif kind == "low" and values[i] == window.min():
            out.append(float(values[i]))
    return out


def _cluster(levels: list[float], tolerance: float = 0.01) -> list[dict]:
    """Group levels within `tolerance` (1%) of each other; touch count = strength."""
    levels = sorted(levels)
    clusters: list[list[float]] = []
    for lv in levels:
        if clusters and abs(lv - clusters[-1][-1]) / clusters[-1][-1] <= tolerance:
            clusters[-1].append(lv)
        else:
            clusters.append([lv])
    return [
        {"price": round(sum(c) / len(c), 2), "strength": len(c)}
        for c in clusters
    ]


def compute(high: pd.Series, low: pd.Series, k: int = 3) -> dict:
    resistances = _cluster(_pivots(high, k, "high"))
    supports = _cluster(_pivots(low, k, "low"))
    # Strongest few of each, most-touched first.
    resistances.sort(key=lambda x: -x["strength"])
    supports.sort(key=lambda x: -x["strength"])
    return {"supports": supports[:5], "resistances": resistances[:5]}


def volume_read(volume: pd.Series, period: int = 20) -> dict:
    """How does the latest volume compare to its recent average?"""
    if volume.empty:
        return {"latest": None, "avg20": None, "vsAvg": None}
    avg = volume.rolling(window=period, min_periods=1).mean().iloc[-1]
    latest = float(volume.iloc[-1])
    return {
        "latest": int(latest),
        "avg20": int(avg),
        "vsAvg": round(latest / avg, 2) if avg else None,  # >1 means above-average activity
    }
