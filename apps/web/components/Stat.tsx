/** A labelled number tile used across the dashboard and portfolio. */
export default function Stat({
  label, value, sub, valueClass = "text-slate-50", subClass = "text-slate-400",
}: { label: string; value: string; sub?: string; valueClass?: string; subClass?: string }) {
  return (
    <div className="card px-4 py-3">
      <div className="text-[11px] font-medium uppercase tracking-wider text-slate-500">{label}</div>
      <div className={`mt-1 text-lg font-semibold tabular-nums sm:text-xl ${valueClass}`}>{value}</div>
      {sub && <div className={`mt-0.5 text-xs tabular-nums ${subClass}`}>{sub}</div>}
    </div>
  );
}
