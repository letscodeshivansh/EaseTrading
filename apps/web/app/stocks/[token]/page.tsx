"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import AnalysisCard from "@/components/AnalysisCard";
import CandleChart from "@/components/CandleChart";
import Header from "@/components/Header";
import IndicatorPanel from "@/components/IndicatorPanel";
import Stat from "@/components/Stat";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { addToWatchlist, analyze, getIndicators, getStock, getWatchlist, removeFromWatchlist } from "@/lib/api";
import { compact, inr, pct, signedInr, tone } from "@/lib/format";
import { STRATEGIES, type Indicators, type StockDetail, type Verdict } from "@/lib/types";

/**
 * One stock: live price + day change, key stats (OHLC, volume, 52-week range,
 * market cap), chart with SMA/Fibonacci overlays, indicators and the AI analysis.
 */
export default function StockPage() {
  const { token } = useParams<{ token: string }>();
  const [stock, setStock] = useState<StockDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [watched, setWatched] = useState(false);
  const [indicators, setIndicators] = useState<Indicators | null>(null);
  const [showSMA, setShowSMA] = useState(true);
  const [showFib, setShowFib] = useState(true);
  const [strategy, setStrategy] = useState("BLACK_BOX");
  const [verdict, setVerdict] = useState<Verdict | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const { quotes } = useLiveQuotes(token ? [token] : []);

  useEffect(() => {
    if (!token) return;
    setStock(null); setIndicators(null); setVerdict(null); setError(null);
    getStock(token).then(setStock).catch((e) => setError(e.message));
    getIndicators(token).then(setIndicators).catch(() => setIndicators(null));
    getWatchlist().then((w) => setWatched(w.some((r) => r.token === token))).catch(() => {});
  }, [token]);

  const q = stock?.quote;
  const live = quotes[token];
  const price = live ?? q?.lastPrice ?? null;
  // Recompute the day change against the previous close as live ticks arrive.
  // Prefer the broker's own day_change (unambiguous); fall back to the OHLC close.
  const prevClose = q && q.dayChange != null ? q.lastPrice - q.dayChange : q?.close ?? null;
  const dayChange = price != null && prevClose ? price - prevClose : q?.dayChange ?? null;
  const dayChangePct = price != null && prevClose ? ((price - prevClose) / prevClose) * 100 : q?.dayChangePct ?? null;

  async function toggleWatch() {
    const rows = watched ? await removeFromWatchlist(token) : await addToWatchlist(token);
    setWatched(rows.some((r) => r.token === token));
  }

  async function runAnalysis() {
    setAnalyzing(true);
    try { setVerdict(await analyze(token, strategy)); } catch { setVerdict(null); } finally { setAnalyzing(false); }
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-4 p-4">
        {error && <p className="rounded border border-down/40 bg-down/10 p-3 text-sm text-down">{error}</p>}

        {/* Title + price */}
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold">{stock?.instrument.symbol ?? "…"}</h1>
            <p className="text-sm text-slate-400">
              {stock?.instrument.name} · {stock?.instrument.exchange}{stock?.isin ? ` · ${stock.isin}` : ""}
            </p>
          </div>
          <div className="text-right">
            <div className="text-3xl font-semibold tabular-nums">{inr(price)}</div>
            <div className={`text-sm tabular-nums ${tone(dayChange)}`}>
              {signedInr(dayChange)} ({pct(dayChangePct)}) today
            </div>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <button onClick={toggleWatch}
            className={`rounded px-3 py-1.5 text-sm ${watched ? "bg-brand/20 text-brand-light" : "bg-slate-800 text-slate-200 hover:bg-slate-700"}`}>
            {watched ? "★ In watchlist" : "☆ Add to watchlist"}
          </button>
          <a href={`/orders?token=${token}`} className="rounded bg-slate-800 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-700">
            Trade (paper)
          </a>
        </div>

        {stock?.quoteError && (
          <p className="text-xs text-amber-400">Live quote unavailable: {stock.quoteError}</p>
        )}

        {/* Key stats */}
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8">
          <Stat label="Open" value={inr(q?.open)} />
          <Stat label="High" value={inr(q?.high)} />
          <Stat label="Low" value={inr(q?.low)} />
          <Stat label="Prev close" value={inr(prevClose)} />
          <Stat label="Volume" value={compact(q?.volume)} />
          <Stat label="52W high" value={inr(q?.week52High)} />
          <Stat label="52W low" value={inr(q?.week52Low)} />
          <Stat label="Market cap" value={q?.marketCap ? `₹${compact(q.marketCap)}` : "—"} />
        </div>

        {/* Chart */}
        <div className="flex items-center justify-end gap-2">
          <Toggle label="SMA 50/200" on={showSMA} onChange={setShowSMA} />
          <Toggle label="Fibonacci" on={showFib} onChange={setShowFib} />
        </div>
        <div className="h-[50vh] overflow-hidden rounded-lg border border-slate-800">
          {token && <CandleChart token={token} ltp={live} showSMA={showSMA} showFib={showFib} indicators={indicators} />}
        </div>

        <div className="rounded-lg border border-slate-800">
          <h2 className="border-b border-slate-800 px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
            Technical indicators
          </h2>
          <IndicatorPanel data={indicators} />
        </div>

        {/* AI analysis */}
        <div className="rounded-lg border border-slate-800">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 px-3 py-2">
            <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400">AI analysis</h2>
            <div className="flex items-center gap-2">
              <select value={strategy} onChange={(e) => setStrategy(e.target.value)}
                className="rounded bg-slate-800 px-2 py-1 text-xs text-slate-200">
                {STRATEGIES.map((s) => <option key={s.key} value={s.key}>{s.name}</option>)}
              </select>
              <button onClick={runAnalysis} disabled={analyzing}
                className="rounded bg-brand px-3 py-1 text-xs font-semibold text-white hover:bg-brand-dark disabled:opacity-50">
                {analyzing ? "Analyzing…" : "Analyze"}
              </button>
            </div>
          </div>
          <AnalysisCard verdict={verdict} loading={analyzing} />
        </div>
      </main>
    </div>
  );
}

function Toggle({ label, on, onChange }: { label: string; on: boolean; onChange: (v: boolean) => void }) {
  return (
    <button onClick={() => onChange(!on)}
      className={`rounded px-2 py-1 text-xs font-medium transition ${on ? "bg-brand/30 text-brand-light" : "bg-slate-800 text-slate-400"}`}>
      {label}
    </button>
  );
}
