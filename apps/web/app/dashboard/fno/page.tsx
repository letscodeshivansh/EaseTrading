"use client";

import { useEffect, useState } from "react";
import InfoTip from "@/components/InfoTip";
import OiChart from "@/components/fno/OiChart";
import OptionChainTable from "@/components/fno/OptionChainTable";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { getFnoExpiries, getFnoUnderlyings, getOptionChain } from "@/lib/api";
import { compact } from "@/lib/format";
import type { FnoUnderlying, OptionChain } from "@/lib/types";

const NUM = new Intl.NumberFormat("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const DATE = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", timeZone: "Asia/Kolkata" });

/**
 * F&O — the option chain for an index, explained for a new trader:
 * pick an index and an expiry, read the summary cards (PCR, max pain, support,
 * resistance), see open interest as a chart, then the full chain.
 */
export default function FnoPage() {
  const [underlyings, setUnderlyings] = useState<FnoUnderlying[]>([]);
  const [underlying, setUnderlying] = useState("NIFTY");
  const [expiries, setExpiries] = useState<string[]>([]);
  const [expiry, setExpiry] = useState<string | null>(null);
  const [chain, setChain] = useState<OptionChain | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showGreeks, setShowGreeks] = useState(false);
  const { quotes } = useLiveQuotes([underlying]);

  useEffect(() => { getFnoUnderlyings().then(setUnderlyings).catch((e) => setError(e.message)); }, []);

  useEffect(() => {
    setExpiries([]); setExpiry(null); setChain(null);
    getFnoExpiries(underlying)
      .then((list) => { setExpiries(list); setExpiry(list[0] ?? null); setError(list.length ? null : "No expiries found for this index."); })
      .catch((e) => setError(e.message));
  }, [underlying]);

  useEffect(() => {
    if (!expiry) return;
    const load = () => getOptionChain(underlying, expiry).then((c) => { setChain(c); setError(null); }).catch((e) => setError(e.message));
    load();
    const t = setInterval(load, 10_000);
    return () => clearInterval(t);
  }, [underlying, expiry]);

  const spot = quotes[underlying] ?? chain?.spot ?? null;
  const c = chain && chain.underlying === underlying && chain.expiry === expiry ? chain : null;
  // ATM follows the live index price, not the 10-second chain snapshot.
  const atm = c && spot != null && c.strikes.length
    ? c.strikes.reduce((best, s) => Math.abs(s.strike - spot) < Math.abs(best - spot) ? s.strike : best, c.strikes[0].strike)
    : c?.atmStrike ?? 0;

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">F&amp;O · Option chain</h1>
          <p className="text-sm text-slate-400">See where options traders expect the index to stay — the big bets show up as open interest.</p>
        </div>
        {spot != null && (
          <div className="text-right">
            <div className="text-[11px] uppercase tracking-wider text-slate-500">{c?.name ?? underlying} now</div>
            <div className="text-2xl font-semibold tabular-nums">{NUM.format(spot)}</div>
          </div>
        )}
      </div>

      {/* Pickers */}
      <div className="card flex flex-wrap items-center gap-x-6 gap-y-3 p-3">
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="mr-1 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Index</span>
          {underlyings.map((u) => (
            <Chip key={u.token} active={u.token === underlying} onClick={() => setUnderlying(u.token)}>{u.symbol}</Chip>
          ))}
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="mr-1 flex items-center text-[11px] font-semibold uppercase tracking-wider text-slate-500">
            Expiry<InfoTip align="left" text="The date the option contract ends. Nearer expiries move faster and lose value faster." />
          </span>
          {expiries.map((e) => (
            <Chip key={e} active={e === expiry} onClick={() => setExpiry(e)}>{DATE.format(new Date(`${e}T00:00:00+05:30`))}</Chip>
          ))}
        </div>
      </div>

      {error && <p className="rounded-xl border border-down/40 bg-down/10 px-3 py-2 text-sm text-down">{error}</p>}

      {/* Summary */}
      <section className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        <Stat label="Put-call ratio" tip="Total put open interest ÷ total call open interest. Above 1 = more puts written, which traders often read as support below the market."
          value={c?.pcr != null ? c.pcr.toFixed(2) : "—"} sub={c?.reading ?? ""}
          cls={c?.pcr == null ? "" : c.pcr >= 1.3 ? "text-up" : c.pcr <= 0.7 ? "text-down" : ""} />
        <Stat label="Max pain" tip="The expiry price at which option buyers, in total, would lose the most. Prices sometimes drift towards it near expiry."
          value={c?.maxPain != null ? String(c.maxPain) : "—"} sub={c?.maxPain != null && spot ? distance(c.maxPain, spot) : ""} />
        <Stat label="Support" tip="The strike with the most put open interest. Many traders are betting the index stays above it."
          value={c?.support != null ? String(c.support) : "—"} sub={c?.support != null && spot ? distance(c.support, spot) : ""} cls="text-up" />
        <Stat label="Resistance" tip="The strike with the most call open interest. Many traders are betting the index stays below it."
          value={c?.resistance != null ? String(c.resistance) : "—"} sub={c?.resistance != null && spot ? distance(c.resistance, spot) : ""} cls="text-down" />
        <Stat label="ATM strike" tip="'At the money' — the strike closest to the index price right now."
          value={c ? String(atm) : "—"} sub={c ? `Lot size ${c.lotSize}` : ""} />
        <Stat label="Days to expiry" tip="Calendar days until this contract ends. Option prices shrink as this goes to zero (time decay)."
          value={c ? String(c.daysToExpiry) : "—"} sub={c ? `Calls ${compact(c.totalCallOi)} · Puts ${compact(c.totalPutOi)}` : ""} />
      </section>

      {/* OI chart */}
      <section className="card p-4">
        <h2 className="section-title mb-3 flex items-center">Open interest by strike
          <InfoTip align="left" text="Open interest = how many contracts are still open at each strike. The tallest bars show where the biggest bets are." />
        </h2>
        {c && spot != null ? <OiChart strikes={c.strikes} spot={spot} atm={atm} /> : <Skeleton h="h-64" />}
      </section>

      {/* Chain */}
      <section className="card p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="section-title flex items-center">Option chain
            <InfoTip align="left" text="Each row is a strike price. Left side = call options, right side = put options. Shaded cells are 'in the money'." />
          </h2>
          <button onClick={() => setShowGreeks(!showGreeks)}
            className={`rounded-lg px-2.5 py-1 text-xs font-medium transition ${showGreeks ? "bg-brand/25 text-brand-light" : "bg-slate-800 text-slate-400"}`}>
            Greeks (IV, Delta) {showGreeks ? "on" : "off"}
          </button>
        </div>
        {c && spot != null ? <OptionChainTable strikes={c.strikes} spot={spot} atm={atm} showGreeks={showGreeks} /> : <Skeleton h="h-96" />}
      </section>

      {/* Learn + risk */}
      <section className="grid gap-4 md:grid-cols-2">
        <details className="card p-4 text-sm text-slate-300" open>
          <summary className="cursor-pointer font-semibold text-slate-100">New to F&amp;O? Start here</summary>
          <ul className="mt-2 list-disc space-y-1.5 pl-5 text-xs leading-relaxed">
            <li>A <b style={{ color: "#3987e5" }}>call</b> gains value when the index goes <b>up</b> past its strike. A <b style={{ color: "#d95926" }}>put</b> gains when the index goes <b>down</b> below its strike.</li>
            <li>You trade in <b>lots</b> — one NIFTY lot is {underlyings.find((u) => u.token === "NIFTY")?.lotSize ?? 75} units, so a ₹10 move on one lot is ₹{(underlyings.find((u) => u.token === "NIFTY")?.lotSize ?? 75) * 10}.</li>
            <li>Options lose value every day as expiry nears (<b>time decay</b>). A buyer needs the move to happen soon.</li>
            <li>The summary cards are a <i>reading</i> of other traders&apos; positions, not a prediction.</li>
          </ul>
        </details>
        <div className="card border-amber-600/40 bg-amber-500/[0.07] p-4 text-xs leading-relaxed text-amber-100">
          <div className="mb-1 text-sm font-semibold text-amber-200">⚠ F&amp;O is high risk</div>
          A SEBI study found that about 93% of individual F&amp;O traders lost money between FY22 and FY24. Losses can be larger than what you
          put in when you sell options. Practise with small sizes, always use a stop-loss, and never trade money you need.
          {c && !quotes[underlying] && <div className="mt-2 text-amber-300/80">Showing demo / delayed data when the live feed isn&apos;t available.</div>}
        </div>
      </section>
    </div>
  );
}

function distance(level: number, spot: number) {
  const d = ((level - spot) / spot) * 100;
  if (Math.abs(d) < 0.05) return "at the index";
  return `${Math.abs(d).toFixed(1)}% ${d > 0 ? "above" : "below"} now`;
}

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button onClick={onClick}
      className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
        active ? "bg-brand text-white" : "bg-slate-800/80 text-slate-300 hover:bg-slate-700"}`}>
      {children}
    </button>
  );
}

function Stat({ label, tip, value, sub, cls = "" }: { label: string; tip: string; value: string; sub: string; cls?: string }) {
  return (
    <div className="card px-3.5 py-3">
      <div className="flex items-center text-[11px] font-medium uppercase tracking-wider text-slate-500">{label}<InfoTip text={tip} align="left" /></div>
      <div className={`mt-1 text-xl font-semibold tabular-nums ${cls || "text-slate-100"}`}>{value}</div>
      <div className="truncate text-[11px] text-slate-400" title={sub}>{sub || " "}</div>
    </div>
  );
}

function Skeleton({ h }: { h: string }) {
  return <div className={`${h} animate-pulse rounded-xl bg-slate-800/40`} />;
}
