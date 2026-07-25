"use client";

import { useState } from "react";
import { Receipt, Plus, Tag, Search, Edit2, Trash2, Clock, Wallet, FileUp } from "lucide-react";
import { HorizonSummary, TransactionItem, CategoryItem } from "../../dashboard/financial-horizon-card";

interface TransactionsTabProps {
  summary: HorizonSummary;
  transactions: TransactionItem[];
  categories: CategoryItem[];
  onOpenAddTransaction: () => void;
  onOpenEditTransaction: (tx: TransactionItem) => void;
  onConfirmDeleteTransaction: (tx: TransactionItem) => void;
  onOpenAddCategory: () => void;
  onOpenStatementUpload?: () => void;
  isPending?: boolean;
}

export default function TransactionsTab({
  summary,
  transactions,
  categories,
  onOpenAddTransaction,
  onOpenEditTransaction,
  onConfirmDeleteTransaction,
  onOpenAddCategory,
  onOpenStatementUpload,
  isPending = false,
}: TransactionsTabProps) {
  const [txCategoryFilter, setTxCategoryFilter] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState<string>("");

  const filteredTransactions = transactions.filter((t) => {
    const matchesCategory = txCategoryFilter === "all" || t.category_name === txCategoryFilter;
    const matchesSearch =
      !searchQuery.trim() ||
      t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (t.notes && t.notes.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesCategory && matchesSearch;
  });

  return (
    <section className="space-y-6">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
        <div>
          <div className="flex items-center gap-2">
            <Receipt className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-bold text-foreground">Transaction History</h2>
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400">
              {transactions.length} Logged
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Log day-to-day transaction records with date & time, category, and optional budget allocations.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap sm:flex-nowrap">
          {onOpenStatementUpload && (
            <button
              onClick={onOpenStatementUpload}
              disabled={isPending}
              className="inline-flex items-center gap-1.5 rounded-xl border border-primary/30 bg-primary/10 px-3.5 py-2 text-xs font-semibold text-primary transition hover:bg-primary/20"
            >
              <FileUp className="h-3.5 w-3.5" /> Import Statement
            </button>
          )}
          <button
            onClick={onOpenAddCategory}
            disabled={isPending}
            className="inline-flex items-center gap-1.5 rounded-xl border border-border bg-card px-3.5 py-2 text-xs font-semibold text-foreground transition hover:bg-secondary"
          >
            <Tag className="h-3.5 w-3.5 text-muted-foreground" /> Add Category
          </button>
          <button
            onClick={onOpenAddTransaction}
            disabled={isPending}
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow transition hover:opacity-90 active:scale-95"
          >
            <Plus className="h-4 w-4" /> Add Transaction
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        {/* Categories Pill List */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 sm:pb-0 scrollbar-none max-w-full sm:max-w-2xl">
          <button
            onClick={() => setTxCategoryFilter("all")}
            className={`rounded-xl px-3 py-1.5 text-xs font-medium transition ${
              txCategoryFilter === "all"
                ? "bg-primary text-primary-foreground font-semibold shadow-xs"
                : "bg-secondary/60 text-muted-foreground hover:bg-secondary hover:text-foreground"
            }`}
          >
            All Categories
          </button>
          {categories.map((cat) => {
            const count = transactions.filter((t) => t.category_id === cat.id || t.category_name === cat.name).length;
            const isSelected = txCategoryFilter === cat.name;
            return (
              <button
                key={cat.id}
                onClick={() => setTxCategoryFilter(isSelected ? "all" : cat.name)}
                className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs transition border shrink-0 ${
                  isSelected
                    ? "border-primary bg-primary/10 text-primary font-semibold"
                    : "border-border/60 bg-card text-muted-foreground hover:border-border hover:text-foreground"
                }`}
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
              </button>
            );
          })}
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-64 shrink-0">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search transactions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
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
              : "Try adjusting your search or category filters to find what you're looking for."}
          </p>
          {transactions.length === 0 && (
            <button
              onClick={onOpenAddTransaction}
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow hover:opacity-90"
            >
              <Plus className="h-4 w-4" /> Log First Transaction
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {filteredTransactions.map((tx) => {
            const matchingCat = categories.find((c) => c.id === tx.category_id || c.name === tx.category_name);
            const catColor = matchingCat?.color || "#64748b";

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
                  <span className="font-extrabold text-foreground text-lg" suppressHydrationWarning>
                    {summary.currency}{tx.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                  </span>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => onOpenEditTransaction(tx)}
                      className="rounded-lg p-1.5 text-muted-foreground hover:bg-secondary hover:text-foreground transition"
                      title="Edit transaction"
                    >
                      <Edit2 className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => onConfirmDeleteTransaction(tx)}
                      className="rounded-lg p-1.5 text-muted-foreground hover:bg-rose-500/10 hover:text-rose-500 transition"
                      title="Delete transaction"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
