// Thin client for the Spring Boot API. All HTTP details live here so components
// just call typed functions and never build URLs themselves.

import type {
  Candle, Instrument, Indicators, Fundamentals, ScreenResult, Verdict,
  Alert,
  Mode, BrokerStatus, BrokerAccount, Funds, BrokerPortfolio, Dashboard, WatchRow, StockDetail,
  MarketOverview, FnoUnderlying, OptionChain, StockNews,
} from "./types";

/**
 * Where the api lives, decided in the browser so ONE build works everywhere:
 *   1. NEXT_PUBLIC_API_BASE_URL set (e.g. on Vercel)  -> use it
 *   2. page opened on port 3000 (local dev)           -> same machine, port 8080
 *   3. anything else (Tailscale Serve, a reverse proxy) -> same address; the proxy
 *      routes /api to the api, so no CORS and no extra port.
 */
function apiBase(): string {
  const env = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (env) return env.replace(/\/$/, "");
  if (typeof window === "undefined") return "http://localhost:8080";
  const { protocol, hostname, port, host } = window.location;
  return port === "3000" ? `${protocol}//${hostname}:8080` : `${protocol}//${host}`;
}

/** WebSocket base for live prices — follows the same rules as apiBase(). */
export function wsBase(): string {
  const env = process.env.NEXT_PUBLIC_WS_BASE_URL;
  if (env) return env.replace(/\/$/, "");
  return apiBase().replace(/^http/, "ws");
}

/**
 * Throws an Error carrying the api's own message ({message: "..."}) when there is one.
 * 401 = not logged in: go to the login page (unless we're already on it).
 */
async function check(res: Response, path: string): Promise<Response> {
  if (res.ok) return res;
  if (res.status === 401 && typeof window !== "undefined" && window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
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
  // credentials: "include" sends the httpOnly login cookie with every request.
  const res = await check(await fetch(`${apiBase()}${path}`, { cache: "no-store", credentials: "include" }), path);
  return res.json() as Promise<T>;
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const res = await check(await fetch(`${apiBase()}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
    credentials: "include",
  }), path);
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
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

// ---- Alerts ----

async function del(path: string): Promise<Response> {
  return check(await fetch(`${apiBase()}${path}`, { method: "DELETE", credentials: "include" }), path);
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

/** "Reconnect": log in to the broker again now (e.g. after approving the key on Groww). */
export function reconnectBroker() {
  return postJson<BrokerStatus>(`/api/broker/reconnect`, {});
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

// ---- Login ----

export interface Me { email: string; role: string }

/** Login page setup: the Google client ID (null = Google sign-in not set up). */
export function getAuthConfig() {
  return getJson<{ googleClientId: string | null }>(`/api/auth/config`);
}

/** Who is logged in (redirects to /login if nobody). */
export function getMe() {
  return getJson<Me>(`/api/auth/me`);
}

export function register(email: string, password: string) {
  return postJson<Me>(`/api/auth/register`, { email, password });
}

export function login(email: string, password: string) {
  return postJson<Me>(`/api/auth/login`, { email, password });
}

/** Google sign-in: send Google's signed credential; the api sets the login cookie. */
export function loginWithGoogle(credential: string) {
  return postJson<Me>(`/api/auth/google`, { credential });
}

export function logout() {
  return postJson<void>(`/api/auth/logout`, {});
}

// ---- My Groww account (Settings) ----

export function getBrokerAccount() {
  return getJson<BrokerAccount>(`/api/broker/account`);
}

/** Save my Groww key + secret (stored encrypted on the server), then try to connect. */
export async function linkBroker(authMode: "approval" | "totp", apiKey: string, secret: string) {
  const res = await check(await fetch(`${apiBase()}/api/broker/account`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ authMode, apiKey, secret }),
    credentials: "include",
  }), "/api/broker/account");
  return res.json() as Promise<BrokerStatus>;
}

export async function unlinkBroker() {
  const res = await del(`/api/broker/account`);
  return res.json() as Promise<BrokerAccount>;
}

// ---- Market + F&O ----

/** Indices, Nifty 50 gainers/losers, market breadth. */
export function getMarketOverview() {
  return getJson<MarketOverview>(`/api/market/overview`);
}

export function getFnoUnderlyings() {
  return getJson<FnoUnderlying[]>(`/api/fno/underlyings`);
}

export function getFnoExpiries(underlying: string) {
  return getJson<string[]>(`/api/fno/expiries?underlying=${encodeURIComponent(underlying)}`);
}

export function getOptionChain(underlying: string, expiry: string) {
  return getJson<OptionChain>(`/api/fno/chain?underlying=${encodeURIComponent(underlying)}&expiry=${expiry}`);
}

export function getStockNews(token: string) {
  return getJson<StockNews>(`/api/news/${encodeURIComponent(token)}`);
}
