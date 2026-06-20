"use client";

import type { Indicators } from "@/lib/types";

/**
 * A compact read-out of the key technical indicators for the selected stock.
 * Colours give an at-a-glance sense: green = bullish-ish, red = bearish-ish.
 */
export default function IndicatorPanel({ data }: { data: Indicators | null }) {
  if (!data) return <div className="p-3 text-sm text-slate-500">Loading indicators…</div>;
  if ("error" in (data as any)) return <div className="p-3 text-sm text-slate-500">Not enough data.</div>;

  // RSI colouring: >70 overbought (red), <30 oversold (green), else neutral.
  const rsi = data.rsi14;
  const rsiColor = rsi == null ? "text-slate-300" : rsi > 70 ? "text-down" : rsi < 30 ? "text-up" : "text-slate-200";

  const macdColor = data.macd.cross === "bullish" ? "text-up" : data.macd.cross === "bearish" ? "text-down" : "text-slate-200";

  return (
    <div className="grid grid-cols-2 gap-2 p-3 text-sm md:grid-cols-3">
      <Stat label="Trend" value={data.trend} highlight={data.trend === "up" ? "up" : data.trend === "down" ? "down" : undefined} />
      <Stat label="RSI (14)" value={rsi?.toFixed(1) ?? "—"} className={rsiColor} />
      <Stat label="MACD" value={data.macd.cross} className={macdColor} />
      <Stat label="50/200" value={data.cross_50_200.replace("_", " ")} />
      <Stat label="SMA 50" value={data.sma["50"]?.toFixed(1) ?? "—"} />
      <Stat label="SMA 200" value={data.sma["200"]?.toFixed(1) ?? "—"} />
      <Stat label="Fib trend" value={data.fibonacci.trend} />
      <Stat label="Fib active" value={data.fibonacci.active ?? "—"} />
      <Stat label="Vol vs avg" value={data.volume.vsAvg ? `${data.volume.vsAvg}x` : "—"} />
    </div>
  );
}

function Stat({
  label, value, className, highlight,
}: { label: string; value: string; className?: string; highlight?: "up" | "down" }) {
  const color = highlight === "up" ? "text-up" : highlight === "down" ? "text-down" : className ?? "text-slate-200";
  return (
    <div className="rounded border border-slate-800 bg-slate-900/50 px-2 py-1">
      <div className="text-[10px] uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`font-medium capitalize ${color}`}>{value}</div>
    </div>
  );
}
