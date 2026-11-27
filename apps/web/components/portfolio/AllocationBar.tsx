"use client";

import { useState } from "react";
import { inr } from "@/lib/format";

/**
 * Where the money is: one segmented bar (the whole = 100% of portfolio value) with a
 * labelled legend. Top 5 holdings get their own colour; the rest fold into "Other".
 *
 * Colours: the validated categorical order for dark surfaces (blue, orange, aqua,
 * yellow, magenta) — checked for colour-blind separation and contrast on the card
 * surface (#111111). Colour follows the holding's rank in THIS list only, and every
 * segment is also named in the legend, so identity never relies on colour alone.
 */
const SERIES = ["#3987e5", "#d95926", "#199e70", "#c98500", "#d55181"];
const OTHER = "#5f5f5f";

export interface Slice { label: string; value: number; pct: number }

export default function AllocationBar({ items }: { items: Slice[] }) {
  const [hover, setHover] = useState<number | null>(null);
  const sorted = [...items].sort((a, b) => b.value - a.value);
  const top = sorted.slice(0, 5);
  const rest = sorted.slice(5);
  const slices: (Slice & { color: string })[] = top.map((s, i) => ({ ...s, color: SERIES[i] }));
  if (rest.length) {
    slices.push({
      label: `Other (${rest.length})`,
      value: rest.reduce((a, s) => a + s.value, 0),
      pct: rest.reduce((a, s) => a + s.pct, 0),
      color: OTHER,
    });
  }

  if (slices.length === 0) {
    return <p className="text-sm text-slate-500">No holdings yet.</p>;
  }

  return (
    <div>
      {/* The bar: 2px surface gaps between segments, rounded outer ends. */}
      <div className="flex h-4 w-full gap-[2px] overflow-hidden rounded-md" role="img"
        aria-label={slices.map((s) => `${s.label} ${s.pct.toFixed(1)}%`).join(", ")}>
        {slices.map((s, i) => (
          <div key={s.label}
            onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}
            title={`${s.label}: ${s.pct.toFixed(1)}% · ${inr(s.value)}`}
            className="h-full transition-opacity"
            style={{
              width: `${Math.max(s.pct, 0.8)}%`,
              background: s.color,
              opacity: hover === null || hover === i ? 1 : 0.35,
            }} />
        ))}
      </div>

      {/* Legend with direct values. */}
      <ul className="mt-4 space-y-1.5">
        {slices.map((s, i) => (
          <li key={s.label}
            onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}
            className={`flex items-center gap-2 rounded-md px-1.5 py-1 text-sm transition ${hover === i ? "bg-slate-800/60" : ""}`}>
            <span className="h-2.5 w-2.5 shrink-0 rounded-sm" style={{ background: s.color }} />
            <span className="min-w-0 flex-1 truncate text-slate-200">{s.label}</span>
            <span className="tabular-nums text-slate-400">{inr(s.value, 0)}</span>
            <span className="w-14 text-right font-medium tabular-nums text-slate-100">{s.pct.toFixed(1)}%</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
