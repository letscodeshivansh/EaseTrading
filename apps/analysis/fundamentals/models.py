"""The normalized fundamentals shape every provider must return.

Using one dataclass means the screener and the rest of the app never care WHERE a
number came from (Yahoo, NSE, mock) — they all look identical.
"""
from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Optional


@dataclass
class Fundamentals:
    symbol: str
    pe_ratio: Optional[float] = None          # price / earnings
    eps: Optional[float] = None               # earnings per share
    roe_pct: Optional[float] = None           # return on equity (%)
    profit_growth_pct: Optional[float] = None # YoY profit growth (%)
    fii_holding_pct: Optional[float] = None    # foreign institutional holding (%)
    dii_holding_pct: Optional[float] = None    # domestic institutional holding (%)
    promoter_pct: Optional[float] = None
    debt_to_equity: Optional[float] = None
    source: str = "unknown"                    # YAHOO | NSE | BSE | MOCK

    def to_dict(self) -> dict:
        return asdict(self)
