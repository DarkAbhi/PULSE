"use client";

import Select from "@/app/components/design-system/select";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney, currencySymbol } from "@/app/lib/currency.mjs";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/time-zone-provider";

import Button from "@/app/components/design-system/button";

import Dialog, { DialogAction, DialogActions } from "@/app/components/design-system/dialog";
import { useEffect, useId, useMemo } from "react";
import {
  Calendar,
  Plus,
  Edit2,
  Trash2,
  ToggleLeft,
  ToggleRight,
  Target,
  Home,
  TrendingUp,
  Zap,
  Tv,
  CreditCard,
  Receipt,
  X,
  ExternalLink,
} from "lucide-react";
import { HorizonSummary, DeductionItem, SubscriptionItem } from "@/app/dashboard/financial-horizon-card";

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

  useEffect(() => {
    if (!isFormOpen || isPending) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onCancelDeductionForm();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isFormOpen, isPending, onCancelDeductionForm]);

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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-divider pb-4">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <Calendar className="h-5 w-5 text-action" />
            <h2 className="text-xl font-bold text-foreground">Fixed Obligations & Subscriptions</h2>
            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-action">
              {totalActiveCount} Active
            </span>
            <span className="inline-flex items-center gap-1 rounded-full bg-lavender/10 px-2.5 py-0.5 text-xs font-semibold text-foreground">
              {formatMoney(totalFixedCommitments, summary.currency)}/mo
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Non-negotiable monthly expenses and active recurring subscriptions subtracted directly from your starting baseline income.
          </p>
        </div>

        <Button variant="primary" size="sm"
          onClick={onOpenAddDeduction}
          disabled={isPending}
          className="shrink-0"
        >
          <Plus className="h-4 w-4" /> Add Fixed Obligation
        </Button>
      </div>

      {/* Category Filter Pills */}
      <div className="flex flex-wrap gap-2">
        <Button variant={activeCategory === "all" ? "primary" : "secondary"} size="sm"
          onClick={() => setActiveCategory("all")}
        >
          All ({totalItemsCount})
        </Button>
        {CATEGORIES.map((cat) => {
          const deductionCount = summary.deductions.filter((d) => d.category === cat.id).length;
          const subCount = cat.id === "subscription" ? activeSubscriptions.length : 0;
          const count = deductionCount + subCount;
          return (
            <Button variant={activeCategory === cat.id ? "primary" : "secondary"} size="sm"
              key={cat.id}
              onClick={() => setActiveCategory(cat.id)}
            >
              {cat.label} ({count})
            </Button>
          );
        })}
      </div>

      {isFormOpen && (
        <Dialog
          labelledBy={titleId}
          size="md"
          panelClassName="sm:p-6"
          onBackdropClick={(event) => {
            if (event.target === event.currentTarget && !isPending) onCancelDeductionForm();
          }}
        >
          <div className="flex items-center justify-between gap-3 border-b border-divider pb-4">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-action">
                <Calendar className="h-5 w-5" />
              </div>
              <h2 id={titleId} className="text-xl font-bold text-foreground">
                {editingDeductionId !== null ? "Edit Fixed Obligation" : "Add Fixed Obligation"}
              </h2>
            </div>
            <Button
              variant="icon"
              type="button"
              onClick={onCancelDeductionForm}
              disabled={isPending}
              aria-label="Close"
            >
              <X className="h-5 w-5" />
            </Button>
          </div>

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
                className="w-full rounded-xl bg-secondary px-3 py-2 text-sm text-foreground"
              />
            </div>

            <div className="space-y-1">
              <label htmlFor={`${titleId}-category`} className="text-xs font-semibold text-muted-foreground">Category</label>
              <Select
                id={`${titleId}-category`}
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full rounded-xl bg-secondary px-3 py-2 text-sm text-foreground"
              >
                {CATEGORIES.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.label}
                  </option>
                ))}
              </Select>
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
                className="w-full rounded-xl bg-secondary px-3 py-2 text-sm text-foreground"
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
                className="w-full rounded-xl bg-secondary px-3 py-2 text-sm text-foreground"
              />
            </div>

            <div className="space-y-1 sm:col-span-2">
              <label htmlFor={`${titleId}-budget`} className="text-xs font-semibold text-muted-foreground">Link to Monthly Budget (Optional)</label>
              <Select
                id={`${titleId}-budget`}
                value={selectedBudgetId ?? ""}
                onChange={(e) => setSelectedBudgetId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full rounded-xl bg-secondary px-3 py-2 text-sm text-foreground"
              >
                <option value="">-- No Budget Link --</option>
                {(summary.budgets ?? []).map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({formatMoney(b.allocated_amount, summary.currency)})
                  </option>
                ))}
              </Select>
            </div>

            <DialogActions className="border-t border-divider pt-4 sm:col-span-2">
              <DialogAction
                variant="secondary"
                type="button"
                onClick={onCancelDeductionForm}
                disabled={isPending}
              >
                Cancel
              </DialogAction>
              <DialogAction type="submit" disabled={isPending}>
                {isPending ? "Saving…" : editingDeductionId !== null ? "Update Obligation" : "Save Obligation"}
              </DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}

      {/* Cards Grid */}
      {filteredDeductions.length === 0 && filteredSubscriptions.length === 0 ? (
        <PulseCard className="p-10 text-center">
          <Receipt className="mx-auto h-10 w-10 text-muted-foreground opacity-50" />
          <h3 className="mt-3 text-base font-semibold text-foreground">No fixed obligations found</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Add your rent, SIP investments, bills, or debt EMIs to calculate your exact baseline.
          </p>
          <Button variant="primary" size="md"
            onClick={onOpenAddDeduction}
            className="mt-4"
          >
            <Plus className="h-4 w-4" /> Add Obligation
          </Button>
        </PulseCard>
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
                className={`group relative p-5 ${
                  item.is_active ? "" : "opacity-60 bg-secondary/20"
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

                  <Button variant="icon" size="sm"
                    onClick={() => onToggleDeductionActive(item)}
                    title={item.is_active ? "Deactivate obligation" : "Activate obligation"}
                  >
                    {item.is_active ? (
                      <ToggleRight className="h-6 w-6 text-success" />
                    ) : (
                      <ToggleLeft className="h-6 w-6 text-muted-foreground" />
                    )}
                  </Button>
                </div>

                <div className="mt-4 flex items-baseline justify-between">
                  <div>
                    <span className="text-xs text-muted-foreground">Monthly Amount</span>
                    <p className="text-xl font-bold text-foreground">
                      {formatMoney(item.amount, summary.currency)}
                    </p>
                  </div>

                  {item.due_day && (
                    <div className="flex items-center gap-1 text-xs text-muted-foreground bg-secondary/60 px-2 py-1 rounded-lg">
                      <Calendar className="h-3 w-3" />
                      <span>Due {formatOrdinalDay(item.due_day)}</span>
                    </div>
                  )}
                </div>

                {linkedBudget && (
                  <div className="mt-3 flex items-center gap-1.5 text-xs text-action bg-primary/10 px-2.5 py-1 rounded-lg">
                    <Target className="h-3.5 w-3.5" />
                    <span>Budget: <strong>{linkedBudget.name}</strong></span>
                  </div>
                )}

                <div className="mt-4 flex items-center justify-end gap-2 border-t border-divider/50 pt-3">
                  <Button variant="icon"
                    onClick={() => onOpenEditDeduction(item)}
                    title="Edit obligation"
                  >
                    <Edit2 className="h-4 w-4" />
                  </Button>
                  <Button variant="iconDanger"
                    onClick={() => onDeleteDeduction(item)}
                    title="Delete obligation"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
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
                className="group relative flex flex-col justify-between bg-gradient-to-br from-card via-card to-lavender/5 p-5"
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
                          <span className="inline-flex items-center gap-1 rounded-md bg-lavender/10 px-2 py-0.5 text-xs font-semibold text-foreground">
                            Subscription
                          </span>
                          {sub.billing_cycle === "yearly" && (
                            <span className="rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
                              Yearly
                            </span>
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

                    <div className="flex items-center gap-1 text-xs text-foreground bg-lavender/10 px-2 py-1 rounded-lg">
                      <Calendar className="h-3 w-3" />
                      <span>
                        {sub.billing_cycle === "yearly"
                          ? `Renews ${formatInstant(sub.next_renewal_date, timeZone, { month: "short", day: "numeric" })}`
                          : `Due ${formatOrdinalDay(sub.billing_day ?? 1)}`}
                      </span>
                    </div>
                  </div>

                  {sub.budget_name && (
                    <div className="mt-3 flex items-center gap-1.5 text-xs text-action bg-primary/10 px-2.5 py-1 rounded-lg">
                      <Target className="h-3.5 w-3.5" />
                      <span>Budget: <strong>{sub.budget_name}</strong></span>
                    </div>
                  )}
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-divider/50 pt-3 text-xs">
                  <span className="text-muted-foreground italic truncate max-w-[150px]">
                    {sub.category_name ?? "Recurring Subscription"}
                  </span>

                  <Button variant="soft" size="sm"
                    onClick={() => {
                      if (onEditSubscription) {
                        onEditSubscription(sub);
                      } else if (onNavigateToSubscriptions) {
                        onNavigateToSubscriptions();
                      }
                    }}
                  >
                    <span>Manage</span>
                    <ExternalLink className="h-3 w-3" />
                  </Button>
                </div>
              </PulseCard>
            );
          })}
        </div>
      )}
    </section>
  );
}
