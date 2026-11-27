"use client";

import { useEffect, useState } from "react";

/** "Market open" / "Market closed" pill with the current India time. */
export default function MarketStatus({ open }: { open: boolean | null }) {
  const [now, setNow] = useState<string>("");
  useEffect(() => {
    const tick = () => setNow(new Date().toLocaleTimeString("en-IN", { timeZone: "Asia/Kolkata", hour: "2-digit", minute: "2-digit" }));
    tick();
    const t = setInterval(tick, 30_000);
    return () => clearInterval(t);
  }, []);
  if (open === null) return null;
  return (
    <span className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-medium ${
      open ? "border-up/30 bg-up/10 text-up" : "border-slate-700 bg-slate-900 text-slate-300"}`}
      title="NSE/BSE regular session: Monday–Friday, 9:15 AM – 3:30 PM IST">
      <span className="relative flex h-2 w-2">
        {open && <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-up opacity-60" />}
        <span className={`relative inline-flex h-2 w-2 rounded-full ${open ? "bg-up" : "bg-slate-500"}`} />
      </span>
      {open ? "Market open" : "Market closed · opens 9:15 AM"}
      {now && <span className="text-slate-500">· {now} IST</span>}
    </span>
  );
}
