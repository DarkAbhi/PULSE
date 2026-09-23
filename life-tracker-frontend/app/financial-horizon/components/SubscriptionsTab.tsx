"use client";

import Button from "../../components/design-system/button";

import { useState, useMemo } from "react";
import {
  CreditCard,
  Plus,
  Search,
  Calendar,
  Clock,
  CheckCircle2,
  PauseCircle,
  XCircle,
  Receipt,
  MoreVertical,
  ExternalLink,
  Edit2,
  Trash2,
  ChevronRight,
  TrendingUp,
  Flame,
  AlertTriangle,
} from "lucide-react";
import {
  SubscriptionItem,
  BudgetItem,
  CategoryItem,
  TransactionItem,
} from "../../dashboard/financial-horizon-card";

interface SubscriptionsTabProps {
  subscriptions: SubscriptionItem[];
  categories: CategoryItem[];
  budgets: BudgetItem[];
  currency?: string;
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
  currency = "₹",
  onAddSubscription,
  onEditSubscription,
  onDeleteSubscription,
  onToggleStatus,
  onLogPayment,
  onViewTransactions,
}: SubscriptionsTabProps) {
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
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const renewal = new Date(dateStr);
    renewal.setHours(0, 0, 0, 0);

    const diffTime = renewal.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "active":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-bold text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="h-3 w-3" /> Active
          </span>
        );
      case "paused":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-bold text-amber-600 dark:text-amber-400 border border-amber-500/20">
            <PauseCircle className="h-3 w-3" /> Paused
          </span>
        );
      case "cancelled":
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-xs font-bold text-muted-foreground border border-border">
            <XCircle className="h-3 w-3" /> Cancelled
          </span>
        );
      default:
        return null;
    }
  };

  const getCycleBadge = (cycle: string) => {
    if (cycle === "yearly") {
      return (
        <span className="rounded-md bg-purple-500/10 px-2 py-0.5 text-xs font-semibold text-purple-600 dark:text-purple-400">
          Yearly
        </span>
      );
    }
    return (
      <span className="rounded-md bg-sky-500/10 px-2 py-0.5 text-xs font-semibold text-sky-600 dark:text-sky-400">
        Monthly
      </span>
    );
  };

  return (
    <div className="space-y-6">
      {/* 1. Header Metrics Grid */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Metric 1: Monthly Burn Rate */}
        <div className="relative overflow-hidden rounded-2xl border border-border/80 bg-card p-5 shadow-xs transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold tracking-wider text-muted-foreground uppercase">
              Monthly Subscription Burn
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
              <Flame className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-foreground">
              {currency}
              {totalMonthlyBurn.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </span>
            <span className="text-xs text-muted-foreground ml-1">/ month</span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            Normalized cost across all active subscriptions
          </p>
        </div>

        {/* Metric 2: Annual Outlay */}
        <div className="relative overflow-hidden rounded-2xl border border-border/80 bg-card p-5 shadow-xs transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold tracking-wider text-muted-foreground uppercase">
              Annual Outlay
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-purple-500/10 text-purple-600 dark:text-purple-400">
              <TrendingUp className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-foreground">
              {currency}
              {totalYearlyOutlay.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </span>
            <span className="text-xs text-muted-foreground ml-1">/ year</span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            Total projected yearly commitment
          </p>
        </div>

        {/* Metric 3: Active Subscriptions */}
        <div className="relative overflow-hidden rounded-2xl border border-border/80 bg-card p-5 shadow-xs transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold tracking-wider text-muted-foreground uppercase">
              Active Subscriptions
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
              <CreditCard className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-black text-foreground">
              {activeSubs.length}
            </span>
            <span className="text-xs text-muted-foreground">
              active ({subscriptions.length} total)
            </span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {subscriptions.length - activeSubs.length} paused or cancelled
          </p>
        </div>

        {/* Metric 4: Next Renewal */}
        <div className="relative overflow-hidden rounded-2xl border border-border/80 bg-card p-5 shadow-xs transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold tracking-wider text-muted-foreground uppercase">
              Next Upcoming Renewal
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
              <Calendar className="h-5 w-5" />
            </div>
          </div>
          {nearestRenewal ? (
            <div className="mt-3">
              <div className="flex items-center justify-between">
                <span className="truncate font-bold text-foreground max-w-[140px]">
                  {nearestRenewal.name}
                </span>
                <span className="text-xs font-bold text-foreground">
                  {currency}
                  {nearestRenewal.amount.toLocaleString()}
                </span>
              </div>
              <p className="mt-1 text-xs font-medium text-amber-600 dark:text-amber-400">
                Renews on{" "}
                {new Date(nearestRenewal.next_renewal_date).toLocaleDateString(
                  undefined,
                  { month: "short", day: "numeric" }
                )}{" "}
                ({getDaysUntilRenewal(nearestRenewal.next_renewal_date)}d left)
              </p>
            </div>
          ) : (
            <div className="mt-3">
              <span className="text-sm font-semibold text-muted-foreground">
                No active renewals
              </span>
            </div>
          )}
        </div>
      </div>

      {/* 2. Control & Search Bar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search subscriptions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-xl border border-border/80 bg-card pl-10 pr-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary transition"
          />
        </div>

        {/* Filters & Add Button */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Cycle Filter */}
          <select
            value={cycleFilter}
            onChange={(e) => setCycleFilter(e.target.value)}
            className="rounded-xl border border-border/80 bg-card px-3 py-2.5 text-xs font-semibold text-foreground focus:border-primary focus:outline-hidden"
          >
            <option value="all">All Cadences</option>
            <option value="monthly">Monthly</option>
            <option value="yearly">Yearly</option>
          </select>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-xl border border-border/80 bg-card px-3 py-2.5 text-xs font-semibold text-foreground focus:border-primary focus:outline-hidden"
          >
            <option value="all">All Statuses</option>
            <option value="active">Active</option>
            <option value="paused">Paused</option>
            <option value="cancelled">Cancelled</option>
          </select>

          {/* Add Subscription Button */}
          <Button variant="primary" size="sm"
            onClick={onAddSubscription}
          >
            <Plus className="h-4 w-4" /> Add Subscription
          </Button>
        </div>
      </div>

      {/* 3. Subscriptions Grid */}
      {filteredSubscriptions.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border p-12 text-center bg-card/50">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary text-muted-foreground mb-3">
            <CreditCard className="h-6 w-6" />
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
            <Button variant="primary" size="sm"
              onClick={onAddSubscription}
              className="mt-4"
            >
              <Plus className="h-4 w-4" /> Add Your First Subscription
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredSubscriptions.map((sub) => {
            const daysLeft = getDaysUntilRenewal(sub.next_renewal_date);

            return (
              <div
                key={sub.id}
                className={`group relative flex flex-col justify-between rounded-2xl border bg-card p-5 shadow-xs transition duration-200 hover:shadow-md ${
                  sub.status === "cancelled"
                    ? "border-border/50 opacity-60"
                    : sub.status === "paused"
                    ? "border-amber-500/30 bg-amber-500/5"
                    : "border-border/80 hover:border-primary/50"
                }`}
              >
                <div>
                  {/* Card Header: Title & Badges */}
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-bold text-foreground text-base tracking-tight group-hover:text-primary transition">
                          {sub.name}
                        </h4>
                      </div>
                      <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                        {getStatusBadge(sub.status)}
                        {getCycleBadge(sub.billing_cycle)}
                        {sub.category_name && (
                          <span className="rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
                            {sub.category_name}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Price Tag */}
                    <div className="text-right shrink-0">
                      <span className="text-lg font-black text-foreground">
                        {currency}
                        {sub.amount.toLocaleString()}
                      </span>
                      <span className="text-xs text-muted-foreground font-medium block">
                        / {sub.billing_cycle === "yearly" ? "year" : "month"}
                      </span>
                    </div>
                  </div>

                  {/* Monthly Equivalent if yearly */}
                  {sub.billing_cycle !== "monthly" && sub.status === "active" && (
                    <div className="mt-2 text-xs text-muted-foreground font-medium bg-secondary/50 rounded-lg px-2.5 py-1 inline-block">
                      ≈ {currency}
                      {sub.monthly_equivalent_amount.toLocaleString()}/mo equivalent
                    </div>
                  )}

                  {/* Details: Start Date & Renewal */}
                  <div className="mt-4 space-y-2 border-t border-border/60 pt-3 text-xs">
                    {/* Cycle Specific Detail (Billing Day for Monthly, Renewal Date for Yearly) */}
                    <div className="flex items-center justify-between text-muted-foreground">
                      <span className="flex items-center gap-1.5">
                        <Clock className="h-3.5 w-3.5 text-muted-foreground" />
                        {sub.billing_cycle === "yearly" ? "Annual Renewal Date:" : "Billing Day of Month:"}
                      </span>
                      <span className="font-semibold text-foreground">
                        {sub.billing_cycle === "yearly"
                          ? new Date(sub.renewal_date || sub.next_renewal_date).toLocaleDateString(undefined, {
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
                          <Calendar className="h-3.5 w-3.5 text-primary" />
                          Next Renewal:
                        </span>
                        <span
                          className={`font-semibold ${
                            daysLeft <= 3
                              ? "text-rose-600 dark:text-rose-400 font-bold"
                              : daysLeft <= 7
                              ? "text-amber-600 dark:text-amber-400"
                              : "text-foreground"
                          }`}
                        >
                          {new Date(sub.next_renewal_date).toLocaleDateString(
                            undefined,
                            { month: "short", day: "numeric", year: "numeric" }
                          )}{" "}
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
                <div className="mt-5 pt-3 border-t border-border/60 flex items-center justify-between gap-2">
                  {/* Left: Linked Transactions Trigger */}
                  <Button variant="tertiary" size="sm"
                    onClick={() => onViewTransactions(sub)}
                    title="View payment history"
                  >
                    <Receipt className="h-3.5 w-3.5 text-primary" />
                    <span>{sub.linked_transaction_count || 0} Paid</span>
                  </Button>

                  {/* Right Action Buttons */}
                  <div className="flex items-center gap-1">
                    {/* Log Payment Quick Button */}
                    <Button variant="soft" size="sm"
                      onClick={() => onLogPayment(sub)}
                      title="Log payment transaction for this subscription"
                    >
                      Log Payment
                    </Button>

                    {/* Edit Button */}
                    <Button variant="icon"
                      onClick={() => onEditSubscription(sub)}
                      title="Edit Subscription"
                    >
                      <Edit2 className="h-3.5 w-3.5" />
                    </Button>

                    {/* Delete Button */}
                    <Button variant="iconDanger" size="sm"
                      onClick={() => onDeleteSubscription(sub)}
                      title="Delete Subscription"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
