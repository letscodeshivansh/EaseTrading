"""Unit tests for the indicator library.

These lock the math against known, hand-checkable values. If a future change breaks
a formula, these tests fail immediately. Run with:  pytest -q
"""
import pandas as pd

from indicators import moving_averages as ma
from indicators import oscillators as osc
from indicators import bands
from indicators import fibonacci
from indicators import support_resistance as sr
from indicators import engine


def test_sma_simple_average():
    close = pd.Series([1, 2, 3, 4, 5], dtype=float)
    # Average of the last 3 values = (3+4+5)/3 = 4.
    assert ma.latest(ma.sma(close, 3)) == 4.0


def test_ema_returns_value():
    close = pd.Series([1, 2, 3, 4, 5], dtype=float)
    assert ma.latest(ma.ema(close, 3)) is not None


def test_detect_golden_cross():
    # fast starts below slow then ends above -> golden cross on the last bar.
    fast = pd.Series([1, 2, 3, 10], dtype=float)
    slow = pd.Series([5, 5, 5, 5], dtype=float)
    assert ma.detect_cross(fast, slow) == "golden_cross"


def test_rsi_all_gains_is_100():
    # A strictly rising series has no losses, so RSI maxes out near 100.
    close = pd.Series(range(1, 30), dtype=float)
    r = osc.rsi(close, 14).dropna().iloc[-1]
    assert 99.0 <= r <= 100.0


def test_rsi_is_bounded():
    close = pd.Series([5, 3, 8, 2, 9, 1, 7, 4, 6, 10, 2, 8, 3, 9, 5], dtype=float)
    r = osc.rsi(close, 14).dropna()
    assert (r >= 0).all() and (r <= 100).all()


def test_macd_constant_series_is_zero():
    close = pd.Series([100] * 40, dtype=float)
    m = osc.macd(close)
    assert m["line"] == 0 and m["signal"] == 0 and m["hist"] == 0


def test_bollinger_constant_series():
    close = pd.Series([50] * 30, dtype=float)
    b = bands.compute(close, period=20)
    # No volatility -> bands collapse onto the mid line.
    assert b["mid"] == 50.0
    assert b["bandwidth"] == 0.0


def test_fibonacci_uptrend_levels():
    # Low (100) first, high (200) last -> uptrend, diff = 100.
    lows = pd.Series([100, 110, 120, 130, 140, 150, 160, 170, 180, 190], dtype=float)
    highs = pd.Series([105, 115, 125, 135, 145, 155, 165, 175, 185, 200], dtype=float)
    close = pd.Series([105, 115, 125, 135, 145, 155, 165, 175, 185, 195], dtype=float)
    fib = fibonacci.compute(highs, lows, close)
    assert fib["trend"] == "up"
    # 38.2% retracement from the high: 200 - 100 * 0.382 = 161.8.
    assert fib["levels"]["0.382"] == 161.8


def test_support_resistance_finds_levels():
    high = pd.Series([1, 5, 1, 1, 6, 1, 1, 5, 1], dtype=float)
    low = pd.Series([3, 1, 3, 3, 1, 3, 3, 1, 3], dtype=float)
    out = sr.compute(high, low, k=1)
    assert len(out["resistances"]) >= 1
    assert len(out["supports"]) >= 1


def test_engine_compute_all_bundles_everything():
    # Build 60 bars of gently rising synthetic data.
    candles = []
    price = 100.0
    for i in range(60):
        price *= 1.005
        candles.append({
            "ts": f"2026-01-{(i % 28) + 1:02d}T00:00:00Z",
            "open": price, "high": price * 1.01, "low": price * 0.99,
            "close": price, "volume": 1000 + i,
        })
    out = engine.compute_all(candles)
    for key in ("rsi14", "macd", "bollinger", "fibonacci", "sma", "supports", "volume"):
        assert key in out
