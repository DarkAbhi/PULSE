import { IndianRupee, Wallet } from "lucide-react";
import type { HorizonSummary } from "@/features/financial-horizon";
import QuickActionCard from "@/features/dashboard/components/quick-actions/quick-action-card";
import { formatMoneyParts } from "@/lib/currency/currency.mjs";

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
      tone="finance"
      href="/financial-horizon"
      title="Your room to move"
      description={balance ? "What's left on your financial horizon" : "We couldn't load your financial horizon."}
      icon={summary?.currency === "INR" ? <IndianRupee strokeWidth={2} /> : <Wallet strokeWidth={1.8} />}
      backdrop={
        <svg
          className="absolute inset-x-0 bottom-0 h-1/3 w-full fill-white opacity-50 dark:opacity-35"
          viewBox="0 0 400 80"
          preserveAspectRatio="none"
        >
          <path className="motion-safe:animate-pulse-drift" d="M-30 49Q39 -2 105 36T240 36T375 36T510 36V80H-30Z" />
        </svg>
      }
      footer={balance && (
        <p className="max-w-full text-3xl leading-none font-extrabold tracking-tighter tabular-nums wrap-anywhere @xs:text-4xl @md:text-5xl dashboard-compact:text-3xl">
          {balance.map(({ type, value }, index) =>
            type === "decimal" || type === "fraction"
              ? <span key={index} className="text-sm font-semibold tracking-normal opacity-80 @xs:text-base @md:text-xl dashboard-compact:text-sm">{value}</span>
              : value,
          )}
        </p>
      )}
      actionLabel={balance ? "Keep going" : "Set up horizon"}
    />
  );
}
