# EaseTrading

AI-assisted stock analysis & trading platform for the Indian markets (NSE & BSE).

Built across four phases — **all four are now complete.**

## Features

- **Broker: Groww** — your real holdings, funds and live prices via the Groww Trade
  API (read-only for now), streamed to the browser over a WebSocket. A built-in
  **mock broker** lets everything run without credentials during development.
- **Personal dashboard** — portfolio value, total and today's P&L, funds, top movers,
  and a watchlist with live prices. Search all NSE stocks; each has its own page.
- **Fundamentals & screening** — P/E, ROE, EPS, profit growth (Yahoo) plus FII/DII &
  promoter holding (NSE/BSE), with an editable rule screener (P/E < 30, ROE > 25%,
  EPS > 0, FII/DII > 5%, profit growth > 0).
- **Technical analysis** — SMA/EMA, RSI, MACD, Bollinger, **Fibonacci**, support /
  resistance and volume, with chart overlays.
- **AI analysis** — 10 strategies (9 investor personas + Black Box) producing grounded
  verdicts (rating, entry/stop/target, memo). Powered by **Claude on your Pro plan**
  via the Agent SDK + MCP; falls back to a deterministic grounded analyst offline.
- **Trading** — order management with a **risk engine** and a **two-step human
  confirmation** flow. Paper-trading by default; live orders arrive in phase G5.
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
infra/        docker-compose (optional): api, analysis, web — no database server needed
docs/         PROJECT_FLOW.md (living flow report), GROWW_INTEGRATION_PLAN.md
```

## Quick start

No database server or Docker needed — the api keeps its data in one SQLite file
(`apps/api/easetrading.db`, created on first run).

```bash
cp .env.example .env                 # defaults are safe: mock broker + paper trading

# 1) api  (Java 21 + Maven)
cd apps/api && set -a && source ../../.env && set +a && mvn spring-boot:run

# 2) web  (in another terminal)
cd apps/web && npm install && npm run build && npm start
# open http://localhost:3000
```

Optional: `cd apps/analysis && pip install -r requirements.txt && uvicorn main:app --port 8000`
for indicators, the screener and AI analysis. Or run everything in Docker:
`cd infra && docker compose --env-file ../.env up --build`.

**What's stored where:** the SQLite file holds only what the app owns — your watchlist,
alerts, paper orders/positions, analysis history and audit log. Holdings, funds, prices
and the stock list always come live from the broker (cached briefly in memory).

## Safety modes (all default to safe)

| Setting | Default | Effect |
|---|---|---|
| `BROKER_PROVIDER` | `mock` | demo data; set `groww` for your real Groww account (read-only) |
| `FUNDAMENTALS_MODE` | `mock` | fake fundamentals; set `auto` for Yahoo + NSE/BSE |
| `TRADING_MODE` | `paper` | simulated orders; `live` is blocked until phase G5 |
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

1. Put your Groww API key + secret in `.env` (single-quoted), set `BROKER_PROVIDER=groww`,
   and approve the key for the day on Groww's API-keys page. Keep the app local-only.
2. Set `FUNDAMENTALS_MODE=auto` for real fundamentals.
3. Real trading arrives in phase G5 (see `docs/GROWW_INTEGRATION_PLAN.md`).
4. To use Claude, install the Agent SDK and authenticate with your Pro plan
   (`claude login`); do **not** set `ANTHROPIC_API_KEY`.

## Safety note

EaseTrading is decision-support, **not** SEBI-registered investment advice. Every live
order requires explicit human confirmation; the AI cannot place trades.
