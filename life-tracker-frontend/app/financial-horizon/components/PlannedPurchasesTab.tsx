"use client";

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
        <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Wallet className="h-4 w-4" />
            </div>
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Pool Before Purchases
            </span>
          </div>
          <p className="text-2xl font-extrabold text-foreground tracking-tight">
            {summary.currency}{uncommittedPool.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            Uncommitted cash after fixed obligations &amp; subscriptions
          </p>
        </div>

        {/* Pool After Purchases */}
        <div className={`rounded-2xl border p-5 shadow-sm ${
          !hasPurchases
            ? "border-border bg-card"
            : netRemainingPool >= 0
            ? "border-emerald-500/30 bg-gradient-to-br from-card via-card to-emerald-500/10"
            : "border-rose-500/30 bg-gradient-to-br from-card via-card to-rose-500/10"
        }`}>
          <div className="flex items-center gap-2 mb-3">
            <div className={`flex h-8 w-8 items-center justify-center rounded-xl ${
              !hasPurchases
                ? "bg-muted/40 text-muted-foreground"
                : netRemainingPool >= 0
                ? "bg-emerald-500/20 text-emerald-600 dark:text-emerald-400"
                : "bg-rose-500/20 text-rose-600 dark:text-rose-400"
            }`}>
              <TrendingDown className="h-4 w-4" />
            </div>
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Pool After Purchases
            </span>
            {hasPurchases && (
              <span className={`ml-auto rounded-full px-2.5 py-0.5 text-xs font-bold border ${
                netRemainingPool >= 0
                  ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400"
                  : "bg-rose-500/10 border-rose-500/30 text-rose-600 dark:text-rose-400"
              }`}>
                {netRemainingPool >= 0 ? "Surplus" : "Deficit"}
              </span>
            )}
          </div>
          <p className={`text-2xl font-extrabold tracking-tight ${
            !hasPurchases
              ? "text-muted-foreground"
              : netRemainingPool >= 0
              ? "text-emerald-600 dark:text-emerald-400"
              : "text-rose-600 dark:text-rose-400"
          }`}>
            {summary.currency}{(hasPurchases ? netRemainingPool : uncommittedPool).toLocaleString("en-IN", { minimumFractionDigits: 2 })}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {hasPurchases
              ? `After deducting ${summary.currency}${purchasesTotal.toLocaleString("en-IN", { minimumFractionDigits: 2 })} in planned purchases`
              : "Add planned purchases below to see the impact"}
          </p>
        </div>
      </section>

      <div className="space-y-10">
      {/* Planned Purchases Section */}
      <section className="rounded-3xl border border-border bg-card p-6 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
          <div>
            <div className="flex items-center gap-2">
              <ShoppingBag className="h-5 w-5 text-amber-500" />
              <h2 className="text-xl font-bold text-foreground">Next Month Planned Purchases</h2>
            </div>
            <p className="mt-1 text-sm text-muted-foreground">
              Planned variable purchases subtracted directly from your uncommitted pool for next month.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-amber-500/10 px-3 py-1.5 text-xs font-bold text-amber-600 dark:text-amber-400 border border-amber-500/20">
              Total Planned: {summary.currency}{purchasesTotal.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
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
          <div className="rounded-2xl border border-dashed border-border bg-background/50 p-10 text-center">
            <ShoppingBag className="mx-auto h-10 w-10 text-amber-500/50" />
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
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {purchases.map((item) => (
              <div
                key={item.id}
                className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-background p-4 shadow-xs transition hover:shadow-md"
              >
                <div className="min-w-0 flex-1">
                  <h4 className="font-semibold text-foreground text-sm truncate">{item.name}</h4>
                  {item.url && (
                    <a
                      href={item.url}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-0.5 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                    >
                      Link <ExternalLink className="h-3 w-3" />
                    </a>
                  )}
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span className="font-bold text-foreground text-sm">
                    {summary.currency}{item.price.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                  </span>
                  <Button variant="iconDanger"
                    onClick={() => onConfirmDeletePurchase(item)}
                    title="Delete purchase"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
      </div>
    </div>
  );
}
