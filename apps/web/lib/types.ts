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

// ---- Prompt 4: orders, portfolio, alerts ----

export interface Order {
  id: string;
  token: string;
  symbol: string;
  side: "BUY" | "SELL";
  type: "MARKET" | "LIMIT";
  qty: number;
  price: number;
  filledPrice: number;
  status: "DRAFT" | "PENDING_CONFIRM" | "SUBMITTED" | "OPEN" | "FILLED" | "REJECTED" | "CANCELLED";
  brokerOrderId: string | null;
  riskPassed: boolean;
  realizedPnl: number;
  createdAt: string;
}

export interface RiskCheck { name: string; passed: boolean; detail: string }
export interface RiskReport { passed: boolean; checks: RiskCheck[] }
export interface DraftResult { order: Order; risk: RiskReport }

export interface Holding {
  token: string; symbol: string; qty: number; avgPrice: number; ltp: number;
  invested: number; marketValue: number; pnl: number; pnlPct: number; allocationPct: number;
}
export interface PortfolioView {
  holdings: Holding[];
  invested: number; marketValue: number; totalPnl: number; totalPnlPct: number;
  risk: { holdings: number; largestPositionPct: number; concentrationIndex: number; assessment: string };
}

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
