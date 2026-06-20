// Thin client for the Spring Boot API. All HTTP details live here so components
// just call typed functions and never build URLs themselves.

import type {
  Candle, Instrument, Indicators, Fundamentals, ScreenResult, Verdict,
  Order, DraftResult, PortfolioView, Alert,
} from "./types";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export const WS_BASE =
  process.env.NEXT_PUBLIC_WS_BASE_URL ?? "ws://localhost:8080";

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, { cache: "no-store" });
  if (!res.ok) throw new Error(`API ${path} failed: ${res.status}`);
  return res.json() as Promise<T>;
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
  });
  if (!res.ok) throw new Error(`API ${path} failed: ${res.status}`);
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

async function del(path: string): Promise<void> {
  const res = await fetch(`${API_BASE}${path}`, { method: "DELETE" });
  if (!res.ok) throw new Error(`API ${path} failed: ${res.status}`);
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

export function getPortfolio() {
  return getJson<PortfolioView>(`/api/portfolio`);
}

export function listAlerts() {
  return getJson<Alert[]>(`/api/alerts`);
}

export function createAlert(body: {
  token: string; symbol: string; type: "PRICE" | "RSI"; operator: "GT" | "LT"; threshold: number;
}) {
  return postJson<Alert>(`/api/alerts`, body);
}

export function deleteAlert(id: string) {
  return del(`/api/alerts/${id}`);
}
