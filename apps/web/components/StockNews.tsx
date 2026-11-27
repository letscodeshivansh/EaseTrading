"use client";

import { useEffect, useState } from "react";
import { getStockNews } from "@/lib/api";
import type { StockNews as News } from "@/lib/types";

/**
 * Latest headlines about one stock from major business outlets (Economic Times,
 * Moneycontrol, Mint, Business Standard, Reuters, …). Tap a headline to read it
 * on the publisher's site.
 */
export default function StockNews({ token }: { token: string }) {
  const [news, setNews] = useState<News | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setNews(null); setError(null);
    getStockNews(token).then(setNews).catch((e) => setError(e.message));
  }, [token]);

  return (
    <div className="rounded-lg border border-slate-800">
      <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
        <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400">Latest news</h2>
        {news && <span className="text-[11px] text-slate-500">Last 30 days · major outlets</span>}
      </div>

      {error && <p className="px-3 py-4 text-sm text-slate-400">Couldn&apos;t load news right now. Try again in a bit.</p>}
      {!error && !news && (
        <div className="space-y-3 p-3">
          {[0, 1, 2].map((i) => <div key={i} className="h-10 animate-pulse rounded bg-slate-800/50" />)}
        </div>
      )}
      {news && news.items.length === 0 && (
        <p className="px-3 py-4 text-sm text-slate-400">No news about {news.query} from major outlets in the last 30 days.</p>
      )}
      {news && news.items.length > 0 && (
        <ul className="divide-y divide-slate-800">
          {news.items.map((n) => (
            <li key={n.url}>
              <a href={n.url} target="_blank" rel="noopener noreferrer" className="group block px-3 py-2.5 hover:bg-slate-900">
                <div className="text-sm leading-snug text-slate-100 group-hover:underline">{n.title}</div>
                <div className="mt-1 flex items-center gap-2 text-[11px] text-slate-500">
                  <span className="font-medium text-slate-300">{n.source}</span>
                  {n.publishedAt && <><span>·</span><span title={new Date(n.publishedAt).toLocaleString("en-IN")}>{ago(n.publishedAt)}</span></>}
                </div>
              </a>
            </li>
          ))}
        </ul>
      )}
      {news && news.items.length > 0 && (
        <p className="border-t border-slate-800 px-3 py-2 text-[11px] text-slate-500">
          Headlines via Google News. News moves prices, but one headline is not a reason to buy or sell.
        </p>
      )}
    </div>
  );
}

function ago(iso: string) {
  const mins = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (mins < 60) return `${mins}m ago`;
  const h = Math.round(mins / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.round(h / 24);
  return d === 1 ? "yesterday" : `${d} days ago`;
}
