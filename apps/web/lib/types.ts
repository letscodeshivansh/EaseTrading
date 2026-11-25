// Shared TypeScript shapes that mirror the backend's JSON. Keeping these in one
// place means the whole frontend agrees on the data model.

export interface Instrument {
  token: string;
  symbol: string;
  name: string;
  exchange: string;
}

export interface Candle {
  token: string;
  interval: string;
  ts: string; // ISO timestamp
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface Tick {
  token: string;
  ltp: number; // last traded price
  ts: string;
}

// ---- Prompt 2: indicators, fundamentals, screener ----

export interface Indicators {
  bars: number;
  lastClose: number;
  trend: string;
  sma: { "50": number | null; "100": number | null; "200": number | null };
  cross_50_200: string;
  rsi14: number | null;
  macd: { line: number | null; signal: number | null; hist: number | null; cross: string };
  bollinger: { mid: number | null; upper: number | null; lower: number | null; pctB: number | null; bandwidth: number | null };
  fibonacci: {
    trend: string;
    swingHigh?: number;
    swingLow?: number;
    levels: Record<string, number>;
    extensions: Record<string, number>;
    active: string | null;
  };
  supports: { price: number; strength: number }[];
  resistances: { price: number; strength: number }[];
  volume: { latest: number | null; avg20: number | null; vsAvg: number | null };
}

export interface Fundamentals {
  token: string;
  peRatio: number | null;
  eps: number | null;
  roePct: number | null;
  profitGrowthPct: number | null;
  fiiHoldingPct: number | null;
  diiHoldingPct: number | null;
  promoterPct: number | null;
  debtToEquity: number | null;
  source: string;
}

export interface ScreenMatch {
  token: string;
  symbol: string;
  exchange: string;
  passedAll: boolean;
  score: number;
  passed: string[];
  failed: string[];
  values: Record<string, number | null>;
}

export interface ScreenResult {
  rulesUsed: { key: string; op: string; value: number; mandatory: boolean }[];
  scanned: number;
  matchCount: number;
  matches: ScreenMatch[];
  all: ScreenMatch[];
}

// ---- Prompt 3: AI analysis verdict ----

export interface Verdict {
  symbol: string;
  strategy: string;
  rating: "STRONG_BUY" | "BUY" | "NEUTRAL" | "SELL" | "STRONG_SELL";
  direction: "bullish" | "bearish" | "neutral";
  confidence: number; // 0..1
  entry: number | null;
  stopLoss: number | null;
  target: number | null;
  rrRatio: number | null;
  subScores?: { technical: number; valuation: number; quality: number; momentum: number; composite: number };
  signals: Record<string, any>;
  memo: string;        // markdown
  source: "claude" | "grounded" | "insufficient_data";
}

// ---- Portfolio risk, alerts ----

export interface RiskView { holdings: number; largestPositionPct: number; concentrationIndex: number; assessment: string }
export interface Alert {
  id: string;
  token: string;
  symbol: string;
  type: "PRICE" | "RSI";
  operator: "GT" | "LT";
  threshold: number;
  status: "ACTIVE" | "TRIGGERED" | "DISABLED";
  lastValue: number | null;
  createdAt: string;
}

// The 10 strategies (mirrors the backend personas) for the dropdown.
export const STRATEGIES: { key: string; name: string }[] = [
  { key: "BLACK_BOX", name: "Black Box (composite)" },
  { key: "CITADEL", name: "Citadel — Technical" },
  { key: "MORGAN_STANLEY", name: "Morgan Stanley — Valuation" },
  { key: "JPMORGAN", name: "JPMorgan — Earnings" },
  { key: "BRIDGEWATER", name: "Bridgewater — Risk" },
  { key: "BLACKROCK", name: "BlackRock — Portfolio" },
  { key: "HARVARD", name: "Harvard — Quality Income" },
  { key: "BAIN", name: "Bain — Competitive" },
  { key: "RENAISSANCE", name: "Renaissance — Quant" },
  { key: "MCKINSEY", name: "McKinsey — Macro" },
];

// ---- Broker (Groww / mock): dashboard, real portfolio, stocks, watchlist ----

export interface Mode { provider: "MOCK" | "GROWW" | string }

export interface BrokerStatus {
  provider: string;
  /** The user has saved Groww keys (Settings). */
  linked: boolean;
  /** Logged in to Groww right now. */
  connected: boolean;
  tokenExpiresAt: string | null;
  message: string;
}

export interface Funds {
  availableCash: number;
  marginUsed: number;
  cncAvailable: number;
  brokerageAndCharges: number;
}

/**
 * One real holding with live numbers (GET /api/portfolio).
 * priceAvailable=false: the broker sent no price (e.g. Groww free plan) — ltp, pnl and
 * day fields are null and marketValue is AT COST. Never show a P&L for these.
 */
export interface HoldingView {
  token: string | null; // null if the broker holds something not in our NSE stock list
  symbol: string;
  name: string;
  qty: number;
  avgPrice: number;
  priceAvailable: boolean;
  ltp: number | null;
  prevClose: number | null;
  invested: number;
  marketValue: number;
  pnl: number | null;
  pnlPct: number | null;
  dayChange: number | null;
  dayChangePct: number | null;
  allocationPct: number;
}

export interface PortfolioSummary {
  holdings: number;
  /** How many holdings have a live price; P&L totals cover only these. */
  pricedHoldings: number;
  invested: number;
  marketValue: number;
  totalPnl: number;
  totalPnlPct: number;
  dayPnl: number;
  dayPnlPct: number;
}

export interface BrokerPortfolio {
  source: string;
  asOf: string;
  summary: PortfolioSummary;
  holdings: HoldingView[];
  risk: RiskView;
}

export interface Dashboard {
  mode: Mode;
  broker: BrokerStatus;
  funds: Funds | null;
  summary: PortfolioSummary | null;
  topGainers: HoldingView[];
  topLosers: HoldingView[];
  errors: string[];
}

export interface WatchRow { token: string; symbol: string; name: string; exchange: string; ltp: number }

export interface Quote {
  lastPrice: number;
  dayChange: number | null;
  dayChangePct: number | null;
  open: number | null;
  high: number | null;
  low: number | null;
  close: number | null;
  volume: number | null;
  week52High: number | null;
  week52Low: number | null;
  marketCap: number | null;
  upperCircuit: number | null;
  lowerCircuit: number | null;
  lastTradeTime: string | null;
}

export interface StockDetail {
  instrument: Instrument;
  isin: string | null;
  lotSize: number | null;
  tickSize: number | null;
  quote: Quote | null;
  quoteError: string | null;
}

/** The user's linked broker account, as the settings page sees it (never the keys). */
export interface BrokerAccount {
  provider: "MOCK" | "GROWW" | string;
  linked: boolean;
  authMode: "approval" | "totp" | null;
  keyHint: string | null;
  updatedAt: string | null;
}
