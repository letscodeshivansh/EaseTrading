"use client";

import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import StockSearch from "@/components/StockSearch";
import Watchlist from "@/components/Watchlist";
import { useLiveQuotes } from "@/hooks/useLiveQuotes";
import { useState } from "react";

/** Stocks: search the full NSE list and open any stock; your watchlist alongside. */
export default function StocksPage() {
  const router = useRouter();
  const [tokens, setTokens] = useState<string[]>([]);
  const { quotes } = useLiveQuotes(tokens);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="mx-auto grid w-full max-w-5xl flex-1 gap-6 p-4 md:grid-cols-[1fr_320px]">
        <section>
          <h1 className="mb-1 text-lg font-bold">Find a stock</h1>
          <p className="mb-3 text-sm text-slate-400">Search every NSE-listed company by symbol or name.</p>
          <StockSearch autoFocus onPick={(i) => router.push(`/stocks/${i.token}`)} />
        </section>
        <aside>
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Watchlist</h2>
          <Watchlist quotes={quotes} onTokensChange={setTokens} />
        </aside>
      </main>
    </div>
  );
}
