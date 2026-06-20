"""The one entry point that turns raw candles into a full indicator bundle.

The API and the strategy layer (Prompt 3) both call compute_all(). Keeping a single
assembly point means every part of the app sees the same, consistent numbers.
"""
from __future__ import annotations

import pandas as pd

from . import moving_averages, oscillators, bands, fibonacci, support_resistance


def _to_frame(candles: list[dict]) -> pd.DataFrame:
    """Convert the incoming candle list into a clean, time-sorted DataFrame."""
    df = pd.DataFrame(candles)
    if df.empty:
        return df
    # Accept either ISO timestamps or epoch; sort oldest -> newest.
    # kind="stable" is important: if two bars share a timestamp, keep their input
    # order instead of letting an unstable sort shuffle them.
    df["ts"] = pd.to_datetime(df["ts"])
    df = df.sort_values("ts", kind="stable").reset_index(drop=True)
    for col in ("open", "high", "low", "close", "volume"):
        df[col] = pd.to_numeric(df[col], errors="coerce")
    return df


def _trend_from_sma(close: pd.Series) -> str:
    """A quick trend read: price vs its 50-period average."""
    s50 = moving_averages.sma(close, 50).dropna()
    if s50.empty:
        return "unknown"
    return "up" if float(close.iloc[-1]) > float(s50.iloc[-1]) else "down"


def compute_all(candles: list[dict]) -> dict:
    """Compute every indicator for one instrument and return them as one dict."""
    df = _to_frame(candles)
    if df.empty or len(df) < 2:
        return {"error": "not_enough_data", "bars": 0 if df.empty else len(df)}

    close, high, low, volume = df["close"], df["high"], df["low"], df["volume"]

    bundle = {
        "bars": len(df),
        "lastClose": round(float(close.iloc[-1]), 2),
        "trend": _trend_from_sma(close),
        **moving_averages.compute(close),       # sma, ema, cross_50_200
        **oscillators.compute(close),           # rsi14, macd
        "bollinger": bands.compute(close),
        "fibonacci": fibonacci.compute(high, low, close),
        **support_resistance.compute(high, low),  # supports, resistances
        "volume": support_resistance.volume_read(volume),
    }
    return bundle
