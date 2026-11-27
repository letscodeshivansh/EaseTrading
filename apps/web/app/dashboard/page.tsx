"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import InfoTip from "@/components/InfoTip";
import Watchlist from "@/components/Watchlist";
import ConnectionBanner from "@/components/dashboard/ConnectionBanner";
import IndexCards from "@/components/market/IndexCards";
import MarketStatus from "@/components/market/MarketStatus";
import MoversList from "@/components/market/MoversList";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { getDashboard, getMarketOverview, getMe } from "@/lib/api";
import { inr, pct, signedInr, tone } from "@/lib/format";
import type { Dashboard, MarketOverview } from "@/lib/types";

/**
 * Overview — everything that matters today, on one screen:
 * greeting + market status, the main indices, my portfolio, funds, what moved,
 * my watchlist, and where to go next. Refreshes every 15 s; prices stream live.
 */
export default function OverviewPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [market, setMarket] = useState<MarketOverview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState<string>("");
  const [watchTokens, setWatchTokens] = useState<string[]>([]);
  const indexTokens = market?.indices.map((i) => i.token) ?? [];
  const { quotes } = useLiveQuotes([...indexTokens, ...watchTokens]);

  useEffect(() => {
    getMe().then((m) => setName(prettyName(m.email))).catch(() => {});
    const load = () => {
      getDashboard().then((d) => { setData(d); setError(null); }).catch((e) => setError(e.message));
      getMarketOverview().then(setMarket).catch(() => {});
    };
    load();
    const t = setInterval(load, 15_000);
    return () => clearInterval(t);
  }, []);

  const s = data?.summary;
  const priced = !!s && s.pricedHoldings > 0;
  const f = data?.funds;

  return (
    <div className="space-y-5">
      {/* Greeting */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{greeting()}{name ? `, ${name}` : ""} 👋</h1>
          <p className="text-sm text-slate-400">Here&apos;s how your money and the market are doing today.</p>
        </div>
        <MarketStatus open={market ? market.marketOpen : null} />
      </div>

      <ConnectionBanner data={data} error={error} onReconnected={() => getDashboard().then(setData).catch(() => {})} />

      {/* Indices */}
      <section>
        <SectionTitle title="Market today" tip="An index tracks a basket of big companies. NIFTY 50 = India's 50 largest NSE stocks; when it's green, the market is broadly up." link={{ href: "/dashboard/live", label: "Live market →" }} />
        <IndexCards indices={market?.indices ?? []} quotes={quotes} />
      </section>

      <div className="grid gap-5 lg:grid-cols-[1fr_340px]">
        <div className="min-w-0 space-y-5">
          {/* Portfolio hero */}
          <section className="card relative overflow-hidden p-5">
            <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-brand/20 blur-3xl" />
            <div className="relative">
              <div className="flex items-center justify-between">
                <span className="section-title">My portfolio</span>
                <Link href="/portfolio" className="text-xs text-brand-light hover:underline">View holdings →</Link>
              </div>
              <div className="mt-2 text-4xl font-semibold tracking-tight tabular-nums">{inr(s?.marketValue)}</div>
              <div className="mt-1 text-xs text-slate-500">{s ? `${s.holdings} holdings · invested ${inr(s.invested, 0)}` : " "}</div>
              <div className="mt-5 grid grid-cols-2 gap-3">
                <Figure label="Today's returns" tip="How much your holdings gained or lost since yesterday's closing prices."
                  value={priced ? signedInr(s?.dayPnl) : "—"} sub={priced ? pct(s?.dayPnlPct) : "needs live prices"}
                  cls={priced ? tone(s?.dayPnl) : "text-slate-400"} />
                <Figure label="Total returns" tip="Current value minus what you paid for your holdings."
                  value={priced ? signedInr(s?.totalPnl) : "—"} sub={priced ? pct(s?.totalPnlPct) : "needs live prices"}
                  cls={priced ? tone(s?.totalPnl) : "text-slate-400"} />
              </div>
            </div>
          </section>

          {/* Funds */}
          <section>
            <SectionTitle title="Funds" tip="Money in your broker account. 'Available for delivery' is what you can use to buy shares to hold." />
            <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
              <Tile label="Available cash" value={inr(f?.availableCash)} />
              <Tile label="Available to invest" value={inr(f?.cncAvailable)} />
              <Tile label="Margin used" value={inr(f?.marginUsed)} />
            </div>
          </section>

          {/* Movers */}
          <section className="grid gap-4 md:grid-cols-2">
            <div className="card p-4">
              <SectionTitle title="Top gainers · Nifty 50" tip="Large companies that are up the most today, by %." />
              <MoversList rows={(market?.gainers ?? []).slice(0, 4)} quotes={quotes} empty="No gainers yet today." />
            </div>
            <div className="card p-4">
              <SectionTitle title="Top losers · Nifty 50" tip="Large companies that are down the most today, by %." />
              <MoversList rows={(market?.losers ?? []).slice(0, 4)} quotes={quotes} empty="No losers yet today." />
            </div>
          </section>

          {/* Explore */}
          <section className="grid gap-3 sm:grid-cols-3">
            <Explore href="/dashboard/live" title="Live market" text="Watch indices move live and see what's rising or falling." />
            <Explore href="/dashboard/fno" title="F&O · Option chain" text="See where traders are betting with open interest and PCR." />
            <Explore href="/stocks" title="Find a stock" text="Search any NSE company and see its price, chart and analysis." />
          </section>
        </div>

        {/* Watchlist */}
        <aside className="card h-fit p-4">
          <SectionTitle title="My watchlist" tip="Stocks you want to keep an eye on. Search to add, × to remove, tap one to open its page." />
          <Watchlist quotes={quotes} onTokensChange={setWatchTokens} />
        </aside>
      </div>
    </div>
  );
}

function SectionTitle({ title, tip, link }: { title: string; tip?: string; link?: { href: string; label: string } }) {
  return (
    <div className="mb-2.5 flex items-center justify-between">
      <h2 className="section-title flex items-center">{title}{tip && <InfoTip text={tip} align="left" />}</h2>
      {link && <Link href={link.href} className="text-xs text-brand-light hover:underline">{link.label}</Link>}
    </div>
  );
}

function Figure({ label, tip, value, sub, cls }: { label: string; tip: string; value: string; sub: string; cls: string }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/40 px-3 py-2.5">
      <div className="flex items-center text-[11px] font-medium uppercase tracking-wider text-slate-500">{label}<InfoTip text={tip} align="left" /></div>
      <div className={`mt-0.5 text-xl font-semibold tabular-nums ${cls}`}>{value}</div>
      <div className={`text-xs tabular-nums ${cls}`}>{sub}</div>
    </div>
  );
}

function Tile({ label, value }: { label: string; value: string }) {
  return (
    <div className="card px-4 py-3">
      <div className="text-[11px] font-medium uppercase tracking-wider text-slate-500">{label}</div>
      <div className="mt-1 text-lg font-semibold tabular-nums">{value}</div>
    </div>
  );
}

function Explore({ href, title, text }: { href: string; title: string; text: string }) {
  return (
    <Link href={href} className="card group p-4 transition hover:border-brand/50">
      <div className="text-sm font-semibold text-slate-100 group-hover:text-brand-light">{title} →</div>
      <p className="mt-1 text-xs leading-relaxed text-slate-400">{text}</p>
    </Link>
  );
}

function greeting() {
  const h = Number(new Date().toLocaleString("en-IN", { timeZone: "Asia/Kolkata", hour: "numeric", hour12: false }));
  return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
}

/** "shivansh.srivastava0999@gmail.com" -> "Shivansh" */
function prettyName(email: string) {
  const first = email.split("@")[0].split(/[._\-0-9]/).filter(Boolean)[0] ?? "";
  return first ? first[0].toUpperCase() + first.slice(1) : "";
}
