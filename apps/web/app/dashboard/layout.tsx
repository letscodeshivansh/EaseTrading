import Header from "@/components/Header";
import DashboardTabs from "@/components/dashboard/DashboardTabs";

/**
 * Dashboard = three sections, each its own folder:
 *   /dashboard        Overview     — your money at a glance
 *   /dashboard/live   Live market  — indices, movers, live chart
 *   /dashboard/fno    F&O          — option chain, open interest, PCR, max pain
 */
export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <DashboardTabs />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-5">{children}</main>
    </div>
  );
}
