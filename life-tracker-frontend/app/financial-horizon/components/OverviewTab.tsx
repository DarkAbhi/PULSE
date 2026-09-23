"use client";

import Button from "../../components/design-system/button";

import {
  Target,
  Plus,
  Wallet,
  Edit2,
  Trash2,
  Receipt,
  ArrowRight,
  ArrowUpRight,
  Clock,
} from "lucide-react";
import {
  HorizonSummary,
  BudgetItem,
  TransactionItem,
  CategoryItem,
} from "../../dashboard/financial-horizon-card";
import { HorizonTab } from "./TabNavigation";

interface OverviewTabProps {
  summary: HorizonSummary;
  transactions: TransactionItem[];
  categories: CategoryItem[];
  uncommittedPool: number;
  onOpenAddBudget: () => void;
  onOpenEditBudget: (b: BudgetItem) => void;
  onConfirmDeleteBudget: (b: BudgetItem) => void;
  onNavigateTab: (tab: HorizonTab) => void;
  isPending?: boolean;
}

export default function OverviewTab({
  summary,
  transactions,
  categories,
  uncommittedPool,
  onOpenAddBudget,
  onOpenEditBudget,
  onConfirmDeleteBudget,
  onNavigateTab,
  isPending = false,
}: OverviewTabProps) {
  const recentTransactions = [...transactions]
    .sort((a, b) => {
      const dateA = a.transaction_date
        ? new Date(a.transaction_date).getTime()
        : 0;
      const dateB = b.transaction_date
        ? new Date(b.transaction_date).getTime()
        : 0;
      return dateB - dateA;
    })
    .slice(0, 5);

  return (
    <div className="space-y-10">
      {/* Active Monthly Budgets Section */}
      <section className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
          <div>
            <div className="flex items-center gap-2">
              <Target className="h-5 w-5 text-primary" />
              <h2 className="text-xl font-bold text-foreground">
                Active Monthly Budgets
              </h2>
              <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                1st – End of Month
              </span>
            </div>
            <p className="mt-1 text-sm text-muted-foreground">
              Set monthly spending allocations. Assigning fixed obligations
              automatically deducts from your available budget balance.
            </p>
          </div>

          <Button variant="soft" size="sm"
            onClick={onOpenAddBudget}
            disabled={isPending}
            className="shrink-0"
          >
            <Plus className="h-4 w-4" /> Add Monthly Budget
          </Button>
        </div>

        {(summary.budgets ?? []).length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border bg-card/50 p-10 text-center">
            <Target className="mx-auto h-10 w-10 text-muted-foreground opacity-50" />
            <h3 className="mt-3 text-base font-semibold text-foreground">
              No monthly budgets configured
            </h3>
            <p className="mt-1 text-sm text-muted-foreground max-w-md mx-auto">
              Create monthly budgets (e.g. Housing, Utilities, SIPs) to track
              allocations and link your fixed obligations.
            </p>
            <Button variant="primary" size="md"
              onClick={onOpenAddBudget}
              className="mt-4"
            >
              <Plus className="h-4 w-4" /> Add Your First Budget
            </Button>
          </div>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {(summary.budgets ?? []).map((b) => {
              const linkedDeductions = summary.deductions.filter(
                (d) => d.budget_id === b.id,
              );
              const isOverBudget = b.available_amount < 0;
              const pct = Math.min(100, Math.max(0, b.usage_percentage));

              return (
                <div
                  key={b.id}
                  className="group relative rounded-2xl border border-border bg-card p-5 shadow-sm flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2.5">
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
                          <Wallet className="h-4 w-4" />
                        </div>
                        <div>
                          <h4 className="font-bold text-foreground text-base leading-tight">
                            {b.name}
                          </h4>
                          <span className="text-xs text-muted-foreground">
                            {linkedDeductions.length} linked obligation
                            {linkedDeductions.length === 1 ? "" : "s"}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        <Button variant="icon"
                          onClick={() => onOpenEditBudget(b)}
                          title="Edit Budget"
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                        </Button>
                        <Button variant="iconDanger"
                          onClick={() => onConfirmDeleteBudget(b)}
                          title="Delete Budget"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </div>

                    {/* Amounts Display */}
                    <div className="mt-4 grid grid-cols-2 gap-3 rounded-xl bg-secondary/30 p-3">
                      <div>
                        <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                          Allocated
                        </span>
                        <p className="text-sm font-bold text-foreground mt-0.5">
                          {summary.currency}
                          {b.allocated_amount.toLocaleString("en-IN", {
                            minimumFractionDigits: 2,
                          })}
                        </p>
                      </div>
                      <div>
                        <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                          Committed/Used
                        </span>
                        <p className="text-sm font-bold text-amber-600 dark:text-amber-400 mt-0.5">
                          {summary.currency}
                          {b.used_amount.toLocaleString("en-IN", {
                            minimumFractionDigits: 2,
                          })}
                        </p>
                      </div>
                    </div>

                    {/* Usage Progress Bar */}
                    <div className="mt-4 space-y-1.5">
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-muted-foreground font-medium">
                          Available Balance
                        </span>
                        <span
                          className={`font-bold ${
                            isOverBudget
                              ? "text-rose-600 dark:text-rose-400"
                              : "text-emerald-600 dark:text-emerald-400"
                          }`}
                        >
                          {summary.currency}
                          {b.available_amount.toLocaleString("en-IN", {
                            minimumFractionDigits: 2,
                          })}
                        </span>
                      </div>
                      <div className="h-2 w-full rounded-full bg-secondary overflow-hidden">
                        <div
                          className={`h-full transition-all duration-500 rounded-full ${
                            isOverBudget
                              ? "bg-rose-500"
                              : pct > 80
                                ? "bg-amber-500"
                                : "bg-emerald-500"
                          }`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <div className="flex justify-end">
                        <span className="text-[11px] text-muted-foreground font-semibold">
                          {b.usage_percentage.toFixed(1)}% committed
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Linked items chips */}
                  {linkedDeductions.length > 0 && (
                    <div className="mt-4 border-t border-border/50 pt-3">
                      <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                        Linked Obligations:
                      </span>
                      <div className="mt-1.5 flex flex-wrap gap-1.5">
                        {linkedDeductions.map((item) => (
                          <span
                            key={item.id}
                            className="inline-flex items-center gap-1 rounded-md bg-background border border-border px-2 py-0.5 text-[11px] font-medium text-foreground"
                          >
                            {item.name} ({summary.currency}
                            {item.amount})
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Recent Activity Section */}
      <section className="rounded-3xl border border-border bg-card p-6 shadow-sm space-y-5">
        <div className="flex items-center justify-between border-b border-border pb-4">
          <div className="flex items-center gap-2">
            <Receipt className="h-5 w-5 text-primary" />
            <h2 className="text-xl font-bold text-foreground">
              Recent Activity
            </h2>
          </div>
          <Button variant="tertiary" size="sm"
            onClick={() => onNavigateTab("transactions")}
          >
            <span>View All ({transactions.length})</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </Button>
        </div>

        {recentTransactions.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
            No recent transactions logged.
          </div>
        ) : (
          <div className="space-y-3">
            {recentTransactions.map((tx) => {
              const matchingCat = categories.find(
                (c) => c.id === tx.category_id || c.name === tx.category_name,
              );
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
                  className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-background p-3.5 shadow-xs transition hover:shadow-md"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-white font-bold shadow-xs"
                      style={{ backgroundColor: catColor }}
                    >
                      <Receipt className="h-4.5 w-4.5" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h4 className="font-bold text-foreground text-sm truncate">
                          {tx.name}
                        </h4>
                        <span
                          className="inline-flex items-center rounded-full px-2 py-0.2 text-[10px] font-semibold border"
                          style={{
                            backgroundColor: `${catColor}15`,
                            color: catColor,
                            borderColor: `${catColor}30`,
                          }}
                        >
                          {tx.category_name}
                        </span>
                        <span
                          className={`inline-flex items-center rounded-full px-2 py-0.2 text-[10px] font-semibold border ${
                            isCredit
                              ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400"
                              : "bg-rose-500/10 border-rose-500/30 text-rose-600 dark:text-rose-400"
                          }`}
                        >
                          {isCredit ? "Credit" : "Debit"}
                        </span>
                      </div>
                      <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                        <span
                          className="flex items-center gap-1"
                          suppressHydrationWarning
                        >
                          <Clock className="h-3 w-3" /> {formattedDate}
                        </span>
                      </div>
                    </div>
                  </div>

                  <span
                    className={`font-extrabold text-base shrink-0 ${
                      isCredit
                        ? "text-emerald-600 dark:text-emerald-400"
                        : "text-foreground"
                    }`}
                    suppressHydrationWarning
                  >
                    {isCredit ? "+" : "-"}
                    {summary.currency}
                    {tx.amount.toLocaleString("en-IN", {
                      minimumFractionDigits: 2,
                    })}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Future Horizon Projections Section */}
      <section className="rounded-3xl border border-border bg-gradient-to-br from-card via-card to-primary/5 p-8 shadow-sm">
        <div className="flex items-center gap-3 mb-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <ArrowUpRight className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-foreground">
              Future Horizon Projections
            </h2>
            <p className="text-sm text-muted-foreground">
              Projected uncommitted cash pool accumulation based on maintaining
              your baseline uncommitted cash flow (fixed obligations &amp;
              subscriptions only).
            </p>
          </div>
        </div>

        <div className="mt-6 grid gap-5 sm:grid-cols-3">
          {[3, 6, 12].map((months) => {
            const label = months === 12 ? "1 Year" : `${months} Months`;
            const projectedReserve = Math.max(0, uncommittedPool * months);

            return (
              <div
                key={months}
                className="rounded-2xl border border-border bg-card/80 p-6 shadow-sm backdrop-blur-xs"
              >
                <div className="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  <span>{label} Horizon</span>
                  <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-emerald-600 dark:text-emerald-400 font-bold">
                    {months} Months
                  </span>
                </div>

                <p className="mt-4 text-2xl font-extrabold text-foreground">
                  {summary.currency}
                  {projectedReserve.toLocaleString("en-IN", {
                    minimumFractionDigits: 2,
                  })}
                </p>

                <p className="mt-2 text-xs text-muted-foreground">
                  Estimated net uncommitted reserve pool accumulated over{" "}
                  {months} months.
                </p>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
