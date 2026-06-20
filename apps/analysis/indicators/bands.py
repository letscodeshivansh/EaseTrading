"""Bollinger Bands: a volatility envelope around price.

The bands widen when the market is volatile and squeeze when it is calm. A squeeze
(very narrow bandwidth) often precedes a big move, so we report it explicitly.
"""
from __future__ import annotations

import pandas as pd


def compute(close: pd.Series, period: int = 20, num_std: float = 2.0) -> dict:
    mid = close.rolling(window=period, min_periods=period).mean()       # the basis (SMA20)
    std = close.rolling(window=period, min_periods=period).std(ddof=0)  # population std-dev
    upper = mid + num_std * std
    lower = mid - num_std * std

    def last(s):
        s = s.dropna()
        return round(float(s.iloc[-1]), 4) if not s.empty else None

    upper_v, lower_v, mid_v = last(upper), last(lower), last(mid)
    price = float(close.iloc[-1])

    # %B = where price sits within the band (0 = lower, 1 = upper). Undefined when
    # the band has zero width (a perfectly flat series), so we leave it None there.
    pct_b = None
    bandwidth = None
    if upper_v is not None and lower_v is not None:
        if upper_v != lower_v:
            pct_b = round((price - lower_v) / (upper_v - lower_v), 3)
        # Bandwidth is always defined (0 means no volatility).
        bandwidth = round((upper_v - lower_v) / mid_v, 4) if mid_v else None

    return {
        "mid": mid_v,
        "upper": upper_v,
        "lower": lower_v,
        "pctB": pct_b,
        "bandwidth": bandwidth,
    }
