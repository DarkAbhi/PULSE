import Link from "next/link";
import { Compass, ArrowRight } from "lucide-react";

export type BudgetItem = {
  id: number;
  name: string;
  allocated_amount: number;
  used_amount: number;
  available_amount: number;
  usage_percentage: number;
};

export type DeductionItem = {
  id: number;
  name: string;
  category: string;
  amount: number;
  due_day?: number | null;
  is_active: boolean;
  budget_id?: number | null;
};

export type ProjectionItem = {
  months: number;
  label: string;
  cumulative_uncommitted: number;
};

export type CategoryItem = {
  id: number;
  name: string;
  icon: string;
  color: string;
  is_default: boolean;
  user_id?: number | null;
};

export type SubscriptionItem = {
  id: number;
  name: string;
  amount: number;
  billing_cycle: "monthly" | "yearly";
  billing_day?: number | null;
  renewal_date?: string | null;
  next_renewal_date: string;
  monthly_equivalent_amount: number;
  status: "active" | "paused" | "cancelled";
  category_id?: number | null;
  category_name?: string | null;
  budget_id?: number | null;
  budget_name?: string | null;
  deduction_id?: number | null;
  notes?: string | null;
  linked_transaction_count: number;
  total_spent: number;
  created_at: string;
  updated_at: string;
};

export type TransactionItem = {
  id: number;
  name: string;
  amount: number;
  type?: "debit" | "credit";
  transaction_date: string;
  category_id?: number | null;
  category_name: string;
  budget_id?: number | null;
  budget_name?: string | null;
  subscription_id?: number | null;
  subscription_name?: string | null;
  notes?: string | null;
  created_at: string;
};

export type HorizonSummary = {
  base_amount: number;
  currency: string;
  total_deductions: number;
  total_transactions?: number;
  remaining_amount: number;
  committed_ratio: number;
  total_budgets_allocated: number;
  total_subscription_burn?: number;
  budgets: BudgetItem[];
  deductions: DeductionItem[];
  categories?: CategoryItem[];
  transactions?: TransactionItem[];
  subscriptions?: SubscriptionItem[];
  projections: ProjectionItem[];
};

interface FinancialHorizonCardProps {
  summary: HorizonSummary | null;
  error?: string;
}

export default function FinancialHorizonCard({
  summary,
  error,
}: FinancialHorizonCardProps) {
  if (error || !summary) {
    return (
      <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
            <Compass className="h-6 w-6" />
          </div>
          <div>
            <h3 className="text-xl font-semibold text-foreground">
              Financial Horizon
            </h3>
          </div>
        </div>
        <p className="mt-4 text-sm text-muted-foreground">
          {error ??
            "Set up your monthly income and fixed obligations to calculate uncommitted cash flow."}
        </p>
        <Link
          className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-primary"
          href="/financial-horizon"
        >
          Set up horizon <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
    );
  }

  const {
    base_amount,
    currency,
    remaining_amount,
  } = summary;
  const isHealthy = remaining_amount >= 0;

  return (
    <Link
      href="/financial-horizon"
      className="relative block overflow-hidden rounded-2xl border border-border bg-gradient-to-br from-card via-card to-secondary/30 p-6 shadow-sm"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary shadow-inner">
            <Compass className="h-6 w-6" />
          </div>
          <div>
            <h3 className="text-xl font-semibold text-foreground">
              Financial Horizon
            </h3>
            <p className="text-xs text-muted-foreground">
              Your monthly room to move
            </p>
          </div>
        </div>
        <ArrowRight className="h-5 w-5 shrink-0 text-muted-foreground" />
      </div>

      <div className="mt-6 grid grid-cols-2 gap-4 rounded-xl bg-background/60 p-4 backdrop-blur-sm">
        <div>
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
            Starting Pool
          </span>
          <p className="mt-1 text-lg font-bold text-foreground">
            {currency}
            {base_amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
          </p>
        </div>

        <div>
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
            Uncommitted
          </span>
          <p
            className={`mt-1 text-lg font-extrabold ${isHealthy ? "text-emerald-600 dark:text-emerald-400" : "text-rose-600 dark:text-rose-400"}`}
          >
            {currency}
            {remaining_amount.toLocaleString("en-IN", {
              minimumFractionDigits: 2,
            })}
          </p>
        </div>
      </div>
    </Link>
  );
}
