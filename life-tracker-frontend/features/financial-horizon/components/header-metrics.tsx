"use client";

import type { ReactNode } from "react";
import { Icon, type IconName } from "@/features/financial-horizon/components/horizon-icon";

import HorizonAction from "@/features/financial-horizon/components/horizon-action";
import PulseIconButton from "@/components/ui/pulse-icon-button";
import PulseCard, { type PulseCardVariant } from "@/components/ui/pulse-card";
import type { HorizonSummary } from "@/features/financial-horizon/types/horizon-types";
import { formatMoney } from "@/lib/currency/currency.mjs";

import HorizonTag from "@/features/financial-horizon/components/horizon-tag";

const poolAmounts = "text-(length:--finance-pool-value-size) leading-tight font-extrabold tracking-tight wrap-anywhere tabular-nums";
const poolEditAction = "bg-white/20! px-3.5! text-white! enabled:hover:bg-white/30!";

const poolTypes = {
  starting: {
    variant: "tile",
    surface: "[--tile-start:var(--finance-pool-start-from)] [--tile-end:var(--finance-pool-start-to)]",
    icon: "bg-white/20",
    label: "text-white/85",
    description: "text-white/85",
  },
  committed: {
    variant: "surface",
    surface: "",
    icon: "bg-chip",
    label: "",
    description: "text-muted-foreground",
  },
  available: {
    variant: "tile",
    surface: "[--tile-start:var(--finance-pool-end)] [--tile-end:var(--finance-pool-end)] [--tile-foreground:var(--finance-pool-end-foreground)]",
    icon: "bg-(--finance-pool-end-foreground)/12",
    label: "",
    description: "opacity-85",
  },
} satisfies Record<string, { variant: PulseCardVariant; surface: string; icon: string; label: string; description: string }>;

function PoolCard({
  type,
  title,
  icon,
  action,
  description,
  children,
}: {
  type: keyof typeof poolTypes;
  title: string;
  icon: IconName;
  action: ReactNode;
  description: ReactNode;
  children: ReactNode;
}) {
  const appearance = poolTypes[type];

  return (
    <PulseCard as="article" variant={appearance.variant} padding="md" className={`flex flex-col gap-3 lg:min-h-46 ${appearance.surface}`}>
      <div className="flex min-h-11 items-center gap-3">
        <span className={`grid size-10 shrink-0 place-items-center rounded-control ${appearance.icon}`}>
          <Icon name={icon} />
        </span>
        <h2 className={`text-caption leading-tight font-bold tracking-wider uppercase ${appearance.label}`}>
          {title}
        </h2>
        {action}
      </div>
      {children}
      <p className={`text-sm leading-snug lg:min-h-[2.7em] ${appearance.description}`}>{description}</p>
    </PulseCard>
  );
}

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
    <section aria-label="Monthly pool summary" className="grid gap-5 lg:grid-cols-3">
      <PoolCard
        type="starting"
        title="Starting pool (base income)"
        icon="wallet"
        description="Baseline monthly revenue stream"
        action={
          <PulseIconButton size="lg"
            type="button"
            onClick={() => setIsEditingBase(!isEditingBase)}
            aria-label="Edit Base Income"
            aria-expanded={isEditingBase}
            className="ms-auto text-white! enabled:hover:bg-white/20! enabled:hover:text-white!"
          >
            <Icon name="edit" className="size-5" />
          </PulseIconButton>
        }
      >
        {isEditingBase ? (
          <form onSubmit={handleSaveBaseConfig} className="flex flex-wrap items-center gap-2">
            <input
              aria-label="Monthly base income"
              className="h-11 min-w-0 flex-[1_1_6.5rem] rounded-control border-2 border-white/60 bg-white/15 px-3.5 text-section font-extrabold text-white"
              type="number"
              step="0.01"
              min="0"
              value={baseInput}
              onChange={(e) => setBaseInput(e.target.value)}
              autoFocus
            />
            <div className="flex gap-2">
              <HorizonAction type="submit" variant="secondary" className={poolEditAction} disabled={isPending}>
                Save
              </HorizonAction>
              <HorizonAction type="button" variant="secondary" className={poolEditAction} onClick={() => setIsEditingBase(false)}>
                Cancel
              </HorizonAction>
            </div>
          </form>
        ) : (
          <p className={poolAmounts}>{money(summary.base_amount)}</p>
        )}
      </PoolCard>

      <PoolCard
        type="committed"
        title="Total committed"
        icon="lock"
        description={`Fixed (${money(summary.total_deductions)}) + Subscriptions (${money(subscriptionBurn)})`}
        action={
          <HorizonTag emphasis tone="warn" className="ms-auto whitespace-nowrap data-[tone=warn]:text-(--finance-budget-warning)">
            {totalCommittedRatio.toFixed(1)}% of pool
          </HorizonTag>
        }
      >
        <p className={poolAmounts}>{money(totalCommitted)}</p>
      </PoolCard>

      <PoolCard
        type="available"
        title="Net available pool"
        icon="spark"
        description="Uncommitted cash remaining after fixed obligations & subscriptions"
        action={
          <HorizonTag emphasis className="ms-auto whitespace-nowrap bg-(--finance-pool-end-foreground)/14!">
            {uncommittedPool < 0 ? "Deficit" : "Surplus"}
          </HorizonTag>
        }
      >
        <p className={poolAmounts}>{money(uncommittedPool)}</p>
      </PoolCard>
    </section>
  );
}
