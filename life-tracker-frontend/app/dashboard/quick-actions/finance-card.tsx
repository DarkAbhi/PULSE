import { IndianRupee, Wallet } from "lucide-react";
import type { HorizonSummary } from "@/app/financial-horizon/horizon-types";
import QuickActionCard from "@/app/dashboard/quick-actions/quick-action-card";
import { formatMoneyParts } from "@/app/lib/currency.mjs";

type HorizonBalance = Pick<HorizonSummary, "remaining_amount" | "currency">;

export default async function FinanceCard({ horizon }: { horizon: Promise<HorizonBalance | null> }) {
  const summary = await horizon;
  const balance = summary
    ? formatMoneyParts(
        summary.remaining_amount,
        summary.currency,
        summary.currency === "INR" ? "en-IN" : undefined,
      )
    : null;

  return (
    <QuickActionCard
      href="/financial-horizon"
      title="Your room to move"
      description={balance ? "What's left on your financial horizon" : "We couldn't load your financial horizon."}
      icon={summary?.currency === "INR" ? <IndianRupee strokeWidth={2} /> : <Wallet strokeWidth={1.8} />}
      backdrop={
        <svg
          className="absolute inset-[auto_0_0] w-full h-[33%] fill-white opacity-50 motion-safe:[&_path]:animate-pulse-drift dark:opacity-35"
          viewBox="0 0 400 80"
          preserveAspectRatio="none"
        >
          <path d="M-30 49Q39 -2 105 36T240 36T375 36T510 36V80H-30Z" />
        </svg>
      }
      footer={balance && (
        <p className="max-w-full text-[clamp(1.75rem,11cqi,3.5rem)] font-extrabold leading-none tracking-[-.04em] tabular-nums wrap-anywhere [&_span]:text-[.42em] [&_span]:font-semibold [&_span]:tracking-normal [&_span]:opacity-78 dashboard-compact:text-[clamp(1.5rem,11cqi,2rem)]">
          {balance.map(({ type, value }, index) =>
            type === "decimal" || type === "fraction"
              ? <span key={index}>{value}</span>
              : value,
          )}
        </p>
      )}
      actionLabel={balance ? "Keep going" : "Set up horizon"}
      className="[--tile-start:var(--finance-pool-end)] [--tile-end:var(--finance-pool-end)] [--tile-foreground:var(--finance-pool-end-foreground)] [--tile-action-fg:var(--brand-gold)]"
    />
  );
}
