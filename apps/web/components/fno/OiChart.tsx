"use client";

import { useState } from "react";
import { compact } from "@/lib/format";
import type { OptionStrike } from "@/lib/types";

const CALL = "#3987e5";
const PUT = "#d95926";

/**
 * Open interest by strike: blue = calls, orange = puts. Tall blue bars above the
 * price often act as a ceiling (resistance), tall orange bars below as a floor
 * (support). The dashed line marks where the index is now.
 */
export default function OiChart({ strikes, spot, atm }: { strikes: OptionStrike[]; spot: number; atm: number }) {
  const [hover, setHover] = useState<number | null>(null);
  const max = Math.max(1, ...strikes.flatMap((s) => [s.call?.openInterest ?? 0, s.put?.openInterest ?? 0]));
  // Where the spot sits, as a fraction across the columns.
  const idx = strikes.findIndex((s) => s.strike >= spot);
  let spotPos: number | null = null;
  if (idx > 0) {
    const a = strikes[idx - 1].strike, b = strikes[idx].strike;
    spotPos = (idx - 1 + 0.5 + (spot - a) / (b - a)) / strikes.length;
  }
  const h = hover != null ? strikes[hover] : null;
  const labelEvery = Math.ceil(strikes.length / 9);
  const atmIdx = strikes.findIndex((s) => s.strike === atm);

  return (
    <div>
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2 text-xs">
        <div className="flex items-center gap-4 text-slate-300">
          <Legend color={CALL} label="Call OI (bets on a cap)" />
          <Legend color={PUT} label="Put OI (bets on a floor)" />
          <span className="flex items-center gap-1.5 text-slate-400"><span className="h-3 border-l border-dashed border-slate-300" />Index now</span>
        </div>
        <div className="h-4 tabular-nums text-slate-300">
          {h && <>Strike <b>{h.strike}</b> · <span style={{ color: CALL }}>Calls {compact(h.call?.openInterest)}</span> · <span style={{ color: PUT }}>Puts {compact(h.put?.openInterest)}</span></>}
        </div>
      </div>
      <div className="relative h-56" onMouseLeave={() => setHover(null)}>
        <div className="absolute inset-0 flex items-end gap-px">
          {strikes.map((s, i) => (
            <div key={s.strike} onMouseEnter={() => setHover(i)}
              className={`flex h-full flex-1 items-end justify-center gap-[2px] rounded-t px-[1px] ${
                hover === i ? "bg-slate-700/40" : s.strike === atm ? "bg-slate-800/50" : ""}`}>
              <div className="w-1/2 max-w-[10px] rounded-t-sm" style={{ height: `${((s.call?.openInterest ?? 0) / max) * 100}%`, background: CALL }} />
              <div className="w-1/2 max-w-[10px] rounded-t-sm" style={{ height: `${((s.put?.openInterest ?? 0) / max) * 100}%`, background: PUT }} />
            </div>
          ))}
        </div>
        {spotPos != null && (
          <div className="pointer-events-none absolute inset-y-0 border-l border-dashed border-slate-300" style={{ left: `${spotPos * 100}%` }}>
            <span className="absolute -top-1 left-1 whitespace-nowrap rounded bg-slate-200 px-1 text-[10px] font-semibold text-slate-900">{spot.toFixed(0)}</span>
          </div>
        )}
      </div>
      <div className="mt-1 flex gap-px border-t border-slate-800 pt-1">
        {strikes.map((s, i) => (
          <div key={s.strike} className={`flex-1 text-center text-[10px] tabular-nums ${s.strike === atm ? "font-semibold text-slate-200" : "text-slate-500"}`}>
            {s.strike === atm || (i % labelEvery === 0 && Math.abs(i - atmIdx) > 1) ? s.strike : ""}
          </div>
        ))}
      </div>
    </div>
  );
}

function Legend({ color, label }: { color: string; label: string }) {
  return <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm" style={{ background: color }} />{label}</span>;
}
