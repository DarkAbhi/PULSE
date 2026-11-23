"use client";

import { Icon } from "@/app/financial-horizon/components/horizon-icon";
import PulseCard from "@/app/components/design-system/pulse-card";

import { formatMoney } from "@/app/lib/currency.mjs";

import HorizonAction from "@/app/financial-horizon/components/horizon-action";
import PulseButton from "@/app/components/design-system/pulse-button";
import SectionHeader from "@/app/financial-horizon/components/section-header";
import HorizonTag from "@/app/financial-horizon/components/horizon-tag";
import HorizonEmpty from "@/app/financial-horizon/components/horizon-empty";

import { TrendingDown } from "lucide-react";
import type { HorizonSummary } from "@/app/financial-horizon/horizon-types";
import type { NextMonthPurchaseItem } from "@/app/financial-horizon/horizon-types";

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
        <PulseCard className="p-6">
          <div className="flex items-center gap-2 mb-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-action">
              <Icon name="wallet" className="h-4 w-4" />
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
        <PulseCard className={`p-6 ${
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
              <span className={`ml-auto rounded-full px-2.5 py-0.5 text-xs font-bold ${
                netRemainingPool >= 0
                  ? "bg-success/10 text-success"
                  : "bg-destructive/10 text-destructive"
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
      <PulseCard as="section" className="space-y-6 p-6">
        <SectionHeader
          title="Next Month Planned Purchases"
          icon="bag"
          description="Planned variable purchases subtracted directly from your uncommitted pool for next month."
        >
          <div className="flex flex-wrap items-center gap-3">
            <HorizonTag emphasis tone="warn">
              Total Planned: {formatMoney(purchasesTotal, summary.currency)}
            </HorizonTag>

            <HorizonAction
              onClick={onOpenAddPurchase}
              disabled={isPending}
            >
              <Icon name="plus" /> Add Planned Purchase
            </HorizonAction>

            {purchases.length > 0 && (
              <HorizonAction variant="destructive"
                onClick={onConfirmClearAllPurchases}
                disabled={isPending || isClearingPurchases}
              >
                <Icon name="trash" /> Clear All
              </HorizonAction>
            )}
          </div>
        </SectionHeader>

        {/* List of Next Month Purchases */}
        {purchases.length === 0 ? (
          <HorizonEmpty>
            <Icon name="bag" className="mx-auto h-10 w-10 text-warning/50" />
            <h3 className="mt-3 text-base font-semibold text-foreground">No planned purchases added</h3>
            <p className="mt-1 text-sm text-muted-foreground max-w-md mx-auto">
              Planning major buys next month? Add them here to calculate how much surplus pool remains after accounting for them.
            </p>
            <HorizonAction
              onClick={onOpenAddPurchase}
              className="mt-4"
            >
              <Icon name="plus" /> Add Next Month Purchase
            </HorizonAction>
          </HorizonEmpty>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {purchases.map((item) => (
              <PulseCard
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-3 bg-chip p-6"
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
                      Link <Icon name="external" className="h-3 w-3" />
                    </a>
                  )}
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span className="font-bold text-foreground text-sm">
                    {formatMoney(item.price, summary.currency)}
                  </span>
                  <PulseButton variant="iconChipDanger" size="lg" shape="pill"
                    onClick={() => onConfirmDeletePurchase(item)}
                    title="Delete purchase"
                  >
                    <Icon name="trash" className="size-5" />
                  </PulseButton>
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
