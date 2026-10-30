"use client";

import PulseCard from "../../components/design-system/pulse-card";

import { formatMoney } from "../../lib/currency.mjs";

import Button from "../../components/design-system/button";

import { ShoppingBag, Plus, Trash2, ExternalLink, Wallet, TrendingDown } from "lucide-react";
import { HorizonSummary } from "../../dashboard/financial-horizon-card";
import { NextMonthPurchaseItem } from "../financial-horizon-client";

interface PlannedPurchasesTabProps {
  summary: HorizonSummary;
  purchases: NextMonthPurchaseItem[];
  purchasesTotal: number;
  uncommittedPool: number;
  netRemainingPool: number;
  onOpenAddPurchase: () => void;
  onConfirmDeletePurchase: (item: NextMonthPurchaseItem) => void;
  onConfirmClearAllPurchases: () => void;
  isPending?: boolean;
  isClearingPurchases?: boolean;
}

export default function PlannedPurchasesTab({
  summary,
  purchases,
  purchasesTotal,
  uncommittedPool,
  netRemainingPool,
  onOpenAddPurchase,
  onConfirmDeletePurchase,
  onConfirmClearAllPurchases,
  isPending = false,
  isClearingPurchases = false,
}: PlannedPurchasesTabProps) {
  const hasPurchases = purchases.length > 0;

  return (
    <div className="space-y-6">
      {/* Pool Impact Summary */}
      <section className="grid gap-4 sm:grid-cols-2">
        {/* Pool Before Purchases */}
        <PulseCard className="p-5">
          <div className="flex items-center gap-2 mb-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-action">
              <Wallet className="h-4 w-4" />
            </div>
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Pool Before Purchases
            </span>
          </div>
          <p className="text-2xl font-extrabold text-foreground tracking-tight">
            {formatMoney(uncommittedPool, summary.currency)}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            Uncommitted cash after fixed obligations &amp; subscriptions
          </p>
        </PulseCard>

        {/* Pool After Purchases */}
        <PulseCard className={`p-5 ${
          !hasPurchases
            ? ""
            : netRemainingPool >= 0
            ? "bg-gradient-to-br from-card via-card to-success/10"
            : "bg-gradient-to-br from-card via-card to-destructive/10"
        }`}>
          <div className="flex items-center gap-2 mb-3">
            <div className={`flex h-8 w-8 items-center justify-center rounded-xl ${
              !hasPurchases
                ? "bg-muted/40 text-muted-foreground"
                : netRemainingPool >= 0
                ? "bg-success/20 text-success"
                : "bg-destructive/20 text-destructive"
            }`}>
              <TrendingDown className="h-4 w-4" />
            </div>
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Pool After Purchases
            </span>
            {hasPurchases && (
              <span className={`ml-auto rounded-full px-2.5 py-0.5 text-xs font-bold border ${
                netRemainingPool >= 0
                  ? "bg-success/10 border-success/30 text-success"
                  : "bg-destructive/10 border-destructive/30 text-destructive"
              }`}>
                {netRemainingPool >= 0 ? "Surplus" : "Deficit"}
              </span>
            )}
          </div>
          <p className={`text-2xl font-extrabold tracking-tight ${
            !hasPurchases
              ? "text-muted-foreground"
              : netRemainingPool >= 0
              ? "text-success dark:text-success"
              : "text-destructive dark:text-destructive"
          }`}>
            {formatMoney((hasPurchases ? netRemainingPool : uncommittedPool), summary.currency)}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {hasPurchases
              ? `After deducting ${formatMoney(purchasesTotal, summary.currency)} in planned purchases`
              : "Add planned purchases below to see the impact"}
          </p>
        </PulseCard>
      </section>

      <div className="space-y-10">
      {/* Planned Purchases Section */}
      <PulseCard as="section" className="p-6 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
          <div>
            <div className="flex items-center gap-2">
              <ShoppingBag className="h-5 w-5 text-warning" />
              <h2 className="text-xl font-bold text-foreground">Next Month Planned Purchases</h2>
            </div>
            <p className="mt-1 text-sm text-muted-foreground">
              Planned variable purchases subtracted directly from your uncommitted pool for next month.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-warning/10 px-3 py-1.5 text-xs font-bold text-warning border border-warning/20">
              Total Planned: {formatMoney(purchasesTotal, summary.currency)}
            </div>

            <Button variant="warning" size="sm"
              onClick={onOpenAddPurchase}
              disabled={isPending}
              className="active:scale-95"
            >
              <Plus className="h-4 w-4" /> Add Planned Purchase
            </Button>

            {purchases.length > 0 && (
              <Button variant="destructiveOutline" size="sm"
                onClick={onConfirmClearAllPurchases}
                disabled={isPending || isClearingPurchases}
              >
                <Trash2 className="h-3.5 w-3.5" /> Clear All
              </Button>
            )}
          </div>
        </div>

        {/* List of Next Month Purchases */}
        {purchases.length === 0 ? (
          <PulseCard className="bg-background/50 p-10 text-center">
            <ShoppingBag className="mx-auto h-10 w-10 text-warning/50" />
            <h3 className="mt-3 text-base font-semibold text-foreground">No planned purchases added</h3>
            <p className="mt-1 text-sm text-muted-foreground max-w-md mx-auto">
              Planning major buys next month? Add them here to calculate how much surplus pool remains after accounting for them.
            </p>
            <Button variant="warning" size="md"
              onClick={onOpenAddPurchase}
              className="mt-4"
            >
              <Plus className="h-4 w-4" /> Add Next Month Purchase
            </Button>
          </PulseCard>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {purchases.map((item) => (
              <PulseCard
                key={item.id}
                className="flex items-center justify-between gap-3 bg-background p-4 transition hover:shadow-md"
              >
                <div className="min-w-0 flex-1">
                  <h4 className="font-semibold text-foreground text-sm truncate">{item.name}</h4>
                  {item.url && (
                    <a
                      href={item.url}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-0.5 inline-flex items-center gap-1 text-xs font-medium text-action hover:underline"
                    >
                      Link <ExternalLink className="h-3 w-3" />
                    </a>
                  )}
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span className="font-bold text-foreground text-sm">
                    {formatMoney(item.price, summary.currency)}
                  </span>
                  <Button variant="iconDanger"
                    onClick={() => onConfirmDeletePurchase(item)}
                    title="Delete purchase"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </PulseCard>
            ))}
          </div>
        )}
      </PulseCard>
      </div>
    </div>
  );
}
