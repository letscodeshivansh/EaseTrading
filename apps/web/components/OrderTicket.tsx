"use client";

import { useState } from "react";
import { draftOrder, confirmOrder } from "@/lib/api";
import type { DraftResult, Order } from "@/lib/types";

/**
 * The order ticket — a deliberate TWO-STEP flow:
 *   1. Fill the form -> "Review" creates a DRAFT and shows the risk report.
 *   2. "Confirm" actually executes (paper or live). "Cancel" discards the draft.
 *
 * This split is the core safety control: you always see the risk checks before any
 * order is placed, and nothing executes without your explicit confirmation.
 */
export default function OrderTicket({
  token, symbol, onPlaced,
}: { token: string; symbol: string; onPlaced?: (order: Order) => void }) {
  const [side, setSide] = useState<"BUY" | "SELL">("BUY");
  const [type, setType] = useState<"MARKET" | "LIMIT">("MARKET");
  const [qty, setQty] = useState(1);
  const [price, setPrice] = useState(0);
  const [draft, setDraft] = useState<DraftResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  async function review() {
    setBusy(true); setMsg(null);
    try {
      setDraft(await draftOrder({ token, side, type, qty, price }));
    } catch (e) {
      setMsg("Could not create draft.");
    } finally {
      setBusy(false);
    }
  }

  async function confirm() {
    if (!draft) return;
    setBusy(true); setMsg(null);
    try {
      const order = await confirmOrder(draft.order.id);
      setMsg(`Order ${order.status}.`);
      setDraft(null);
      onPlaced?.(order);
    } catch (e) {
      setMsg("Confirmation failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="rounded-lg border border-slate-800 p-3">
      <h3 className="mb-2 text-sm font-semibold">{symbol} — Order ticket</h3>

      {/* Step 1: the form */}
      {!draft && (
        <div className="space-y-2">
          <div className="flex gap-2">
            {(["BUY", "SELL"] as const).map((s) => (
              <button key={s} onClick={() => setSide(s)}
                className={`flex-1 rounded px-2 py-1 text-sm font-semibold ${
                  side === s ? (s === "BUY" ? "bg-up/30 text-up" : "bg-down/30 text-down") : "bg-slate-800 text-slate-400"
                }`}>{s}</button>
            ))}
          </div>
          <div className="flex gap-2">
            {(["MARKET", "LIMIT"] as const).map((t) => (
              <button key={t} onClick={() => setType(t)}
                className={`flex-1 rounded px-2 py-1 text-xs ${type === t ? "bg-brand/30 text-brand" : "bg-slate-800 text-slate-400"}`}>{t}</button>
            ))}
          </div>
          <label className="block text-xs text-slate-400">
            Quantity
            <input type="number" min={1} value={qty} onChange={(e) => setQty(Number(e.target.value))}
              className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-sm" />
          </label>
          {type === "LIMIT" && (
            <label className="block text-xs text-slate-400">
              Limit price
              <input type="number" value={price} onChange={(e) => setPrice(Number(e.target.value))}
                className="mt-1 w-full rounded bg-slate-800 px-2 py-1 text-sm" />
            </label>
          )}
          <button onClick={review} disabled={busy}
            className="w-full rounded bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-50">
            {busy ? "Checking…" : "Review order"}
          </button>
        </div>
      )}

      {/* Step 2: review the risk report, then confirm */}
      {draft && (
        <div className="space-y-2">
          <div className="text-sm">
            <span className={side === "BUY" ? "text-up" : "text-down"}>{draft.order.side}</span>{" "}
            {draft.order.qty} {symbol} ({draft.order.type}) @ ₹{draft.order.price}
          </div>

          <div className="rounded border border-slate-800 p-2 text-xs">
            <div className={`mb-1 font-semibold ${draft.risk.passed ? "text-up" : "text-down"}`}>
              Risk checks: {draft.risk.passed ? "PASSED" : "FAILED"}
            </div>
            <ul className="space-y-0.5">
              {draft.risk.checks.map((c) => (
                <li key={c.name} className="flex justify-between gap-2">
                  <span className={c.passed ? "text-slate-300" : "text-down"}>
                    {c.passed ? "✓" : "✗"} {c.name}
                  </span>
                  <span className="text-slate-500">{c.detail}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="flex gap-2">
            <button onClick={() => setDraft(null)} className="flex-1 rounded bg-slate-800 px-3 py-2 text-sm text-slate-300">
              Cancel
            </button>
            <button onClick={confirm} disabled={busy || !draft.risk.passed}
              title={!draft.risk.passed ? "Order failed risk checks" : ""}
              className="flex-1 rounded bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-50">
              {busy ? "Placing…" : "Confirm order"}
            </button>
          </div>
        </div>
      )}

      {msg && <p className="mt-2 text-xs text-slate-400">{msg}</p>}
    </div>
  );
}
