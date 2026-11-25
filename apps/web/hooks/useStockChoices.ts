"use client";

import { useEffect, useState } from "react";
import { getStock, getWatchlist } from "@/lib/api";

export interface StockChoice { token: string; symbol: string }

/**
 * Stocks a form can pick from = my watchlist, plus the stock given in the URL as
 * ?token=…, which is pre-selected.
 */
export function useStockChoices() {
  const [choices, setChoices] = useState<StockChoice[]>([]);
  const [sel, setSel] = useState<StockChoice | null>(null);

  useEffect(() => {
    const wanted = new URLSearchParams(window.location.search).get("token");
    (async () => {
      let list: StockChoice[] = [];
      try {
        list = (await getWatchlist()).map((w) => ({ token: w.token, symbol: w.symbol }));
      } catch {
        /* api down: empty list */
      }
      if (wanted && !list.some((c) => c.token === wanted)) {
        try {
          const s = await getStock(wanted);
          list = [{ token: wanted, symbol: s.instrument.symbol }, ...list];
        } catch {
          /* unknown token: ignore */
        }
      }
      setChoices(list);
      setSel(list.find((c) => c.token === wanted) ?? list[0] ?? null);
    })();
  }, []);

  return { choices, sel, setSel };
}
