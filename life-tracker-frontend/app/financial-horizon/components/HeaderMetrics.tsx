"use client";

import { Icon } from "./HorizonIcon";
import { ArrowDownRight, ArrowUpRight } from "lucide-react";

import Button from "@/app/components/design-system/button";
import PulseCard from "@/app/components/design-system/pulse-card";
import type { HorizonSummary } from "@/app/dashboard/financial-horizon-card";
import { currencySymbol, formatMoney } from "@/app/lib/currency.mjs";

import theme from "../horizon-theme.module.css";

interface HeaderMetricsProps {
  summary: HorizonSummary;
  uncommittedPool: number;
  totalCommitted: number;
  totalCommittedRatio: number;
  isEditingBase: boolean;
  setIsEditingBase: (val: boolean) => void;
  baseInput: string;
  setBaseInput: (val: string) => void;
  handleSaveBaseConfig: (e: React.FormEvent) => void;
  isPending?: boolean;
}

export default function HeaderMetrics({
  summary,
  uncommittedPool,
  totalCommitted,
  totalCommittedRatio,
  isEditingBase,
  setIsEditingBase,
  baseInput,
  setBaseInput,
  handleSaveBaseConfig,
  isPending = false,
}: HeaderMetricsProps) {
  const money = (amount: number) => formatMoney(amount, summary.currency);
  const subscriptionBurn = summary.total_subscription_burn ?? 0;
  const isDeficit = uncommittedPool < 0;
  const PoolIcon = isDeficit ? ArrowDownRight : ArrowUpRight;

  return (
    <section aria-label="Monthly pool summary" className="grid gap-4 lg:grid-cols-[1.3fr_1fr]">
      <PulseCard className={`${theme.hero} relative isolate flex flex-col justify-between gap-8 overflow-hidden p-6 sm:p-8`}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h2 className="flex items-center gap-3 text-base font-semibold sm:text-lg">
            <span className="grid size-10 shrink-0 place-items-center rounded-2xl bg-white/12">
              <PoolIcon aria-hidden="true" className="size-5" />
            </span>
            Net Available Pool
          </h2>
          {uncommittedPool !== 0 && (
            <span className={`rounded-full bg-black/25 px-3 py-1 text-xs font-semibold ${isDeficit ? "text-[#ffc4b8]" : "text-[#adf4d9]"}`}>
              {isDeficit ? "Deficit" : "Surplus"}
            </span>
          )}
        </div>
        <div>
          <p className="wrap-anywhere text-4xl leading-none font-bold tracking-tight tabular-nums sm:text-5xl xl:text-6xl">
            {money(uncommittedPool)}
          </p>
          <p className="mt-3 text-sm text-[#e8defa]">After monthly fixed obligations and subscriptions</p>
        </div>
      </PulseCard>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-1">
        <PulseCard className="p-6">
          <div className="flex items-center justify-between gap-3">
            <h2 className="flex items-center gap-2.5 text-sm font-semibold text-muted-foreground">
              <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-accent text-action">
                <Icon name="wallet" aria-hidden="true" className="size-4" />
              </span>
              Starting Pool (Base Income)
            </h2>
            <Button
              variant="icon"
              size="sm"
              type="button"
              onClick={() => setIsEditingBase(!isEditingBase)}
              title="Edit Base Income"
              aria-label="Edit Base Income"
              aria-expanded={isEditingBase}
              className={theme.iconAction}
            >
              <Icon name="edit" aria-hidden="true" className="size-4" />
            </Button>
          </div>
          {isEditingBase ? (
            <form onSubmit={handleSaveBaseConfig} className="mt-3 space-y-2">
              <div className="flex items-center gap-2">
                <span aria-hidden="true" className="text-sm font-semibold">{currencySymbol(summary.currency)}</span>
                <input
                  aria-label="Monthly base income"
                  type="number"
                  step="0.01"
                  min="0"
                  value={baseInput}
                  onChange={(e) => setBaseInput(e.target.value)}
                  className="min-w-0 flex-1 rounded-xl bg-secondary px-3 py-2 text-sm font-bold text-foreground"
                  autoFocus
                />
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="secondary" size="sm" type="button" className={theme.softAction} onClick={() => setIsEditingBase(false)}>
                  Cancel
                </Button>
                <Button className={theme.primary} size="sm" type="submit" disabled={isPending}>
                  Save
                </Button>
              </div>
            </form>
          ) : (
            <p className="mt-3 wrap-anywhere text-2xl font-bold tracking-tight tabular-nums sm:text-3xl">
              {money(summary.base_amount)}
            </p>
          )}
        </PulseCard>

        <PulseCard className="p-6">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="flex items-center gap-2.5 text-sm font-semibold text-muted-foreground">
              <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-warning-soft text-warning">
                <Icon name="lock" aria-hidden="true" className="size-4" />
              </span>
              Total Committed
            </h2>
            {summary.base_amount > 0 && (
              <span className="rounded-full bg-warning-soft px-2.5 py-1 text-xs font-semibold text-warning">
                {totalCommittedRatio.toFixed(1)}% of pool
              </span>
            )}
          </div>
          <p className="mt-3 wrap-anywhere text-2xl font-bold tracking-tight tabular-nums sm:text-3xl">
            {money(totalCommitted)}
          </p>
          <p className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
            <span>Fixed {money(summary.total_deductions)}</span>
            {subscriptionBurn > 0 && <span>Subscriptions {money(subscriptionBurn)}</span>}
          </p>
        </PulseCard>
      </div>
    </section>
  );
}
