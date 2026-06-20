"""Fundamentals providers.

Each provider knows how to fetch fundamentals from one source and return them in the
common Fundamentals shape. The service layer tries real providers first and falls
back to the mock so development never blocks on network access.

  - YahooProvider : ratios & growth via the yfinance library (.NS / .BO tickers)
  - NseBseProvider: per-company FII/DII/promoter holding (placeholder fetch wired,
                    real scraping hardened later) — merged on top of Yahoo data
  - MockProvider  : deterministic fake data for offline dev & tests
"""
from __future__ import annotations

import hashlib
from typing import Optional

from .models import Fundamentals


class FundamentalsProvider:
    """Interface every provider implements."""
    def fetch(self, symbol: str, exchange: str = "NSE") -> Optional[Fundamentals]:
        raise NotImplementedError


class YahooProvider(FundamentalsProvider):
    """Pulls ratios from Yahoo Finance via yfinance.

    Indian tickers use a suffix: ".NS" for NSE, ".BO" for BSE (e.g. RELIANCE.NS).
    yfinance is unofficial, so we wrap everything defensively and return None on any
    error — the service will then fall back to the mock provider.
    """
    SUFFIX = {"NSE": ".NS", "BSE": ".BO"}

    def fetch(self, symbol: str, exchange: str = "NSE") -> Optional[Fundamentals]:
        try:
            import yfinance as yf  # imported lazily so the app boots without it
        except ImportError:
            return None

        try:
            ticker = yf.Ticker(symbol + self.SUFFIX.get(exchange, ".NS"))
            info = ticker.info or {}
            if not info:
                return None

            roe = info.get("returnOnEquity")
            growth = info.get("earningsGrowth")
            return Fundamentals(
                symbol=symbol,
                pe_ratio=_num(info.get("trailingPE")),
                eps=_num(info.get("trailingEps")),
                roe_pct=_num(roe * 100) if roe is not None else None,
                profit_growth_pct=_num(growth * 100) if growth is not None else None,
                debt_to_equity=_num(info.get("debtToEquity")),
                source="YAHOO",
            )
        except Exception:
            return None


class NseBseProvider(FundamentalsProvider):
    """Fetches per-company shareholding (FII/DII/promoter) from NSE/BSE filings.

    These are the CORRECT source for the "FII/DII holding > 5%" rule (the daily
    NSE FII/DII report is market-wide flow, not per-stock holding).

    NOTE: NSE/BSE endpoints need careful headers/session handling and polite
    rate-limiting. The network call is intentionally left as a guarded stub here and
    hardened in a follow-up; until then this returns None and the mock fills the gap.
    """
    def fetch(self, symbol: str, exchange: str = "NSE") -> Optional[Fundamentals]:
        # TODO: implement resilient NSE/BSE shareholding-pattern fetch.
        return None


class MockProvider(FundamentalsProvider):
    """Deterministic fake fundamentals so dev/tests never depend on the internet.

    The numbers are derived from a hash of the symbol, so each stock looks distinct
    but the SAME stock always returns the SAME values (important for stable tests).
    """
    def fetch(self, symbol: str, exchange: str = "NSE") -> Optional[Fundamentals]:
        seed = int(hashlib.md5(symbol.encode()).hexdigest(), 16)

        def pick(lo, hi):
            return round(lo + (seed % 1000) / 1000 * (hi - lo), 2)

        # Shift the seed for each field so they vary independently.
        seed //= 7
        return Fundamentals(
            symbol=symbol,
            pe_ratio=pick(8, 45),
            eps=pick(-5, 90),
            roe_pct=pick(5, 40),
            profit_growth_pct=pick(-10, 35),
            fii_holding_pct=pick(0, 25),
            dii_holding_pct=pick(0, 20),
            promoter_pct=pick(30, 75),
            debt_to_equity=pick(0, 1.5),
            source="MOCK",
        )


def _num(v) -> Optional[float]:
    """Coerce a value to float, or None if it isn't a usable number."""
    try:
        if v is None:
            return None
        return round(float(v), 4)
    except (TypeError, ValueError):
        return None
