"use client";

import { useId, useState } from "react";

/**
 * A small ⓘ next to a term that a new trader may not know. Hover, focus or tap it
 * to see a one- or two-line plain-English explanation.
 */
export default function InfoTip({ text, align = "center" }: { text: string; align?: "center" | "left" | "right" }) {
  const [open, setOpen] = useState(false);
  const id = useId();
  const pos = align === "left" ? "left-0" : align === "right" ? "right-0" : "left-1/2 -translate-x-1/2";
  return (
    <span className="relative inline-flex align-middle">
      <button type="button" aria-describedby={id} aria-label="What is this?"
        onMouseEnter={() => setOpen(true)} onMouseLeave={() => setOpen(false)}
        onFocus={() => setOpen(true)} onBlur={() => setOpen(false)}
        onClick={() => setOpen((o) => !o)}
        className="ml-1 grid h-4 w-4 place-items-center rounded-full border border-slate-600 text-[9px] font-bold normal-case tracking-normal text-slate-400 hover:border-brand-light hover:text-brand-light">
        i
      </button>
      <span id={id} role="tooltip"
        className={`pointer-events-none absolute top-6 z-40 w-64 rounded-lg border border-slate-700 bg-slate-900 p-2.5 text-xs font-normal normal-case leading-relaxed tracking-normal text-slate-200 shadow-xl transition ${pos} ${
          open ? "opacity-100" : "invisible opacity-0"}`}>
        {text}
      </span>
    </span>
  );
}
