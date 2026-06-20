# EaseTrading

AI-assisted stock analysis & trading platform for the Indian markets (NSE & BSE).

Built across four phases — **all four are now complete.**

## Features

- **Live market data** — Angel One SmartAPI (exchange-sourced, free) with a live
  WebSocket stream to the browser. A built-in **mock feed** lets everything run
  without credentials during development.
- **Fundamentals & screening** — P/E, ROE, EPS, profit growth (Yahoo) plus FII/DII &
  promoter holding (NSE/BSE), with an editable rule screener (P/E < 30, ROE > 25%,
  EPS > 0, FII/DII > 5%, profit growth > 0).
- **Technical analysis** — SMA/EMA, RSI, MACD, Bollinger, **Fibonacci**, support /
  resistance and volume, with chart overlays.
- **AI analysis** — 10 strategies (9 investor personas + Black Box) producing grounded
  verdicts (rating, entry/stop/target, memo). Powered by **Claude on your Pro plan**
  via the Agent SDK + MCP; falls back to a deterministic grounded analyst offline.
- **Trading** — order management with a **risk engine** and a **two-step human
  confirmation** flow. Paper-trading by default; live Angel One orders when enabled.
  The AI can draft but never place orders.
- **Alerts** — price & RSI alerts evaluated continuously, with notifications.
- **Portfolio & risk** — holdings, live P&L, allocation and a concentration risk view.
- **Audit log** — every order action and verdict recorded.

## Architecture

```
apps/
  web/        Next.js 14 frontend (TypeScript, Tailwind) — responsive
  api/        Spring Boot 3 backend (Java 21) — system of record
  analysis/   Python FastAPI — indicators, screener, AI verdicts, MCP server
infra/        docker-compose: postgres(+timescale), redis, all services
docs/         HLD.docx, LLD.docx
```

## Quick start

```bash
cp .env.example .env          # defaults are safe: mock feed + paper trading
cd infra
docker compose up --build     # starts db, redis, api, analysis, web
# open http://localhost:3000
```

Everything works out of the box with **no credentials** (mock data, paper trades).

## Safety modes (all default to safe)

| Setting | Default | Effect |
|---|---|---|
| `MARKET_FEED_MODE` | `mock` | simulated ticks; set `live` for Angel One |
| `FUNDAMENTALS_MODE` | `mock` | fake fundamentals; set `auto` for Yahoo + NSE/BSE |
| `TRADING_MODE` | `paper` | simulated orders; set `live` for real money |
| `ANTHROPIC_API_KEY` | unset | leave unset — the AI uses your Claude Pro plan, not the paid API |

## Testing walkthrough

1. **Live data** — open the dashboard; the chart and watchlist update in real time.
2. **Indicators** — toggle SMA / Fib overlays; check the indicator read-out.
3. **Screener** — go to Screener, adjust thresholds, Run screen, see ranked matches.
4. **AI analysis** — on the dashboard pick a strategy, click Analyze, read the verdict.
5. **Paper trade** — Orders page: pick a stock, Review (see the risk report), Confirm.
6. **Portfolio** — see the holding, live P&L and the risk view appear.
7. **Alerts** — create a price alert; it flips to TRIGGERED when the condition is met.

### Automated tests

```bash
# Python (indicators, screener, verdict engine)
cd apps/analysis && pip install -r requirements.txt && pytest -q   # 22 tests

# Java (position math, risk engine) — runs in your environment with Maven
cd apps/api && mvn test
```

## Going live (when ready)

1. Set Angel One credentials in `.env` and `MARKET_FEED_MODE=live`.
2. Set `FUNDAMENTALS_MODE=auto` for real fundamentals.
3. To enable real trading, set `TRADING_MODE=live` — **start with tiny quantities**.
4. To use Claude, install the Agent SDK and authenticate with your Pro plan
   (`claude login`); do **not** set `ANTHROPIC_API_KEY`.

## Safety note

EaseTrading is decision-support, **not** SEBI-registered investment advice. Every live
order requires explicit human confirmation; the AI cannot place trades.
