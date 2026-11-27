"use client";

import { useState } from "react";
import { reconnectBroker } from "@/lib/api";
import type { Dashboard } from "@/lib/types";

/**
 * Broker connection line at the top of the dashboard: connected / not linked /
 * not connected, with the one button that fixes it (Link or Reconnect).
 */
export default function ConnectionBanner({ data, error, onReconnected }: {
  data: Dashboard | null; error: string | null; onReconnected: () => void;
}) {
  const [busy, setBusy] = useState(false);
  if (error) {
    return (
      <div className="rounded-xl border border-down/40 bg-down/10 p-3 text-sm text-down">
        Can&apos;t reach the EaseTrading api — is it running? ({error})
      </div>
    );
  }
  if (!data) return null;
  const b = data.broker;
  if (b.connected && b.provider !== "MOCK") return null; // all good: stay out of the way
  return (
    <div className={`flex flex-wrap items-center justify-between gap-2 rounded-xl border px-3 py-2.5 text-sm ${
      b.connected ? "border-slate-800 bg-slate-900/50" : "border-amber-600/40 bg-amber-500/10"}`}>
      <div className="flex min-w-0 items-center gap-2">
        <span className={`h-2 w-2 shrink-0 rounded-full ${b.connected ? "bg-slate-400" : "bg-amber-400"}`} />
        <span className="font-medium">{b.provider === "GROWW" ? "Groww" : "Demo mode"}</span>
        <span className="truncate text-slate-400">{b.message}</span>
      </div>
      {!b.linked && b.provider === "GROWW" && (
        <a href="/settings" className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-black hover:bg-brand-dark">
          Link Groww account
        </a>
      )}
      {b.linked && !b.connected && b.provider === "GROWW" && (
        <button disabled={busy}
          onClick={() => { setBusy(true); reconnectBroker().finally(() => { setBusy(false); onReconnected(); }); }}
          className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-black hover:bg-brand-dark disabled:opacity-50">
          {busy ? "Connecting…" : "Reconnect"}
        </button>
      )}
    </div>
  );
}
