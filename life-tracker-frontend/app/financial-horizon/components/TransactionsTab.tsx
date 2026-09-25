"use client";

import Button from "../../components/design-system/button";

import { Receipt, Plus, Tag, Search, Edit2, Trash2, Clock, Wallet, FileUp } from "lucide-react";
import { HorizonSummary, TransactionItem, CategoryItem } from "../../dashboard/financial-horizon-card";
import type { TransactionPage } from "../actions";

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
  txCategoryFilter: string;
  onTxCategoryFilterChange: (v: string) => void;
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
  // Category filter is still applied client-side (it's not sent to the API)
  // because categories are metadata on already-fetched records.
  const filteredTransactions = transactions.filter((t) => {
    return txCategoryFilter === "all" || t.category_name === txCategoryFilter;
  });

  return (
    <section className="space-y-6">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
        <div>
          <div className="flex items-center gap-2">
            <Receipt className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-bold text-foreground">Transaction History</h2>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap sm:flex-nowrap">
          {onOpenStatementUpload && (
            <Button variant="soft" size="sm"
              onClick={onOpenStatementUpload}
              disabled={isPending}
            >
              <FileUp className="h-3.5 w-3.5" /> Import Statement
            </Button>
          )}
          <Button variant="secondary" size="sm"
            onClick={onOpenAddCategory}
            disabled={isPending}
          >
            <Tag className="h-3.5 w-3.5 text-muted-foreground" /> Add Category
          </Button>
          <Button variant="primary" size="sm"
            onClick={onOpenAddTransaction}
            disabled={isPending}
          >
            <Plus className="h-4 w-4" /> Add Transaction
          </Button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 flex-wrap">
          {/* Transaction Type Filter Segment */}
          <div className="inline-flex rounded-xl border border-border bg-secondary/30 p-1 text-xs">
            <Button variant={txTypeFilter === "all" ? "primary" : "secondary"} size="sm"
              onClick={() => onTxTypeFilterChange("all")}
            >
              All Types
            </Button>
            <Button variant={txTypeFilter === "debit" ? "primary" : "secondary"} size="sm"
              onClick={() => onTxTypeFilterChange("debit")}
            >
              Debit
            </Button>
            <Button variant={txTypeFilter === "credit" ? "primary" : "secondary"} size="sm"
              onClick={() => onTxTypeFilterChange("credit")}
            >
              Credit
            </Button>
          </div>

          {/* Categories Pill List */}
          <div className="flex items-center gap-2 overflow-x-auto pb-2 sm:pb-0 scrollbar-none max-w-full sm:max-w-xl">
            <Button variant={txCategoryFilter === "all" ? "primary" : "secondary"} size="sm"
              onClick={() => onTxCategoryFilterChange("all")}
              className="shrink-0"
            >
              All Categories
            </Button>
            {categories.map((cat) => {
              const count = transactions.filter((t) => t.category_id === cat.id || t.category_name === cat.name).length;
              const isSelected = txCategoryFilter === cat.name;
              return (
                <Button variant={isSelected ? "soft" : "secondary"} size="sm"
                  key={cat.id}
                  onClick={() => onTxCategoryFilterChange(isSelected ? "all" : cat.name)}
                  className="shrink-0"
                >
                  <span
                    className="h-2 w-2 rounded-full"
                    style={{ backgroundColor: cat.color || "#64748b" }}
                  />
                  <span>{cat.name}</span>
                  {count > 0 && (
                    <span className="ml-0.5 rounded-md bg-secondary px-1.5 py-0.2 text-[10px] font-bold">
                      {count}
                    </span>
                  )}
                </Button>
              );
            })}
          </div>
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-64 shrink-0">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <input
            type="text"
            placeholder="Search transactions..."
            value={searchQuery}
            onChange={(e) => onSearchQueryChange(e.target.value)}
            className="w-full rounded-xl border border-border bg-background pl-9 pr-3 py-1.5 text-xs text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
      </div>

      {/* Transactions List */}
      {filteredTransactions.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-card/50 p-10 text-center">
          <Receipt className="mx-auto h-10 w-10 text-muted-foreground opacity-50" />
          <h3 className="mt-3 text-base font-semibold text-foreground">
            {transactions.length === 0 ? "No transactions recorded yet" : "No matching transactions found"}
          </h3>
          <p className="mt-1 text-sm text-muted-foreground max-w-md mx-auto">
            {transactions.length === 0
              ? "Log your financial transactions to track actual spending against pre-filled categories and monthly budgets."
              : "Try adjusting your search or filters to find what you're looking for."}
          </p>
          {transactions.length === 0 && (
            <Button variant="primary" size="md"
              onClick={onOpenAddTransaction}
              className="mt-4"
            >
              <Plus className="h-4 w-4" /> Log First Transaction
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {filteredTransactions.map((tx) => {
            const matchingCat = categories.find((c) => c.id === tx.category_id || c.name === tx.category_name);
            const catColor = matchingCat?.color || "#64748b";
            const isCredit = tx.type === "credit";

            const formattedDate = tx.transaction_date
              ? new Date(tx.transaction_date).toLocaleString("en-IN", {
                  dateStyle: "medium",
                  timeStyle: "short",
                })
              : "N/A";

            return (
              <div
                key={tx.id}
                className="group flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-2xl border border-border bg-card p-4 shadow-xs transition hover:shadow-md"
              >
                <div className="flex items-start gap-3.5 min-w-0">
                  <div
                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-white font-bold shadow-xs mt-0.5 sm:mt-0"
                    style={{ backgroundColor: catColor }}
                  >
                    <Receipt className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h4 className="font-bold text-foreground text-base truncate">{tx.name}</h4>
                      <span
                        className="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold border"
                        style={{
                          backgroundColor: `${catColor}15`,
                          color: catColor,
                          borderColor: `${catColor}30`,
                        }}
                      >
                        {tx.category_name}
                      </span>
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold border ${
                          isCredit
                            ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400"
                            : "bg-rose-500/10 border-rose-500/30 text-rose-600 dark:text-rose-400"
                        }`}
                      >
                        {isCredit ? "Credit" : "Debit"}
                      </span>
                      {tx.budget_name && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 border border-primary/20 px-2 py-0.5 text-[11px] font-medium text-primary">
                          <Wallet className="h-3 w-3" /> {tx.budget_name}
                        </span>
                      )}
                    </div>

                    <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1" suppressHydrationWarning>
                        <Clock className="h-3.5 w-3.5" /> {formattedDate}
                      </span>
                      {tx.notes && <span className="truncate italic max-w-xs">&quot;{tx.notes}&quot;</span>}
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-3 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-border/50">
                  <span
                    className={`font-extrabold text-lg ${
                      isCredit ? "text-emerald-600 dark:text-emerald-400" : "text-foreground"
                    }`}
                    suppressHydrationWarning
                  >
                    {isCredit ? "+" : "-"}{summary.currency}{tx.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                  </span>
                  <div className="flex items-center gap-1">
                    <Button variant="icon"
                      onClick={() => onOpenEditTransaction(tx)}
                      title="Edit transaction"
                    >
                      <Edit2 className="h-4 w-4" />
                    </Button>
                    <Button variant="iconDanger"
                      onClick={() => onConfirmDeleteTransaction(tx)}
                      title="Delete transaction"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            );
          })}

          {transactionPage.total_pages > 1 && (
            <div className="flex flex-wrap items-center justify-center gap-3 pt-3">
              <label className="text-xs text-muted-foreground">
                <span className="sr-only">Transactions per page</span>
                <select
                  value={transactionPage.page_size}
                  onChange={(event) => onPageSizeChange(Number(event.target.value))}
                  disabled={isPending}
                  className="rounded-lg border border-border bg-card px-2 py-1.5 text-foreground"
                >
                  <option value={10}>10 per page</option>
                  <option value={25}>25 per page</option>
                </select>
              </label>
              <nav className="flex flex-wrap items-center justify-center gap-2" aria-label="Transaction pages">
                {Array.from({ length: transactionPage.total_pages }, (_, index) => index + 1).map((page) => (
                <Button variant={page === transactionPage.page ? "primary" : "secondary"} size="sm"
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
