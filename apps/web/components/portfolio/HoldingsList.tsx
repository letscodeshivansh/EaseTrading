"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { inr, pct, signedInr, tone } from "@/lib/format";
import type { HoldingView } from "@/lib/types";

type SortKey = "value" | "pnl" | "pnlPct" | "day" | "name";

const SORTS: { key: SortKey; label: string }[] = [
  { key: "value", label: "Current value" },
  { key: "pnl", label: "Returns (₹)" },
  { key: "pnlPct", label: "Returns (%)" },
  { key: "day", label: "Today's change" },
  { key: "name", label: "Name A–Z" },
];

/**
 * Holdings: a table on wide screens, stacked cards on phones. Sortable.
 * Rows without a live price show "Price unavailable" instead of a fake P&L.
 */
export default function HoldingsList({ rows }: { rows: HoldingView[] }) {
  const [sort, setSort] = useState<SortKey>("value");

  const sorted = useMemo(() => {
    const val = (h: HoldingView): number => {
      switch (sort) {
        case "pnl": return h.pnl ?? -Infinity;
        case "pnlPct": return h.pnlPct ?? -Infinity;
        case "day": return h.dayChangePct ?? -Infinity;
        default: return h.marketValue;
      }
    };
    return [...rows].sort((a, b) => (sort === "name" ? a.symbol.localeCompare(b.symbol) : val(b) - val(a)));
  }, [rows, sort]);

  return (
    <section className="card overflow-hidden">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 px-4 py-3">
        <h2 className="section-title">Holdings <span className="ml-1 text-slate-500">({rows.length})</span></h2>
        <label className="flex items-center gap-2 text-xs text-slate-400">
          Sort by
          <select value={sort} onChange={(e) => setSort(e.target.value as SortKey)}
            className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-200 outline-none focus:border-brand">
            {SORTS.map((s) => <option key={s.key} value={s.key}>{s.label}</option>)}
          </select>
        </label>
      </div>

      {rows.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-slate-500">No holdings in this account yet.</p>
      ) : (
        <>
          {/* Desktop / tablet table */}
          <div className="hidden overflow-x-auto md:block">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-wider text-slate-500">
                <tr className="border-b border-slate-800">
                  <th className="px-4 py-2.5 font-medium">Stock</th>
                  <th className="px-4 py-2.5 text-right font-medium">Qty · Avg</th>
                  <th className="px-4 py-2.5 text-right font-medium">LTP</th>
                  <th className="px-4 py-2.5 text-right font-medium">Current value</th>
                  <th className="px-4 py-2.5 text-right font-medium">Returns</th>
                  <th className="w-36 px-4 py-2.5 font-medium">Allocation</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((h) => (
                  <tr key={h.symbol} className="border-b border-slate-800/60 transition last:border-0 hover:bg-slate-800/30">
                    <td className="px-4 py-3"><Name h={h} /></td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      <div className="text-slate-100">{h.qty}</div>
                      <div className="text-xs text-slate-500">@ {inr(h.avgPrice)}</div>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {h.priceAvailable ? (
                        <>
                          <div className="text-slate-100">{inr(h.ltp)}</div>
                          <div className={`text-xs ${tone(h.dayChangePct)}`}>{pct(h.dayChangePct)}</div>
                        </>
                      ) : <Unavailable />}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      <div className="text-slate-100">{inr(h.marketValue)}</div>
                      <div className="text-xs text-slate-500">
                        {h.priceAvailable ? `Invested ${inr(h.invested, 0)}` : "at cost"}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {h.priceAvailable ? (
                        <>
                          <div className={`font-medium ${tone(h.pnl)}`}>{signedInr(h.pnl)}</div>
                          <div className={`text-xs ${tone(h.pnl)}`}>{pct(h.pnlPct)}</div>
                        </>
                      ) : <span className="text-slate-500">—</span>}
                    </td>
                    <td className="px-4 py-3"><AllocMeter pctValue={h.allocationPct} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Phone cards */}
          <ul className="divide-y divide-slate-800/70 md:hidden">
            {sorted.map((h) => (
              <li key={h.symbol} className="px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                  <Name h={h} />
                  <div className="text-right tabular-nums">
                    {h.priceAvailable ? (
                      <>
                        <div className={`font-semibold ${tone(h.pnl)}`}>{signedInr(h.pnl)}</div>
                        <div className={`text-xs ${tone(h.pnl)}`}>{pct(h.pnlPct)}</div>
                      </>
                    ) : <Unavailable />}
                  </div>
                </div>
                <div className="mt-2 grid grid-cols-3 gap-2 text-xs tabular-nums">
                  <Cell label="Qty · Avg" value={`${h.qty} @ ${inr(h.avgPrice)}`} />
                  <Cell label="LTP" value={h.priceAvailable ? inr(h.ltp) : "—"}
                        sub={h.priceAvailable ? pct(h.dayChangePct) : undefined} subClass={tone(h.dayChangePct)} />
                  <Cell label={h.priceAvailable ? "Value" : "Value (cost)"} value={inr(h.marketValue, 0)} right />
                </div>
                <div className="mt-2"><AllocMeter pctValue={h.allocationPct} /></div>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

function Name({ h }: { h: HoldingView }) {
  const initials = h.symbol.slice(0, 2);
  const body = (
    <div className="flex min-w-0 items-center gap-3">
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-slate-800 text-xs font-bold text-slate-300">
        {initials}
      </span>
      <div className="min-w-0">
        <div className="font-medium text-slate-50 group-hover:text-brand-light">{h.symbol}</div>
        <div className="max-w-[14rem] truncate text-xs text-slate-500">{h.name}</div>
      </div>
    </div>
  );
  return h.token ? <Link href={`/stocks/${h.token}`} className="group min-w-0">{body}</Link> : body;
}

/** Thin same-hue meter: this holding's share of the whole portfolio. */
function AllocMeter({ pctValue }: { pctValue: number }) {
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-800">
        <div className="h-full rounded-full bg-brand-light/80" style={{ width: `${Math.min(100, pctValue)}%` }} />
      </div>
      <span className="w-12 text-right text-xs tabular-nums text-slate-300">{pctValue.toFixed(1)}%</span>
    </div>
  );
}

function Cell({ label, value, sub, subClass = "", right = false }: {
  label: string; value: string; sub?: string; subClass?: string; right?: boolean;
}) {
  return (
    <div className={right ? "text-right" : ""}>
      <div className="text-[10px] uppercase tracking-wider text-slate-500">{label}</div>
      <div className="text-slate-200">{value}</div>
      {sub && <div className={subClass}>{sub}</div>}
    </div>
  );
}

function Unavailable() {
  return (
    <span className="inline-flex items-center rounded-full border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[11px] text-amber-300">
      Price unavailable
    </span>
  );
}
