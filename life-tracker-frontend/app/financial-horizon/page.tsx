import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import FinancialHorizonClient, { NextMonthPurchaseItem } from "./financial-horizon-client";
import { HorizonSummary } from "../dashboard/financial-horizon-card";
import type { TransactionPage } from "./actions";

export const metadata = {
  title: "Financial Horizon | Life Tracker",
};

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

export default async function FinancialHorizonPage() {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on server
  const sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!sessionResponse.ok) {
    redirect("/");
  }

  // Fetch financial horizon details and next month purchases in parallel
  const [horizonRes, purchasesRes, transactionsRes] = await Promise.all([
    fetch(`${apiBaseURL}/api/horizon`, {
      headers: { Cookie: cookieHeader },
    }),
    fetch(`${apiBaseURL}/api/next-month-purchases`, {
      headers: { Cookie: cookieHeader },
    }),
    fetch(`${apiBaseURL}/api/horizon/transactions?page=1&page_size=10`, {
      headers: { Cookie: cookieHeader },
    }),
  ]);

  if (!horizonRes.ok) {
    return (
      <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10">
        <div className="mx-auto max-w-4xl">
          <Link
            className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80"
            href="/dashboard"
          >
            <ArrowLeft className="h-4 w-4" /> Back to Dashboard
          </Link>
          <header className="mt-6">
            <h1 className="text-3xl font-bold text-foreground">Financial Horizon</h1>
          </header>
          <p className="mt-8 rounded-2xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
            We couldn&apos;t load your financial horizon. Please try again.
          </p>
        </div>
      </main>
    );
  }

  const initialSummary = (await horizonRes.json()) as HorizonSummary;
  const initialTransactionPage: TransactionPage = transactionsRes.ok
    ? await transactionsRes.json()
    : { transactions: [], page: 1, page_size: 10, total: 0, total_pages: 0 };

  let initialPurchases: NextMonthPurchaseItem[] = [];
  let initialPurchasesTotal = 0;

  if (purchasesRes.ok) {
    const purchasesData = (await purchasesRes.json()) as { items: NextMonthPurchaseItem[]; total: number };
    initialPurchases = purchasesData.items ?? [];
    initialPurchasesTotal = purchasesData.total ?? 0;
  }

  return (
    <FinancialHorizonClient
      initialSummary={initialSummary}
      initialTransactionPage={initialTransactionPage}
      initialPurchases={initialPurchases}
      initialPurchasesTotal={initialPurchasesTotal}
    />
  );
}
