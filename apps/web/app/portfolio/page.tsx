"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import { getPortfolio } from "@/lib/api";
import type { PortfolioView } from "@/lib/types";

export default function PortfolioPage() {
  const [data, setData] = useState<PortfolioView | null>(null);

  useEffect(() => {
    getPortfolio().then(setData).catch(() => setData(null));
  }, []);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1 p-4">
        {/* Totals */}
        <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Stat label="Invested" value={data ? `₹${data.invested.toLocaleString()}` : "—"} />
          <Stat label="Market value" value={data ? `₹${data.marketValue.toLocaleString()}` : "—"} />
          <Stat label="Total P&L" value={data ? `₹${data.totalPnl.toLocaleString()}` : "—"}
                color={data && data.totalPnl >= 0 ? "up" : "down"} />
          <Stat label="P&L %" value={data ? `${data.totalPnlPct.toFixed(2)}%` : "—"}
                color={data && data.totalPnl >= 0 ? "up" : "down"} />
        </div>

        {/* Holdings */}
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Holdings</h2>
        <div className="mb-4 overflow-x-auto rounded border border-slate-800">
          <table className="w-full text-sm">
            <thead className="bg-slate-900 text-left text-xs uppercase text-slate-400">
              <tr>
                <th className="px-3 py-2">Symbol</th><th className="px-3 py-2">Qty</th>
                <th className="px-3 py-2">Avg</th><th className="px-3 py-2">LTP</th>
                <th className="px-3 py-2">Value</th><th className="px-3 py-2">P&L</th>
                <th className="px-3 py-2">Alloc</th>
              </tr>
            </thead>
            <tbody>
              {data?.holdings.map((h) => (
                <tr key={h.token} className="border-t border-slate-800">
                  <td className="px-3 py-2 font-medium">{h.symbol}</td>
                  <td className="px-3 py-2">{h.qty}</td>
                  <td className="px-3 py-2">₹{h.avgPrice}</td>
                  <td className="px-3 py-2">₹{h.ltp}</td>
                  <td className="px-3 py-2">₹{h.marketValue.toLocaleString()}</td>
                  <td className={`px-3 py-2 ${h.pnl >= 0 ? "text-up" : "text-down"}`}>
                    ₹{h.pnl.toFixed(0)} ({h.pnlPct.toFixed(1)}%)
                  </td>
                  <td className="px-3 py-2">{h.allocationPct.toFixed(1)}%</td>
                </tr>
              ))}
              {(!data || data.holdings.length === 0) && (
                <tr><td colSpan={7} className="px-3 py-6 text-center text-slate-500">
                  No holdings yet — place a (paper) order to get started.
                </td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Risk panel */}
        {data && data.holdings.length > 0 && (
          <div className="rounded-lg border border-slate-800 p-3">
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Risk view</h2>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              <Stat label="Holdings" value={String(data.risk.holdings)} />
              <Stat label="Largest position" value={`${data.risk.largestPositionPct.toFixed(1)}%`} />
              <Stat label="Concentration (HHI)" value={data.risk.concentrationIndex.toFixed(2)} />
            </div>
            <p className="mt-2 text-sm text-slate-300">{data.risk.assessment}</p>
          </div>
        )}
      </main>
    </div>
  );
}

function Stat({ label, value, color }: { label: string; value: string; color?: "up" | "down" }) {
  const c = color === "up" ? "text-up" : color === "down" ? "text-down" : "text-slate-100";
  return (
    <div className="rounded border border-slate-800 px-3 py-2">
      <div className="text-[10px] uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`text-lg font-semibold tabular-nums ${c}`}>{value}</div>
    </div>
  );
}
