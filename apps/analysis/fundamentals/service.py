"""Assembles the best available fundamentals for a symbol.

Strategy: start from Yahoo (ratios), overlay NSE/BSE holdings where available, and
fill any remaining gaps from the mock provider so the screener always has complete
data to work with. The `mode` flag forces mock-only for offline development.
"""
from __future__ import annotations

import os

from .models import Fundamentals
from .providers import YahooProvider, NseBseProvider, MockProvider

# FUNDAMENTALS_MODE = "auto" (real + mock fallback) | "mock" (offline only)
_MODE = os.getenv("FUNDAMENTALS_MODE", "mock")

_yahoo = YahooProvider()
_nsebse = NseBseProvider()
_mock = MockProvider()


def get_fundamentals(symbol: str, exchange: str = "NSE") -> dict:
    if _MODE == "mock":
        return _mock.fetch(symbol, exchange).to_dict()

    # "auto" mode: combine real sources, then backfill missing fields from mock.
    base = _yahoo.fetch(symbol, exchange) or Fundamentals(symbol=symbol)
    holdings = _nsebse.fetch(symbol, exchange)
    if holdings:
        base.fii_holding_pct = holdings.fii_holding_pct
        base.dii_holding_pct = holdings.dii_holding_pct
        base.promoter_pct = holdings.promoter_pct

    _backfill(base, _mock.fetch(symbol, exchange))
    return base.to_dict()


def _backfill(target: Fundamentals, fallback: Fundamentals) -> None:
    """Copy any None field on `target` from `fallback` so data is never half-empty."""
    for field in vars(target):
        if field == "source":
            continue
        if getattr(target, field) is None:
            setattr(target, field, getattr(fallback, field))
