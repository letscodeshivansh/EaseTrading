"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import StockSearch from "@/components/StockSearch";
import { addToWatchlist, getWatchlist, removeFromWatchlist } from "@/lib/api";
import { inr } from "@/lib/format";
import type { WatchRow } from "@/lib/types";

/**
 * My watchlist: search to add, × to remove, click a stock to open its page.
 * Prices update live from the WebSocket (quotes prop); before the first tick we show
 * the last price the api knows.
 */
export default function Watchlist({
  quotes, onTokensChange,
}: { quotes: Record<string, number>; onTokensChange?: (tokens: string[]) => void }) {
  const [rows, setRows] = useState<WatchRow[]>([]);
  const [error, setError] = useState<string | null>(null);

  function apply(r: WatchRow[]) {
    setRows(r);
    setError(null);
    onTokensChange?.(r.map((x) => x.token));
  }

  useEffect(() => {
    getWatchlist().then(apply).catch((e) => setError(e.message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="flex flex-col">
      <div className="mb-2"><StockSearch onPick={(i) => addToWatchlist(i.token).then(apply).catch((e) => setError(e.message))}
        placeholder="Add a stock…" /></div>
      {error && <p className="mb-2 text-xs text-down">{error}</p>}
      <ul className="divide-y divide-slate-800 rounded border border-slate-800">
        {rows.map((r) => {
          const price = quotes[r.token] ?? (r.ltp > 0 ? r.ltp : null);
          return (
            <li key={r.token} className="group flex items-center justify-between px-3 py-2 text-sm hover:bg-slate-900">
              <Link href={`/stocks/${r.token}`} className="min-w-0 flex-1">
                <div className="font-medium">{r.symbol}</div>
                <div className="truncate text-[11px] text-slate-500">{r.name}</div>
              </Link>
              <span className="ml-2 tabular-nums text-slate-200">{inr(price)}</span>
              <button onClick={() => removeFromWatchlist(r.token).then(apply)}
                title="Remove from watchlist"
                className="ml-2 text-slate-600 opacity-0 transition hover:text-down group-hover:opacity-100">×</button>
            </li>
          );
        })}
        {rows.length === 0 && !error && (
          <li className="px-3 py-4 text-center text-xs text-slate-500">Your watchlist is empty — search above to add stocks.</li>
        )}
      </ul>
    </div>
  );
}
