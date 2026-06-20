"""Moving averages and crossover detection.

A moving average smooths price into a trend line. We report the classic
50/100/200-period simple moving averages and detect "golden / death" crosses,
which traders use as long-term trend-change signals.
"""
from __future__ import annotations

import pandas as pd


def sma(close: pd.Series, period: int) -> pd.Series:
    """Simple Moving Average: the plain average of the last `period` closes."""
    return close.rolling(window=period, min_periods=period).mean()


def ema(close: pd.Series, period: int) -> pd.Series:
    """Exponential Moving Average: like SMA but weights recent prices more."""
    return close.ewm(span=period, adjust=False).mean()


def latest(series: pd.Series):
    """Return the last non-NaN value as a plain float, or None if unavailable."""
    s = series.dropna()
    return round(float(s.iloc[-1]), 4) if not s.empty else None


def detect_cross(fast: pd.Series, slow: pd.Series) -> str:
    """Compare two averages on the latest two bars to classify the relationship.

    Returns one of:
      golden_cross : fast crossed ABOVE slow on the last bar (bullish)
      death_cross  : fast crossed BELOW slow on the last bar (bearish)
      above        : fast is above slow (uptrend, no fresh cross)
      below        : fast is below slow (downtrend, no fresh cross)
      unknown      : not enough data
    """
    pair = pd.concat([fast, slow], axis=1).dropna()
    if len(pair) < 2:
        return "unknown"

    prev_fast, prev_slow = pair.iloc[-2]
    curr_fast, curr_slow = pair.iloc[-1]

    if prev_fast <= prev_slow and curr_fast > curr_slow:
        return "golden_cross"
    if prev_fast >= prev_slow and curr_fast < curr_slow:
        return "death_cross"
    return "above" if curr_fast > curr_slow else "below"


def compute(close: pd.Series) -> dict:
    """Bundle the moving-average view used by the dashboard and strategies."""
    sma50, sma100, sma200 = sma(close, 50), sma(close, 100), sma(close, 200)
    return {
        "sma": {"50": latest(sma50), "100": latest(sma100), "200": latest(sma200)},
        "ema": {"12": latest(ema(close, 12)), "26": latest(ema(close, 26))},
        # 50/200 cross is the most-watched long-term trend signal.
        "cross_50_200": detect_cross(sma50, sma200),
    }
