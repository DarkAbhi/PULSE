import "server-only";
import { readJSON } from "@/lib/api/server-api";
import type { HorizonSummary, NextMonthPurchaseItem, TransactionPage } from "@/features/financial-horizon/types/horizon-types";

export async function loadFinancialHorizon() {
  const [summary, purchases, transactions] = await Promise.all([
    readJSON<HorizonSummary>("/api/horizon"),
    readJSON<{ items: NextMonthPurchaseItem[] }>("/api/next-month-purchases"),
    readJSON<TransactionPage>("/api/horizon/transactions?page=1&page_size=10"),
  ]);

  return { summary, purchases: purchases.data?.items ?? null, transactions: transactions.data };
}
