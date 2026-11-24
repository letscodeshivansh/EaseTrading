// Small display helpers so every screen formats money and percentages the same way.

const INR = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2, minimumFractionDigits: 2 });
const INR0 = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 0 });

/** ₹1,23,456.78 (Indian grouping). "—" for missing values. */
export function inr(v: number | null | undefined, decimals = 2): string {
  if (v == null || Number.isNaN(v)) return "—";
  return `₹${(decimals === 0 ? INR0 : INR).format(v)}`;
}

/** +₹1,234.50 / −₹980.00 — for profit & loss. */
export function signedInr(v: number | null | undefined): string {
  if (v == null || Number.isNaN(v)) return "—";
  return `${v >= 0 ? "+" : "−"}${inr(Math.abs(v))}`;
}

/** +1.25% / −0.80% */
export function pct(v: number | null | undefined): string {
  if (v == null || Number.isNaN(v)) return "—";
  return `${v >= 0 ? "+" : "−"}${Math.abs(v).toFixed(2)}%`;
}

/** Green for gains, red for losses, neutral for zero/missing. */
export function tone(v: number | null | undefined): string {
  if (v == null || v === 0 || Number.isNaN(v)) return "text-slate-300";
  return v > 0 ? "text-up" : "text-down";
}

/** 1.2 Cr / 34.5 L / 12,345 — compact Indian units for big numbers like market cap. */
export function compact(v: number | null | undefined): string {
  if (v == null || Number.isNaN(v)) return "—";
  if (Math.abs(v) >= 1e7) return `${(v / 1e7).toFixed(2)} Cr`;
  if (Math.abs(v) >= 1e5) return `${(v / 1e5).toFixed(2)} L`;
  return INR0.format(v);
}
