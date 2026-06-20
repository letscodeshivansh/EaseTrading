"""Unit tests for the screening engine — independent of any data source."""
from screener.rules import evaluate, default_rules
from screener.service import run_screen


def test_passing_stock_clears_all_mandatory():
    # A textbook "good" stock that satisfies every default rule.
    fundamentals = {
        "pe_ratio": 18, "roe_pct": 30, "eps": 45,
        "fii_holding_pct": 12, "dii_holding_pct": 9,
        "profit_growth_pct": 14, "debt_to_equity": 0.4,
    }
    result = evaluate(fundamentals, default_rules())
    assert result["mandatory_pass"] is True
    assert result["score"] == 1.0  # passes optional rule too


def test_failing_one_mandatory_excludes_stock():
    # P/E of 40 breaks the "< 30" rule, so it must not pass.
    fundamentals = {
        "pe_ratio": 40, "roe_pct": 30, "eps": 45,
        "fii_holding_pct": 12, "dii_holding_pct": 9,
        "profit_growth_pct": 14, "debt_to_equity": 0.4,
    }
    result = evaluate(fundamentals, default_rules())
    assert result["mandatory_pass"] is False
    assert "pe_ratio" in result["failed"]


def test_missing_value_counts_as_fail():
    result = evaluate({"pe_ratio": 10}, default_rules())
    # Only pe_ratio present; the other mandatory rules have no data -> fail.
    assert result["mandatory_pass"] is False


def test_run_screen_ranks_and_filters():
    universe = [
        {"symbol": "AAA", "exchange": "NSE", "token": "1"},
        {"symbol": "BBB", "exchange": "NSE", "token": "2"},
    ]
    out = run_screen(universe)  # uses mock fundamentals (deterministic)
    assert out["scanned"] == 2
    assert out["matchCount"] == len(out["matches"])
    # Matches must be sorted by score descending.
    scores = [m["score"] for m in out["matches"]]
    assert scores == sorted(scores, reverse=True)
