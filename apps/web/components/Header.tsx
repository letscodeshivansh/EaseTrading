import Link from "next/link";

// Top bar with navigation. Server component (no interactivity needed).
export default function Header() {
  return (
    <header className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
      <div className="flex items-center gap-4">
        <span className="text-xl font-bold text-brand">EaseTrading</span>
        <nav className="flex gap-3 text-sm text-slate-300">
          <Link href="/" className="hover:text-brand">Dashboard</Link>
          <Link href="/screener" className="hover:text-brand">Screener</Link>
          <Link href="/portfolio" className="hover:text-brand">Portfolio</Link>
          <Link href="/orders" className="hover:text-brand">Orders</Link>
          <Link href="/alerts" className="hover:text-brand">Alerts</Link>
        </nav>
      </div>
      <span className="rounded bg-brand-light/10 px-2 py-1 text-xs text-brand">
        Paper trading
      </span>
    </header>
  );
}
