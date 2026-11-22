"use client";

import { Icon } from "./HorizonIcon";
import CategoryOptions from "./CategoryOptions";

import { HorizonSearch, HorizonSelect } from "./HorizonFilters";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney } from "@/app/lib/currency.mjs";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/time-zone-provider";

import Button from "@/app/components/design-system/button";
import theme from "../horizon-theme.module.css";

import { HorizonSummary, TransactionItem, CategoryItem } from "@/app/dashboard/financial-horizon-card";
import type { TransactionPage } from "@/app/financial-horizon/actions";

interface TransactionsTabProps {
  summary: HorizonSummary;
  transactions: TransactionItem[];
  transactionPage: TransactionPage;
  categories: CategoryItem[];
  onOpenAddTransaction: () => void;
  onOpenEditTransaction: (tx: TransactionItem) => void;
  onConfirmDeleteTransaction: (tx: TransactionItem) => void;
  onOpenAddCategory: () => void;
  onOpenStatementUpload?: () => void;
  isPending?: boolean;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  // Filter state — owned by parent so filter changes trigger server re-fetches
  txTypeFilter: "all" | "debit" | "credit";
  onTxTypeFilterChange: (v: "all" | "debit" | "credit") => void;
  searchQuery: string;
  onSearchQueryChange: (v: string) => void;
  txCategoryFilter: number | null;
  onTxCategoryFilterChange: (v: number | null) => void;
}

export default function TransactionsTab({
  summary,
  transactions,
  transactionPage,
  categories,
  onOpenAddTransaction,
  onOpenEditTransaction,
  onConfirmDeleteTransaction,
  onOpenAddCategory,
  onOpenStatementUpload,
  isPending = false,
  onPageChange,
  onPageSizeChange,
  txTypeFilter,
  onTxTypeFilterChange,
  searchQuery,
  onSearchQueryChange,
  txCategoryFilter,
  onTxCategoryFilterChange,
}: TransactionsTabProps) {
  const { timeZone } = useTimeZone();
  const hasFilters = txTypeFilter !== "all" || txCategoryFilter != null || searchQuery.trim() !== "";

  return (
    <section className="space-y-6">
      {/* Header Bar */}
      <div className={theme.sectionHeader}>
        <div>
          <div className="flex flex-wrap items-center gap-2.5">
            <Icon name="receipt" aria-hidden="true" className={`size-6 ${theme.sectionIcon}`} />
            <h2 className={theme.sectionTitle}>Transaction History</h2>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {onOpenStatementUpload && (
            <Button variant="soft" size="sm"
              className={theme.softAction}
              onClick={onOpenStatementUpload}
              disabled={isPending}
            >
              <Icon name="upload" className="h-3.5 w-3.5" /> Import Statement
            </Button>
          )}
          <Button variant="secondary" size="sm"
            className={theme.softAction}
            onClick={onOpenAddCategory}
            disabled={isPending}
          >
            <Icon name="tag" className="h-3.5 w-3.5 text-muted-foreground" /> Add Category
          </Button>
          <Button variant="primary" size="sm"
            className={theme.primary}
            onClick={onOpenAddTransaction}
            disabled={isPending}
          >
            <Icon name="plus" className="h-4 w-4" /> Add Transaction
          </Button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 flex-wrap">
          {/* Transaction Type Filter Segment */}
          <div role="group" aria-label="Transaction type" className="inline-flex items-center gap-0.5 rounded-full bg-(--horizon-chip) p-1">
            <Button variant={txTypeFilter === "all" ? "primary" : "secondary"} size="sm" className={theme.segment}
              aria-pressed={txTypeFilter === "all"}
              onClick={() => onTxTypeFilterChange("all")}
            >
              All Types
            </Button>
            <Button variant={txTypeFilter === "debit" ? "primary" : "secondary"} size="sm" className={theme.segment}
              aria-pressed={txTypeFilter === "debit"}
              onClick={() => onTxTypeFilterChange("debit")}
            >
              Debit
            </Button>
            <Button variant={txTypeFilter === "credit" ? "primary" : "secondary"} size="sm" className={theme.segment}
              aria-pressed={txTypeFilter === "credit"}
              onClick={() => onTxTypeFilterChange("credit")}
            >
              Credit
            </Button>
          </div>

          <HorizonSelect className="sm:max-w-xs" aria-label="Filter by category"
            value={txCategoryFilter ?? ""}
            onChange={(e) => onTxCategoryFilterChange(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">All Categories</option>
            <CategoryOptions categories={categories} />
          </HorizonSelect>
        </div>

        {/* Search Input */}
        <HorizonSearch aria-label="Search transactions"
          placeholder="Search transactions..."
          value={searchQuery}
          onChange={(e) => onSearchQueryChange(e.target.value)}
        />
      </div>

      {/* Transactions List */}
      {transactions.length === 0 ? (
        <PulseCard className={`${theme.empty} text-center`}>
          <Icon name="receipt" className="mx-auto h-10 w-10 text-muted-foreground opacity-50" />
          <h3 className="mt-3 text-base font-semibold text-foreground">
            {hasFilters ? "No matching transactions found" : "No transactions recorded yet"}
          </h3>
          <p className="mt-1 text-sm text-muted-foreground max-w-md mx-auto">
            {hasFilters
              ? "Try adjusting your search or filters."
              : "Log a transaction to start tracking your spending."}
          </p>
          {!hasFilters && (
            <Button variant="primary" size="md"
              onClick={onOpenAddTransaction}
              className={`${theme.primary} mt-4`}
            >
              <Icon name="plus" className="h-4 w-4" /> Log First Transaction
            </Button>
          )}
        </PulseCard>
      ) : (
        <div className="space-y-3">
          {transactions.map((tx) => {
            const matchingCat = categories.find((c) => c.id === tx.category_id);
            const isCredit = tx.type === "credit";

            const formattedDate = tx.transaction_date
              ? formatInstant(tx.transaction_date, timeZone, {
                  dateStyle: "medium",
                  timeStyle: "short",
                })
              : "N/A";

            return (
              <PulseCard
                key={tx.id}
                className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-6"
              >
                <div className="flex items-start gap-3.5 min-w-0">
                  <div className={theme.iconTile}>
                    <Icon name="receipt" aria-hidden="true" className="size-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h4 className="font-bold text-foreground text-base truncate">{tx.name}</h4>
                      {tx.category_name && (
                        <span className={theme.chip} data-tone="dim">
                          {matchingCat?.color && (
                            <span aria-hidden="true" className="size-2 shrink-0 rounded-full" style={{ backgroundColor: matchingCat.color }} />
                          )}
                          {tx.category_name}
                        </span>
                      )}
                      <span
                        className={theme.chip}
                        data-tone={isCredit ? "good" : "bad"}
                      >
                        {isCredit ? "Credit" : "Debit"}
                      </span>
                      {tx.budget_name && (
                        <span className={theme.chip}>
                          <Icon name="wallet" className="h-3 w-3" /> {tx.budget_name}
                        </span>
                      )}
                    </div>

                    <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1" suppressHydrationWarning>
                        <Icon name="clock" className="h-3.5 w-3.5" /> {formattedDate}
                      </span>
                      {tx.notes && <span className="truncate italic max-w-xs">&quot;{tx.notes}&quot;</span>}
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-3 shrink-0">
                  <span
                    className={`text-lg font-extrabold tabular-nums ${
                      isCredit ? "text-success dark:text-success" : "text-foreground"
                    }`}
                    suppressHydrationWarning
                  >
                    {isCredit ? "+" : "-"}{formatMoney(tx.amount, summary.currency)}
                  </span>
                  <div className="flex items-center gap-1">
                    <Button variant="icon"
                      className={theme.iconAction}
                      onClick={() => onOpenEditTransaction(tx)}
                      title="Edit transaction"
                    >
                      <Icon name="edit" className="h-4 w-4" />
                    </Button>
                    <Button variant="iconDanger"
                      className={theme.dangerIcon}
                      onClick={() => onConfirmDeleteTransaction(tx)}
                      title="Delete transaction"
                    >
                      <Icon name="trash" className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </PulseCard>
            );
          })}

          {transactionPage.total_pages > 1 && (
            <div className="flex flex-wrap items-center justify-center gap-3 pt-3">
              <HorizonSelect compact aria-label="Transactions per page"
                value={transactionPage.page_size}
                onChange={(event) => onPageSizeChange(Number(event.target.value))}
                disabled={isPending}
              >
                <option value={10}>10 per page</option>
                <option value={25}>25 per page</option>
              </HorizonSelect>
              <nav className="flex flex-wrap items-center justify-center gap-2" aria-label="Transaction pages">
                {Array.from({ length: transactionPage.total_pages }, (_, index) => index + 1).map((page) => (
                <Button variant={page === transactionPage.page ? "primary" : "secondary"} size="sm"
                  className={page === transactionPage.page ? theme.primary : theme.softAction}
                  key={page}
                  type="button"
                  onClick={() => onPageChange(page)}
                  disabled={isPending || page === transactionPage.page}
                  aria-current={page === transactionPage.page ? "page" : undefined}
                >
                  {page}
                </Button>
                ))}
              </nav>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
