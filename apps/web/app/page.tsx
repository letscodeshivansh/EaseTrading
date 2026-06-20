"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import Watchlist from "@/components/Watchlist";
import CandleChart from "@/components/CandleChart";
import IndicatorPanel from "@/components/IndicatorPanel";
import AnalysisCard from "@/components/AnalysisCard";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { getIndicators, analyze } from "@/lib/api";
import { STRATEGIES, type Instrument, type Indicators, type Verdict } from "@/lib/types";

/**
 * Phase 2 dashboard: live chart + indicator overlays + an indicator read-out.
 * The default watchlist mirrors the symbols the backend seeds.
 */
const DEFAULT_WATCHLIST: Instrument[] = [
  { token: "2885", symbol: "RELIANCE", name: "Reliance Industries", exchange: "NSE" },
  { token: "11536", symbol: "TCS", name: "Tata Consultancy Services", exchange: "NSE" },
  { token: "1333", symbol: "HDFCBANK", name: "HDFC Bank", exchange: "NSE" },
];

export default function Dashboard() {
  const { quotes } = useLiveQuotes();
  const [selected, setSelected] = useState<string>("2885");
  const [indicators, setIndicators] = useState<Indicators | null>(null);
  const [showSMA, setShowSMA] = useState(true);
  const [showFib, setShowFib] = useState(true);

  // AI analysis state.
  const [strategy, setStrategy] = useState<string>("BLACK_BOX");
  const [verdict, setVerdict] = useState<Verdict | null>(null);
  const [analyzing, setAnalyzing] = useState(false);

  const selectedInstrument =
    DEFAULT_WATCHLIST.find((i) => i.token === selected) ?? DEFAULT_WATCHLIST[0];
  const livePrice = quotes[selected];

  // Load indicators whenever the selected stock changes; clear any old verdict.
  useEffect(() => {
    setIndicators(null);
    setVerdict(null);
    getIndicators(selected).then(setIndicators).catch(() => setIndicators(null));
  }, [selected]);

  async function runAnalysis() {
    setAnalyzing(true);
    try {
      setVerdict(await analyze(selected, strategy));
    } catch {
      setVerdict(null);
    } finally {
      setAnalyzing(false);
    }
  }

  return (
    <div className="flex h-screen flex-col">
      <Header />

      <div className="flex flex-1 flex-col overflow-hidden md:flex-row">
        <aside className="max-h-44 overflow-y-auto border-b border-slate-800 md:max-h-none md:w-64 md:border-b-0 md:border-r">
          <Watchlist
            instruments={DEFAULT_WATCHLIST}
            quotes={quotes}
            selected={selected}
            onSelect={setSelected}
          />
        </aside>

        <main className="flex flex-1 flex-col overflow-y-auto p-3">
          {/* Symbol header + live price + overlay toggles. */}
          <div className="mb-2 flex flex-wrap items-baseline justify-between gap-2">
            <div>
              <h1 className="text-lg font-bold">{selectedInstrument.symbol}</h1>
              <p className="text-xs text-slate-400">
                {selectedInstrument.name} · {selectedInstrument.exchange}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <Toggle label="SMA" on={showSMA} onChange={setShowSMA} />
              <Toggle label="Fib" on={showFib} onChange={setShowFib} />
              <div className="text-right">
                <div className="tabular-nums text-2xl font-semibold">
                  {livePrice != null ? `₹${livePrice.toFixed(2)}` : "—"}
                </div>
                <div className="text-xs text-slate-500">live</div>
              </div>
            </div>
          </div>

          {/* Chart with overlays. */}
          <div className="h-[55vh] overflow-hidden rounded-lg border border-slate-800 md:h-[60vh]">
            <CandleChart
              token={selected}
              ltp={livePrice}
              showSMA={showSMA}
              showFib={showFib}
              indicators={indicators}
            />
          </div>

          {/* Indicator read-out below the chart. */}
          <div className="mt-3 rounded-lg border border-slate-800">
            <h2 className="border-b border-slate-800 px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
              Technical Indicators
            </h2>
            <IndicatorPanel data={indicators} />
          </div>

          {/* AI analysis: strategy picker + verdict card. */}
          <div className="mt-3 rounded-lg border border-slate-800">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 px-3 py-2">
              <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                AI Analysis
              </h2>
              <div className="flex items-center gap-2">
                <select
                  value={strategy}
                  onChange={(e) => setStrategy(e.target.value)}
                  className="rounded bg-slate-800 px-2 py-1 text-xs text-slate-200"
                >
                  {STRATEGIES.map((s) => (
                    <option key={s.key} value={s.key}>{s.name}</option>
                  ))}
                </select>
                <button
                  onClick={runAnalysis}
                  disabled={analyzing}
                  className="rounded bg-brand px-3 py-1 text-xs font-semibold text-white hover:bg-brand-dark disabled:opacity-50"
                >
                  {analyzing ? "Analyzing…" : "Analyze"}
                </button>
              </div>
            </div>
            <AnalysisCard verdict={verdict} loading={analyzing} />
          </div>
        </main>
      </div>
    </div>
  );
}

/** A small on/off pill used for the overlay toggles. */
function Toggle({ label, on, onChange }: { label: string; on: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      onClick={() => onChange(!on)}
      className={`rounded px-2 py-1 text-xs font-medium transition ${
        on ? "bg-brand/30 text-brand" : "bg-slate-800 text-slate-400"
      }`}
    >
      {label}
    </button>
  );
}
