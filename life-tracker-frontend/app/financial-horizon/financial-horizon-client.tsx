"use client";

import { useState, useTransition } from "react";
import Link from "next/link";
import { ArrowLeft, Compass, X } from "lucide-react";

import ConfirmationDialog from "../components/design-system/confirmation-dialog";
import TransactionDialog from "./transaction-dialog";
import CategoryDialog from "./category-dialog";
import BudgetDialog from "./budget-dialog";
import PlannedPurchaseDialog from "./components/PlannedPurchaseDialog";
import StatementUploadDialog from "./components/StatementUploadDialog";

import QuickActionDropdown from "./components/QuickActionDropdown";
import TabNavigation, { HorizonTab } from "./components/TabNavigation";
import HeaderMetrics from "./components/HeaderMetrics";
import OverviewTab from "./components/OverviewTab";
import TransactionsTab from "./components/TransactionsTab";
import FixedObligationsTab from "./components/FixedObligationsTab";
import PlannedPurchasesTab from "./components/PlannedPurchasesTab";

import { HorizonSummary, DeductionItem, BudgetItem, CategoryItem, TransactionItem } from "../dashboard/financial-horizon-card";
import {
  updateHorizonConfigAction,
  addDeductionAction,
  updateDeductionAction,
  deleteDeductionAction,
  addNextMonthPurchaseHorizonAction,
  deleteNextMonthPurchaseAction,
  clearAllNextMonthPurchasesAction,
  addBudgetAction,
  updateBudgetAction,
  deleteBudgetAction,
  addHorizonCategoryAction,
  addTransactionAction,
  bulkAddTransactionsAction,
  updateTransactionAction,
  deleteTransactionAction,
} from "./actions";

export type NextMonthPurchaseItem = {
  id: number;
  name: string;
  price: number;
  url: string | null;
};

interface FinancialHorizonClientProps {
  initialSummary: HorizonSummary;
  initialPurchases: NextMonthPurchaseItem[];
  initialPurchasesTotal: number;
}

export default function FinancialHorizonClient({
  initialSummary,
  initialPurchases,
  initialPurchasesTotal,
}: FinancialHorizonClientProps) {
  // Navigation & Active Tab State
  const [activeTab, setActiveTab] = useState<HorizonTab>("overview");

  // Horizon Data Summary & Next Month Purchases
  const [summary, setSummary] = useState<HorizonSummary>({
    ...initialSummary,
    budgets: initialSummary.budgets ?? [],
  });
  const [purchases, setPurchases] = useState<NextMonthPurchaseItem[]>(initialPurchases);

  // Base Income Editing State
  const [isEditingBase, setIsEditingBase] = useState(false);
  const [baseInput, setBaseInput] = useState(initialSummary.base_amount.toString());
  const [currencyInput, setCurrencyInput] = useState(initialSummary.currency);

  // Fixed Deduction Category Filter State
  const [activeCategory, setActiveCategory] = useState<string>("all");

  // Budget Form & Dialog State
  const [isBudgetDialogOpen, setIsBudgetDialogOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState<BudgetItem | null>(null);
  const [budgetToDelete, setBudgetToDelete] = useState<BudgetItem | null>(null);
  const [deletingBudgetId, setDeletingBudgetId] = useState<number | null>(null);

  // Fixed Deduction Form State
  const [isAddingDeduction, setIsAddingDeduction] = useState(false);
  const [editingDeductionId, setEditingDeductionId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState<string>("housing");
  const [dueDay, setDueDay] = useState<string>("");
  const [selectedBudgetId, setSelectedBudgetId] = useState<number | null>(null);

  // Next Month Purchase Dialog & Confirmation States
  const [isPlannedPurchaseDialogOpen, setIsPlannedPurchaseDialogOpen] = useState(false);
  const [purchaseToDelete, setPurchaseToDelete] = useState<NextMonthPurchaseItem | null>(null);
  const [deletingPurchaseID, setDeletingPurchaseID] = useState<number | null>(null);
  const [isConfirmingClearAll, setIsConfirmingClearAll] = useState(false);
  const [isClearingPurchases, setIsClearingPurchases] = useState(false);

  // Transactions & Categories State
  const [transactions, setTransactions] = useState<TransactionItem[]>(initialSummary.transactions ?? []);
  const [categories, setCategories] = useState<CategoryItem[]>(initialSummary.categories ?? []);

  // Transaction & Category Dialog State
  const [isTransactionDialogOpen, setIsTransactionDialogOpen] = useState(false);
  const [isStatementUploadOpen, setIsStatementUploadOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<TransactionItem | null>(null);
  const [txToDelete, setTxToDelete] = useState<TransactionItem | null>(null);
  const [isCategoryDialogOpen, setIsCategoryDialogOpen] = useState(false);

  const handleImportTransactions = async (
    items: {
      name: string;
      amount: number;
      transactionDate: string;
      categoryId?: number | null;
      notes?: string | null;
    }[]
  ) => {
    if (items.length === 0) return;

    const res = await bulkAddTransactionsAction(items);

    if (res.ok && res.transactions && Array.isArray(res.transactions)) {
      const newItems = res.transactions as TransactionItem[];
      recalculateSummary({
        transactionsUpdater: (prev) => [...newItems, ...prev],
      });
    } else {
      setErrorMsg(res.error || "Failed to bulk add transactions.");
    }
  };

  // Error & Transition Hook
  const [errorMsg, setErrorMsg] = useState("");
  const [isPending, startTransition] = useTransition();

  // Calculated Metrics
  const purchasesTotal = purchases.reduce((sum, item) => sum + item.price, 0);
  const uncommittedPool = summary.base_amount - summary.total_deductions;
  const netRemainingPool = uncommittedPool - purchasesTotal;
  const totalCommitted = summary.total_deductions + purchasesTotal;
  const totalCommittedRatio =
    summary.base_amount > 0
      ? Math.round((totalCommitted / summary.base_amount) * 10000) / 100
      : 0;

  // Recalculation engine preserving exact business logic
  const recalculateSummary = ({
    deductionsUpdater,
    transactionsUpdater,
    budgetsUpdater,
  }: {
    deductionsUpdater?: (prev: DeductionItem[]) => DeductionItem[];
    transactionsUpdater?: (prev: TransactionItem[]) => TransactionItem[];
    budgetsUpdater?: (prev: BudgetItem[]) => BudgetItem[];
  } = {}) => {
    let nextTx = transactions;
    if (transactionsUpdater) {
      nextTx = transactionsUpdater(transactions);
      setTransactions(nextTx);
    }

    setSummary((prev) => {
      const newDeductions = deductionsUpdater ? deductionsUpdater(prev.deductions) : prev.deductions;
      const newBudgetsList = budgetsUpdater ? budgetsUpdater(prev.budgets ?? []) : (prev.budgets ?? []);

      const totalDeductions = newDeductions
        .filter((d) => d.is_active)
        .reduce((sum, d) => sum + d.amount, 0);

      const totalBudgetsAllocated = newBudgetsList.reduce((sum, b) => sum + b.allocated_amount, 0);
      const remainingAmount = prev.base_amount - totalDeductions;
      const committedRatio =
        prev.base_amount > 0
          ? Math.round((totalDeductions / prev.base_amount) * 10000) / 100
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
        deductions: newDeductions,
        budgets: updatedBudgets,
      };
    });
  };

  // Save Base Config Handler
  const handleSaveBaseConfig = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");
    const parsedAmount = parseFloat(baseInput);
    if (isNaN(parsedAmount) || parsedAmount < 0) {
      setErrorMsg("Please enter a valid non-negative base monthly income.");
      return;
    }

    startTransition(async () => {
      const res = await updateHorizonConfigAction(parsedAmount, currencyInput);
      if (res.ok && res.summary) {
        setSummary({
          ...res.summary,
          budgets: res.summary.budgets ?? [],
        });
        setIsEditingBase(false);
      } else {
        setErrorMsg(res.error ?? "Failed to update starting pool.");
      }
    });
  };

  // Budget Handlers
  const handleOpenAddBudgetForm = () => {
    setEditingBudget(null);
    setIsBudgetDialogOpen(true);
  };

  const handleOpenEditBudgetForm = (b: BudgetItem) => {
    setEditingBudget(b);
    setIsBudgetDialogOpen(true);
  };

  const handleSaveBudget = async (data: { id?: number; name: string; allocatedAmount: number }) => {
    setErrorMsg("");
    startTransition(async () => {
      if (data.id) {
        const res = await updateBudgetAction(data.id, data.name, data.allocatedAmount);
        if (res.ok && res.budget) {
          recalculateSummary({
            budgetsUpdater: (prev) =>
              prev.map((b) => (b.id === data.id ? res.budget : b)),
          });
          setIsBudgetDialogOpen(false);
          setEditingBudget(null);
        } else {
          setErrorMsg(res.error ?? "Failed to update budget.");
        }
      } else {
        const res = await addBudgetAction(data.name, data.allocatedAmount);
        if (res.ok && res.budget) {
          recalculateSummary({
            budgetsUpdater: (prev) => [...prev, res.budget],
          });
          setIsBudgetDialogOpen(false);
        } else {
          setErrorMsg(res.error ?? "Failed to add budget.");
        }
      }
    });
  };

  const handleDeleteBudget = (id: number) => {
    setErrorMsg("");
    setDeletingBudgetId(id);
    startTransition(async () => {
      const res = await deleteBudgetAction(id);
      if (res.ok) {
        recalculateSummary({
          budgetsUpdater: (prev) => prev.filter((b) => b.id !== id),
          deductionsUpdater: (prev) =>
            prev.map((d) => (d.budget_id === id ? { ...d, budget_id: null } : d)),
          transactionsUpdater: (prev) =>
            prev.map((t) => (t.budget_id === id ? { ...t, budget_id: null, budget_name: null } : t)),
        });
        setBudgetToDelete(null);
      } else {
        setErrorMsg(res.error ?? "Failed to delete budget.");
      }
      setDeletingBudgetId(null);
    });
  };

  // Fixed Obligation Handlers
  const handleOpenAddDeductionForm = () => {
    setName("");
    setAmount("");
    setCategory("housing");
    setDueDay("");
    setSelectedBudgetId(null);
    setErrorMsg("");
    setIsAddingDeduction(true);
    setEditingDeductionId(null);
  };

  const handleOpenEditDeductionForm = (item: DeductionItem) => {
    setName(item.name);
    setAmount(item.amount.toString());
    setCategory(item.category);
    setDueDay(item.due_day ? item.due_day.toString() : "");
    setSelectedBudgetId(item.budget_id ?? null);
    setErrorMsg("");
    setEditingDeductionId(item.id);
    setIsAddingDeduction(false);
  };

  const handleCancelDeductionForm = () => {
    setIsAddingDeduction(false);
    setEditingDeductionId(null);
    setSelectedBudgetId(null);
    setErrorMsg("");
  };

  const handleSaveDeduction = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");
    const parsedAmount = parseFloat(amount);
    if (!name.trim()) {
      setErrorMsg("Deduction name is required.");
      return;
    }
    if (isNaN(parsedAmount) || parsedAmount < 0) {
      setErrorMsg("Please enter a valid non-negative amount.");
      return;
    }

    const parsedDueDay = dueDay ? parseInt(dueDay, 10) : null;
    if (parsedDueDay !== null && (parsedDueDay < 1 || parsedDueDay > 31)) {
      setErrorMsg("Due day must be between 1 and 31.");
      return;
    }

    startTransition(async () => {
      if (editingDeductionId !== null) {
        const existing = summary.deductions.find((d) => d.id === editingDeductionId);
        const res = await updateDeductionAction(
          editingDeductionId,
          name,
          category,
          parsedAmount,
          parsedDueDay,
          existing ? existing.is_active : true,
          selectedBudgetId
        );
        if (res.ok && res.deduction) {
          recalculateSummary({
            deductionsUpdater: (prev) =>
              prev.map((d) => (d.id === editingDeductionId ? res.deduction : d)),
          });
          setEditingDeductionId(null);
          setSelectedBudgetId(null);
        } else {
          setErrorMsg(res.error ?? "Failed to update item.");
        }
      } else {
        const res = await addDeductionAction(
          name,
          category,
          parsedAmount,
          parsedDueDay,
          selectedBudgetId
        );
        if (res.ok && res.deduction) {
          recalculateSummary({
            deductionsUpdater: (prev) => [res.deduction, ...prev],
          });
          setIsAddingDeduction(false);
          setSelectedBudgetId(null);
        } else {
          setErrorMsg(res.error ?? "Failed to add item.");
        }
      }
    });
  };

  const handleToggleDeductionActive = (item: DeductionItem) => {
    startTransition(async () => {
      const updatedActive = !item.is_active;
      const res = await updateDeductionAction(
        item.id,
        item.name,
        item.category,
        item.amount,
        item.due_day,
        updatedActive,
        item.budget_id ?? null
      );
      if (res.ok && res.deduction) {
        recalculateSummary({
          deductionsUpdater: (prev) =>
            prev.map((d) => (d.id === item.id ? res.deduction : d)),
        });
      }
    });
  };

  const handleDeleteDeduction = (id: number) => {
    startTransition(async () => {
      const res = await deleteDeductionAction(id);
      if (res.ok) {
        recalculateSummary({
          deductionsUpdater: (prev) => prev.filter((d) => d.id !== id),
        });
      } else {
        setErrorMsg(res.error ?? "Failed to delete item.");
      }
    });
  };

  // Next Month Purchase Handlers
  const handleSavePlannedPurchase = async (data: { name: string; price: number; url: string | null }) => {
    setErrorMsg("");
    startTransition(async () => {
      const res = await addNextMonthPurchaseHorizonAction(data.name, data.price, data.url);
      if (res.ok && res.item) {
        setPurchases((prev) => [res.item, ...prev]);
        setIsPlannedPurchaseDialogOpen(false);
      } else {
        setErrorMsg(res.error ?? "Failed to add purchase item.");
      }
    });
  };

  const handleDeletePurchase = (item: NextMonthPurchaseItem) => {
    setErrorMsg("");
    setDeletingPurchaseID(item.id);
    startTransition(async () => {
      const res = await deleteNextMonthPurchaseAction(item.id);
      if (res.ok) {
        setPurchases((prev) => prev.filter((p) => p.id !== item.id));
        setPurchaseToDelete(null);
      } else {
        setErrorMsg(res.error ?? "Failed to delete purchase.");
      }
      setDeletingPurchaseID(null);
    });
  };

  const handleClearAllPurchases = () => {
    setErrorMsg("");
    setIsClearingPurchases(true);
    startTransition(async () => {
      const res = await clearAllNextMonthPurchasesAction();
      if (res.ok) {
        setPurchases([]);
        setIsConfirmingClearAll(false);
      } else {
        setErrorMsg(res.error ?? "Failed to clear purchases.");
      }
      setIsClearingPurchases(false);
    });
  };

  // Transaction & Category Handlers
  const handleOpenAddTransaction = () => {
    setEditingTransaction(null);
    setIsTransactionDialogOpen(true);
  };

  const handleOpenEditTransaction = (tx: TransactionItem) => {
    setEditingTransaction(tx);
    setIsTransactionDialogOpen(true);
  };

  const handleSaveTransaction = async (data: {
    id?: number;
    name: string;
    amount: number;
    transactionDate: string;
    categoryId?: number | null;
    budgetId?: number | null;
    notes?: string | null;
  }) => {
    setErrorMsg("");
    startTransition(async () => {
      if (data.id) {
        const res = await updateTransactionAction(
          data.id,
          data.name,
          data.amount,
          data.transactionDate,
          data.categoryId,
          data.budgetId,
          data.notes
        );
        if (res.ok && res.transaction) {
          recalculateSummary({
            transactionsUpdater: (prev) =>
              prev.map((t) => (t.id === data.id ? res.transaction : t)),
          });
          setIsTransactionDialogOpen(false);
          setEditingTransaction(null);
        } else {
          setErrorMsg(res.error ?? "Failed to update transaction.");
        }
      } else {
        const res = await addTransactionAction(
          data.name,
          data.amount,
          data.transactionDate,
          data.categoryId,
          data.budgetId,
          data.notes
        );
        if (res.ok && res.transaction) {
          recalculateSummary({
            transactionsUpdater: (prev) => [res.transaction, ...prev],
          });
          setIsTransactionDialogOpen(false);
        } else {
          setErrorMsg(res.error ?? "Failed to add transaction.");
        }
      }
    });
  };

  const handleDeleteTransaction = () => {
    if (!txToDelete) return;
    setErrorMsg("");
    startTransition(async () => {
      const res = await deleteTransactionAction(txToDelete.id);
      if (res.ok) {
        recalculateSummary({
          transactionsUpdater: (prev) => prev.filter((t) => t.id !== txToDelete.id),
        });
        setTxToDelete(null);
      } else {
        setErrorMsg(res.error ?? "Failed to delete transaction.");
      }
    });
  };

  const handleSaveCategory = async (data: { name: string; color: string }) => {
    setErrorMsg("");
    startTransition(async () => {
      const res = await addHorizonCategoryAction(data.name, "tag", data.color);
      if (res.ok && res.category) {
        setCategories((prev) => [...prev, res.category]);
        setIsCategoryDialogOpen(false);
      } else {
        setErrorMsg(res.error ?? "Failed to add category.");
      }
    });
  };

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl space-y-8">
        {/* Navigation & Unified Header */}
        <div>
          <Link
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary transition hover:opacity-80"
            href="/dashboard"
          >
            <ArrowLeft className="h-4 w-4" /> Back to Dashboard
          </Link>
          <div className="mt-4 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <div>
              <div className="flex items-center gap-2.5">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Compass className="h-6 w-6" />
                </div>
                <h1 className="text-3xl font-extrabold text-foreground tracking-tight">
                  Financial Horizon
                </h1>
              </div>
              <p className="mt-2 text-sm text-muted-foreground max-w-2xl">
                Your month&apos;s starting line calculator. Manages monthly budgets, subtracts fixed obligations and planned purchases from base income to reveal your exact net uncommitted cash pool.
              </p>
            </div>

            {/* Unified Quick Action Trigger */}
            <div className="shrink-0">
              <QuickActionDropdown
                onLogTransaction={handleOpenAddTransaction}
                onAddFixedObligation={() => {
                  setActiveTab("fixed");
                  handleOpenAddDeductionForm();
                }}
                onAddPlannedPurchase={() => setIsPlannedPurchaseDialogOpen(true)}
                onAddBudget={handleOpenAddBudgetForm}
                onAddCategory={() => setIsCategoryDialogOpen(true)}
                isPending={isPending}
              />
            </div>
          </div>
        </div>

        {/* Global Error Banner */}
        {errorMsg && (
          <div
            className="rounded-2xl bg-destructive/10 border border-destructive/20 p-4 text-sm text-destructive flex items-center justify-between shadow-xs"
            role="alert"
          >
            <span>{errorMsg}</span>
            <button onClick={() => setErrorMsg("")} className="p-1 hover:opacity-80">
              <X className="h-4 w-4" />
            </button>
          </div>
        )}

        {/* Header Streamlined KPI Metrics Cards */}
        <HeaderMetrics
          summary={summary}
          purchasesTotal={purchasesTotal}
          netRemainingPool={netRemainingPool}
          totalCommitted={totalCommitted}
          totalCommittedRatio={totalCommittedRatio}
          isEditingBase={isEditingBase}
          setIsEditingBase={setIsEditingBase}
          baseInput={baseInput}
          setBaseInput={setBaseInput}
          currencyInput={currencyInput}
          setCurrencyInput={setCurrencyInput}
          handleSaveBaseConfig={handleSaveBaseConfig}
          isPending={isPending}
        />

        {/* Sub-Navigation Tabs */}
        <TabNavigation
          activeTab={activeTab}
          onTabChange={setActiveTab}
          transactionsCount={transactions.length}
          fixedObligationsCount={summary.deductions.filter((d) => d.is_active).length}
          plannedPurchasesCount={purchases.length}
        />

        {/* Tab Content Views */}
        {activeTab === "overview" && (
          <OverviewTab
            summary={summary}
            transactions={transactions}
            categories={categories}
            netRemainingPool={netRemainingPool}
            onOpenAddBudget={handleOpenAddBudgetForm}
            onOpenEditBudget={handleOpenEditBudgetForm}
            onConfirmDeleteBudget={setBudgetToDelete}
            onNavigateTab={setActiveTab}
            isPending={isPending}
          />
        )}

        {activeTab === "transactions" && (
          <TransactionsTab
            summary={summary}
            transactions={transactions}
            categories={categories}
            onOpenAddTransaction={handleOpenAddTransaction}
            onOpenEditTransaction={handleOpenEditTransaction}
            onConfirmDeleteTransaction={setTxToDelete}
            onOpenAddCategory={() => setIsCategoryDialogOpen(true)}
            onOpenStatementUpload={() => setIsStatementUploadOpen(true)}
            isPending={isPending}
          />
        )}

        {activeTab === "fixed" && (
          <FixedObligationsTab
            summary={summary}
            activeCategory={activeCategory}
            setActiveCategory={setActiveCategory}
            isAddingDeduction={isAddingDeduction}
            editingDeductionId={editingDeductionId}
            name={name}
            setName={setName}
            amount={amount}
            setAmount={setAmount}
            category={category}
            setCategory={setCategory}
            dueDay={dueDay}
            setDueDay={setDueDay}
            selectedBudgetId={selectedBudgetId}
            setSelectedBudgetId={setSelectedBudgetId}
            onOpenAddDeduction={handleOpenAddDeductionForm}
            onOpenEditDeduction={handleOpenEditDeductionForm}
            onCancelDeductionForm={handleCancelDeductionForm}
            onSaveDeduction={handleSaveDeduction}
            onToggleDeductionActive={handleToggleDeductionActive}
            onDeleteDeduction={handleDeleteDeduction}
            isPending={isPending}
          />
        )}

        {activeTab === "planner" && (
          <PlannedPurchasesTab
            summary={summary}
            purchases={purchases}
            purchasesTotal={purchasesTotal}
            netRemainingPool={netRemainingPool}
            onOpenAddPurchase={() => setIsPlannedPurchaseDialogOpen(true)}
            onConfirmDeletePurchase={setPurchaseToDelete}
            onConfirmClearAllPurchases={() => setIsConfirmingClearAll(true)}
            isPending={isPending}
            isClearingPurchases={isClearingPurchases}
          />
        )}
      </div>

      {/* Confirmation Dialog for Budget Deletion */}
      <ConfirmationDialog
        isOpen={!!budgetToDelete}
        onClose={() => setBudgetToDelete(null)}
        onConfirm={() => {
          if (budgetToDelete) handleDeleteBudget(budgetToDelete.id);
        }}
        title={budgetToDelete ? `Delete ${budgetToDelete.name}?` : ""}
        description="This will permanently delete this monthly budget. Any linked obligations will remain intact but become unbudgeted."
        confirmText="Delete budget"
        confirmLoadingText="Deleting…"
        isLoading={deletingBudgetId !== null}
        error={errorMsg}
        variant="destructive"
      />

      {/* Confirmation Dialogs for Purchase Deletions */}
      <ConfirmationDialog
        isOpen={!!purchaseToDelete}
        onClose={() => setPurchaseToDelete(null)}
        onConfirm={() => {
          if (purchaseToDelete) handleDeletePurchase(purchaseToDelete);
        }}
        title={purchaseToDelete ? `Delete ${purchaseToDelete.name}?` : ""}
        description="This permanently removes it from your next month purchases."
        confirmText="Delete purchase"
        confirmLoadingText="Deleting…"
        isLoading={deletingPurchaseID !== null}
        error={errorMsg}
        variant="destructive"
      />

      <ConfirmationDialog
        isOpen={isConfirmingClearAll}
        onClose={() => setIsConfirmingClearAll(false)}
        onConfirm={handleClearAllPurchases}
        title="Clear all next month purchases?"
        description={`This permanently removes all ${purchases.length} planned purchase${purchases.length === 1 ? "" : "s"}.`}
        confirmText="Clear all"
        confirmLoadingText="Clearing…"
        isLoading={isClearingPurchases}
        error={errorMsg}
        variant="destructive"
      />

      {/* Confirmation Dialog for Transaction Deletion */}
      <ConfirmationDialog
        isOpen={!!txToDelete}
        onClose={() => setTxToDelete(null)}
        onConfirm={handleDeleteTransaction}
        title={txToDelete ? `Delete "${txToDelete.name}"?` : ""}
        description="This will permanently delete this transaction record."
        confirmText="Delete transaction"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={errorMsg}
        variant="destructive"
      />

      {/* Transaction Entry Dialog */}
      <TransactionDialog
        isOpen={isTransactionDialogOpen}
        onClose={() => {
          setIsTransactionDialogOpen(false);
          setEditingTransaction(null);
        }}
        onSave={handleSaveTransaction}
        editingTransaction={editingTransaction}
        categories={categories}
        budgets={summary.budgets ?? []}
        currency={summary.currency}
        isPending={isPending}
      />

      {/* Category Entry Dialog */}
      <CategoryDialog
        isOpen={isCategoryDialogOpen}
        onClose={() => setIsCategoryDialogOpen(false)}
        onSave={handleSaveCategory}
        isPending={isPending}
      />

      {/* Budget Entry Dialog */}
      <BudgetDialog
        isOpen={isBudgetDialogOpen}
        onClose={() => {
          setIsBudgetDialogOpen(false);
          setEditingBudget(null);
        }}
        onSave={handleSaveBudget}
        editingBudget={editingBudget}
        currency={summary.currency}
        isPending={isPending}
      />

      {/* Planned Purchase Dialog */}
      <PlannedPurchaseDialog
        isOpen={isPlannedPurchaseDialogOpen}
        onClose={() => setIsPlannedPurchaseDialogOpen(false)}
        onSave={handleSavePlannedPurchase}
        currency={summary.currency}
        isPending={isPending}
      />

      {/* Statement Upload & Extraction Dialog */}
      <StatementUploadDialog
        isOpen={isStatementUploadOpen}
        onClose={() => setIsStatementUploadOpen(false)}
        categories={categories}
        onImportTransactions={handleImportTransactions}
      />
    </main>
  );
}
