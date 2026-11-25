"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import Header from "@/components/Header";
import Stat from "@/components/Stat";
import Watchlist from "@/components/Watchlist";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { getDashboard, reconnectBroker } from "@/lib/api";
import { inr, pct, signedInr, tone } from "@/lib/format";
import type { Dashboard, HoldingView } from "@/lib/types";

/**
 * Personal dashboard (home):
 *   broker connection · funds · portfolio summary (invested, current, total & today's P&L)
 *   · today's top movers in my holdings · my watchlist with live prices.
 * Refreshes every 15 seconds; watchlist prices stream live.
 */
export default function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [watchTokens, setWatchTokens] = useState<string[]>([]);
  const { quotes } = useLiveQuotes(watchTokens);

  useEffect(() => {
    const load = () => getDashboard().then((d) => { setData(d); setError(null); }).catch((e) => setError(e.message));
    load();
    const t = setInterval(load, 15_000);
    return () => clearInterval(t);
  }, []);

  const s = data?.summary;
  const priced = !!s && s.pricedHoldings > 0;

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="mx-auto grid w-full max-w-7xl flex-1 gap-4 p-4 lg:grid-cols-[1fr_320px]">
        <div className="flex min-w-0 flex-col gap-4">
          {/* Connection */}
          <ConnectionBanner data={data} error={error} onReconnected={() => getDashboard().then(setData).catch(() => {})} />

          {/* Portfolio summary */}
          <section>
            <div className="mb-2 flex items-baseline justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-400">My portfolio</h2>
              <Link href="/portfolio" className="text-xs text-brand-light hover:underline">View holdings →</Link>
            </div>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <Stat label="Current value" value={inr(s?.marketValue)} sub={s ? `${s.holdings} holdings` : undefined} />
              <Stat label="Invested" value={inr(s?.invested)} />
              {/* P&L needs live prices; with none (e.g. Groww free plan) show "—", never ₹0. */}
              <Stat label="Total P&L" value={priced ? signedInr(s?.totalPnl) : "—"} valueClass={priced ? tone(s?.totalPnl) : "text-slate-400"}
                    sub={priced ? pct(s?.totalPnlPct) : "needs live prices"} subClass={priced ? tone(s?.totalPnl) : "text-slate-500"} />
              <Stat label="Today's P&L" value={priced ? signedInr(s?.dayPnl) : "—"} valueClass={priced ? tone(s?.dayPnl) : "text-slate-400"}
                    sub={priced ? pct(s?.dayPnlPct) : "needs live prices"} subClass={priced ? tone(s?.dayPnl) : "text-slate-500"} />
            </div>
          </section>

          {/* Funds */}
          <section>
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Funds</h2>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <Stat label="Available cash" value={inr(data?.funds?.availableCash)} />
              <Stat label="Available for delivery" value={inr(data?.funds?.cncAvailable)} />
              <Stat label="Margin used" value={inr(data?.funds?.marginUsed)} />
              <Stat label="Charges today" value={inr(data?.funds?.brokerageAndCharges)} />
            </div>
          </section>

          {/* Movers */}
          <section className="grid gap-3 md:grid-cols-2">
            <Movers title="Top gainers today" rows={data?.topGainers ?? []} />
            <Movers title="Top losers today" rows={data?.topLosers ?? []} />
          </section>

          {data?.errors?.length ? (
            <ul className="rounded border border-down/40 bg-down/10 p-3 text-xs text-down">
              {data.errors.map((e) => <li key={e}>{e}</li>)}
            </ul>
          ) : null}
        </div>

        {/* Watchlist */}
        <aside>
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Watchlist</h2>
          <Watchlist quotes={quotes} onTokensChange={setWatchTokens} />
        </aside>
      </main>
    </div>
  );
}

function ConnectionBanner({ data, error, onReconnected }: {
  data: Dashboard | null; error: string | null; onReconnected: () => void;
}) {
  const [busy, setBusy] = useState(false);
  if (error) {
    return (
      <div className="rounded-lg border border-down/40 bg-down/10 p-3 text-sm text-down">
        Can&apos;t reach the EaseTrading api — is it running? ({error})
      </div>
    );
  }
  if (!data) return <div className="rounded-lg border border-slate-800 p-3 text-sm text-slate-400">Loading…</div>;
  const b = data.broker;
  return (
    <div className={`flex flex-wrap items-center justify-between gap-2 rounded-lg border p-3 text-sm ${
      b.connected ? "border-slate-800" : "border-amber-600/50 bg-amber-500/10"}`}>
      <div className="flex items-center gap-2">
        <span className={`h-2 w-2 rounded-full ${b.connected ? "bg-up" : "bg-amber-500"}`} />
        <span className="font-medium">{b.provider === "GROWW" ? "Groww" : "Demo broker"}</span>
        <span className="text-slate-400">{b.message}</span>
      </div>
      {!b.connected && b.provider === "GROWW" && (
        <button disabled={busy}
          onClick={() => { setBusy(true); reconnectBroker().finally(() => { setBusy(false); onReconnected(); }); }}
          className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-dark disabled:opacity-50">
          {busy ? "Connecting…" : "Reconnect"}
        </button>
      )}
      {b.tokenExpiresAt && (
        <span className="text-xs text-slate-500">
          session until {new Date(b.tokenExpiresAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata", dateStyle: "medium", timeStyle: "short" })}
        </span>
      )}
    </div>
  );
}

function Movers({ title, rows }: { title: string; rows: HoldingView[] }) {
  return (
    <div className="rounded-lg border border-slate-800">
      <h3 className="border-b border-slate-800 px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-400">{title}</h3>
      <ul>
        {rows.map((h) => (
          <li key={h.symbol} className="flex items-center justify-between px-3 py-2 text-sm">
            {h.token ? <Link href={`/stocks/${h.token}`} className="font-medium hover:text-brand-light">{h.symbol}</Link>
                     : <span className="font-medium">{h.symbol}</span>}
            <span className="tabular-nums text-slate-300">{inr(h.ltp)}</span>
            <span className={`w-20 text-right tabular-nums ${tone(h.dayChangePct)}`}>{pct(h.dayChangePct)}</span>
          </li>
        ))}
        {rows.length === 0 && <li className="px-3 py-4 text-center text-xs text-slate-500">Nothing here yet.</li>}
      </ul>
    </div>
  );
}
