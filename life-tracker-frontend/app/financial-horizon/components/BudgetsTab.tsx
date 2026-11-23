"use client";

import { Icon } from "@/app/financial-horizon/components/HorizonIcon";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney } from "@/app/lib/currency.mjs";

import HorizonAction from "@/app/financial-horizon/components/HorizonAction";
import Button from "@/app/components/design-system/button";
import SectionHeader from "@/app/financial-horizon/components/SectionHeader";
import HorizonTag from "@/app/financial-horizon/components/HorizonTag";
import HorizonEmpty from "@/app/financial-horizon/components/HorizonEmpty";
import HorizonIconTile from "@/app/financial-horizon/components/HorizonIconTile";

import {
  type HorizonSummary,
  type BudgetItem,
} from "@/app/dashboard/financial-horizon-card";

const budgetLinks = "mt-0.5 text-sm text-muted-foreground wrap-anywhere";

function BudgetAmount({ label, amount, emphasized }: { label: string; amount: string; emphasized?: boolean }) {
  return (
    <div className="px-[clamp(0.5rem,3.5cqi,1rem)] py-3.5">
      <dt className="text-caption font-bold tracking-wider text-muted-foreground uppercase">{label}</dt>
      <dd className={`mt-0.5 text-section font-extrabold wrap-anywhere ${emphasized ? "text-(--finance-budget-warning)" : ""}`}>
        {amount}
      </dd>
    </div>
  );
}

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
    <section className="@container/budgets space-y-5">
      <SectionHeader
        title="Active Monthly Budgets"
        icon="target"
        description="Set monthly spending allocations. Assigning fixed obligations automatically deducts from your available budget balance."
        badges={
          <HorizonTag emphasis tone="warn" className="data-[tone=warn]:text-(--finance-budget-warning) whitespace-nowrap">
            1st – End of Month
          </HorizonTag>
        }
      >
        <HorizonAction
          onClick={onOpenAddBudget}
          disabled={isPending}
          className="shrink-0"
        >
          <Icon name="plus" /> Add Monthly Budget
        </HorizonAction>
      </SectionHeader>

      {budgets.length === 0 ? (
        <HorizonEmpty as="p" className="text-muted-foreground">
          No budgets yet. Add one to start tracking an allocation.
        </HorizonEmpty>
      ) : (
        <div className="grid grid-cols-[minmax(0,var(--finance-budget-card-max))] gap-5
          @min-[740px]/budgets:grid-cols-[repeat(2,minmax(0,var(--finance-budget-card-max)))]
          @min-[1120px]/budgets:grid-cols-[repeat(3,minmax(0,var(--finance-budget-card-max)))]"
        >
          {budgets.map((budget) => {
            const linkedDeductions = summary.deductions.filter(
              (deduction) => deduction.budget_id === budget.id,
            );
            const linkedCount = `${linkedDeductions.length} linked obligation${linkedDeductions.length === 1 ? "" : "s"}`;
            const overBudget = budget.available_amount < 0;
            const usage = budget.usage_percentage;
            const committed = usage.toFixed(1);
            const progressFill = usage > 100 ? "bg-(--finance-budget-deficit)"
              : usage > 85 ? "bg-(image:--finance-budget-high)"
              : usage > 60 ? "bg-(image:--finance-budget-medium)"
              : "bg-(image:--finance-budget-low)";

            return (
              <PulseCard as="article" key={budget.id} className="@container grid grid-rows-[auto_1fr_auto] gap-4 p-6">
                <header className="flex min-h-11 items-start gap-3 @max-[300px]:grid @max-[300px]:grid-cols-[auto_minmax(0,1fr)] @max-[300px]:gap-y-1">
                  <HorizonIconTile>
                    <Icon name="wallet" />
                  </HorizonIconTile>
                  <div className="min-w-0 flex-1">
                    <h3 className="text-section font-extrabold tracking-tight leading-tight wrap-anywhere">{budget.name}</h3>
                    {linkedDeductions.length > 0 ? (
                      <details className={budgetLinks}>
                        <summary className="w-fit cursor-pointer">{linkedCount}</summary>
                        <ul className="mt-1.5 space-y-1">
                          {linkedDeductions.map((item) => (
                            <li key={item.id}>{item.name} ({money(item.amount)})</li>
                          ))}
                        </ul>
                      </details>
                    ) : (
                      <p className={budgetLinks}>{linkedCount}</p>
                    )}
                  </div>
                  <div className="flex flex-none gap-2 @max-[300px]:col-start-2 @max-[300px]:justify-self-end">
                    <Button
                      variant="iconChip" size="lg" shape="pill"
                      onClick={() => onOpenEditBudget(budget)}
                      aria-label="Edit Budget"
                      title="Edit Budget"
                    >
                      <Icon name="edit" className="size-5" />
                    </Button>
                    <Button
                      variant="iconChipDanger" size="lg" shape="pill"
                      onClick={() => onConfirmDeleteBudget(budget)}
                      aria-label="Delete Budget"
                      title="Delete Budget"
                    >
                      <Icon name="trash" className="size-5" />
                    </Button>
                  </div>
                </header>

                <dl className="grid grid-cols-2 divide-x divide-divider rounded-2xl bg-muted tabular-nums">
                  <BudgetAmount label="Allocated" amount={money(budget.allocated_amount)} />
                  <BudgetAmount
                    label="Committed/Used"
                    amount={money(budget.used_amount)}
                    emphasized={budget.used_amount !== 0}
                  />
                </dl>

                <div>
                  <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1 text-control text-muted-foreground">
                    <span>{overBudget ? "Over budget by" : "Available Balance"}</span>
                    <b className={`font-extrabold wrap-anywhere tabular-nums ${overBudget ? "text-(--finance-budget-deficit)" : "text-(--finance-budget-available)"}`}>
                      {money(Math.abs(budget.available_amount))}
                    </b>
                  </div>
                  <div role="img" aria-label={`${committed} percent committed`} className="mt-2.5 h-2.5 overflow-hidden rounded-full bg-chip">
                    <div
                      className={`h-full rounded-full forced-colors:[background:Highlight] forced-colors:forced-color-adjust-none ${progressFill}`}
                      style={{ width: `${Math.min(100, Math.max(0, usage))}%` }}
                    />
                  </div>
                  <p className="tabular-nums mt-1.5 text-muted-foreground text-caption text-end">{committed}% committed</p>
                </div>
              </PulseCard>
            );
          })}
        </div>
      )}
    </section>
  );
}
