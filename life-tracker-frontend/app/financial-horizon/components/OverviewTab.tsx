"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney, currencySymbol } from "@/app/lib/currency.mjs";

import Button from "@/app/components/design-system/button";

import {
  Target,
  Plus,
  Wallet,
  Edit2,
  Trash2,
  ArrowUpRight,
  Info,
} from "lucide-react";
import {
  HorizonSummary,
  BudgetItem,
} from "@/app/dashboard/financial-horizon-card";

interface OverviewTabProps {
  summary: HorizonSummary;
  uncommittedPool: number;
  onOpenAddBudget: () => void;
  onOpenEditBudget: (b: BudgetItem) => void;
  onConfirmDeleteBudget: (b: BudgetItem) => void;
  isPending?: boolean;
}

export default function OverviewTab({
  summary,
  uncommittedPool,
  onOpenAddBudget,
  onOpenEditBudget,
  onConfirmDeleteBudget,
  isPending = false,
}: OverviewTabProps) {

  return (
    <div className="space-y-10">
      {/* Active Monthly Budgets Section */}
      <section className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-divider pb-4">
          <div>
            <div className="flex items-center gap-2">
              <Target className="h-5 w-5 text-action" />
              <h2 className="text-xl font-bold text-foreground">
                Active Monthly Budgets
              </h2>
              <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-action">
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
          <PulseCard className="p-10 text-center">
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
          </PulseCard>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {(summary.budgets ?? []).map((b) => {
              const linkedDeductions = summary.deductions.filter(
                (d) => d.budget_id === b.id,
              );
              const isOverBudget = b.available_amount < 0;
              const pct = Math.min(100, Math.max(0, b.usage_percentage));

              return (
                <PulseCard
                  key={b.id}
                  className="group relative p-5 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2.5">
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-action">
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
                          {formatMoney(b.allocated_amount, summary.currency)}
                        </p>
                      </div>
                      <div>
                        <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                          Committed/Used
                        </span>
                        <p className="text-sm font-bold text-warning mt-0.5">
                          {formatMoney(b.used_amount, summary.currency)}
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
                              ? "text-destructive dark:text-destructive"
                              : "text-success dark:text-success"
                          }`}
                        >
                          {formatMoney(b.available_amount, summary.currency)}
                        </span>
                      </div>
                      <div className="h-2 w-full rounded-full bg-secondary overflow-hidden">
                        <div
                          className={`h-full transition-all duration-500 rounded-full ${
                            isOverBudget
                              ? "bg-destructive"
                              : pct > 80
                                ? "bg-warning"
                                : "bg-success"
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
                    <div className="mt-4 border-t border-divider/50 pt-3">
                      <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                        Linked Obligations:
                      </span>
                      <div className="mt-1.5 flex flex-wrap gap-1.5">
                        {linkedDeductions.map((item) => (
                          <span
                            key={item.id}
                            className="inline-flex items-center gap-1 rounded-md bg-background px-2 py-0.5 text-[11px] font-medium text-foreground"
                          >
                            {item.name} ({currencySymbol(summary.currency)}
                            {item.amount})
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </PulseCard>
              );
            })}
          </div>
        )}
      </section>

      <PulseCard as="section" aria-labelledby="horizon-projections-title" className="p-4 sm:p-6">
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-action">
            <ArrowUpRight aria-hidden="true" className="h-5 w-5" />
          </div>
          <div>
            <h2 id="horizon-projections-title" className="text-lg font-bold text-foreground sm:text-xl">
              Future Horizon Projections
            </h2>
            <p className="text-sm text-muted-foreground">Estimated cash you could set aside</p>
          </div>
        </div>

        <dl className="mt-5 divide-y divide-divider rounded-xl bg-background/50 sm:grid sm:grid-cols-3 sm:divide-x sm:divide-y-0">
          {[3, 6, 12].map((months) => (
            <div key={months} className="flex min-w-0 flex-wrap items-baseline justify-between gap-x-3 gap-y-1 p-4 sm:block">
              <dt className="text-sm font-medium text-muted-foreground">{months} months</dt>
              <dd className="min-w-0 wrap-anywhere text-xl font-bold tracking-tight text-foreground tabular-nums sm:mt-1 sm:text-2xl">
                {formatMoney(Math.max(0, uncommittedPool * months), summary.currency)}
              </dd>
            </div>
          ))}
        </dl>

        <div className="mt-4 flex items-start gap-2.5 rounded-xl bg-secondary/50 p-3 text-sm">
          <Info aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-action" />
          <div>
            <p className="font-semibold text-foreground">Projection assumptions</p>
            <p className="mt-0.5 text-muted-foreground">
              Estimates assume the same monthly income, fixed obligations, and subscriptions.
              Allow separately for day-to-day spending and planned purchases. A monthly shortfall is shown as zero savings.
            </p>
          </div>
        </div>
      </PulseCard>
    </div>
  );
}
