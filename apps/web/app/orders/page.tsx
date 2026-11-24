"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import OrderTicket from "@/components/OrderTicket";
import { listOrders, cancelOrder } from "@/lib/api";
import { useStockChoices } from "@/hooks/useStockChoices";
import type { Order } from "@/lib/types";

const STATUS_COLOR: Record<string, string> = {
  FILLED: "text-up", REJECTED: "text-down", CANCELLED: "text-slate-500",
  DRAFT: "text-slate-400", SUBMITTED: "text-brand-light", OPEN: "text-brand-light",
};

export default function OrdersPage() {
  // Stocks to choose from = my watchlist (+ the stock passed as ?token=… from a stock page).
  const { choices: SYMBOLS, sel, setSel } = useStockChoices();
  const [orders, setOrders] = useState<Order[]>([]);

  function refresh() {
    listOrders().then(setOrders).catch(() => setOrders([]));
  }
  useEffect(refresh, []);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex flex-1 flex-col gap-4 p-4 md:flex-row">
        {/* Ticket */}
        <section className="md:w-80">
          <div className="mb-2">
            <label className="text-xs text-slate-400">Instrument</label>
            <select
              value={sel?.token ?? ""}
              onChange={(e) => setSel(SYMBOLS.find((s) => s.token === e.target.value) ?? null)}
              className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-sm"
            >
              {SYMBOLS.map((s) => <option key={s.token} value={s.token}>{s.symbol}</option>)}
            </select>
          </div>
          {sel ? <OrderTicket token={sel.token} symbol={sel.symbol} onPlaced={refresh} />
               : <p className="text-sm text-slate-500">Add stocks to your watchlist to trade them.</p>}
          <p className="mt-2 text-[11px] text-slate-500">
            Paper trading — orders are simulated against real prices. Real orders arrive in a
            later phase, after the safety checks are in place.
          </p>
        </section>

        {/* History */}
        <section className="flex-1">
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Order history</h2>
          <div className="overflow-x-auto rounded border border-slate-800">
            <table className="w-full text-sm">
              <thead className="bg-slate-900 text-left text-xs uppercase text-slate-400">
                <tr>
                  <th className="px-3 py-2">Symbol</th><th className="px-3 py-2">Side</th>
                  <th className="px-3 py-2">Qty</th><th className="px-3 py-2">Price</th>
                  <th className="px-3 py-2">Status</th><th className="px-3 py-2">P&L</th>
                  <th className="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id} className="border-t border-slate-800">
                    <td className="px-3 py-2 font-medium">{o.symbol}</td>
                    <td className={`px-3 py-2 ${o.side === "BUY" ? "text-up" : "text-down"}`}>{o.side}</td>
                    <td className="px-3 py-2">{o.qty}</td>
                    <td className="px-3 py-2">₹{o.filledPrice || o.price}</td>
                    <td className={`px-3 py-2 ${STATUS_COLOR[o.status] ?? ""}`}>{o.status}</td>
                    <td className={`px-3 py-2 ${o.realizedPnl >= 0 ? "text-up" : "text-down"}`}>
                      {o.realizedPnl ? `₹${o.realizedPnl.toFixed(0)}` : "—"}
                    </td>
                    <td className="px-3 py-2">
                      {(o.status === "DRAFT" || o.status === "SUBMITTED" || o.status === "OPEN") && (
                        <button onClick={() => cancelOrder(o.id).then(refresh)} className="text-xs text-down hover:underline">
                          cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
                {orders.length === 0 && (
                  <tr><td colSpan={7} className="px-3 py-6 text-center text-slate-500">No orders yet.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}
