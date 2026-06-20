"use client";

import { useState } from "react";
import Header from "@/components/Header";
import { runScreener } from "@/lib/api";
import type { ScreenResult } from "@/lib/types";

/**
 * Screener page: edit the fundamental thresholds, run the screen, and view ranked
 * matches. Rules are sent to the backend as data, so changing a number here changes
 * the screen without any code change.
 */
interface Rule {
  key: string;
  op: string;
  value: number;
  mandatory: boolean;
  label: string; // human-friendly name for the UI
}

const DEFAULT_RULES: Rule[] = [
  { key: "pe_ratio", op: "lt", value: 30, mandatory: true, label: "P/E ratio <" },
  { key: "roe_pct", op: "gt", value: 25, mandatory: true, label: "ROE % >" },
  { key: "eps", op: "gt", value: 0, mandatory: true, label: "EPS >" },
  { key: "fii_holding_pct", op: "gt", value: 5, mandatory: true, label: "FII holding % >" },
  { key: "dii_holding_pct", op: "gt", value: 5, mandatory: true, label: "DII holding % >" },
  { key: "profit_growth_pct", op: "gt", value: 0, mandatory: true, label: "Profit growth % >" },
  { key: "debt_to_equity", op: "lt", value: 1, mandatory: false, label: "Debt/Equity <" },
];

export default function ScreenerPage() {
  const [rules, setRules] = useState<Rule[]>(DEFAULT_RULES);
  const [result, setResult] = useState<ScreenResult | null>(null);
  const [loading, setLoading] = useState(false);

  function updateValue(index: number, value: number) {
    setRules((prev) => prev.map((r, i) => (i === index ? { ...r, value } : r)));
  }

  async function run() {
    setLoading(true);
    try {
      // Strip the UI-only "label" before sending to the backend.
      const payload = rules.map(({ label, ...rule }) => rule);
      setResult(await runScreener(payload));
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex flex-1 flex-col gap-4 p-4 md:flex-row">
        {/* Rule builder */}
        <section className="md:w-72">
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">
            Screening rules
          </h2>
          <div className="space-y-2">
            {rules.map((rule, i) => (
              <div key={rule.key} className="flex items-center justify-between rounded border border-slate-800 px-2 py-1.5">
                <span className="text-sm text-slate-300">{rule.label}</span>
                <input
                  type="number"
                  value={rule.value}
                  onChange={(e) => updateValue(i, Number(e.target.value))}
                  className="w-20 rounded bg-slate-800 px-2 py-1 text-right text-sm"
                />
              </div>
            ))}
          </div>
          <button
            onClick={run}
            disabled={loading}
            className="mt-3 w-full rounded bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-50"
          >
            {loading ? "Screening…" : "Run screen"}
          </button>
        </section>

        {/* Results */}
        <section className="flex-1">
          {result && (
            <p className="mb-2 text-sm text-slate-400">
              {result.matchCount} of {result.scanned} stocks passed all mandatory rules.
            </p>
          )}
          <div className="overflow-x-auto rounded border border-slate-800">
            <table className="w-full text-sm">
              <thead className="bg-slate-900 text-left text-xs uppercase text-slate-400">
                <tr>
                  <th className="px-3 py-2">Symbol</th>
                  <th className="px-3 py-2">Score</th>
                  <th className="px-3 py-2">P/E</th>
                  <th className="px-3 py-2">ROE%</th>
                  <th className="px-3 py-2">EPS</th>
                  <th className="px-3 py-2">FII%</th>
                  <th className="px-3 py-2">DII%</th>
                  <th className="px-3 py-2">Growth%</th>
                </tr>
              </thead>
              <tbody>
                {result?.matches.map((m) => (
                  <tr key={m.token} className="border-t border-slate-800">
                    <td className="px-3 py-2 font-medium">{m.symbol}</td>
                    <td className="px-3 py-2 text-brand">{(m.score * 100).toFixed(0)}%</td>
                    <td className="px-3 py-2">{fmt(m.values.pe_ratio)}</td>
                    <td className="px-3 py-2">{fmt(m.values.roe_pct)}</td>
                    <td className="px-3 py-2">{fmt(m.values.eps)}</td>
                    <td className="px-3 py-2">{fmt(m.values.fii_holding_pct)}</td>
                    <td className="px-3 py-2">{fmt(m.values.dii_holding_pct)}</td>
                    <td className="px-3 py-2">{fmt(m.values.profit_growth_pct)}</td>
                  </tr>
                ))}
                {result && result.matches.length === 0 && (
                  <tr>
                    <td colSpan={8} className="px-3 py-6 text-center text-slate-500">
                      No stocks matched. Try relaxing a threshold.
                    </td>
                  </tr>
                )}
                {!result && (
                  <tr>
                    <td colSpan={8} className="px-3 py-6 text-center text-slate-500">
                      Set your thresholds and click “Run screen”.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}

function fmt(v: number | null | undefined): string {
  return v == null ? "—" : Number(v).toFixed(1);
}
