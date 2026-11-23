import "server-only";
import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import type { HorizonSummary, NextMonthPurchaseItem, TransactionPage } from "@/app/financial-horizon/horizon-types";

async function readResource<T>(path: string): Promise<{ status: number; data: T | null }> {
  try {
    const response = await apiFetch(`${apiBaseURL}${path}`, { signal: AbortSignal.timeout(10_000) });
    return { status: response.status, data: response.ok ? await response.json() : null };
  } catch {
    return { status: 0, data: null };
  }
}

export async function loadFinancialHorizon() {
  const [summary, purchases, transactions] = await Promise.all([
    readResource<HorizonSummary>("/api/horizon"),
    readResource<{ items: NextMonthPurchaseItem[] }>("/api/next-month-purchases"),
    readResource<TransactionPage>("/api/horizon/transactions?page=1&page_size=10"),
  ]);

  return { summary, purchases: purchases.data?.items ?? null, transactions: transactions.data };
}
