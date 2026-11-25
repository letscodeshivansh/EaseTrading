"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import Stat from "@/components/Stat";
import AllocationBar from "@/components/portfolio/AllocationBar";
import HoldingsList from "@/components/portfolio/HoldingsList";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { getPortfolio } from "@/lib/api";
import { inr, pct, signedInr, tone } from "@/lib/format";
import type { BrokerPortfolio, HoldingView, RiskView } from "@/lib/types";

/**
 * Portfolio: real holdings from the broker (Groww), or demo holdings in mock mode.
 * Live prices stream in over the WebSocket; totals refresh every 15 s.
 */
export default function PortfolioPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="mx-auto w-full max-w-7xl flex-1 space-y-5 px-4 py-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Portfolio</h1>
            <p className="text-sm text-slate-400">Your holdings, returns and how your money is spread.</p>
          </div>
        </div>
        <RealPortfolio />
      </main>
    </div>
  );
}

// ---------------------------------------------------------------- real portfolio

function RealPortfolio() {
  const [data, setData] = useState<BrokerPortfolio | null>(null);
  const [error, setError] = useState<string | null>(null);
  const tokens = (data?.holdings ?? []).filter((h) => h.priceAvailable && h.token).map((h) => h.token as string);
  const { quotes } = useLiveQuotes(tokens);

  useEffect(() => {
    const load = () => getPortfolio().then((d) => { setData(d); setError(null); }).catch((e) => setError(e.message));
    load();
    const t = setInterval(load, 15_000);
    return () => clearInterval(t);
  }, []);

  if (error) return <Notice tone="danger">{error}</Notice>;
  if (!data) return <Skeleton />;

  // Overlay live ticks on rows that have a price.
  const rows: HoldingView[] = data.holdings.map((h) => {
    const live = h.token ? quotes[h.token] : undefined;
    if (!h.priceAvailable || live == null) return h;
    const marketValue = live * h.qty;
    const pnl = marketValue - h.invested;
    return {
      ...h, ltp: live, marketValue, pnl,
      pnlPct: h.invested ? (pnl / h.invested) * 100 : 0,
      dayChangePct: h.prevClose ? ((live - h.prevClose) / h.prevClose) * 100 : h.dayChangePct,
    };
  });
  const s = data.summary;
  const anyPrice = s.pricedHoldings > 0;
  const allPriced = s.pricedHoldings === s.holdings;
  const value = rows.reduce((a, h) => a + h.marketValue, 0);

  return (
    <div className="space-y-5">
      {!allPriced && s.holdings > 0 && (
        <Notice tone="warn">
          Live prices are unavailable for {s.holdings - s.pricedHoldings} of {s.holdings} holdings
          {data.source === "GROWW" ? " — Groww's free API plan doesn't include market data" : ""}.
          Those are valued at cost and left out of returns.
        </Notice>
      )}

      <div className="grid gap-5 lg:grid-cols-5">
        {/* Hero: value + returns */}
        <section className="card relative overflow-hidden p-5 lg:col-span-3">
          <div className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-brand/20 blur-3xl" />
          <div className="relative">
            <div className="flex items-center justify-between">
              <span className="section-title">Current value</span>
              <span className="text-xs text-slate-500">
                {data.source === "GROWW" ? "Groww" : "Demo"} · {new Date(data.asOf).toLocaleTimeString("en-IN", { timeZone: "Asia/Kolkata", hour: "2-digit", minute: "2-digit" })}
              </span>
            </div>
            <div className="mt-2 text-4xl font-semibold tracking-tight tabular-nums sm:text-5xl">{inr(value)}</div>
            {!allPriced && <div className="mt-1 text-xs text-amber-300/80">includes holdings at cost</div>}

            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
              <HeroFigure label="Invested" value={inr(s.invested)} />
              <HeroFigure label="Total returns"
                value={anyPrice ? signedInr(s.totalPnl) : "—"}
                sub={anyPrice ? pct(s.totalPnlPct) : "needs live prices"}
                tone={anyPrice ? tone(s.totalPnl) : "text-slate-400"} />
              <HeroFigure label="Today's returns"
                value={anyPrice ? signedInr(s.dayPnl) : "—"}
                sub={anyPrice ? pct(s.dayPnlPct) : "needs live prices"}
                tone={anyPrice ? tone(s.dayPnl) : "text-slate-400"} />
            </div>
          </div>
        </section>

        {/* Allocation */}
        <section className="card p-5 lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="section-title">Allocation</h2>
            <span className="text-xs text-slate-500">{s.holdings} holdings</span>
          </div>
          <AllocationBar items={rows.map((h) => ({ label: h.symbol, value: h.marketValue, pct: value ? (h.marketValue / value) * 100 : 0 }))} />
        </section>
      </div>

      <HoldingsList rows={rows} />

      {rows.length > 0 && <Risk risk={data.risk} />}
    </div>
  );
}

function HeroFigure({ label, value, sub, tone: t = "text-slate-50" }: { label: string; value: string; sub?: string; tone?: string }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2.5">
      <div className="text-[11px] font-medium uppercase tracking-wider text-slate-500">{label}</div>
      <div className={`mt-0.5 text-lg font-semibold tabular-nums ${t}`}>{value}</div>
      {sub && <div className={`text-xs tabular-nums ${t === "text-slate-50" ? "text-slate-400" : t}`}>{sub}</div>}
    </div>
  );
}

// ---------------------------------------------------------------- risk

/** Concentration risk: largest holding vs a 25% comfort line, plus the HHI index. */
function Risk({ risk }: { risk: RiskView }) {
  const level = risk.largestPositionPct > 40 || risk.concentrationIndex > 0.5 ? "high"
    : risk.largestPositionPct > 25 || risk.concentrationIndex > 0.3 ? "moderate" : "low";
  const chip = {
    high: "border-down/30 bg-down/10 text-down",
    moderate: "border-amber-500/30 bg-amber-500/10 text-amber-300",
    low: "border-up/30 bg-up/10 text-up",
  }[level];
  return (
    <section className="card p-5">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h2 className="section-title">Risk view</h2>
        <span className={`rounded-full border px-2.5 py-0.5 text-xs font-medium ${chip}`}>
          {level === "high" ? "High" : level === "moderate" ? "Moderate" : "Low"} concentration
        </span>
      </div>
      <div className="grid gap-5 sm:grid-cols-3">
        <div className="sm:col-span-2">
          <div className="mb-1.5 flex justify-between text-xs text-slate-400">
            <span>Largest single holding</span>
            <span className="font-medium tabular-nums text-slate-100">{risk.largestPositionPct.toFixed(1)}%</span>
          </div>
          <div className="relative h-2.5 overflow-hidden rounded-full bg-slate-800">
            <div className="h-full rounded-full bg-brand-light/80" style={{ width: `${Math.min(100, risk.largestPositionPct)}%` }} />
            {/* 25% comfort line */}
            <div className="absolute inset-y-0 w-px bg-slate-300/60" style={{ left: "25%" }} />
          </div>
          <div className="mt-1 text-[11px] text-slate-500">Line marks 25% — above it, one stock moves your whole portfolio.</div>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-1">
          <MiniFigure label="Holdings" value={String(risk.holdings)} />
          <MiniFigure label="Concentration (HHI)" value={risk.concentrationIndex.toFixed(2)} />
        </div>
      </div>
      <p className="mt-4 text-sm text-slate-300">{risk.assessment}</p>
    </section>
  );
}

function MiniFigure({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
      <div className="text-[11px] uppercase tracking-wider text-slate-500">{label}</div>
      <div className="mt-0.5 font-semibold tabular-nums">{value}</div>
    </div>
  );
}

// ---------------------------------------------------------------- bits

function Notice({ tone: t, children }: { tone: "warn" | "danger"; children: React.ReactNode }) {
  const styles = {
    warn: "border-amber-500/30 bg-amber-500/10 text-amber-200",
    danger: "border-down/30 bg-down/10 text-down",
  }[t];
  return <div className={`rounded-xl border px-4 py-3 text-sm ${styles}`}>{children}</div>;
}

function Skeleton() {
  return (
    <div className="grid animate-pulse gap-5 lg:grid-cols-5">
      <div className="card h-52 lg:col-span-3" />
      <div className="card h-52 lg:col-span-2" />
      <div className="card h-64 lg:col-span-5" />
    </div>
  );
}
