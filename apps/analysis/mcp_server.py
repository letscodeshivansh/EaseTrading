"""EaseTrading MCP tool server.

Exposes a small set of READ-ONLY tools that Claude can call while writing a verdict,
so it pulls exact, live numbers instead of guessing. This is the heart of "grounded"
analysis.

Tools:
  get_quote(symbol)                 -> latest price
  get_candles(symbol, interval)     -> recent OHLCV
  get_fundamentals(symbol)          -> P/E, ROE, EPS, growth, FII/DII
  get_indicators(symbol)            -> RSI/MACD/Bollinger/Fib/S-R
  run_screener(...)                 -> matching symbols
  get_portfolio()                   -> holdings (Prompt 4)

SAFETY: there is NO order-placement tool here. Trading actions (Prompt 4) go through
the Spring Boot backend with explicit human confirmation; the AI can never trade.

Run as a standalone process; the Agent SDK connects to it. Fundamentals/indicators
are computed locally; quote/candles/portfolio come from the Spring Boot API.
"""
from __future__ import annotations

import os
import httpx

# FastMCP gives us a tiny decorator-based way to declare tools.
try:
    from mcp.server.fastmcp import FastMCP
except Exception:  # pragma: no cover - server only runs when mcp is installed
    FastMCP = None

from fundamentals.service import get_fundamentals as _get_fundamentals
from indicators import engine
from screener.service import run_screen

API_BASE = os.getenv("API_BASE_URL", "http://api:8080")

mcp = FastMCP("easetrading") if FastMCP else None


def _candles_from_api(symbol_token: str, interval: str = "1d", days: int = 250) -> list[dict]:
    """Fetch candles from the Spring Boot API by instrument token."""
    url = f"{API_BASE}/api/candles/{symbol_token}?interval={interval}&days={days}"
    with httpx.Client(timeout=10) as client:
        return client.get(url).json()


if mcp:
    @mcp.tool()
    def get_quote(token: str) -> dict:
        """Latest cached price for an instrument token."""
        with httpx.Client(timeout=10) as client:
            return client.get(f"{API_BASE}/api/quotes/{token}").json()

    @mcp.tool()
    def get_candles(token: str, interval: str = "1d", days: int = 250) -> list[dict]:
        """Recent OHLCV candles for an instrument token."""
        return _candles_from_api(token, interval, days)

    @mcp.tool()
    def get_fundamentals(symbol: str, exchange: str = "NSE") -> dict:
        """P/E, ROE, EPS, profit growth and FII/DII holdings for a symbol."""
        return _get_fundamentals(symbol, exchange)

    @mcp.tool()
    def get_indicators(token: str, interval: str = "1d") -> dict:
        """Full technical-indicator bundle for an instrument token."""
        candles = _candles_from_api(token, interval)
        return engine.compute_all(candles)

    @mcp.tool()
    def run_screener(rules: list[dict] | None = None) -> dict:
        """Run the fundamental screen across the instrument universe."""
        with httpx.Client(timeout=20) as client:
            instruments = client.get(f"{API_BASE}/api/instruments/search?q=").json()
        universe = [{"symbol": i["symbol"], "exchange": i["exchange"], "token": i["token"]}
                    for i in instruments]
        return run_screen(universe, rules)

    @mcp.tool()
    def get_portfolio() -> dict:
        """Current holdings & P&L."""
        with httpx.Client(timeout=10) as client:
            return client.get(f"{API_BASE}/api/portfolio").json()

    @mcp.tool()
    def prepare_order(token: str, side: str, type: str, qty: int, price: float = 0) -> dict:
        """Prepare a DRAFT order and return it WITH its risk report.

        IMPORTANT SAFETY BOUNDARY: this tool only DRAFTS an order — it never executes
        one. The draft still requires explicit human confirmation in the app (a
        separate, non-AI action) before anything reaches the broker. Claude cannot
        confirm or place orders.
        """
        body = {"token": token, "side": side, "type": type, "qty": qty, "price": price}
        with httpx.Client(timeout=10) as client:
            draft = client.post(f"{API_BASE}/api/orders", json=body).json()
        return {
            "note": "DRAFT only — requires human confirmation in the app. Not executed.",
            "draft": draft,
        }


if __name__ == "__main__":
    if not mcp:
        raise SystemExit("The 'mcp' package is not installed. pip install mcp")
    # Serve over stdio so the Agent SDK can launch and talk to us.
    mcp.run()
