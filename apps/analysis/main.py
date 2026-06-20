"""
EaseTrading — Analysis Service (Python / FastAPI).

Prompt 2 endpoints (live):
  POST /indicators   -> technical indicators for one instrument
  GET  /fundamentals -> P/E, ROE, EPS, growth, FII/DII for one symbol
  POST /screen       -> run the fundamental screen across a universe

Prompt 3 will add the Claude-powered /analyze endpoint (Agent SDK + MCP, Pro plan).
"""
from fastapi import FastAPI

from models import IndicatorRequest, ScreenRequest, AnalyzeRequest
from indicators import engine
from fundamentals.service import get_fundamentals
from screener.service import run_screen
from strategies.service import analyze as run_analysis
from strategies.personas import list_strategies

app = FastAPI(title="EaseTrading Analysis Service", version="2.0.0")


@app.get("/health")
def health():
    """Liveness check used by docker-compose and the api service."""
    return {"status": "ok", "service": "easetrading-analysis"}


@app.post("/indicators")
def indicators(req: IndicatorRequest):
    """Compute RSI, MACD, Bollinger, moving averages, Fibonacci, S/R and volume."""
    candles = [c.model_dump() for c in req.candles]
    return engine.compute_all(candles)


@app.get("/fundamentals/{symbol}")
def fundamentals(symbol: str, exchange: str = "NSE"):
    """Return normalized fundamentals for one symbol (Yahoo + NSE/BSE, mock fallback)."""
    return get_fundamentals(symbol, exchange)


@app.post("/screen")
def screen(req: ScreenRequest):
    """Apply the fundamental rules across a universe and return ranked matches."""
    universe = [u.model_dump() for u in req.universe]
    rules = [r.model_dump() for r in req.rules] if req.rules else None
    return run_screen(universe, rules)


@app.get("/strategies")
def strategies():
    """List the 10 analysis strategies for the frontend dropdown."""
    return list_strategies()


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    """Produce a grounded verdict (Claude on the Pro plan, deterministic fallback)."""
    candles = [c.model_dump() for c in req.candles]
    return run_analysis(req.symbol, req.strategy, candles, req.fundamentals)
