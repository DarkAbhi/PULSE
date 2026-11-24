"use client";

import { Icon } from "@/app/financial-horizon/components/horizon-icon";
import CategoryOptions from "@/app/financial-horizon/components/category-options";

import { HorizonSearch, HorizonSelect } from "@/app/financial-horizon/components/horizon-filters";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney } from "@/app/lib/currency.mjs";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/profile-preferences-provider";

import HorizonSegments from "@/app/financial-horizon/components/horizon-segments";
import HorizonAction from "@/app/financial-horizon/components/horizon-action";
import PulseIconButton from "@/app/components/design-system/pulse-icon-button";
import SectionHeader from "@/app/financial-horizon/components/section-header";
import HorizonTag from "@/app/financial-horizon/components/horizon-tag";
import HorizonEmpty from "@/app/financial-horizon/components/horizon-empty";
import HorizonIconTile from "@/app/financial-horizon/components/horizon-icon-tile";

import type { HorizonSummary, TransactionItem, CategoryItem } from "@/app/financial-horizon/horizon-types";
import type { TransactionPage } from "@/app/financial-horizon/horizon-types";

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

const transactionTypes = [
  { value: "all", label: "All Types" },
  { value: "debit", label: "Debit" },
  { value: "credit", label: "Credit" },
] as const;

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
      <SectionHeader title="Transaction History" icon="receipt">
        <div className="flex flex-wrap items-center gap-2">
          {onOpenStatementUpload && (
            <HorizonAction variant="chip"
              onClick={onOpenStatementUpload}
              disabled={isPending}
            >
              <Icon name="upload" /> Import Statement
            </HorizonAction>
          )}
          <HorizonAction variant="chip"
            onClick={onOpenAddCategory}
            disabled={isPending}
          >
            <Icon name="tag" className="text-muted-foreground" /> Add Category
          </HorizonAction>
          <HorizonAction
            onClick={onOpenAddTransaction}
            disabled={isPending}
          >
            <Icon name="plus" /> Add Transaction
          </HorizonAction>
        </div>
      </SectionHeader>

      {/* Filter and Search Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 flex-wrap">
          <HorizonSegments
            label="Transaction type"
            options={transactionTypes}
            value={txTypeFilter}
            onChange={onTxTypeFilterChange}
          />

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
        <HorizonEmpty>
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
            <HorizonAction
              onClick={onOpenAddTransaction}
              className="mt-4"
            >
              <Icon name="plus" /> Log First Transaction
            </HorizonAction>
          )}
        </HorizonEmpty>
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
                  <HorizonIconTile>
                    <Icon name="receipt" aria-hidden="true" className="size-5" />
                  </HorizonIconTile>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h4 className="font-bold text-foreground text-base truncate">{tx.name}</h4>
                      {tx.category_name && (
                        <HorizonTag tone="dim">
                          {matchingCat?.color && (
                            <span aria-hidden="true" className="size-2 shrink-0 rounded-full" style={{ backgroundColor: matchingCat.color }} />
                          )}
                          {tx.category_name}
                        </HorizonTag>
                      )}
                      <HorizonTag tone={isCredit ? "good" : "bad"}>
                        {isCredit ? "Credit" : "Debit"}
                      </HorizonTag>
                      {tx.budget_name && (
                        <HorizonTag>
                          <Icon name="wallet" className="h-3 w-3" /> {tx.budget_name}
                        </HorizonTag>
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
                    <PulseIconButton size="lg"
                      onClick={() => onOpenEditTransaction(tx)}
                      title="Edit transaction"
                      aria-label="Edit transaction"
                    >
                      <Icon name="edit" className="size-5" />
                    </PulseIconButton>
                    <PulseIconButton variant="danger" size="lg"
                      onClick={() => onConfirmDeleteTransaction(tx)}
                      title="Delete transaction"
                      aria-label="Delete transaction"
                    >
                      <Icon name="trash" className="size-5" />
                    </PulseIconButton>
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
                <HorizonAction variant={page === transactionPage.page ? "primary" : "chip"}
                  key={page}
                  type="button"
                  onClick={() => onPageChange(page)}
                  disabled={isPending || page === transactionPage.page}
                  aria-current={page === transactionPage.page ? "page" : undefined}
                >
                  {page}
                </HorizonAction>
                ))}
              </nav>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
