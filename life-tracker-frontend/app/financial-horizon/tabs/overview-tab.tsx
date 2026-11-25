"use client";

import { Icon } from "@/app/financial-horizon/components/horizon-icon";
import { ArrowUpRight } from "lucide-react";

import PulseCard from "@/app/components/design-system/pulse-card";
import { formatMoney } from "@/app/lib/currency.mjs";
import HorizonIconTile from "@/app/financial-horizon/components/horizon-icon-tile";

const projectionMonths = [3, 6, 12];

interface OverviewTabProps {
  currency: string;
  uncommittedPool: number;
}

export default function OverviewTab({ currency, uncommittedPool }: OverviewTabProps) {
  return (
    <PulseCard as="section" aria-labelledby="horizon-projections-title" padding="md">
      <div className="flex items-start gap-3">
        <HorizonIconTile>
          <ArrowUpRight aria-hidden="true" className="h-5 w-5" />
        </HorizonIconTile>
        <div>
          <h2 id="horizon-projections-title" className="text-(length:--finance-section-heading) font-extrabold leading-tight tracking-tight">
            Future Horizon Projections
          </h2>
          <p className="text-sm text-muted-foreground">Estimated cash you could set aside</p>
        </div>
      </div>

      <dl className="mt-5 grid gap-3 sm:grid-cols-3">
        {projectionMonths.map((months) => (
          <div key={months} className="min-w-0 rounded-2xl bg-secondary p-4 sm:p-5">
            <dt className="text-sm font-semibold text-muted-foreground">{months} months</dt>
            <dd className="mt-1 wrap-anywhere text-2xl font-bold tracking-tight tabular-nums lg:text-3xl">
              {formatMoney(Math.max(0, uncommittedPool * months), currency)}
              <span aria-hidden="true" className="mt-3 block h-1.5 overflow-hidden rounded-full bg-surface-strong">
                <span
                  className={`bg-(image:--primary-gradient) forced-colors:[background:Highlight] forced-colors:forced-color-adjust-none block h-full rounded-full`}
                  style={{ width: `${uncommittedPool > 0 ? months / 12 * 100 : 0}%` }}
                />
              </span>
            </dd>
          </div>
        ))}
      </dl>

      <div className="mt-5 flex items-start gap-2.5 border-t border-divider pt-4 text-sm">
        <Icon name="info" aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-action" />
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
