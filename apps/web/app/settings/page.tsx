"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import { getBrokerAccount, getBrokerStatus, linkBroker, reconnectBroker, unlinkBroker } from "@/lib/api";
import type { BrokerAccount, BrokerStatus } from "@/lib/types";

/**
 * Settings → My Groww account.
 * Paste your own Groww Trading API key + secret; the server stores them encrypted
 * and never shows them again (only the last 4 characters of the key).
 */
export default function SettingsPage() {
  const [account, setAccount] = useState<BrokerAccount | null>(null);
  const [status, setStatus] = useState<BrokerStatus | null>(null);
  const [mode, setMode] = useState<"approval" | "totp">("approval");
  const [apiKey, setApiKey] = useState("");
  const [secret, setSecret] = useState("");
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function load() {
    getBrokerAccount().then(setAccount).catch((e) => setError(e.message));
    getBrokerStatus().then(setStatus).catch(() => setStatus(null));
  }
  useEffect(load, []);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      setStatus(await linkBroker(mode, apiKey, secret));
      setApiKey("");
      setSecret("");
      setEditing(false);
      getBrokerAccount().then(setAccount);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!confirm("Remove your Groww keys from EaseTrading?")) return;
    setBusy(true);
    try {
      setAccount(await unlinkBroker());
      getBrokerStatus().then(setStatus);
    } finally {
      setBusy(false);
    }
  }

  async function reconnect() {
    setBusy(true);
    try { setStatus(await reconnectBroker()); } finally { setBusy(false); }
  }

  const demo = account?.provider === "MOCK";
  const showForm = !demo && account && (!account.linked || editing);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="mx-auto w-full max-w-2xl flex-1 space-y-5 px-4 py-6">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
          <p className="text-sm text-slate-400">Link your own Groww account to see your holdings and funds.</p>
        </div>

        <section className="card p-5">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <h2 className="section-title">My Groww account</h2>
            {status && (
              <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium ${
                status.connected ? "border-up/30 bg-up/10 text-up"
                  : status.linked ? "border-amber-500/30 bg-amber-500/10 text-amber-300"
                  : "border-slate-700 bg-slate-900 text-slate-400"}`}>
                <span className={`h-1.5 w-1.5 rounded-full ${status.connected ? "bg-up" : status.linked ? "bg-amber-400" : "bg-slate-500"}`} />
                {status.connected ? "Connected" : status.linked ? "Not connected" : "Not linked"}
              </span>
            )}
          </div>

          {demo && <p className="text-sm text-slate-400">The server is running in demo mode, so every account shows demo data. There is nothing to link.</p>}

          {status && !demo && <p className="mb-4 text-sm text-slate-300">{status.message}</p>}

          {account?.linked && !editing && !demo && (
            <div className="space-y-4">
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
                  <dt className="text-[11px] uppercase tracking-wider text-slate-500">Saved key</dt>
                  <dd className="font-mono">{account.keyHint}</dd>
                </div>
                <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
                  <dt className="text-[11px] uppercase tracking-wider text-slate-500">Login type</dt>
                  <dd>{account.authMode === "totp" ? "API key + TOTP" : "API key + secret"}</dd>
                </div>
              </dl>
              <div className="flex flex-wrap gap-2">
                {!status?.connected && (
                  <button onClick={reconnect} disabled={busy}
                    className="rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-50">
                    {busy ? "Connecting…" : "Reconnect"}
                  </button>
                )}
                <button onClick={() => { setEditing(true); setMode(account.authMode ?? "approval"); }}
                  className="rounded-lg border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:border-slate-500">
                  Replace keys
                </button>
                <button onClick={remove} disabled={busy}
                  className="rounded-lg border border-down/30 px-3 py-1.5 text-sm text-down hover:bg-down/10 disabled:opacity-50">
                  Remove
                </button>
              </div>
            </div>
          )}

          {showForm && (
            <form onSubmit={save} className="space-y-3">
              <label className="block text-xs text-slate-400">
                Login type
                <select value={mode} onChange={(e) => setMode(e.target.value as "approval" | "totp")}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand">
                  <option value="approval">API key + API secret (approve daily on Groww)</option>
                  <option value="totp">API key + TOTP secret</option>
                </select>
              </label>
              <label className="block text-xs text-slate-400">
                API key
                <input value={apiKey} onChange={(e) => setApiKey(e.target.value)} required autoComplete="off" spellCheck={false}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-brand" />
              </label>
              <label className="block text-xs text-slate-400">
                {mode === "totp" ? "TOTP secret" : "API secret"}
                <input type="password" value={secret} onChange={(e) => setSecret(e.target.value)} required autoComplete="off"
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-brand" />
              </label>
              {error && <p className="rounded-lg border border-down/30 bg-down/10 px-3 py-2 text-xs text-down">{error}</p>}
              <div className="flex gap-2">
                <button type="submit" disabled={busy}
                  className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-50">
                  {busy ? "Saving…" : "Save & connect"}
                </button>
                {editing && (
                  <button type="button" onClick={() => setEditing(false)}
                    className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300">Cancel</button>
                )}
              </div>
              <p className="text-[11px] text-slate-500">
                Stored encrypted on the server and never shown again — only the last 4 characters of the key.
              </p>
            </form>
          )}
        </section>

        {!demo && (
          <section className="card p-5 text-sm text-slate-300">
            <h2 className="section-title mb-3">Where do I get these?</h2>
            <ol className="list-decimal space-y-1.5 pl-5">
              <li>In the Groww app or website: <span className="text-slate-100">Profile → Trading APIs</span> (needs Groww&apos;s Trading API subscription).</li>
              <li>Create an API key. Copy the <span className="text-slate-100">API key</span> and <span className="text-slate-100">API secret</span> — Groww shows the secret only once.</li>
              <li>Each day, <span className="text-slate-100">approve the key</span> on Groww&apos;s API-keys page, then press Reconnect here.</li>
            </ol>
          </section>
        )}
      </main>
    </div>
  );
}
