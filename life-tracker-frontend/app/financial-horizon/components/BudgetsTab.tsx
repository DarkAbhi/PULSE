"use client";

import { Icon } from "./HorizonIcon";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney } from "@/app/lib/currency.mjs";

import Button from "@/app/components/design-system/button";
import theme from "../horizon-theme.module.css";

import {
  type HorizonSummary,
  type BudgetItem,
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
  const budgets = summary.budgets ?? [];
  const money = (amount: number) => formatMoney(amount, summary.currency);

  return (
    <section className="space-y-5">
      <div className={theme.sectionHeader}>
        <div>
          <div className="flex flex-wrap items-center gap-2.5">
            <Icon name="target" aria-hidden="true" className={`size-6 ${theme.sectionIcon}`} />
            <h2 className={theme.sectionTitle}>
              Active Monthly Budgets
            </h2>
            <span className={`${theme.badge} ${theme.budgetPeriod}`} data-tone="warn">
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

      {budgets.length === 0 ? (
        <p className={`${theme.empty} text-center text-muted-foreground`}>
          No budgets yet. Add one to start tracking an allocation.
        </p>
      ) : (
        <div className={theme.budgetGrid}>
          {budgets.map((budget) => {
            const linkedDeductions = summary.deductions.filter(
              (deduction) => deduction.budget_id === budget.id,
            );
            const linkedCount = `${linkedDeductions.length} linked obligation${linkedDeductions.length === 1 ? "" : "s"}`;
            const overBudget = budget.available_amount < 0;
            const usage = budget.usage_percentage;
            const committed = usage.toFixed(1);
            const progressStyle = usage > 100 ? theme.budgetOver
              : usage > 85 ? theme.budgetHigh
              : usage > 60 ? theme.budgetMedium
              : theme.budgetLow;

            return (
              <PulseCard as="article" key={budget.id} className={theme.budgetCard}>
                <header className={theme.budgetHeader}>
                  <span className={`${theme.iconTile} ${theme.budgetIcon}`}>
                    <Icon name="wallet" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <h3 className={theme.budgetTitle}>{budget.name}</h3>
                    {linkedDeductions.length > 0 ? (
                      <details className={theme.budgetLinks}>
                        <summary>{linkedCount}</summary>
                        <ul className="mt-1.5 space-y-1">
                          {linkedDeductions.map((item) => (
                            <li key={item.id}>{item.name} ({money(item.amount)})</li>
                          ))}
                        </ul>
                      </details>
                    ) : (
                      <p className={theme.budgetLinks}>{linkedCount}</p>
                    )}
                  </div>
                  <div className={theme.budgetActions}>
                    <Button
                      variant="icon"
                      className={theme.iconAction}
                      onClick={() => onOpenEditBudget(budget)}
                      aria-label="Edit Budget"
                      title="Edit Budget"
                    >
                      <Icon name="edit" />
                    </Button>
                    <Button
                      variant="iconDanger"
                      className={theme.dangerIcon}
                      onClick={() => onConfirmDeleteBudget(budget)}
                      aria-label="Delete Budget"
                      title="Delete Budget"
                    >
                      <Icon name="trash" />
                    </Button>
                  </div>
                </header>

                <dl className={theme.budgetAmounts}>
                  <div>
                    <dt>Allocated</dt>
                    <dd>{money(budget.allocated_amount)}</dd>
                  </div>
                  <div>
                    <dt>Committed/Used</dt>
                    <dd className={budget.used_amount !== 0 ? theme.budgetUsed : undefined}>
                      {money(budget.used_amount)}
                    </dd>
                  </div>
                </dl>

                <div>
                  <div className={theme.budgetBalance}>
                    <span>{overBudget ? "Over budget by" : "Available Balance"}</span>
                    <b className={overBudget ? theme.budgetDeficit : theme.budgetAvailable}>
                      {money(Math.abs(budget.available_amount))}
                    </b>
                  </div>
                  <div role="img" aria-label={`${committed} percent committed`} className={theme.budgetTrack}>
                    <div className={progressStyle} style={{ width: `${Math.min(100, Math.max(0, usage))}%` }} />
                  </div>
                  <p className={theme.budgetPercent}>{committed}% committed</p>
                </div>
              </PulseCard>
            );
          })}
        </div>
      )}
    </section>
  );
}
