import Link from "next/link";
import { inr, pct } from "@/lib/format";
import type { Mover } from "@/lib/types";

/** A short ranked list of stocks with their price and today's % change. */
export default function MoversList({ rows, quotes, empty = "Nothing here yet." }: {
  rows: Mover[]; quotes?: Record<string, number>; empty?: string;
}) {
  if (rows.length === 0) return <p className="px-1 py-6 text-center text-xs text-slate-500">{empty}</p>;
  return (
    <ul className="divide-y divide-slate-800/70">
      {rows.map((m, i) => (
        <li key={m.token}>
          <Link href={`/stocks/${m.token}`} className="flex items-center gap-3 rounded-lg px-1.5 py-2 transition hover:bg-slate-800/40">
            <span className="w-4 text-xs tabular-nums text-slate-500">{i + 1}</span>
            <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-slate-800 text-[10px] font-bold text-slate-300">
              {m.symbol.slice(0, 2)}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium text-slate-100">{m.symbol}</span>
              <span className="block truncate text-[11px] text-slate-500">{m.name}</span>
            </span>
            <span className="text-right">
              <span className="block text-sm tabular-nums text-slate-200">{inr(quotes?.[m.token] ?? m.ltp)}</span>
              <span className={`block text-xs font-semibold tabular-nums ${m.changePct >= 0 ? "text-up" : "text-down"}`}>{pct(m.changePct)}</span>
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
