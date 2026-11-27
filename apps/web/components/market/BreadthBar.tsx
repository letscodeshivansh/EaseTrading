import InfoTip from "@/components/InfoTip";

/** How many Nifty 50 stocks are up vs down today, as one bar with labels. */
export default function BreadthBar({ advances, declines, unchanged }: { advances: number; declines: number; unchanged: number }) {
  const total = advances + declines + unchanged;
  const mood = total === 0 ? null : advances > declines * 1.5 ? "Most stocks are rising" : declines > advances * 1.5 ? "Most stocks are falling" : "Mixed day";
  return (
    <div>
      <div className="mb-2 flex items-center justify-between text-xs">
        <span className="font-semibold uppercase tracking-wider text-slate-400">
          Market breadth
          <InfoTip align="left" text="Out of the Nifty 50 stocks, how many are up (green) vs down (red) today. More green than red usually means a broadly positive day." />
        </span>
        {mood && <span className="text-slate-300">{mood}</span>}
      </div>
      {total === 0 ? (
        <p className="text-xs text-slate-500">Waiting for prices…</p>
      ) : (
        <>
          <div className="flex h-2.5 w-full gap-[2px] overflow-hidden rounded-full" role="img"
            aria-label={`${advances} up, ${declines} down, ${unchanged} unchanged`}>
            <div className="bg-up" style={{ width: `${(advances / total) * 100}%` }} />
            {unchanged > 0 && <div className="bg-slate-500" style={{ width: `${(unchanged / total) * 100}%` }} />}
            <div className="bg-down" style={{ width: `${(declines / total) * 100}%` }} />
          </div>
          <div className="mt-1.5 flex justify-between text-xs tabular-nums">
            <span className="text-up">▲ {advances} up</span>
            {unchanged > 0 && <span className="text-slate-400">{unchanged} flat</span>}
            <span className="text-down">{declines} down ▼</span>
          </div>
        </>
      )}
    </div>
  );
}
