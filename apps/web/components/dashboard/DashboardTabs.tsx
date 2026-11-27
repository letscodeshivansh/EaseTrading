"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { href: "/dashboard", label: "Overview", hint: "Your money at a glance", icon: "M3 12l9-8 9 8M5 10v10h14V10" },
  { href: "/dashboard/live", label: "Live market", hint: "Indices, movers, charts", icon: "M3 17l6-6 4 4 8-8M14 7h7v7" },
  { href: "/dashboard/fno", label: "F&O", hint: "Option chain & OI", icon: "M4 20V10M10 20V4M16 20v-7M22 20H2" },
];

/** The three dashboard sections, as big tappable tabs. */
export default function DashboardTabs() {
  const path = usePathname();
  return (
    <nav className="mx-auto w-full max-w-7xl px-4 pt-5">
      <div className="grid grid-cols-3 gap-2 rounded-2xl border border-slate-800 bg-slate-900/60 p-1.5">
        {TABS.map((t) => {
          const active = t.href === "/dashboard" ? path === "/dashboard" : path?.startsWith(t.href);
          return (
            <Link key={t.href} href={t.href}
              className={`flex items-center justify-center gap-2.5 rounded-xl px-3 py-2.5 transition sm:justify-start ${
                active ? "bg-brand text-black" : "text-slate-300 hover:bg-slate-800/70"}`}>
              <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0" fill="none" stroke="currentColor" strokeWidth="2"
                strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={t.icon} /></svg>
              <span className="min-w-0">
                <span className="block text-sm font-semibold">{t.label}</span>
                <span className={`hidden truncate text-[11px] sm:block ${active ? "text-black/60" : "text-slate-500"}`}>{t.hint}</span>
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
