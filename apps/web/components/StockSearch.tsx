"use client";

import { useEffect, useRef, useState } from "react";
import { searchInstruments } from "@/lib/api";
import type { Instrument } from "@/lib/types";

/**
 * Search box over the full NSE stock list. Type a symbol or company name; results
 * appear after a short pause. onPick receives the chosen stock.
 */
export default function StockSearch({
  onPick, placeholder = "Search stocks — e.g. TATA, INFY, HDFC", autoFocus = false,
}: { onPick: (i: Instrument) => void; placeholder?: string; autoFocus?: boolean }) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<Instrument[]>([]);
  const [open, setOpen] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);

  // Debounced search: wait 250 ms after typing stops.
  useEffect(() => {
    const query = q.trim();
    if (query.length < 1) { setResults([]); return; }
    const t = setTimeout(() => {
      searchInstruments(query).then((r) => { setResults(r); setOpen(true); }).catch(() => setResults([]));
    }, 250);
    return () => clearTimeout(t);
  }, [q]);

  // Close the dropdown when clicking elsewhere.
  useEffect(() => {
    function onDoc(e: MouseEvent) {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);

  function pick(i: Instrument) {
    onPick(i);
    setQ("");
    setResults([]);
    setOpen(false);
  }

  return (
    <div ref={boxRef} className="relative">
      <input
        value={q}
        autoFocus={autoFocus}
        onChange={(e) => setQ(e.target.value)}
        onFocus={() => results.length && setOpen(true)}
        onKeyDown={(e) => { if (e.key === "Enter" && results[0]) pick(results[0]); }}
        placeholder={placeholder}
        className="w-full rounded border border-slate-700 bg-slate-900 px-3 py-2 text-sm outline-none focus:border-brand"
      />
      {open && results.length > 0 && (
        <ul className="absolute z-20 mt-1 max-h-80 w-full overflow-y-auto rounded border border-slate-700 bg-slate-900 shadow-lg">
          {results.map((r) => (
            <li key={r.token}>
              <button onClick={() => pick(r)}
                className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-slate-800">
                <span className="font-medium">{r.symbol}</span>
                <span className="ml-3 truncate text-xs text-slate-400">{r.name} · {r.exchange}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
