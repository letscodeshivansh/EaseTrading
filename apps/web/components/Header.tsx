"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { getMe, getMode, logout, type Me } from "@/lib/api";
import type { Mode } from "@/lib/types";

const NAV = [
  { href: "/", label: "Dashboard" },
  { href: "/portfolio", label: "Portfolio" },
  { href: "/stocks", label: "Stocks" },
  { href: "/screener", label: "Screener" },
  { href: "/alerts", label: "Alerts" },
];

/**
 * Sticky top bar: logo, pill navigation (scrolls sideways on phones) and the REAL
 * data source (Groww or demo) from the api.
 */
export default function Header() {
  const path = usePathname();
  const [mode, setMode] = useState<Mode | null>(null);
  const [checked, setChecked] = useState(false);
  const [me, setMe] = useState<Me | null>(null);

  useEffect(() => {
    getMode().then(setMode).catch(() => setMode(null)).finally(() => setChecked(true));
    getMe().then(setMe).catch(() => setMe(null)); // not logged in -> api helper goes to /login
  }, []);

  return (
    <header className="sticky top-0 z-30 border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3">
        <Link href="/" className="flex shrink-0 items-center gap-2">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-gradient-to-br from-brand to-violet-500 text-sm font-black text-white shadow-lg shadow-brand/30">
            E
          </span>
          <span className="hidden text-lg font-semibold tracking-tight sm:inline">EaseTrading</span>
        </Link>

        <nav className="-mx-1 flex flex-1 gap-1 overflow-x-auto px-1 text-sm [scrollbar-width:none]">
          {NAV.map((n) => {
            const active = n.href === "/" ? path === "/" : path?.startsWith(n.href);
            return (
              <Link key={n.href} href={n.href}
                className={`whitespace-nowrap rounded-lg px-3 py-1.5 transition ${
                  active ? "bg-brand/15 font-medium text-brand-light"
                         : "text-slate-400 hover:bg-slate-800/60 hover:text-slate-100"}`}>
                {n.label}
              </Link>
            );
          })}
        </nav>

        <div className="hidden shrink-0 items-center gap-2 text-xs md:flex">
          {mode ? (
            <Badge tone={mode.provider === "GROWW" ? "brand" : "neutral"}>
              {mode.provider === "GROWW" ? "Groww" : "Demo data"}
            </Badge>
          ) : checked ? (
            <Badge tone="danger">API offline</Badge>
          ) : null}
        </div>

        {me && (
          <div className="flex shrink-0 items-center gap-2 text-xs">
            <span className="hidden max-w-[12rem] truncate text-slate-400 lg:inline" title={me.email}>{me.email}</span>
            <button onClick={() => logout().finally(() => { window.location.href = "/login"; })}
              className="rounded-lg border border-slate-700 px-2.5 py-1 text-slate-300 hover:border-slate-500 hover:text-slate-100">
              Log out
            </button>
          </div>
        )}
      </div>
    </header>
  );
}

function Badge({ tone, children }: { tone: "brand" | "neutral" | "danger"; children: React.ReactNode }) {
  const styles = {
    brand: "border-brand/30 bg-brand/10 text-brand-light",
    neutral: "border-slate-700 bg-slate-900 text-slate-300",
    danger: "border-down/30 bg-down/10 text-down",
  }[tone];
  const dot = { brand: "bg-brand-light", neutral: "bg-slate-500", danger: "bg-down" }[tone];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 font-medium ${styles}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${dot}`} />
      {children}
    </span>
  );
}
