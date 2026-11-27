"use client";

import { useEffect, useState } from "react";
import CandleChart from "@/components/CandleChart";
import InfoTip from "@/components/InfoTip";
import BreadthBar from "@/components/market/BreadthBar";
import IndexCards from "@/components/market/IndexCards";
import MarketStatus from "@/components/market/MarketStatus";
import MoversList from "@/components/market/MoversList";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { getMarketOverview } from "@/lib/api";
import { pct, tone } from "@/lib/format";
import type { MarketOverview } from "@/lib/types";

const NUM = new Intl.NumberFormat("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * Live market — pick an index, watch its chart move live, and see which big
 * companies are pulling it up or down (Nifty 50 gainers/losers + breadth).
 */
export default function LiveMarketPage() {
  const [market, setMarket] = useState<MarketOverview | null>(null);
  const [selected, setSelected] = useState("NIFTY");
  const [tab, setTab] = useState<"gainers" | "losers">("gainers");
  const [showSMA, setShowSMA] = useState(true);
  const tokens = [
    ...(market?.indices.map((i) => i.token) ?? []),
    ...(market?.gainers.map((m) => m.token) ?? []),
    ...(market?.losers.map((m) => m.token) ?? []),
  ];
  const { quotes } = useLiveQuotes(tokens);

  useEffect(() => {
    const load = () => getMarketOverview().then(setMarket).catch(() => {});
    load();
    const t = setInterval(load, 10_000);
    return () => clearInterval(t);
  }, []);

  const ix = market?.indices.find((i) => i.token === selected);
  const ltp = quotes[selected] ?? ix?.ltp ?? null;
  const change = ltp != null && ix?.prevClose ? ltp - ix.prevClose : null;
  const changePct = ltp != null && ix?.prevClose ? (change! / ix.prevClose) * 100 : null;

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Live market</h1>
          <p className="text-sm text-slate-400">Tap an index to see its chart. Prices update live during market hours.</p>
        </div>
        <MarketStatus open={market ? market.marketOpen : null} />
      </div>

      {market?.message && <p className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">{market.message}</p>}

      <IndexCards indices={market?.indices ?? []} quotes={quotes} selected={selected} onSelect={setSelected} />

      <div className="grid gap-5 lg:grid-cols-[1fr_340px]">
        {/* Chart */}
        <section className="card min-w-0 p-4">
          <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
            <div>
              <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">{ix?.name ?? selected}</div>
              <div className="flex items-baseline gap-3">
                <span className="text-3xl font-semibold tabular-nums">{ltp != null ? NUM.format(ltp) : "—"}</span>
                <span className={`text-sm font-medium tabular-nums ${tone(change)}`}>
                  {change != null ? `${change >= 0 ? "+" : "−"}${NUM.format(Math.abs(change))} (${pct(changePct)})` : ""}
                </span>
              </div>
            </div>
            <button onClick={() => setShowSMA(!showSMA)}
              className={`rounded-lg px-2.5 py-1 text-xs font-medium transition ${showSMA ? "bg-brand/25 text-brand-light" : "bg-slate-800 text-slate-400"}`}>
              Moving averages {showSMA ? "on" : "off"}
            </button>
          </div>
          <div className="h-[46vh] min-h-[300px] overflow-hidden rounded-xl border border-slate-800">
            <CandleChart key={selected} token={selected} ltp={quotes[selected]} showSMA={showSMA} showFib={false} indicators={null} />
          </div>
          <details className="mt-3 rounded-xl border border-slate-800 bg-slate-950/40 p-3 text-xs text-slate-300">
            <summary className="cursor-pointer font-medium text-slate-200">How do I read this chart?</summary>
            <ul className="mt-2 list-disc space-y-1 pl-5 leading-relaxed">
              <li>Each <b>candle</b> is one day. <span className="text-up">Green</span> = closed higher than it opened; <span className="text-down">red</span> = closed lower.</li>
              <li>The thin lines above and below a candle (wicks) show that day&apos;s highest and lowest point.</li>
              <li>The <span className="text-sky-400">blue</span> line is the 50-day average and the <span className="text-violet-400">purple</span> one the 200-day average. Price above both usually means an uptrend.</li>
              <li>The last candle moves live while the market is open.</li>
            </ul>
          </details>
        </section>

        {/* Breadth + movers */}
        <aside className="space-y-4">
          <div className="card p-4">
            <BreadthBar advances={market?.breadth.advances ?? 0} declines={market?.breadth.declines ?? 0} unchanged={market?.breadth.unchanged ?? 0} />
          </div>
          <div className="card p-4">
            <div className="mb-2 flex items-center justify-between">
              <h2 className="section-title flex items-center">Nifty 50 movers
                <InfoTip align="left" text="The biggest gainers and losers today among India's 50 largest companies." />
              </h2>
            </div>
            <div className="mb-2 inline-flex w-full rounded-lg border border-slate-800 bg-slate-950/60 p-0.5">
              {(["gainers", "losers"] as const).map((t) => (
                <button key={t} onClick={() => setTab(t)}
                  className={`flex-1 rounded-md py-1 text-xs font-semibold transition ${
                    tab === t ? (t === "gainers" ? "bg-up/15 text-up" : "bg-down/15 text-down") : "text-slate-400"}`}>
                  {t === "gainers" ? "▲ Gainers" : "▼ Losers"}
                </button>
              ))}
            </div>
            <MoversList rows={tab === "gainers" ? market?.gainers ?? [] : market?.losers ?? []} quotes={quotes}
              empty={tab === "gainers" ? "No gainers yet today." : "No losers yet today."} />
            {market && <p className="mt-2 text-[11px] text-slate-500">From {market.basketSize} Nifty 50 stocks we have prices for.</p>}
          </div>
        </aside>
      </div>
    </div>
  );
}
