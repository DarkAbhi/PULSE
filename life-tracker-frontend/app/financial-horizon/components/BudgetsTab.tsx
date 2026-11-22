"use client";

import { Icon } from "./HorizonIcon";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney, currencySymbol } from "@/app/lib/currency.mjs";

import Button from "@/app/components/design-system/button";
import theme from "../horizon-theme.module.css";

import {
  HorizonSummary,
  BudgetItem,
} from "@/app/dashboard/financial-horizon-card";

interface BudgetsTabProps {
  summary: HorizonSummary;
  onOpenAddBudget: () => void;
  onOpenEditBudget: (b: BudgetItem) => void;
  onConfirmDeleteBudget: (b: BudgetItem) => void;
  isPending?: boolean;
}

export default function BudgetsTab({
  summary,
  onOpenAddBudget,
  onOpenEditBudget,
  onConfirmDeleteBudget,
  isPending = false,
}: BudgetsTabProps) {
  return (
    <section className="space-y-6">
      <div className={theme.sectionHeader}>
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <Icon name="target" aria-hidden="true" className={`size-6 ${theme.sectionIcon}`} />
            <h2 className={theme.sectionTitle}>
              Active Monthly Budgets
            </h2>
            <span className={theme.badge}>
              1st – End of Month
            </span>
          </div>
          <p className={theme.sectionDescription}>
            Set monthly spending allocations. Assigning fixed obligations
            automatically deducts from your available budget balance.
          </p>
        </div>

        <Button variant="primary" size="sm"
          onClick={onOpenAddBudget}
          disabled={isPending}
          className={`${theme.primary} shrink-0`}
        >
          <Icon name="plus" className="h-4 w-4" /> Add Monthly Budget
        </Button>
      </div>

      {(summary.budgets ?? []).length === 0 ? (
        <PulseCard className={`${theme.empty} text-center`}>
          <Icon name="target" className="mx-auto h-10 w-10 text-muted-foreground opacity-50" />
          <h3 className="mt-3 text-base font-semibold text-foreground">
            No monthly budgets configured
          </h3>
          <p className="mt-1 text-sm text-muted-foreground max-w-md mx-auto">
            Create monthly budgets (e.g. Housing, Utilities, SIPs) to track
            allocations and link your fixed obligations.
          </p>
          <Button variant="primary" size="md"
            onClick={onOpenAddBudget}
            className={`${theme.primary} mt-4`}
          >
            <Icon name="plus" className="h-4 w-4" /> Add Your First Budget
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
            const progressStyle = isOverBudget ? "bg-destructive"
              : pct > 85 ? theme.budgetHigh
              : pct > 60 ? theme.budgetMedium
              : theme.budgetLow;

            return (
              <PulseCard
                key={b.id}
                className="group relative p-6 flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className={theme.iconTile}>
                        <Icon name="wallet" className="h-4 w-4" />
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
                        className={theme.iconAction}
                        onClick={() => onOpenEditBudget(b)}
                        title="Edit Budget"
                      >
                        <Icon name="edit" className="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="iconDanger"
                        className={theme.dangerIcon}
                        onClick={() => onConfirmDeleteBudget(b)}
                        title="Delete Budget"
                      >
                        <Icon name="trash" className="h-3.5 w-3.5" />
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
                    <div aria-hidden="true" className="h-2.5 w-full overflow-hidden rounded-full bg-secondary">
                      <div
                        className={`${progressStyle} h-full rounded-full transition-[width] duration-500 motion-reduce:transition-none`}
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
                  <div className="mt-4 border-t border-divider pt-3">
                    <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                      Linked Obligations:
                    </span>
                    <div className="mt-1.5 flex flex-wrap gap-1.5">
                      {linkedDeductions.map((item) => (
                        <span
                          key={item.id}
                          className={theme.chip}
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
  );
}
