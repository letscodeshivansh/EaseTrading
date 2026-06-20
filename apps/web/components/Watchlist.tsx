"use client";

import type { Instrument } from "@/lib/types";

/**
 * The list of symbols on the left. Shows each instrument with its live price,
 * highlights the selected one, and reports clicks back to the dashboard.
 *
 * This is a "presentational" component: it holds no data of its own, it just
 * renders the props it is given. That keeps it simple and easy to reuse.
 */
export default function Watchlist({
  instruments,
  quotes,
  selected,
  onSelect,
}: {
  instruments: Instrument[];
  quotes: Record<string, number>;
  selected: string;
  onSelect: (token: string) => void;
}) {
  return (
    <div className="flex h-full flex-col">
      <h2 className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
        Watchlist
      </h2>
      <ul className="flex-1 overflow-y-auto">
        {instruments.map((inst) => {
          const price = quotes[inst.token];
          const isSelected = inst.token === selected;
          return (
            <li key={inst.token}>
              <button
                onClick={() => onSelect(inst.token)}
                className={`flex w-full items-center justify-between px-3 py-2 text-left text-sm transition
                  ${isSelected ? "bg-brand/20 text-brand" : "hover:bg-slate-800"}`}
              >
                <span className="font-medium">{inst.symbol}</span>
                <span className="tabular-nums text-slate-300">
                  {price != null ? price.toFixed(2) : "—"}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
