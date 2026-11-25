"use client";

import { Icon } from "@/app/financial-horizon/components/horizon-icon";
import PulseSelect from "@/app/components/design-system/pulse-select";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney, currencySymbol } from "@/app/lib/currency.mjs";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/profile-preferences-provider";

import PulseTabs from "@/app/components/design-system/pulse-tabs";
import HorizonAction from "@/app/financial-horizon/components/horizon-action";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseIconButton from "@/app/components/design-system/pulse-icon-button";
import SectionHeader from "@/app/financial-horizon/components/section-header";
import HorizonTag from "@/app/financial-horizon/components/horizon-tag";
import HorizonEmpty from "@/app/financial-horizon/components/horizon-empty";

import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import { useId, useMemo } from "react";
import { Home, TrendingUp, Zap, Tv, CreditCard, Receipt } from "lucide-react";
import type { HorizonSummary, DeductionItem, SubscriptionItem } from "@/app/financial-horizon/horizon-types";

const CATEGORIES = [
  { id: "housing", label: "Housing & Rent", icon: Home, color: "bg-info/10 text-info" },
  { id: "investment", label: "Investments & SIPs", icon: TrendingUp, color: "bg-success/10 text-success" },
  { id: "bill", label: "Bills & Utilities", icon: Zap, color: "bg-warning/10 text-warning" },
  { id: "subscription", label: "Subscriptions", icon: Tv, color: "bg-lavender/10 text-foreground" },
  { id: "debt", label: "Debt & EMIs", icon: CreditCard, color: "bg-destructive/10 text-destructive" },
  { id: "other", label: "Other Fixed", icon: Receipt, color: "bg-muted/10 text-muted-foreground" },
] as const;

function formatOrdinalDay(day: number): string {
  const mod10 = day % 10;
  const mod100 = day % 100;
  if (mod10 === 1 && mod100 !== 11) return `${day}st`;
  if (mod10 === 2 && mod100 !== 12) return `${day}nd`;
  if (mod10 === 3 && mod100 !== 13) return `${day}rd`;
  return `${day}th`;
}

interface FixedObligationsTabProps {
  summary: HorizonSummary;
  subscriptions?: SubscriptionItem[];
  activeCategory: string;
  setActiveCategory: (cat: string) => void;
  isAddingDeduction: boolean;
  editingDeductionId: number | null;
  name: string;
  setName: (val: string) => void;
  amount: string;
  setAmount: (val: string) => void;
  category: string;
  setCategory: (val: string) => void;
  dueDay: string;
  setDueDay: (val: string) => void;
  selectedBudgetId: number | null;
  setSelectedBudgetId: (val: number | null) => void;
  onOpenAddDeduction: () => void;
  onOpenEditDeduction: (item: DeductionItem) => void;
  onCancelDeductionForm: () => void;
  onSaveDeduction: (e: React.FormEvent) => void;
  onToggleDeductionActive: (item: DeductionItem) => void;
  onDeleteDeduction: (item: DeductionItem) => void;
  onEditSubscription?: (sub: SubscriptionItem) => void;
  onNavigateToSubscriptions?: () => void;
  isPending?: boolean;
  error?: string;
}

export default function FixedObligationsTab({
  summary,
  subscriptions = [],
  activeCategory,
  setActiveCategory,
  isAddingDeduction,
  editingDeductionId,
  name,
  setName,
  amount,
  setAmount,
  category,
  setCategory,
  dueDay,
  setDueDay,
  selectedBudgetId,
  setSelectedBudgetId,
  onOpenAddDeduction,
  onOpenEditDeduction,
  onCancelDeductionForm,
  onSaveDeduction,
  onToggleDeductionActive,
  onDeleteDeduction,
  onEditSubscription,
  onNavigateToSubscriptions,
  isPending = false,
  error,
}: FixedObligationsTabProps) {
  const { timeZone } = useTimeZone();
  const titleId = useId();
  const isFormOpen = isAddingDeduction || editingDeductionId !== null;

  const getCategoryConfig = (catKey: string) => {
    return (
      CATEGORIES.find((c) => c.id === catKey) ?? {
        id: "other",
        label: "Other Fixed",
        icon: Receipt,
        color: "bg-muted/10 text-muted-foreground",
      }
    );
  };

  // Active subscriptions automatically included in Fixed Obligations view
  const activeSubscriptions = useMemo(() => {
    const subs = subscriptions.length > 0 ? subscriptions : summary.subscriptions ?? [];
    return subs.filter((s) => s.status === "active");
  }, [subscriptions, summary.subscriptions]);

  const filteredDeductions = summary.deductions.filter((d) =>
    activeCategory === "all" ? true : d.category === activeCategory
  );

  const filteredSubscriptions = useMemo(() => {
    if (activeCategory === "all" || activeCategory === "subscription") {
      return activeSubscriptions;
    }
    return [];
  }, [activeSubscriptions, activeCategory]);

  const activeDeductionsCount = summary.deductions.filter((d) => d.is_active).length;
  const totalActiveCount = activeDeductionsCount + activeSubscriptions.length;

  const totalDeductionsAmount = summary.deductions
    .filter((d) => d.is_active)
    .reduce((sum, d) => sum + d.amount, 0);

  const totalSubscriptionsAmount = activeSubscriptions.reduce(
    (sum, s) => sum + (s.monthly_equivalent_amount || s.amount),
    0
  );

  const totalFixedCommitments = totalDeductionsAmount + totalSubscriptionsAmount;
  const totalItemsCount = summary.deductions.length + activeSubscriptions.length;

  return (
    <section className="space-y-6">
      {/* Section Header */}
      <SectionHeader
        title="Fixed Obligations & Subscriptions"
        icon="calendar"
        description="Non-negotiable monthly expenses and active recurring subscriptions subtracted directly from your starting baseline income."
        badges={
          <>
            <HorizonTag emphasis>{totalActiveCount} Active</HorizonTag>
            <HorizonTag emphasis>{formatMoney(totalFixedCommitments, summary.currency)}/mo</HorizonTag>
          </>
        }
      >
        <HorizonAction
          onClick={onOpenAddDeduction}
          disabled={isPending}
          className="shrink-0"
        >
          <Icon name="plus" /> Add Fixed Obligation
        </HorizonAction>
      </SectionHeader>

      <PulseTabs
        variant="segmented"
        label="Obligation category"
        className="flex-wrap"
        value={activeCategory}
        onChange={setActiveCategory}
        items={[
          { value: "all", label: `All (${totalItemsCount})` },
          ...CATEGORIES.map(category => {
            const deductionCount = summary.deductions.filter(item => item.category === category.id).length;
            const subscriptionCount = category.id === "subscription" ? activeSubscriptions.length : 0;
            return { value: category.id, label: `${category.label} (${deductionCount + subscriptionCount})` };
          }),
        ]}
      />

      {isFormOpen && (
        <PulseDialog
          labelledBy={titleId}
          onDismiss={isPending ? undefined : onCancelDeductionForm}
        >
          <PulseDialogHeader
            id={titleId}
            icon={<Icon name="calendar" className="size-5" />}
          >
            {editingDeductionId !== null ? "Edit Fixed Obligation" : "Add Fixed Obligation"}
          </PulseDialogHeader>

          <form onSubmit={onSaveDeduction} className="mt-5 grid gap-4 sm:grid-cols-2">
            {error && (
              <p role="alert" className="rounded-xl bg-destructive/10 p-3 text-sm text-destructive sm:col-span-2">
                {error}
              </p>
            )}
            <div className="space-y-1">
              <label htmlFor={`${titleId}-name`} className="text-xs font-semibold text-muted-foreground">Title / Name</label>
              <input
                id={`${titleId}-name`}
                autoFocus
                type="text"
                required
                placeholder="e.g. Apartment Rent, SIP"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 text-sm text-foreground"
              />
            </div>

            <div className="space-y-1">
              <label htmlFor={`${titleId}-category`} className="text-xs font-semibold text-muted-foreground">Category</label>
              <PulseSelect
                id={`${titleId}-category`}
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3 py-2 text-sm text-foreground"
              >
                {CATEGORIES.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.label}
                  </option>
                ))}
              </PulseSelect>
            </div>

            <div className="space-y-1">
              <label htmlFor={`${titleId}-amount`} className="text-xs font-semibold text-muted-foreground">Monthly Amount ({currencySymbol(summary.currency)})</label>
              <input
                id={`${titleId}-amount`}
                type="number"
                step="0.01"
                min="0"
                required
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full px-3 py-2 text-sm text-foreground"
              />
            </div>

            <div className="space-y-1">
              <label htmlFor={`${titleId}-due-day`} className="text-xs font-semibold text-muted-foreground">Due Day (1-31, optional)</label>
              <input
                id={`${titleId}-due-day`}
                type="number"
                min="1"
                max="31"
                placeholder="e.g. 5"
                value={dueDay}
                onChange={(e) => setDueDay(e.target.value)}
                className="w-full px-3 py-2 text-sm text-foreground"
              />
            </div>

            <div className="space-y-1 sm:col-span-2">
              <label htmlFor={`${titleId}-budget`} className="text-xs font-semibold text-muted-foreground">Link to Monthly Budget (Optional)</label>
              <PulseSelect
                id={`${titleId}-budget`}
                value={selectedBudgetId ?? ""}
                onChange={(e) => setSelectedBudgetId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full px-3 py-2 text-sm text-foreground"
              >
                <option value="">-- No Budget Link --</option>
                {(summary.budgets ?? []).map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({formatMoney(b.allocated_amount, summary.currency)})
                  </option>
                ))}
              </PulseSelect>
            </div>

            <PulseDialogActions>
              <PulseDialogAction
                variant="secondary"
                type="button"
                onClick={onCancelDeductionForm}
                disabled={isPending}
              >
                Cancel
              </PulseDialogAction>
              <PulseDialogAction type="submit" disabled={isPending}>
                {isPending ? "Saving…" : editingDeductionId !== null ? "Update Obligation" : "Save Obligation"}
              </PulseDialogAction>
            </PulseDialogActions>
          </form>
        </PulseDialog>
      )}

      {/* Cards Grid */}
      {filteredDeductions.length === 0 && filteredSubscriptions.length === 0 ? (
        <HorizonEmpty>
          <Icon name="receipt" className="mx-auto h-10 w-10 text-muted-foreground opacity-50" />
          <h3 className="mt-3 text-base font-semibold text-foreground">No fixed obligations found</h3>
          <p className="mt-1 max-w-prose text-control text-muted-foreground">
            Add your rent, SIP investments, bills, or debt EMIs to calculate your exact baseline.
          </p>
          <HorizonAction
            onClick={onOpenAddDeduction}
            className="mt-4"
          >
            <Icon name="plus" /> Add Obligation
          </HorizonAction>
        </HorizonEmpty>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {/* Render Manual Fixed Obligations */}
          {filteredDeductions.map((item) => {
            const catCfg = getCategoryConfig(item.category);
            const IconComponent = catCfg.icon;
            const linkedBudget = summary.budgets?.find((b) => b.id === item.budget_id);

            return (
              <PulseCard
                key={`deduction-${item.id}`}
                padding="md"
                className={`group relative ${
                  item.is_active ? "" : "opacity-60"
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${catCfg.color}`}>
                      <IconComponent className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-foreground text-base leading-tight">
                        {item.name}
                      </h4>
                      <span className="text-xs text-muted-foreground capitalize">
                        {catCfg.label}
                      </span>
                    </div>
                  </div>

                  <button
                    type="button"
                    role="switch"
                    aria-checked={item.is_active}
                    aria-label={`Activate ${item.name}`}
                    className="relative flex-none h-7 w-12 rounded-full mt-2 bg-divider transition-colors aria-checked:bg-(image:--primary-gradient) after:absolute after:start-1 after:top-1 after:size-5 after:rounded-full after:bg-white after:transition-transform aria-checked:after:translate-x-5 rtl:aria-checked:after:-translate-x-5 motion-reduce:transition-none motion-reduce:after:transition-none forced-colors:outline-1 forced-colors:outline-[ButtonText] forced-colors:after:bg-[ButtonText]"
                    onClick={() => onToggleDeductionActive(item)}
                    title={item.is_active ? "Deactivate obligation" : "Activate obligation"}
                  />
                </div>

                <div className="mt-4 flex items-baseline justify-between">
                  <div>
                    <span className="text-xs text-muted-foreground">Monthly Amount</span>
                    <p className="text-xl font-bold text-foreground">
                      {formatMoney(item.amount, summary.currency)}
                    </p>
                  </div>

                  {item.due_day && (
                    <HorizonTag tone="dim">
                      <Icon name="calendar" className="h-3 w-3" />
                      <span>Due {formatOrdinalDay(item.due_day)}</span>
                    </HorizonTag>
                  )}
                </div>

                {linkedBudget && (
                  <HorizonTag className="mt-3">
                    <Icon name="target" className="h-3.5 w-3.5" />
                    <span>Budget: <strong>{linkedBudget.name}</strong></span>
                  </HorizonTag>
                )}

                <div className="mt-4 flex items-center justify-end gap-2 border-t border-divider pt-3">
                  <PulseIconButton size="lg"
                    onClick={() => onOpenEditDeduction(item)}
                    title="Edit obligation"
                    aria-label="Edit obligation"
                  >
                    <Icon name="edit" className="size-5" />
                  </PulseIconButton>
                  <PulseIconButton variant="danger" size="lg"
                    onClick={() => onDeleteDeduction(item)}
                    title="Delete obligation"
                    aria-label="Delete obligation"
                  >
                    <Icon name="trash" className="size-5" />
                  </PulseIconButton>
                </div>
              </PulseCard>
            );
          })}

          {/* Render Auto-Included Active Subscriptions */}
          {filteredSubscriptions.map((sub) => {
            const monthlyAmount = sub.monthly_equivalent_amount || sub.amount;

            return (
              <PulseCard
                key={`subscription-${sub.id}`}
                padding="md"
                className="group relative flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-lavender/10 text-foreground">
                        <Tv className="h-5 w-5" />
                      </div>
                      <div>
                        <h4 className="font-semibold text-foreground text-base leading-tight">
                          {sub.name}
                        </h4>
                        <div className="flex items-center gap-1.5 mt-0.5">
                          <HorizonTag>
                            Subscription
                          </HorizonTag>
                          {sub.billing_cycle === "yearly" && (
                            <HorizonTag tone="dim">
                              Yearly
                            </HorizonTag>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="mt-4 flex items-baseline justify-between">
                    <div>
                      <span className="text-xs text-muted-foreground">Monthly Equivalent</span>
                      <p className="text-xl font-bold text-foreground">
                        {formatMoney(monthlyAmount, summary.currency)}
                      </p>
                      {sub.billing_cycle === "yearly" && (
                        <span className="text-xs text-muted-foreground block mt-0.5">
                          ({formatMoney(sub.amount, summary.currency)}/year)
                        </span>
                      )}
                    </div>

                    <HorizonTag>
                      <Icon name="calendar" className="h-3 w-3" />
                      <span>
                        {sub.billing_cycle === "yearly"
                          ? `Renews ${formatInstant(sub.next_renewal_date, timeZone, { month: "short", day: "numeric" })}`
                          : `Due ${formatOrdinalDay(sub.billing_day ?? 1)}`}
                      </span>
                    </HorizonTag>
                  </div>

                  {sub.budget_name && (
                    <HorizonTag className="mt-3">
                      <Icon name="target" className="h-3.5 w-3.5" />
                      <span>Budget: <strong>{sub.budget_name}</strong></span>
                    </HorizonTag>
                  )}
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-divider pt-3 text-xs">
                  <span className="text-muted-foreground italic truncate max-w-[150px]">
                    {sub.category_name ?? "Recurring Subscription"}
                  </span>

                  <PulseButton variant="secondary" size="sm"
                    onClick={() => {
                      if (onEditSubscription) {
                        onEditSubscription(sub);
                      } else if (onNavigateToSubscriptions) {
                        onNavigateToSubscriptions();
                      }
                    }}
                  >
                    <span>Manage</span>
                    <Icon name="external" className="h-3 w-3" />
                  </PulseButton>
                </div>
              </PulseCard>
            );
          })}
        </div>
      )}
    </section>
  );
}
