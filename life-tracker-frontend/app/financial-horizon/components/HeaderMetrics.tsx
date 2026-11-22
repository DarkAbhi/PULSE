"use client";

import { Icon } from "./HorizonIcon";

import Button from "@/app/components/design-system/button";
import PulseCard from "@/app/components/design-system/pulse-card";
import type { HorizonSummary } from "@/app/dashboard/financial-horizon-card";
import { formatMoney } from "@/app/lib/currency.mjs";

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
  return (
    <section aria-label="Monthly pool summary" className={theme.summary}>
      <PulseCard as="article" className={`${theme.summaryCard} ${theme.startingPool}`}>
        <div className={theme.summaryHeader}>
          <span className={theme.summaryIcon}><Icon name="wallet" /></span>
          <h2 className={theme.summaryLabel}>Starting pool (base income)</h2>
          <Button
            variant="icon"
            type="button"
            onClick={() => setIsEditingBase(!isEditingBase)}
            aria-label="Edit Base Income"
            aria-expanded={isEditingBase}
            className={`${theme.iconAction} ${theme.summaryEditButton}`}
          >
            <Icon name="edit" />
          </Button>
        </div>
        {isEditingBase ? (
          <form onSubmit={handleSaveBaseConfig} className={theme.summaryEditForm}>
            <input
              aria-label="Monthly base income"
              type="number"
              step="0.01"
              min="0"
              value={baseInput}
              onChange={(e) => setBaseInput(e.target.value)}
              autoFocus
            />
            <div className="flex gap-2">
              <Button type="submit" className={theme.softAction} disabled={isPending}>
                Save
              </Button>
              <Button variant="secondary" type="button" className={theme.softAction} onClick={() => setIsEditingBase(false)}>
                Cancel
              </Button>
            </div>
          </form>
        ) : (
          <p className={theme.summaryValue}>{money(summary.base_amount)}</p>
        )}
        <p className={theme.summaryHint}>Baseline monthly revenue stream</p>
      </PulseCard>

      <PulseCard as="article" className={`${theme.summaryCard} ${theme.committedPool}`}>
        <div className={theme.summaryHeader}>
          <span className={theme.summaryIcon}><Icon name="lock" /></span>
          <h2 className={theme.summaryLabel}>Total committed</h2>
          <span className={`${theme.badge} ${theme.summaryBadge}`} data-tone="warn">
            {totalCommittedRatio.toFixed(1)}% of pool
          </span>
        </div>
        <p className={theme.summaryValue}>{money(totalCommitted)}</p>
        <p className={theme.summaryHint}>
          Fixed ({money(summary.total_deductions)}) + Subscriptions ({money(subscriptionBurn)})
        </p>
      </PulseCard>

      <PulseCard as="article" className={`${theme.summaryCard} ${theme.availablePool}`}>
        <div className={theme.summaryHeader}>
          <span className={theme.summaryIcon}><Icon name="spark" /></span>
          <h2 className={theme.summaryLabel}>Net available pool</h2>
          <span className={`${theme.badge} ${theme.summaryBadge}`}>
            {uncommittedPool < 0 ? "Deficit" : "Surplus"}
          </span>
        </div>
        <p className={theme.summaryValue}>{money(uncommittedPool)}</p>
        <p className={theme.summaryHint}>Uncommitted cash remaining after fixed obligations &amp; subscriptions</p>
      </PulseCard>
    </section>
  );
}
