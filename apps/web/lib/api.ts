// Thin client for the Spring Boot API. All HTTP details live here so components
// just call typed functions and never build URLs themselves.

import type {
  Candle, Instrument, Indicators, Fundamentals, ScreenResult, Verdict,
  Order, DraftResult, PaperPortfolio, Alert,
  Mode, BrokerStatus, Funds, BrokerPortfolio, Dashboard, WatchRow, StockDetail,
} from "./types";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export const WS_BASE =
  process.env.NEXT_PUBLIC_WS_BASE_URL ?? "ws://localhost:8080";

/** Throws an Error carrying the api's own message ({message: "..."}) when there is one. */
async function check(res: Response, path: string): Promise<Response> {
  if (res.ok) return res;
  let message = `API ${path} failed: ${res.status}`;
  try {
    const body = await res.json();
    if (body?.message) message = body.message;
  } catch {
    /* not JSON */
  }
  throw new Error(message);
}

async function getJson<T>(path: string): Promise<T> {
  const res = await check(await fetch(`${API_BASE}${path}`, { cache: "no-store" }), path);
  return res.json() as Promise<T>;
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const res = await check(await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
  }), path);
  return res.json() as Promise<T>;
}

/** Historical candles for the chart. */
export function getCandles(token: string, interval = "1d", days = 180) {
  return getJson<Candle[]>(`/api/candles/${token}?interval=${interval}&days=${days}`);
}

/** Symbol search for the watchlist add box. */
export function searchInstruments(q: string) {
  return getJson<Instrument[]>(`/api/instruments/search?q=${encodeURIComponent(q)}`);
}

/** Technical indicators (RSI, MACD, Bollinger, Fibonacci, S/R) for one instrument. */
export function getIndicators(token: string, interval = "1d", days = 250) {
  return getJson<Indicators>(`/api/indicators/${token}?interval=${interval}&days=${days}`);
}

/** Fundamentals (P/E, ROE, EPS, growth, FII/DII) for one instrument. */
export function getFundamentals(token: string) {
  return getJson<Fundamentals>(`/api/fundamentals/${token}`);
}

/** Run the screener. Pass custom rules, or omit to use the defaults. */
export function runScreener(rules?: unknown) {
  return postJson<ScreenResult>(`/api/screener/run`, { rules: rules ?? null });
}

/** Request an AI verdict for one instrument under a chosen strategy. */
export function analyze(token: string, strategy: string) {
  return postJson<Verdict>(`/api/analysis/${token}`, { strategy });
}

// ---- Prompt 4: orders, portfolio, alerts ----

async function del(path: string): Promise<Response> {
  return check(await fetch(`${API_BASE}${path}`, { method: "DELETE" }), path);
}

/** Step 1: create a DRAFT order and get back the risk report (nothing is executed). */
export function draftOrder(body: {
  token: string; side: "BUY" | "SELL"; type: "MARKET" | "LIMIT"; qty: number; price: number;
}) {
  return postJson<DraftResult>(`/api/orders`, body);
}

/** Step 2: the explicit human confirmation that actually executes the order. */
export function confirmOrder(id: string) {
  return postJson<Order>(`/api/orders/${id}/confirm`, {});
}

export function cancelOrder(id: string) {
  return postJson<Order>(`/api/orders/${id}/cancel`, {});
}

export function listOrders() {
  return getJson<Order[]>(`/api/orders`);
}

/** Holdings built from paper trades placed in this app. */
export function getPaperPortfolio() {
  return getJson<PaperPortfolio>(`/api/portfolio/paper`);
}

export function listAlerts() {
  return getJson<Alert[]>(`/api/alerts`);
}

export function createAlert(body: {
  token: string; symbol: string; type: "PRICE" | "RSI"; operator: "GT" | "LT"; threshold: number;
}) {
  return postJson<Alert>(`/api/alerts`, body);
}

export async function deleteAlert(id: string): Promise<void> {
  await del(`/api/alerts/${id}`);
}

// ---- Broker: dashboard, real portfolio, stocks, watchlist ----

export function getMode() {
  return getJson<Mode>(`/api/dashboard/mode`);
}

/** Everything the home screen needs in one call. */
export function getDashboard() {
  return getJson<Dashboard>(`/api/dashboard`);
}

export function getBrokerStatus() {
  return getJson<BrokerStatus>(`/api/broker/status`);
}

export function getFunds() {
  return getJson<Funds>(`/api/broker/funds`);
}

/** My real holdings (Groww) or demo holdings (mock), with live P&L. */
export function getPortfolio() {
  return getJson<BrokerPortfolio>(`/api/portfolio`);
}

/** One stock + its live quote. */
export function getStock(token: string) {
  return getJson<StockDetail>(`/api/instruments/${token}`);
}

export function getWatchlist() {
  return getJson<WatchRow[]>(`/api/watchlist`);
}

export function addToWatchlist(token: string) {
  return postJson<WatchRow[]>(`/api/watchlist`, { token });
}

export async function removeFromWatchlist(token: string) {
  const res = await del(`/api/watchlist/${token}`);
  return res.json() as Promise<WatchRow[]>;
}
