"use client";

import type { Verdict } from "@/lib/types";

/**
 * The AI Analysis report card: a colour-coded rating, the trade plan
 * (entry / stop / target / risk-to-reward), and the analyst's written memo.
 *
 * The memo is markdown; we render it with a tiny inline formatter so we don't pull
 * in a heavy markdown library for Prompt 3.
 */
const RATING_STYLE: Record<string, string> = {
  STRONG_BUY: "bg-up/20 text-up border-up/40",
  BUY: "bg-up/10 text-up border-up/30",
  NEUTRAL: "bg-slate-700/30 text-slate-200 border-slate-600",
  SELL: "bg-down/10 text-down border-down/30",
  STRONG_SELL: "bg-down/20 text-down border-down/40",
};

export default function AnalysisCard({ verdict, loading }: { verdict: Verdict | null; loading: boolean }) {
  if (loading) return <div className="p-4 text-sm text-slate-400">Analyzing…</div>;
  if (!verdict) return <div className="p-4 text-sm text-slate-500">Pick a strategy and click “Analyze”.</div>;

  if (verdict.source === "insufficient_data") {
    return <div className="p-4 text-sm text-slate-400">{verdict.memo}</div>;
  }

  const ratingClass = RATING_STYLE[verdict.rating] ?? RATING_STYLE.NEUTRAL;

  return (
    <div className="space-y-4 p-4">
      {/* Rating + trade plan header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className={`rounded-lg border px-3 py-1.5 text-sm font-bold ${ratingClass}`}>
          {verdict.rating.replace("_", " ")}
        </div>
        <div className="text-xs text-slate-400">
          conviction {(verdict.confidence * 100).toFixed(0)}% ·{" "}
          <span className={verdict.source === "claude" ? "text-brand" : "text-slate-500"}>
            {verdict.source === "claude" ? "Claude" : "grounded analyst"}
          </span>
        </div>
      </div>

      {/* Trade plan numbers */}
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Plan label="Entry" value={verdict.entry} />
        <Plan label="Stop-loss" value={verdict.stopLoss} className="text-down" />
        <Plan label="Target" value={verdict.target} className="text-up" />
        <Plan label="Risk : Reward" value={verdict.rrRatio} prefix="" suffix="x" />
      </div>

      {/* Sub-score bars */}
      {verdict.subScores && (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          <Bar label="Technical" v={verdict.subScores.technical} />
          <Bar label="Valuation" v={verdict.subScores.valuation} />
          <Bar label="Quality" v={verdict.subScores.quality} />
          <Bar label="Momentum" v={verdict.subScores.momentum} />
        </div>
      )}

      {/* Memo */}
      <div className="rounded-lg border border-slate-800 bg-slate-900/40 p-3 text-sm leading-relaxed">
        <Memo markdown={verdict.memo} />
      </div>
    </div>
  );
}

function Plan({ label, value, className, prefix = "₹", suffix = "" }: {
  label: string; value: number | null; className?: string; prefix?: string; suffix?: string;
}) {
  return (
    <div className="rounded border border-slate-800 px-2 py-1.5">
      <div className="text-[10px] uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`tabular-nums font-semibold ${className ?? "text-slate-100"}`}>
        {value == null ? "—" : `${prefix}${value}${suffix}`}
      </div>
    </div>
  );
}

function Bar({ label, v }: { label: string; v: number }) {
  const pct = Math.round(v * 100);
  const color = v >= 0.6 ? "bg-up" : v <= 0.4 ? "bg-down" : "bg-slate-500";
  return (
    <div>
      <div className="mb-1 flex justify-between text-[10px] uppercase tracking-wide text-slate-500">
        <span>{label}</span><span>{pct}%</span>
      </div>
      <div className="h-1.5 rounded bg-slate-800">
        <div className={`h-1.5 rounded ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

/** Minimal markdown: ## / ### headings, **bold**, and paragraphs. */
function Memo({ markdown }: { markdown: string }) {
  const lines = markdown.split("\n");
  return (
    <div className="space-y-1.5">
      {lines.map((line, i) => {
        if (line.startsWith("### ")) return <h4 key={i} className="mt-2 font-semibold text-brand">{line.slice(4)}</h4>;
        if (line.startsWith("## ")) return <h3 key={i} className="text-base font-bold">{line.slice(3)}</h3>;
        if (line.trim() === "") return null;
        return <p key={i} className="text-slate-300">{renderBold(line)}</p>;
      })}
    </div>
  );
}

/** Turn **text** into <strong> spans. */
function renderBold(text: string) {
  return text.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith("**") && part.endsWith("**")
      ? <strong key={i} className="text-slate-100">{part.slice(2, -2)}</strong>
      : <span key={i}>{part}</span>
  );
}
