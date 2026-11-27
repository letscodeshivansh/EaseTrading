"use client";

import { Fragment, useEffect, useRef } from "react";
import { compact } from "@/lib/format";
import type { OptionLeg, OptionStrike } from "@/lib/types";

const NUM = new Intl.NumberFormat("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * The option chain: calls on the left, strike in the middle, puts on the right.
 * Shaded cells are "in the money" (already worth something if expiry were now).
 * The ATM strike (closest to the index) is highlighted and scrolled into view.
 */
export default function OptionChainTable({ strikes, spot, atm, showGreeks }: {
  strikes: OptionStrike[]; spot: number; atm: number; showGreeks: boolean;
}) {
  const atmRef = useRef<HTMLTableRowElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const maxOi = Math.max(1, ...strikes.flatMap((s) => [s.call?.openInterest ?? 0, s.put?.openInterest ?? 0]));

  // Centre the ATM row when a new chain loads (not on every live tick, so the user can scroll freely).
  const chainKey = `${strikes[0]?.strike}-${strikes.length}`;
  useEffect(() => {
    const box = scrollRef.current, row = atmRef.current;
    if (box && row) box.scrollTop = row.offsetTop - box.clientHeight / 2;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chainKey]);

  const spotIdx = strikes.findIndex((s) => s.strike > spot);

  return (
    <div ref={scrollRef} className="max-h-[520px] overflow-auto rounded-xl border border-slate-800">
      <table className="w-full min-w-[640px] border-collapse text-xs tabular-nums">
        <thead className="sticky top-0 z-10 bg-slate-900 text-[10px] uppercase tracking-wider text-slate-400">
          <tr>
            <th colSpan={showGreeks ? 5 : 3} className="border-b border-slate-800 py-1.5 text-center font-semibold" style={{ color: "#3987e5" }}>Calls</th>
            <th className="border-b border-slate-800 bg-slate-950" />
            <th colSpan={showGreeks ? 5 : 3} className="border-b border-slate-800 py-1.5 text-center font-semibold" style={{ color: "#d95926" }}>Puts</th>
          </tr>
          <tr className="text-right">
            {showGreeks && <><th className="px-2 py-1.5 font-medium">IV</th><th className="px-2 font-medium">Delta</th></>}
            <th className="px-2 font-medium">OI</th>
            <th className="px-2 font-medium">Volume</th>
            <th className="px-2 font-medium">Price</th>
            <th className="bg-slate-950 px-3 text-center font-semibold text-slate-200">Strike</th>
            <th className="px-2 text-left font-medium">Price</th>
            <th className="px-2 text-left font-medium">Volume</th>
            <th className="px-2 text-left font-medium">OI</th>
            {showGreeks && <><th className="px-2 text-left font-medium">Delta</th><th className="px-2 text-left font-medium">IV</th></>}
          </tr>
        </thead>
        <tbody>
          {strikes.map((s, i) => {
            const callItm = s.strike < spot, putItm = s.strike > spot, isAtm = s.strike === atm;
            return (
              <Fragment key={s.strike}>
                {i === spotIdx && i > 0 && (
                  <tr>
                    <td colSpan={showGreeks ? 11 : 7} className="px-2 py-1">
                      <div className="flex items-center gap-2">
                        <span className="flex-1 border-t border-dashed border-slate-400" />
                        <span className="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] font-semibold text-slate-900">Index {NUM.format(spot)}</span>
                        <span className="flex-1 border-t border-dashed border-slate-400" />
                      </div>
                    </td>
                  </tr>
                )}
                <tr ref={isAtm ? atmRef : undefined}
                  className={`border-b border-slate-800/70 hover:bg-slate-800/40 ${isAtm ? "outline outline-1 -outline-offset-1 outline-brand/60" : ""}`}>
                  <Side leg={s.call} itm={callItm} maxOi={maxOi} showGreeks={showGreeks} color="#3987e5" align="right" />
                  <td className={`bg-slate-950 px-3 py-1.5 text-center font-semibold ${isAtm ? "text-brand-light" : "text-slate-200"}`}>
                    {s.strike}{isAtm && <span className="ml-1 text-[9px] font-bold text-brand-light">ATM</span>}
                  </td>
                  <Side leg={s.put} itm={putItm} maxOi={maxOi} showGreeks={showGreeks} color="#d95926" align="left" />
                </tr>
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

/** One side (calls or puts) of a row. Calls read right-to-left towards the strike, puts left-to-right. */
function Side({ leg, itm, maxOi, showGreeks, color, align }: {
  leg: OptionLeg | null; itm: boolean; maxOi: number; showGreeks: boolean; color: string; align: "left" | "right";
}) {
  const cell = `px-2 ${align === "right" ? "text-right" : "text-left"} ${itm ? "bg-amber-400/[0.06]" : ""}`;
  const oiShare = ((leg?.openInterest ?? 0) / maxOi) * 100;
  const g = leg?.greeks;
  const cells = [
    ...(showGreeks ? [
      <td key="iv" className={`${cell} text-slate-400`}>{g?.iv != null ? `${g.iv.toFixed(1)}%` : "—"}</td>,
      <td key="delta" className={`${cell} text-slate-400`}>{g?.delta != null ? g.delta.toFixed(2) : "—"}</td>,
    ] : []),
    <td key="oi" className={`${cell} relative`}>
      <span className="absolute inset-y-1 opacity-25" style={{ [align]: 0, width: `${oiShare}%`, background: color }} />
      <span className="relative">{leg ? compact(leg.openInterest) : "—"}</span>
    </td>,
    <td key="vol" className={`${cell} text-slate-400`}>{leg ? compact(leg.volume) : "—"}</td>,
    <td key="price" className={`${cell} font-medium text-slate-100`}>{leg ? NUM.format(leg.ltp) : "—"}</td>,
  ];
  return <>{align === "right" ? cells : cells.reverse()}</>;
}
