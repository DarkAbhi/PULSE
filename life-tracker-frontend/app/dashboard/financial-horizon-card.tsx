import { PulseCardLink } from "@/app/components/design-system/pulse-card";
import type { HorizonSummary } from "@/app/financial-horizon/horizon-types";
import { formatMoneyParts } from "@/app/lib/currency.mjs";
import { ArrowRight } from "lucide-react";

const horizonAmountClass =
  "max-w-full text-[clamp(2rem,13cqi,5rem)] font-extrabold leading-none tracking-[-.04em] tabular-nums wrap-anywhere " +
  "[&_span]:text-[.38em] [&_span]:font-semibold [&_span]:tracking-normal [&_span]:opacity-78";

const horizonCardClass =
  "@container relative isolate flex flex-col min-h-50 overflow-hidden rounded-[32px_32px_32px_clamp(56px,8vw,96px)] " +
  "bg-(--finance-pool-end) text-(--finance-pool-end-foreground) shadow-(--dashboard-finance-shadow)";

interface FinancialHorizonCardProps {
  summary: Pick<HorizonSummary, "remaining_amount" | "currency"> | null;
}

export default function FinancialHorizonCard({
  summary,
}: FinancialHorizonCardProps) {
  const balance = summary
    ? formatMoneyParts(
        summary.remaining_amount,
        summary.currency,
        summary.currency === "INR" ? "en-IN" : undefined,
      )
    : null;

  return (
    <PulseCardLink href="/financial-horizon" className={horizonCardClass}>
      <svg
        className="absolute inset-[auto_0_0] -z-1 w-full h-[33%] pointer-events-none fill-white opacity-50 motion-safe:[&_path]:animate-pulse-drift dark:opacity-35"
        aria-hidden="true"
        viewBox="0 0 400 80"
        preserveAspectRatio="none"
      >
        <path d="M-30 49Q39 -2 105 36T240 36T375 36T510 36V80H-30Z" />
      </svg>
      <div className="flex flex-1 flex-col items-start gap-4 p-[clamp(20px,5cqi,32px)]">
        <h2 className="mb-auto text-[clamp(1.125rem,4cqi,1.4rem)] font-semibold">Your room to move</h2>
        {balance ? (
          <p className={horizonAmountClass}>
            {balance.map(({ type, value }, index) =>
              type === "decimal" || type === "fraction"
                ? <span key={index}>{value}</span>
                : value,
            )}
          </p>
        ) : (
          <p className="max-w-[45ch] text-control leading-normal wrap-anywhere">We couldn&apos;t load your financial horizon.</p>
        )}
        <span className="inline-flex shrink-0 justify-center items-center gap-2 rounded-full bg-(--dashboard-finance-action) py-3 px-4 min-h-11 text-sm font-medium hover:brightness-95">
          {balance ? "Keep going" : "Set up horizon"}
          <ArrowRight className="h-4 w-4" aria-hidden="true" />
        </span>
      </div>
    </PulseCardLink>
  );
}
