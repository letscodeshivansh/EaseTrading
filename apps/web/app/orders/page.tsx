"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import OrderTicket from "@/components/OrderTicket";
import { listOrders, cancelOrder } from "@/lib/api";
import type { Order } from "@/lib/types";

// Mirrors the seeded watchlist; in Prompt 4+ this comes from the user's watchlist API.
const SYMBOLS = [
  { token: "2885", symbol: "RELIANCE" },
  { token: "11536", symbol: "TCS" },
  { token: "1333", symbol: "HDFCBANK" },
];

const STATUS_COLOR: Record<string, string> = {
  FILLED: "text-up", REJECTED: "text-down", CANCELLED: "text-slate-500",
  DRAFT: "text-slate-400", SUBMITTED: "text-brand", OPEN: "text-brand",
};

export default function OrdersPage() {
  const [sel, setSel] = useState(SYMBOLS[0]);
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
              value={sel.token}
              onChange={(e) => setSel(SYMBOLS.find((s) => s.token === e.target.value)!)}
              className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-sm"
            >
              {SYMBOLS.map((s) => <option key={s.token} value={s.token}>{s.symbol}</option>)}
            </select>
          </div>
          <OrderTicket token={sel.token} symbol={sel.symbol} onPlaced={refresh} />
          <p className="mt-2 text-[11px] text-slate-500">
            Paper-trading mode by default — orders are simulated. No real money moves until
            you switch the backend to live mode.
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
