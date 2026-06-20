"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import { listAlerts, createAlert, deleteAlert } from "@/lib/api";
import type { Alert } from "@/lib/types";

const SYMBOLS = [
  { token: "2885", symbol: "RELIANCE" },
  { token: "11536", symbol: "TCS" },
  { token: "1333", symbol: "HDFCBANK" },
];

const STATUS_COLOR: Record<string, string> = {
  ACTIVE: "text-brand", TRIGGERED: "text-up", DISABLED: "text-slate-500",
};

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [sel, setSel] = useState(SYMBOLS[0]);
  const [type, setType] = useState<"PRICE" | "RSI">("PRICE");
  const [operator, setOperator] = useState<"GT" | "LT">("GT");
  const [threshold, setThreshold] = useState(1500);

  function refresh() {
    listAlerts().then(setAlerts).catch(() => setAlerts([]));
  }
  useEffect(refresh, []);

  async function add() {
    await createAlert({ token: sel.token, symbol: sel.symbol, type, operator, threshold });
    refresh();
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex flex-1 flex-col gap-4 p-4 md:flex-row">
        {/* Create */}
        <section className="md:w-80">
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">New alert</h2>
          <div className="space-y-2 rounded-lg border border-slate-800 p-3">
            <select value={sel.token} onChange={(e) => setSel(SYMBOLS.find((s) => s.token === e.target.value)!)}
              className="w-full rounded bg-slate-800 px-2 py-1 text-sm">
              {SYMBOLS.map((s) => <option key={s.token} value={s.token}>{s.symbol}</option>)}
            </select>
            <div className="flex gap-2">
              <select value={type} onChange={(e) => setType(e.target.value as any)}
                className="flex-1 rounded bg-slate-800 px-2 py-1 text-sm">
                <option value="PRICE">Price</option><option value="RSI">RSI</option>
              </select>
              <select value={operator} onChange={(e) => setOperator(e.target.value as any)}
                className="rounded bg-slate-800 px-2 py-1 text-sm">
                <option value="GT">above</option><option value="LT">below</option>
              </select>
            </div>
            <input type="number" value={threshold} onChange={(e) => setThreshold(Number(e.target.value))}
              className="w-full rounded bg-slate-800 px-2 py-1 text-sm" />
            <button onClick={add} className="w-full rounded bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-dark">
              Create alert
            </button>
          </div>
        </section>

        {/* List */}
        <section className="flex-1">
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">My alerts</h2>
          <div className="space-y-2">
            {alerts.map((a) => (
              <div key={a.id} className="flex items-center justify-between rounded border border-slate-800 px-3 py-2 text-sm">
                <div>
                  <span className="font-medium">{a.symbol}</span>{" "}
                  <span className="text-slate-400">
                    {a.type} {a.operator === "GT" ? ">" : "<"} {a.threshold}
                  </span>
                  {a.lastValue != null && <span className="ml-2 text-xs text-slate-500">last {a.lastValue.toFixed(2)}</span>}
                </div>
                <div className="flex items-center gap-3">
                  <span className={`text-xs font-semibold ${STATUS_COLOR[a.status]}`}>{a.status}</span>
                  <button onClick={() => deleteAlert(a.id).then(refresh)} className="text-xs text-down hover:underline">delete</button>
                </div>
              </div>
            ))}
            {alerts.length === 0 && <p className="text-sm text-slate-500">No alerts yet.</p>}
          </div>
        </section>
      </main>
    </div>
  );
}
