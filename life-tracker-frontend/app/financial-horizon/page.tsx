import { redirect } from "next/navigation";
import { frame } from "@/app/components/design-system/page-frame";
import TopAppBar from "@/app/components/top-app-bar";
import FinancialHorizonClient from "@/app/financial-horizon/financial-horizon-client";
import { loadFinancialHorizon } from "@/app/financial-horizon/data";

export const metadata = { title: "Financial Horizon | PULSE" };

export default async function FinancialHorizonPage() {
  const { summary, purchases, transactions } = await loadFinancialHorizon();
  if (summary.status === 401) redirect("/");

  const appBar = <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" />;
  if (!summary.data) {
    return (
      <main id="main-content" tabIndex={-1} className={frame.page}>
        <div className={frame.content}>
          {appBar}
          <h1 className="mt-6 text-3xl font-bold">Financial Horizon</h1>
          <p className="mt-8 rounded-2xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
            We couldn&apos;t load your financial horizon. Please try again.
          </p>
        </div>
      </main>
    );
  }

  return (
    <FinancialHorizonClient
      appBar={appBar}
      initialSummary={summary.data}
      initialTransactionPage={transactions ?? { transactions: [], page: 1, page_size: 10, total: 0, total_pages: 0 }}
      initialPurchases={purchases ?? []}
      initialLoadError={purchases === null || !transactions}
    />
  );
}
