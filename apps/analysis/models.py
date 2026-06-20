"""Pydantic request/response models for the API layer.

Pydantic validates incoming JSON automatically and gives FastAPI its docs.
"""
from __future__ import annotations

from typing import Optional
from pydantic import BaseModel


class Candle(BaseModel):
    ts: str
    open: float
    high: float
    low: float
    close: float
    volume: float = 0


class IndicatorRequest(BaseModel):
    token: str
    interval: str = "1d"
    candles: list[Candle]


class UniverseItem(BaseModel):
    symbol: str
    exchange: str = "NSE"
    token: Optional[str] = None


class Rule(BaseModel):
    key: str
    op: str            # lt | lte | gt | gte | eq
    value: float
    mandatory: bool = True


class ScreenRequest(BaseModel):
    universe: list[UniverseItem]
    rules: Optional[list[Rule]] = None  # None = use the default rule set


class AnalyzeRequest(BaseModel):
    symbol: str
    strategy: str = "BLACK_BOX"          # one of the 10 persona keys
    candles: list[Candle]                # price history for indicators
    fundamentals: dict = {}              # P/E, ROE, etc. (snake_case keys)
