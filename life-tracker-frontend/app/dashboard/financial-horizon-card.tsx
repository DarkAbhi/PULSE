
import { formatMoney } from "../lib/currency.mjs";
import Link from "next/link";
import { House, ArrowRight } from "lucide-react";

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
  const unavailable = error || !summary;
  return (
    <Link href="/financial-horizon" className="pulse-summary-card pulse-horizon-card">
      <span className="pulse-card-icon" aria-hidden="true"><House className="h-6 w-6" fill="currentColor" /></span>
      <h2>Your room to move</h2>
      {unavailable ? (
        <p className="pulse-card-message">{error || "Set up your monthly income and fixed obligations to see your room to move."}</p>
      ) : (
        <p className="pulse-money">{formatMoney(summary.remaining_amount, summary.currency)}</p>
      )}
      <div className="pulse-card-footer">
        <span className="pulse-card-cta">{unavailable ? "Set up horizon" : "Keep going"}<ArrowRight className="h-4 w-4" /></span>
        <img src="/dashboard/cat.webp" width="240" height="160" alt="" className="pulse-card-art" />
      </div>
    </Link>
  );
}
