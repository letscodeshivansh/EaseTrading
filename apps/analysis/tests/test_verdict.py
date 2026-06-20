"""Tests for the strategy/verdict engine.

These run WITHOUT Claude (the SDK isn't installed in CI), so they exercise the
deterministic grounded analyst — proving the app produces valid, grounded verdicts
on its own.
"""
from strategies.personas import get_persona, list_strategies, PERSONAS
from strategies.verdict import compute_verdict, validate, RATINGS
from strategies.service import analyze


def _bullish_indicators():
    return {
        "bars": 250, "lastClose": 1000.0, "trend": "up", "cross_50_200": "golden_cross",
        "rsi14": 58.0, "macd": {"line": 5.0, "signal": 3.0, "hist": 2.0, "cross": "bullish"},
        "sma": {"50": 980.0, "100": 950.0, "200": 900.0},
        "bollinger": {"mid": 990, "upper": 1050, "lower": 930, "pctB": 0.6, "bandwidth": 0.12},
        "fibonacci": {"trend": "up", "swingHigh": 1100, "swingLow": 800,
                      "levels": {"0.382": 985, "0.5": 950, "0.618": 915},
                      "extensions": {"1.272": 1180, "1.618": 1285}, "active": "0.382"},
        "supports": [{"price": 950, "strength": 3}], "resistances": [{"price": 1080, "strength": 2}],
        "volume": {"latest": 2000000, "avg20": 1500000, "vsAvg": 1.33},
    }


def _good_fundamentals():
    return {"pe_ratio": 18, "roe_pct": 30, "eps": 50, "profit_growth_pct": 20,
            "fii_holding_pct": 12, "dii_holding_pct": 9, "debt_to_equity": 0.4}


def test_rating_is_always_valid():
    v = compute_verdict("TEST", "BLACK_BOX", get_persona("BLACK_BOX"),
                        _bullish_indicators(), _good_fundamentals())
    assert v["rating"] in RATINGS


def test_strong_setup_is_bullish():
    v = compute_verdict("TEST", "CITADEL", get_persona("CITADEL"),
                        _bullish_indicators(), _good_fundamentals())
    assert v["rating"] in ("BUY", "STRONG_BUY")
    assert v["direction"] == "bullish"


def test_trade_levels_make_sense_for_long():
    v = compute_verdict("TEST", "CITADEL", get_persona("CITADEL"),
                        _bullish_indicators(), _good_fundamentals())
    # For a long: stop below entry, target above entry, positive R:R.
    assert v["stopLoss"] < v["entry"] < v["target"]
    assert v["rrRatio"] is not None and v["rrRatio"] > 0


def test_verdict_passes_validation():
    v = compute_verdict("TEST", "BLACK_BOX", get_persona("BLACK_BOX"),
                        _bullish_indicators(), _good_fundamentals())
    assert validate(v) is True


def test_signals_are_grounded_in_inputs():
    # The numbers on the card must equal the numbers we fed in (no invention).
    ind = _bullish_indicators()
    v = compute_verdict("TEST", "BLACK_BOX", get_persona("BLACK_BOX"), ind, _good_fundamentals())
    assert v["signals"]["rsi14"] == ind["rsi14"]
    assert v["signals"]["sma"]["200"] == ind["sma"]["200"]
    assert v["signals"]["fundamentals"]["roePct"] == 30


def test_personas_tilt_the_score():
    # Citadel (technical-heavy) and Morgan Stanley (valuation-heavy) should not always
    # agree — confirm their composite scores can differ on the same data.
    ind, fund = _bullish_indicators(), _good_fundamentals()
    citadel = compute_verdict("T", "CITADEL", get_persona("CITADEL"), ind, fund)
    ms = compute_verdict("T", "MORGAN_STANLEY", get_persona("MORGAN_STANLEY"), ind, fund)
    assert citadel["subScores"]["composite"] != ms["subScores"]["composite"]


def test_all_ten_strategies_exist():
    assert len(PERSONAS) == 10
    assert len(list_strategies()) == 10


def test_analyze_end_to_end_produces_memo():
    # Build 250 rising candles and run the full analyze() path (falls back to grounded).
    candles = []
    price = 100.0
    for i in range(250):
        price *= 1.004
        candles.append({"ts": f"2026-01-01T00:00:00Z", "open": price, "high": price * 1.01,
                        "low": price * 0.99, "close": price, "volume": 1000})
    result = analyze("TEST", "BLACK_BOX", candles, _good_fundamentals())
    assert result["rating"] in RATINGS
    assert result["memo"]  # a non-empty memo was produced
    assert result["source"] in ("grounded", "claude")
