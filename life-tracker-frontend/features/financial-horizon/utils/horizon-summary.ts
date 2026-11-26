import type { BudgetItem, DeductionItem, HorizonSummary, SubscriptionItem, TransactionItem } from "@/features/financial-horizon/types/horizon-types";

export interface HorizonUpdates {
  deductionsUpdater?: (items: DeductionItem[]) => DeductionItem[];
  transactionsUpdater?: (items: TransactionItem[]) => TransactionItem[];
  budgetsUpdater?: (items: BudgetItem[]) => BudgetItem[];
}

export function calculateHorizonSummary(prev: HorizonSummary, { deductionsUpdater, transactionsUpdater, budgetsUpdater }: HorizonUpdates): HorizonSummary {
  const recentTransactions = prev.transactions ?? [];
  const nextTx = transactionsUpdater ? transactionsUpdater(recentTransactions) : recentTransactions;
  const newDeductions = deductionsUpdater ? deductionsUpdater(prev.deductions) : prev.deductions;
  const newBudgetsList = budgetsUpdater ? budgetsUpdater(prev.budgets ?? []) : (prev.budgets ?? []);

  const totalDeductions = newDeductions
    .filter((d) => d.is_active)
    .reduce((sum, d) => sum + d.amount, 0);

  const totalSubscriptionBurn = prev.total_subscription_burn ?? 0;
  const totalFixedObligations = totalDeductions + totalSubscriptionBurn;
  const totalBudgetsAllocated = newBudgetsList.reduce((sum, b) => sum + b.allocated_amount, 0);
  const remainingAmount = prev.base_amount - totalFixedObligations;
  const committedRatio =
    prev.base_amount > 0
      ? Math.round((totalFixedObligations / prev.base_amount) * 10000) / 100
      : 0;

  // Recalculate budget used amounts from active deductions AND transactions
  const budgetUsedMap: Record<number, number> = {};
  newDeductions.forEach((d) => {
    if (d.is_active && d.budget_id) {
      budgetUsedMap[d.budget_id] = (budgetUsedMap[d.budget_id] ?? 0) + d.amount;
    }
  });

  nextTx.forEach((t) => {
    if (t.budget_id) {
      budgetUsedMap[t.budget_id] = (budgetUsedMap[t.budget_id] ?? 0) + t.amount;
    }
  });

  const updatedBudgets = newBudgetsList.map((b) => {
    const usedAmount = budgetUsedMap[b.id] ?? 0;
    const availableAmount = b.allocated_amount - usedAmount;
    const usagePercentage =
      b.allocated_amount > 0
        ? Math.round((usedAmount / b.allocated_amount) * 10000) / 100
        : 0;
    return {
      ...b,
      used_amount: usedAmount,
      available_amount: availableAmount,
      usage_percentage: usagePercentage,
    };
  });

  return {
    ...prev,
    total_deductions: totalDeductions,
    total_budgets_allocated: totalBudgetsAllocated,
    remaining_amount: remainingAmount,
    committed_ratio: committedRatio,
    transactions: nextTx,
    deductions: newDeductions,
    budgets: updatedBudgets,
  };
}

export function updateHorizonSubscriptions(previous: HorizonSummary, update: (items: SubscriptionItem[]) => SubscriptionItem[]): HorizonSummary {
  const subscriptions = update(previous.subscriptions ?? []);
  const totalSubscriptionBurn = subscriptions
    .filter(subscription => subscription.status === "active")
    .reduce((total, subscription) => total + (subscription.monthly_equivalent_amount || subscription.amount), 0);
  const totalFixed = previous.total_deductions + totalSubscriptionBurn;
  return {
    ...previous,
    subscriptions,
    total_subscription_burn: totalSubscriptionBurn,
    remaining_amount: previous.base_amount - totalFixed,
    committed_ratio: previous.base_amount > 0
      ? Math.round((totalFixed / previous.base_amount) * 10000) / 100
      : 0,
  };
}
