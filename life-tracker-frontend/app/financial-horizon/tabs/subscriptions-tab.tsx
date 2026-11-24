"use client";

import { Icon, type IconName } from "@/app/financial-horizon/components/horizon-icon";
import { HorizonSearch, HorizonSelect } from "@/app/financial-horizon/components/horizon-filters";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney } from "@/app/lib/currency.mjs";

import { formatInstant, formatCalendarDate, dateKey, calendarDayNumber } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/profile-preferences-provider";

import HorizonAction from "@/app/financial-horizon/components/horizon-action";
import PulseButton from "@/app/components/design-system/pulse-button";
import SectionHeader from "@/app/financial-horizon/components/section-header";
import HorizonTag from "@/app/financial-horizon/components/horizon-tag";
import HorizonEmpty from "@/app/financial-horizon/components/horizon-empty";

import { useState, useMemo, type ReactNode } from "react";
import { CheckCircle2, PauseCircle, XCircle } from "lucide-react";
import type {
  SubscriptionItem,
  BudgetItem,
  CategoryItem,
  TransactionItem,
} from "@/app/financial-horizon/horizon-types";

interface SubscriptionsTabProps {
  subscriptions: SubscriptionItem[];
  categories: CategoryItem[];
  budgets: BudgetItem[];
  currency: string;
  onAddSubscription: () => void;
  onEditSubscription: (sub: SubscriptionItem) => void;
  onDeleteSubscription: (sub: SubscriptionItem) => void;
  onToggleStatus: (sub: SubscriptionItem, newStatus: string) => void;
  onLogPayment: (sub: SubscriptionItem) => void;
  onViewTransactions: (sub: SubscriptionItem) => void;
}

export default function SubscriptionsTab({
  subscriptions,
  categories,
  budgets,
  currency,
  onAddSubscription,
  onEditSubscription,
  onDeleteSubscription,
  onToggleStatus,
  onLogPayment,
  onViewTransactions,
}: SubscriptionsTabProps) {
  const { timeZone } = useTimeZone();
  const [searchQuery, setSearchQuery] = useState("");
  const [cycleFilter, setCycleFilter] = useState<string>("all");
  const [statusFilter, setStatusFilter] = useState<string>("all");

  // Calculate Metrics
  const activeSubs = useMemo(
    () => subscriptions.filter((s) => s.status === "active"),
    [subscriptions]
  );

  const totalMonthlyBurn = useMemo(
    () =>
      activeSubs.reduce(
        (sum, s) => sum + (s.monthly_equivalent_amount || s.amount),
        0
      ),
    [activeSubs]
  );

  const totalYearlyOutlay = useMemo(() => {
    return activeSubs.reduce((sum, s) => {
      return s.billing_cycle === "yearly" ? sum + s.amount : sum + s.amount * 12;
    }, 0);
  }, [activeSubs]);

  // Find nearest upcoming renewal among active subscriptions
  const nearestRenewal = useMemo(() => {
    if (activeSubs.length === 0) return null;
    const sorted = [...activeSubs].sort((a, b) => {
      const dateA = new Date(a.next_renewal_date).getTime();
      const dateB = new Date(b.next_renewal_date).getTime();
      return dateA - dateB;
    });
    return sorted[0];
  }, [activeSubs]);

  // Filter subscriptions
  const filteredSubscriptions = useMemo(() => {
    return subscriptions.filter((sub) => {
      const matchesSearch =
        sub.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (sub.category_name &&
          sub.category_name.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (sub.notes && sub.notes.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesCycle =
        cycleFilter === "all" || sub.billing_cycle === cycleFilter;

      const matchesStatus =
        statusFilter === "all" || sub.status === statusFilter;

      return matchesSearch && matchesCycle && matchesStatus;
    });
  }, [subscriptions, searchQuery, cycleFilter, statusFilter]);

  // Days until next renewal calculation helper
  const getDaysUntilRenewal = (dateStr: string) => {
    return calendarDayNumber(dateKey(dateStr, timeZone)) - calendarDayNumber(dateKey(new Date(), timeZone));
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "active":
        return (
          <HorizonTag tone="good">
            <CheckCircle2 className="h-3 w-3" /> Active
          </HorizonTag>
        );
      case "paused":
        return (
          <HorizonTag tone="warn">
            <PauseCircle className="h-3 w-3" /> Paused
          </HorizonTag>
        );
      case "cancelled":
        return (
          <HorizonTag tone="dim">
            <XCircle className="h-3 w-3" /> Cancelled
          </HorizonTag>
        );
      default:
        return null;
    }
  };

  const getCycleBadge = (cycle: string) => {
    if (cycle === "yearly") {
      return (
        <HorizonTag>
          Yearly
        </HorizonTag>
      );
    }
    return (
      <HorizonTag>
        Monthly
      </HorizonTag>
    );
  };

  return (
    <section aria-labelledby="subscriptions-heading" className="space-y-6">
      <SectionHeader id="subscriptions-heading" title="Subscriptions" icon="card" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SubscriptionMetric title="Monthly Subscription Burn" icon="flame">
          <MetricAmount value={formatMoney(totalMonthlyBurn, currency)} unit="/ month" />
          <p className="mt-1 text-xs text-muted-foreground">
            Normalized cost across all active subscriptions
          </p>
        </SubscriptionMetric>

        <SubscriptionMetric title="Annual Outlay" icon="trend">
          <MetricAmount value={formatMoney(totalYearlyOutlay, currency)} unit="/ year" />
          <p className="mt-1 text-xs text-muted-foreground">
            Total projected yearly commitment
          </p>
        </SubscriptionMetric>

        <SubscriptionMetric title="Active Subscriptions" icon="card" tone="success">
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-black text-foreground">{activeSubs.length}</span>
            <span className="text-xs text-muted-foreground">active ({subscriptions.length} total)</span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {subscriptions.length - activeSubs.length} paused or cancelled
          </p>
        </SubscriptionMetric>

        <SubscriptionMetric title="Next Upcoming Renewal" icon="calendar" tone="warning">
          {nearestRenewal ? (
            <div>
              <div className="flex items-center justify-between">
                <span className="truncate font-bold text-foreground max-w-[140px]">
                  {nearestRenewal.name}
                </span>
                <span className="text-xs font-bold text-foreground">
                  {formatMoney(nearestRenewal.amount, currency)}
                </span>
              </div>
              <p className="mt-1 text-xs font-medium text-warning">
                Renews on{" "}
                {formatInstant(nearestRenewal.next_renewal_date, timeZone, { month: "short", day: "numeric" })}{" "}
                ({getDaysUntilRenewal(nearestRenewal.next_renewal_date)}d left)
              </p>
            </div>
          ) : (
            <div>
              <span className="text-sm font-semibold text-muted-foreground">
                No active renewals
              </span>
            </div>
          )}
        </SubscriptionMetric>
      </div>

      {/* 2. Control & Search Bar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        {/* Search */}
        <HorizonSearch aria-label="Search subscriptions"
          placeholder="Search subscriptions..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />

        {/* Filters & Add Button */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Cycle Filter */}
          <HorizonSelect compact aria-label="Billing cadence"
            value={cycleFilter}
            onChange={(e) => setCycleFilter(e.target.value)}
          >
            <option value="all">All Cadences</option>
            <option value="monthly">Monthly</option>
            <option value="yearly">Yearly</option>
          </HorizonSelect>

          {/* Status Filter */}
          <HorizonSelect compact aria-label="Subscription status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="all">All Statuses</option>
            <option value="active">Active</option>
            <option value="paused">Paused</option>
            <option value="cancelled">Cancelled</option>
          </HorizonSelect>

          {/* Add Subscription Button */}
          <HorizonAction
            onClick={onAddSubscription}
          >
            <Icon name="plus" /> Add Subscription
          </HorizonAction>
        </div>
      </div>

      {/* 3. Subscriptions Grid */}
      {filteredSubscriptions.length === 0 ? (
        <HorizonEmpty className="flex flex-col items-center justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary text-muted-foreground mb-3">
            <Icon name="card" className="h-6 w-6" />
          </div>
          <h3 className="text-base font-bold text-foreground">
            No Subscriptions Found
          </h3>
          <p className="mt-1 text-xs text-muted-foreground max-w-sm">
            {searchQuery || cycleFilter !== "all" || statusFilter !== "all"
              ? "No subscriptions match your current filter criteria."
              : "Keep track of your recurring monthly, yearly, and software subscriptions in one place."}
          </p>
          {!searchQuery && cycleFilter === "all" && statusFilter === "all" && (
            <HorizonAction
              onClick={onAddSubscription}
              className="mt-4"
            >
              <Icon name="plus" /> Add Your First Subscription
            </HorizonAction>
          )}
        </HorizonEmpty>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredSubscriptions.map((sub) => {
            const daysLeft = getDaysUntilRenewal(sub.next_renewal_date);

            return (
              <PulseCard
                key={sub.id}
                className={`relative flex flex-col justify-between p-6 ${sub.status === "cancelled" ? "opacity-60" : ""}`}
              >
                <div>
                  {/* Card Header: Title & Badges */}
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-bold text-foreground text-base tracking-tight">
                          {sub.name}
                        </h4>
                      </div>
                      <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                        {getStatusBadge(sub.status)}
                        {getCycleBadge(sub.billing_cycle)}
                        {sub.category_name && (
                          <HorizonTag tone="dim">
                            {sub.category_name}
                          </HorizonTag>
                        )}
                      </div>
                    </div>

                    {/* Price Tag */}
                    <div className="text-right shrink-0">
                      <span className="text-lg font-black text-foreground">
                        {formatMoney(sub.amount, currency)}
                      </span>
                      <span className="text-xs text-muted-foreground font-medium block">
                        / {sub.billing_cycle === "yearly" ? "year" : "month"}
                      </span>
                    </div>
                  </div>

                  {/* Monthly Equivalent if yearly */}
                  {sub.billing_cycle !== "monthly" && sub.status === "active" && (
                    <HorizonTag tone="dim" className="mt-2">
                      ≈ {formatMoney(sub.monthly_equivalent_amount, currency)}/mo equivalent
                    </HorizonTag>
                  )}

                  {/* Details: Start Date & Renewal */}
                  <div className="mt-4 space-y-2 border-t border-divider pt-3 text-xs">
                    {/* Cycle Specific Detail (Billing Day for Monthly, Renewal Date for Yearly) */}
                    <div className="flex items-center justify-between text-muted-foreground">
                      <span className="flex items-center gap-1.5">
                        <Icon name="clock" className="h-3.5 w-3.5 text-muted-foreground" />
                        {sub.billing_cycle === "yearly" ? "Annual Renewal Date:" : "Billing Day of Month:"}
                      </span>
                      <span className="font-semibold text-foreground">
                        {sub.billing_cycle === "yearly"
                          ? formatCalendarDate(sub.renewal_date || sub.next_renewal_date, {
                              year: "numeric",
                              month: "short",
                              day: "numeric",
                            })
                          : `Day ${sub.billing_day ?? 1}`}
                      </span>
                    </div>

                    {/* Next Renewal */}
                    {sub.status === "active" && (
                      <div className="flex items-center justify-between">
                        <span className="flex items-center gap-1.5 text-muted-foreground">
                          <Icon name="calendar" className="h-3.5 w-3.5 text-action" />
                          Next Renewal:
                        </span>
                        <span
                          className={`font-semibold ${
                            daysLeft <= 3
                              ? "text-destructive dark:text-destructive font-bold"
                              : daysLeft <= 7
                              ? "text-warning dark:text-warning"
                              : "text-foreground"
                          }`}
                        >
                          {formatInstant(sub.next_renewal_date, timeZone, { month: "short", day: "numeric", year: "numeric" })}{" "}
                          ({daysLeft === 0 ? "Today" : `${daysLeft}d`})
                        </span>
                      </div>
                    )}

                    {/* Budget Link if any */}
                    {sub.budget_name && (
                      <div className="flex items-center justify-between text-muted-foreground">
                        <span>Budget Bucket:</span>
                        <span className="font-medium text-foreground">
                          {sub.budget_name}
                        </span>
                      </div>
                    )}

                    {/* Notes if any */}
                    {sub.notes && (
                      <p className="text-xs text-muted-foreground italic truncate pt-1">
                        "{sub.notes}"
                      </p>
                    )}
                  </div>
                </div>

                {/* Card Footer Actions */}
                <div className="mt-5 pt-3 border-t border-divider flex flex-wrap items-center justify-between gap-2">
                  {/* Left: Linked Transactions Trigger */}
                  <PulseButton variant="tertiary" size="sm"
                    onClick={() => onViewTransactions(sub)}
                    title="View payment history"
                  >
                    <Icon name="receipt" className="h-3.5 w-3.5 text-action" />
                    <span>{sub.linked_transaction_count || 0} Paid</span>
                  </PulseButton>

                  {/* Right Action Buttons */}
                  <div className="flex items-center gap-1">
                    {/* Log Payment Quick Button */}
                    <PulseButton variant="soft" size="sm"
                      onClick={() => onLogPayment(sub)}
                      title="Log payment transaction for this subscription"
                    >
                      Log Payment
                    </PulseButton>

                    {/* Edit Button */}
                    <PulseButton variant="icon" size="lg"
                      onClick={() => onEditSubscription(sub)}
                      title="Edit Subscription"
                    >
                      <Icon name="edit" className="size-5" />
                    </PulseButton>

                    {/* Delete Button */}
                    <PulseButton variant="iconDanger" size="lg"
                      onClick={() => onDeleteSubscription(sub)}
                      title="Delete Subscription"
                    >
                      <Icon name="trash" className="size-5" />
                    </PulseButton>
                  </div>
                </div>
              </PulseCard>
            );
          })}
        </div>
      )}
    </section>
  );
}

const metricIconTones = {
  neutral: "bg-lavender/10 text-foreground",
  success: "bg-success/10 text-success",
  warning: "bg-warning/10 text-warning",
};

function SubscriptionMetric({
  title,
  icon,
  tone = "neutral",
  children,
}: {
  title: string;
  icon: IconName;
  tone?: keyof typeof metricIconTones;
  children: ReactNode;
}) {
  return (
    <PulseCard className="relative overflow-hidden p-6">
      <div className="flex items-center justify-between">
        <span className="text-xs font-bold tracking-wider text-muted-foreground uppercase">{title}</span>
        <span className={`grid size-9 shrink-0 place-items-center rounded-xl ${metricIconTones[tone]}`}>
          <Icon name={icon} aria-hidden="true" className="size-5" />
        </span>
      </div>
      <div className="mt-3">{children}</div>
    </PulseCard>
  );
}

function MetricAmount({ value, unit }: { value: string; unit: string }) {
  return (
    <div>
      <span className="text-2xl font-black text-foreground">{value}</span>
      <span className="ms-1 text-xs text-muted-foreground">{unit}</span>
    </div>
  );
}
