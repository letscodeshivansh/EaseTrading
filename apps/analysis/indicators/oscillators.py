"""Momentum oscillators: RSI and MACD.

These tell you whether a stock is over-extended and which way momentum is turning.
"""
from __future__ import annotations

import pandas as pd


def rsi(close: pd.Series, period: int = 14) -> pd.Series:
    """Relative Strength Index (Wilder's smoothing), ranges 0-100.

    Reading: above 70 = overbought (may pull back), below 30 = oversold (may bounce).
    """
    delta = close.diff()
    gain = delta.clip(lower=0)          # positive moves only
    loss = -delta.clip(upper=0)         # absolute value of negative moves

    # Wilder's smoothing is an EMA with alpha = 1/period.
    avg_gain = gain.ewm(alpha=1 / period, adjust=False).mean()
    avg_loss = loss.ewm(alpha=1 / period, adjust=False).mean()

    rs = avg_gain / avg_loss
    return 100 - (100 / (1 + rs))


def macd(close: pd.Series, fast: int = 12, slow: int = 26, signal: int = 9) -> dict:
    """Moving Average Convergence Divergence.

    - macd line  = EMA(fast) - EMA(slow)   (momentum)
    - signal     = EMA(signal) of the macd line
    - histogram  = macd line - signal      (momentum of momentum)

    A histogram crossing from negative to positive is a bullish trigger.
    """
    ema_fast = close.ewm(span=fast, adjust=False).mean()
    ema_slow = close.ewm(span=slow, adjust=False).mean()
    macd_line = ema_fast - ema_slow
    signal_line = macd_line.ewm(span=signal, adjust=False).mean()
    histogram = macd_line - signal_line

    def last(s):
        return round(float(s.iloc[-1]), 4) if not s.dropna().empty else None

    # Classify a fresh histogram cross for plain-English signalling.
    cross = "none"
    hist = histogram.dropna()
    if len(hist) >= 2:
        if hist.iloc[-2] <= 0 < hist.iloc[-1]:
            cross = "bullish"
        elif hist.iloc[-2] >= 0 > hist.iloc[-1]:
            cross = "bearish"

    return {
        "line": last(macd_line),
        "signal": last(signal_line),
        "hist": last(histogram),
        "cross": cross,
    }


def compute(close: pd.Series) -> dict:
    rsi_series = rsi(close, 14)
    rsi_val = round(float(rsi_series.dropna().iloc[-1]), 2) if not rsi_series.dropna().empty else None
    return {"rsi14": rsi_val, "macd": macd(close)}
