"use client";

import { ArrowUpRight, Info } from "lucide-react";

import PulseCard from "@/app/components/design-system/pulse-card";
import { formatMoney } from "@/app/lib/currency.mjs";

interface OverviewTabProps {
  currency: string;
  uncommittedPool: number;
}

export default function OverviewTab({ currency, uncommittedPool }: OverviewTabProps) {
  return (
    <PulseCard as="section" aria-labelledby="horizon-projections-title" className="p-4 sm:p-6">
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-action">
          <ArrowUpRight aria-hidden="true" className="h-5 w-5" />
        </div>
        <div>
          <h2 id="horizon-projections-title" className="text-lg font-bold text-foreground sm:text-xl">
            Future Horizon Projections
          </h2>
          <p className="text-sm text-muted-foreground">Estimated cash you could set aside</p>
        </div>
      </div>

      <dl className="mt-5 divide-y divide-divider rounded-xl bg-background/50 sm:grid sm:grid-cols-3 sm:divide-x sm:divide-y-0">
        {[3, 6, 12].map((months) => (
          <div key={months} className="flex min-w-0 flex-wrap items-baseline justify-between gap-x-3 gap-y-1 p-4 sm:block">
            <dt className="text-sm font-medium text-muted-foreground">{months} months</dt>
            <dd className="min-w-0 wrap-anywhere text-xl font-bold tracking-tight text-foreground tabular-nums sm:mt-1 sm:text-2xl">
              {formatMoney(Math.max(0, uncommittedPool * months), currency)}
            </dd>
          </div>
        ))}
      </dl>

      <div className="mt-4 flex items-start gap-2.5 rounded-xl bg-secondary/50 p-3 text-sm">
        <Info aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-action" />
        <div>
          <p className="font-semibold text-foreground">Projection assumptions</p>
          <p className="mt-0.5 text-muted-foreground">
            Estimates assume the same monthly income, fixed obligations, and subscriptions.
            Allow separately for day-to-day spending and planned purchases. A monthly shortfall is shown as zero savings.
          </p>
        </div>
      </div>
    </PulseCard>
  );
}
