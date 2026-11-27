"use client";

import Sparkline from "@/components/market/Sparkline";
import { pct, tone } from "@/lib/format";
import type { IndexCard } from "@/lib/types";

const NUM = new Intl.NumberFormat("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * One card per index (NIFTY 50, BANK NIFTY…): live value, day change and a 30-day
 * trend line. Live ticks (quotes) override the snapshot. Optional selection for the
 * Live market page.
 */
export default function IndexCards({ indices, quotes, selected, onSelect }: {
  indices: IndexCard[];
  quotes: Record<string, number>;
  selected?: string;
  onSelect?: (token: string) => void;
}) {
  if (indices.length === 0) {
    return <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">{[0, 1, 2, 3].map((i) => <div key={i} className="card h-28 animate-pulse" />)}</div>;
  }
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      {indices.map((ix) => {
        const ltp = quotes[ix.token] ?? ix.ltp;
        const change = ltp != null && ix.prevClose ? ltp - ix.prevClose : ix.change;
        const changePct = ltp != null && ix.prevClose ? ((ltp - ix.prevClose) / ix.prevClose) * 100 : ix.changePct;
        const isSel = selected === ix.token;
        const body = (
          <>
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <div className="truncate text-xs font-semibold uppercase tracking-wider text-slate-400">{ix.name}</div>
                <div className="mt-1 text-xl font-semibold tabular-nums text-slate-50">{ltp != null ? NUM.format(ltp) : "—"}</div>
              </div>
              <span className={`shrink-0 rounded-md px-1.5 py-0.5 text-xs font-semibold tabular-nums ${
                changePct == null ? "bg-slate-800 text-slate-400" : changePct >= 0 ? "bg-up/15 text-up" : "bg-down/15 text-down"}`}>
                {pct(changePct)}
              </span>
            </div>
            <div className={`text-xs tabular-nums ${tone(change)}`}>
              {change == null ? "price unavailable" : `${change >= 0 ? "+" : "−"}${NUM.format(Math.abs(change))} today`}
            </div>
            <div className="mt-2"><Sparkline values={ix.spark} label={`${ix.name}, last ${ix.spark.length} days`} /></div>
          </>
        );
        return onSelect ? (
          <button key={ix.token} onClick={() => onSelect(ix.token)}
            className={`card p-3 text-left transition hover:border-slate-600 ${isSel ? "ring-2 ring-brand" : ""}`}>
            {body}
          </button>
        ) : (
          <div key={ix.token} className="card p-3">{body}</div>
        );
      })}
    </div>
  );
}
