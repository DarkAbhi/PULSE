"use client";

import { calculateHorizonSummary, updateHorizonSubscriptions, type HorizonUpdates } from "@/app/financial-horizon/horizon-summary";
import { formatMoney } from "@/app/lib/currency.mjs";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useCurrency, useTimeZone } from "@/app/components/profile-preferences-provider";

import PulseIconButton from "@/app/components/design-system/pulse-icon-button";

import { useState, useTransition, useEffect, useMemo, type ReactNode } from "react";
import dynamic from "next/dynamic";
import { useSearchParams } from "next/navigation";
import { useTransactions } from "@/app/financial-horizon/use-transactions";
import { frame } from "@/app/components/design-system/page-frame";
import { Compass, X } from "lucide-react";

import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

import QuickActionDropdown from "@/app/financial-horizon/components/quick-action-dropdown";
import TabNavigation, { horizonTab, type HorizonTab } from "@/app/financial-horizon/components/tab-navigation";
import HeaderMetrics from "@/app/financial-horizon/components/header-metrics";
import OverviewTab from "@/app/financial-horizon/tabs/overview-tab";

import type { HorizonSummary, NextMonthPurchaseItem, DeductionItem, BudgetItem, CategoryItem, TransactionItem, SubscriptionItem } from "@/app/financial-horizon/horizon-types";
import {
  updateHorizonConfigAction,
  addDeductionAction,
  updateDeductionAction,
  deleteDeductionAction,
  addNextMonthPurchaseHorizonAction,
  deleteNextMonthPurchaseAction,
  addBudgetAction,
  updateBudgetAction,
  deleteBudgetAction,
  addHorizonCategoryAction,
  addTransactionAction,
  updateTransactionAction,
  deleteTransactionAction,
  addSubscriptionAction,
  updateSubscriptionAction,
  deleteSubscriptionAction,
  getSubscriptionTransactionsAction,
  linkSubscriptionTransactionAction,
} from "@/app/financial-horizon/actions";
import type { TransactionPage } from "@/app/financial-horizon/horizon-types";

const TransactionDialog = dynamic(() => import("@/app/financial-horizon/dialogs/transaction-dialog"));
const LinkSubscriptionPaymentDialog = dynamic(() => import("@/app/financial-horizon/dialogs/link-subscription-payment-dialog"));
const CategoryDialog = dynamic(() => import("@/app/financial-horizon/dialogs/category-dialog"));
const BudgetDialog = dynamic(() => import("@/app/financial-horizon/dialogs/budget-dialog"));
const SubscriptionDialog = dynamic(() => import("@/app/financial-horizon/dialogs/subscription-dialog"));
const PlannedPurchaseDialog = dynamic(() => import("@/app/financial-horizon/dialogs/planned-purchase-dialog"));
const StatementUploadDialog = dynamic(() => import("@/app/financial-horizon/dialogs/statement-upload-dialog"));
const AccountsTab = dynamic(() => import("@/app/financial-horizon/tabs/accounts-tab"));
const BudgetsTab = dynamic(() => import("@/app/financial-horizon/tabs/budgets-tab"));
const TransactionsTab = dynamic(() => import("@/app/financial-horizon/tabs/transactions-tab"));
const SubscriptionsTab = dynamic(() => import("@/app/financial-horizon/tabs/subscriptions-tab"));
const FixedObligationsTab = dynamic(() => import("@/app/financial-horizon/tabs/fixed-obligations-tab"));
const PlannedPurchasesTab = dynamic(() => import("@/app/financial-horizon/tabs/planned-purchases-tab"));

interface FinancialHorizonClientProps {
  appBar: ReactNode;
  initialSummary: HorizonSummary;
  initialLoadError: boolean;
  initialPurchases: NextMonthPurchaseItem[];
  initialTransactionPage: TransactionPage;
}

export default function FinancialHorizonClient({
  appBar,
  initialSummary,
  initialLoadError,
  initialPurchases,
  initialTransactionPage,
}: FinancialHorizonClientProps) {
  const { timeZone } = useTimeZone();
  const { currency } = useCurrency();
  // Navigation & Active Tab State
  const activeTab = horizonTab(useSearchParams().get("tab"));

  function setActiveTab(tab: HorizonTab) {
    const url = new URL(window.location.href);
    if (tab === "overview") url.searchParams.delete("tab");
    else url.searchParams.set("tab", tab);
    if (url.href !== window.location.href) window.history.pushState(null, "", url);
  }

  // Horizon Data Summary & Next Month Purchases
  const [summaryData, setSummary] = useState<HorizonSummary>({
    ...initialSummary,
    budgets: initialSummary.budgets ?? [],
  });
  const summary = useMemo(() => ({ ...summaryData, currency }), [summaryData, currency]);
  const [purchases, setPurchases] = useState<NextMonthPurchaseItem[]>(initialPurchases);

  // Base Income Editing State
  const [isEditingBase, setIsEditingBase] = useState(false);
  const [baseInput, setBaseInput] = useState(initialSummary.base_amount.toString());

  // Fixed Deduction Category Filter State
  const [activeCategory, setActiveCategory] = useState<string>("all");

  // Budget Form & Dialog State
  const [isBudgetDialogOpen, setIsBudgetDialogOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState<BudgetItem | null>(null);
  const [budgetToDelete, setBudgetToDelete] = useState<BudgetItem | null>(null);
  const [deletingBudgetId, setDeletingBudgetId] = useState<number | null>(null);

  // Fixed Deduction Form & Confirmation State
  const [isAddingDeduction, setIsAddingDeduction] = useState(false);
  const [editingDeductionId, setEditingDeductionId] = useState<number | null>(null);
  const [deductionToDelete, setDeductionToDelete] = useState<DeductionItem | null>(null);
  const [deletingDeductionId, setDeletingDeductionId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState<string>("housing");
  const [dueDay, setDueDay] = useState<string>("");
  const [selectedBudgetId, setSelectedBudgetId] = useState<number | null>(null);

  // Next Month Purchase Dialog & Confirmation States
  const [isPlannedPurchaseDialogOpen, setIsPlannedPurchaseDialogOpen] = useState(false);
  const [purchaseToDelete, setPurchaseToDelete] = useState<NextMonthPurchaseItem | null>(null);
  const [deletingPurchaseID, setDeletingPurchaseID] = useState<number | null>(null);

  const categories = summary.categories ?? [];

  // Subscriptions State
  const subscriptions = summary.subscriptions ?? [];
  const [isSubscriptionDialogOpen, setIsSubscriptionDialogOpen] = useState(false);
  const [editingSubscription, setEditingSubscription] = useState<SubscriptionItem | null>(null);
  const [subToDelete, setSubToDelete] = useState<SubscriptionItem | null>(null);
  const [deletingSubId, setDeletingSubId] = useState<number | null>(null);
  const [viewingSubTransactions, setViewingSubTransactions] = useState<SubscriptionItem | null>(null);
  const [subTransactionsList, setSubTransactionsList] = useState<TransactionItem[]>([]);
  const [isLoadingSubTx, setIsLoadingSubTx] = useState(false);

  useEffect(() => {
    setSummary({ ...initialSummary, budgets: initialSummary.budgets ?? [] });
  }, [initialSummary]);

  function updateSubscriptionsState(update: (items: SubscriptionItem[]) => SubscriptionItem[]) {
    setSummary(previous => updateHorizonSubscriptions(previous, update));
  }

  // Transaction & Category Dialog State
  const [isTransactionDialogOpen, setIsTransactionDialogOpen] = useState(false);
  const [isStatementUploadOpen, setIsStatementUploadOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<TransactionItem | null>(null);
  const [paymentSubscription, setPaymentSubscription] = useState<SubscriptionItem | null>(null);
  const [linkingPaymentSubscription, setLinkingPaymentSubscription] = useState<SubscriptionItem | null>(null);
  const [txToDelete, setTxToDelete] = useState<TransactionItem | null>(null);
  const [isCategoryDialogOpen, setIsCategoryDialogOpen] = useState(false);

  // Error & Transition Hook
  const [errorMsg, setErrorMsg] = useState("");
  const [isPending, startTransition] = useTransition();
  const {
    page: transactionPage, total: totalTransactionsCount, setTotal: setTotalTransactionsCount,
    type: txTypeFilter, setType: setTxTypeFilter, search: txSearchQuery, setSearch: setTxSearchQuery,
    category: txCategoryFilter, setCategory: setTxCategoryFilter, updateTransactions,
    loadPage: loadTransactionsPage, isPending: isLoadingTransactions,
  } = useTransactions(initialTransactionPage, setErrorMsg);

  // Calculated Metrics
  const purchasesTotal = purchases.reduce((sum, item) => sum + item.price, 0);
  const subscriptionBurnTotal = summary.total_subscription_burn ?? 0;
  const totalFixedObligations = summary.total_deductions + subscriptionBurnTotal;
  const uncommittedPool = summary.base_amount - totalFixedObligations;
  const netRemainingPool = uncommittedPool - purchasesTotal;
  const totalCommitted = totalFixedObligations + purchasesTotal;
  const totalCommittedRatio =
    summary.base_amount > 0
      ? Math.round((totalCommitted / summary.base_amount) * 10000) / 100
      : 0;

  // Recalculation engine preserving exact business logic
  const recalculateSummary = ({
    deductionsUpdater,
    transactionsUpdater,
    budgetsUpdater,
  }: HorizonUpdates = {}) => {
    setSummary(previous => calculateHorizonSummary(previous, { deductionsUpdater, transactionsUpdater, budgetsUpdater }));
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
      const res = await updateHorizonConfigAction(parsedAmount);
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
        const unlinkBudget = (items: TransactionItem[]) =>
          items.map(t => t.budget_id === id ? { ...t, budget_id: null, budget_name: null } : t);
        updateTransactions(unlinkBudget);
        recalculateSummary({
          budgetsUpdater: (prev) => prev.filter((b) => b.id !== id),
          deductionsUpdater: (prev) =>
            prev.map((d) => (d.budget_id === id ? { ...d, budget_id: null } : d)),
          transactionsUpdater: unlinkBudget,
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

  const handleConfirmDeleteDeduction = () => {
    if (!deductionToDelete) return;
    const id = deductionToDelete.id;
    setDeletingDeductionId(id);
    setErrorMsg("");
    startTransition(async () => {
      const res = await deleteDeductionAction(id);
      setDeletingDeductionId(null);
      if (res.ok) {
        setDeductionToDelete(null);
        recalculateSummary({
          deductionsUpdater: (prev) => prev.filter((d) => d.id !== id),
        });
      } else {
        setErrorMsg(res.error ?? "Failed to delete obligation.");
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

  // Transaction & Category Handlers
  const handleOpenAddTransaction = () => {
    setEditingTransaction(null);
    setPaymentSubscription(null);
    setIsTransactionDialogOpen(true);
  };

  const handleOpenEditTransaction = (tx: TransactionItem) => {
    setEditingTransaction(tx);
    setPaymentSubscription(null);
    setIsTransactionDialogOpen(true);
  };

  const handleSaveTransaction = async (data: {
    id?: number;
    name: string;
    amount: number;
    type?: "debit" | "credit";
    transactionDate: string;
    categoryId?: number | null;
    budgetId?: number | null;
    subscriptionId?: number | null;
    notes?: string | null;
    accountId?: number | null;
  }) => {
    setErrorMsg("");
    startTransition(async () => {
      if (data.id) {
        const res = await updateTransactionAction(
          data.id,
          data.name,
          data.amount,
          data.type,
          data.transactionDate,
          data.categoryId,
          data.budgetId,
          data.subscriptionId,
          data.notes,
          data.accountId,
        );
        if (res.ok && res.transaction) {
          recalculateSummary({
            transactionsUpdater: (prev) =>
              prev.map((t) => (t.id === data.id ? res.transaction : t)),
          });
          setIsTransactionDialogOpen(false);
          setEditingTransaction(null);
          loadTransactionsPage(transactionPage.page);
        } else {
          setErrorMsg(res.error ?? "Failed to update transaction.");
        }
      } else {
        const res = await addTransactionAction(
          data.name,
          data.amount,
          data.type,
          data.transactionDate,
          data.categoryId,
          data.budgetId,
          data.subscriptionId,
          data.notes,
          data.accountId,
        );
        if (res.ok && res.transaction) {
          recalculateSummary({
            transactionsUpdater: (prev) => [res.transaction, ...prev],
          });
          if (data.subscriptionId) {
            updateSubscriptionsState((prev) => prev.map((sub) => sub.id === data.subscriptionId ? {
              ...sub,
              linked_transaction_count: sub.linked_transaction_count + 1,
              total_spent: sub.total_spent + res.transaction.amount,
            } : sub));
          }
          setTotalTransactionsCount((prev) => prev + 1);
          setIsTransactionDialogOpen(false);
          setPaymentSubscription(null);
          loadTransactionsPage(1);
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
        setTxToDelete(null);
        updateTransactions(prev => prev.filter(t => t.id !== txToDelete.id));
        recalculateSummary({
          transactionsUpdater: (prev) => prev.filter((t) => t.id !== txToDelete.id),
        });
        setTotalTransactionsCount((prev) => Math.max(0, prev - 1));
        loadTransactionsPage(transactionPage.page);
      } else {
        setErrorMsg(res.error ?? "Failed to delete transaction.");
      }
    });
  };

  const openCategoryDialog = () => {
    setErrorMsg("");
    setIsCategoryDialogOpen(true);
  };

  const handleSaveCategory = async (data: { name: string; color: string; parentId: number | null }) => {
    setErrorMsg("");
    startTransition(async () => {
      const res = await addHorizonCategoryAction(data.name, "tag", data.color, data.parentId);
      if (res.ok && res.category) {
        setSummary(previous => {
          const items = previous.categories ?? [];
          return items.some(item => item.id === res.category.id)
            ? previous
            : { ...previous, categories: [...items, res.category] };
        });
        setIsCategoryDialogOpen(false);
      } else {
        setErrorMsg(res.error ?? "Failed to add category.");
      }
    });
  };

  // Subscription Handlers
  const handleOpenAddSubscription = () => {
    setEditingSubscription(null);
    setIsSubscriptionDialogOpen(true);
  };

  const handleOpenEditSubscription = (sub: SubscriptionItem) => {
    setEditingSubscription(sub);
    setIsSubscriptionDialogOpen(true);
  };

  const handleSaveSubscription = async (data: {
    name: string;
    amount: number;
    billing_cycle: string;
    billing_day?: number | null;
    renewal_date?: string | null;
    status: string;
    category_id?: number | null;
    budget_id?: number | null;
    notes?: string | null;
  }) => {
    setErrorMsg("");
    if (editingSubscription) {
      const res = await updateSubscriptionAction(editingSubscription.id, data);
      if (res.ok && res.subscription) {
        const updatedSub = res.subscription as SubscriptionItem;
        updateSubscriptionsState((prev) =>
          prev.map((s) => (s.id === editingSubscription.id ? updatedSub : s))
        );
        setIsSubscriptionDialogOpen(false);
        setEditingSubscription(null);
      } else {
        throw new Error(res.error ?? "Failed to update subscription.");
      }
    } else {
      const res = await addSubscriptionAction(data);
      if (res.ok && res.subscription) {
        const newSub = res.subscription as SubscriptionItem;
        updateSubscriptionsState((prev) => [newSub, ...prev]);
        setIsSubscriptionDialogOpen(false);
      } else {
        throw new Error(res.error ?? "Failed to add subscription.");
      }
    }
  };

  const handleDeleteSubscription = (id: number) => {
    setErrorMsg("");
    setDeletingSubId(id);
    startTransition(async () => {
      const res = await deleteSubscriptionAction(id);
      if (res.ok) {
        updateSubscriptionsState((prev) => prev.filter((s) => s.id !== id));
        setSubToDelete(null);
      } else {
        setErrorMsg(res.error ?? "Failed to delete subscription.");
      }
      setDeletingSubId(null);
    });
  };

  const handleToggleSubscriptionStatus = (sub: SubscriptionItem, newStatus: string) => {
    startTransition(async () => {
      const res = await updateSubscriptionAction(sub.id, {
        name: sub.name,
        amount: sub.amount,
        billing_cycle: sub.billing_cycle,
        billing_day: sub.billing_day,
        renewal_date: sub.renewal_date,
        status: newStatus,
        category_id: sub.category_id,
        budget_id: sub.budget_id,
        notes: sub.notes,
      });
      if (res.ok && res.subscription) {
        const updatedSub = res.subscription as SubscriptionItem;
        updateSubscriptionsState((prev) =>
          prev.map((s) => (s.id === sub.id ? updatedSub : s))
        );
      }
    });
  };

  const handleLogSubscriptionPayment = (sub: SubscriptionItem) => {
    setLinkingPaymentSubscription(sub);
  };

  const handleLinkSubscriptionPayment = async (transaction: TransactionItem) => {
    if (!linkingPaymentSubscription) return;
    const subscription = linkingPaymentSubscription;
    const res = await linkSubscriptionTransactionAction(subscription.id, transaction.id);
    if (!res.ok) throw new Error(res.error);
    const linked = res.transaction as TransactionItem;
    updateTransactions((prev) => prev.map((item) => item.id === linked.id ? linked : item));
    updateSubscriptionsState((prev) => prev.map((item) => item.id === subscription.id ? {
      ...item,
      linked_transaction_count: item.linked_transaction_count + 1,
      total_spent: item.total_spent + linked.amount,
    } : item));
    setLinkingPaymentSubscription(null);
  };

  const handleViewSubscriptionTransactions = async (sub: SubscriptionItem) => {
    setViewingSubTransactions(sub);
    setIsLoadingSubTx(true);
    const res = await getSubscriptionTransactionsAction(sub.id);
    if (res.ok && res.transactions) {
      setSubTransactionsList(res.transactions as TransactionItem[]);
    } else {
      setSubTransactionsList([]);
    }
    setIsLoadingSubTx(false);
  };

  return (
    <main id="main-content" tabIndex={-1} className={`${frame.page} [&_.pulse-card]:shadow-card`}>
      <div className={`${frame.content} space-y-6 sm:space-y-7`}>
        {appBar}
        <header>
          <div className="flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
            <h1 className="flex items-center gap-3 text-3xl leading-tight font-extrabold tracking-tight sm:gap-4 sm:text-5xl">
              <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-accent text-action sm:size-14">
                <Compass aria-hidden="true" className="size-6 sm:size-7" />
              </span>
              Financial Horizon
            </h1>

            <div className="shrink-0">
              <QuickActionDropdown
                onLogTransaction={handleOpenAddTransaction}
                onAddFixedObligation={() => {
                  setActiveTab("fixed");
                  handleOpenAddDeductionForm();
                }}
                onAddPlannedPurchase={() => setIsPlannedPurchaseDialogOpen(true)}
                onAddBudget={handleOpenAddBudgetForm}
                onAddCategory={openCategoryDialog}
                isPending={isPending}
              />
            </div>
          </div>
        </header>

        {/* Global Error Banner */}
        {initialLoadError && (
          <p className="rounded-xl bg-warning-soft p-4 text-sm text-warning" role="status">
            Some transaction or purchase data couldn’t load. Refresh this page for up-to-date totals.
          </p>
        )}
        {errorMsg && !(activeTab === "fixed" && (isAddingDeduction || editingDeductionId !== null)) && (
          <div
            className="rounded-2xl bg-destructive/10 p-4 text-sm text-destructive flex items-center justify-between shadow-xs"
            role="alert"
          >
            <span>{errorMsg}</span>
            <PulseIconButton variant="danger" size="sm" aria-label="Dismiss error" onClick={() => setErrorMsg("")}>
              <X aria-hidden="true" className="size-4" />
            </PulseIconButton>
          </div>
        )}

        {/* Header Streamlined KPI Metrics Cards */}
        <HeaderMetrics
          summary={summary}
          uncommittedPool={uncommittedPool}
          totalCommitted={totalFixedObligations}
          totalCommittedRatio={
            summary.base_amount > 0
              ? Math.round((totalFixedObligations / summary.base_amount) * 10000) / 100
              : 0
          }
          isEditingBase={isEditingBase}
          setIsEditingBase={setIsEditingBase}
          baseInput={baseInput}
          setBaseInput={setBaseInput}
          handleSaveBaseConfig={handleSaveBaseConfig}
          isPending={isPending}
        />

        {/* Sub-Navigation Tabs */}
        <TabNavigation activeTab={activeTab} onTabChange={setActiveTab} />

        {/* Tab Content Views */}
        {activeTab === "accounts" && <AccountsTab currency={summary.currency} />}
        {activeTab === "overview" && (
          <OverviewTab
            currency={summary.currency}
            uncommittedPool={uncommittedPool}
          />
        )}

        {activeTab === "budgets" && (
          <BudgetsTab
            summary={summary}
            onOpenAddBudget={handleOpenAddBudgetForm}
            onOpenEditBudget={handleOpenEditBudgetForm}
            onConfirmDeleteBudget={setBudgetToDelete}
            isPending={isPending}
          />
        )}

        {activeTab === "transactions" && (
          <TransactionsTab
            summary={summary}
            transactions={transactionPage.transactions}
            transactionPage={transactionPage}
            categories={categories}
            onOpenAddTransaction={handleOpenAddTransaction}
            onOpenEditTransaction={handleOpenEditTransaction}
            onConfirmDeleteTransaction={setTxToDelete}
            onOpenAddCategory={openCategoryDialog}
            onOpenStatementUpload={() => setIsStatementUploadOpen(true)}
            onPageChange={(page) => loadTransactionsPage(page)}
            onPageSizeChange={(pageSize) => loadTransactionsPage(1, pageSize)}
            isPending={isPending || isLoadingTransactions}
            txTypeFilter={txTypeFilter}
            onTxTypeFilterChange={(v) => setTxTypeFilter(v)}
            searchQuery={txSearchQuery}
            onSearchQueryChange={(v) => setTxSearchQuery(v)}
            txCategoryFilter={txCategoryFilter}
            onTxCategoryFilterChange={(v) => setTxCategoryFilter(v)}
          />
        )}

        {activeTab === "subscriptions" && (
          <SubscriptionsTab
            subscriptions={subscriptions}
            categories={categories}
            budgets={summary.budgets ?? []}
            currency={summary.currency}
            onAddSubscription={handleOpenAddSubscription}
            onEditSubscription={handleOpenEditSubscription}
            onDeleteSubscription={setSubToDelete}
            onToggleStatus={handleToggleSubscriptionStatus}
            onLogPayment={handleLogSubscriptionPayment}
            onViewTransactions={handleViewSubscriptionTransactions}
          />
        )}

        {activeTab === "fixed" && (
          <FixedObligationsTab
            summary={summary}
            subscriptions={subscriptions}
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
            error={errorMsg}
            onToggleDeductionActive={handleToggleDeductionActive}
            onDeleteDeduction={setDeductionToDelete}
            onEditSubscription={(sub) => {
              setEditingSubscription(sub);
              setIsSubscriptionDialogOpen(true);
            }}
            onNavigateToSubscriptions={() => setActiveTab("subscriptions")}
            isPending={isPending}
          />
        )}

        {activeTab === "planner" && (
          <PlannedPurchasesTab
            summary={summary}
            purchases={purchases}
            purchasesTotal={purchasesTotal}
            uncommittedPool={uncommittedPool}
            netRemainingPool={netRemainingPool}
            onOpenAddPurchase={() => setIsPlannedPurchaseDialogOpen(true)}
            onConfirmDeletePurchase={setPurchaseToDelete}
            isPending={isPending}
          />
        )}
      </div>

      {/* Confirmation Dialog for Fixed Obligation Deletion */}
      <PulseConfirmationDialog
        isOpen={!!deductionToDelete}
        onClose={() => { setDeductionToDelete(null); setErrorMsg(""); }}
        onConfirm={handleConfirmDeleteDeduction}
        title={deductionToDelete ? `Delete "${deductionToDelete.name}"?` : ""}
        description="This will permanently delete this fixed obligation. This action cannot be undone."
        confirmText="Delete obligation"
        confirmLoadingText="Deleting…"
        isLoading={deletingDeductionId !== null}
        error={errorMsg}
        variant="destructive"
      />

      {/* Confirmation Dialog for Budget Deletion */}
      <PulseConfirmationDialog
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
      <PulseConfirmationDialog
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

      {/* Confirmation Dialog for Transaction Deletion */}
      <PulseConfirmationDialog
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

      {/* Confirmation Dialog for Subscription Deletion */}
      <PulseConfirmationDialog
        isOpen={!!subToDelete}
        onClose={() => setSubToDelete(null)}
        onConfirm={() => {
          if (subToDelete) handleDeleteSubscription(subToDelete.id);
        }}
        title={subToDelete ? `Delete ${subToDelete.name}?` : ""}
        description="This will permanently delete this subscription. Linked past transactions will remain intact."
        confirmText="Delete subscription"
        confirmLoadingText="Deleting…"
        isLoading={deletingSubId !== null}
        error={errorMsg}
        variant="destructive"
      />

      {/* Subscription Linked Transactions Dialog */}
      {viewingSubTransactions && (
        <PulseDialog size="md" labelledBy="subscription-payment-history-title" onDismiss={() => setViewingSubTransactions(null)}>
            <PulseDialogHeader
              id="subscription-payment-history-title"
              description={`${subTransactionsList.length} recorded payments • Total spent: ${formatMoney(subTransactionsList.reduce((sum, t) => sum + t.amount, 0), summary.currency)}`}
            >
              {viewingSubTransactions.name} Payment History
            </PulseDialogHeader>

            <div className="mt-4 max-h-96 overflow-y-auto space-y-2 pr-1">
              {isLoadingSubTx ? (
                <div className="py-8 text-center text-xs text-muted-foreground">
                  Loading linked transactions...
                </div>
              ) : subTransactionsList.length === 0 ? (
                <div className="py-8 text-center text-xs text-muted-foreground">
                  No linked transactions recorded for this subscription yet.
                </div>
              ) : (
                subTransactionsList.map((tx) => (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between rounded-xl bg-background p-3 text-sm"
                  >
                    <div>
                      <span className="font-semibold text-foreground">{tx.name}</span>
                      <span className="block text-xs text-muted-foreground">
                        {formatInstant(tx.transaction_date, timeZone, {
                          year: "numeric",
                          month: "short",
                          day: "numeric",
                        })}
                      </span>
                    </div>
                    <span className="font-bold text-foreground">
                      {formatMoney(tx.amount, summary.currency)}
                    </span>
                  </div>
                ))
              )}
            </div>

            <PulseDialogActions>
              <PulseDialogAction
                type="button"
                variant="secondary"
                onClick={() => setViewingSubTransactions(null)}
              >
                Close
              </PulseDialogAction>
            </PulseDialogActions>
        </PulseDialog>
      )}

      {linkingPaymentSubscription && <LinkSubscriptionPaymentDialog
        subscription={linkingPaymentSubscription}
        currency={summary.currency}
        transactionCount={totalTransactionsCount}
        onClose={() => setLinkingPaymentSubscription(null)}
        onCreateNew={() => {
          setPaymentSubscription(linkingPaymentSubscription);
          setEditingTransaction(null);
          setLinkingPaymentSubscription(null);
          setIsTransactionDialogOpen(true);
        }}
        onLink={handleLinkSubscriptionPayment}
      />}

      {/* Transaction Entry Dialog */}
      {isTransactionDialogOpen && (
        <TransactionDialog
          isOpen={isTransactionDialogOpen}
          onClose={() => {
            setIsTransactionDialogOpen(false);
            setEditingTransaction(null);
            setPaymentSubscription(null);
          }}
          onSave={handleSaveTransaction}
          editingTransaction={editingTransaction}
          paymentSubscription={paymentSubscription}
          categories={categories}
          budgets={summary.budgets ?? []}
          subscriptions={subscriptions}
          currency={summary.currency}
          isPending={isPending}
        />
      )}

      {/* Subscription Entry Dialog */}
      {isSubscriptionDialogOpen && (
        <SubscriptionDialog
          isOpen={isSubscriptionDialogOpen}
          onClose={() => {
            setIsSubscriptionDialogOpen(false);
            setEditingSubscription(null);
          }}
          onSave={handleSaveSubscription}
          editingSubscription={editingSubscription}
          categories={categories}
          budgets={summary.budgets ?? []}
          currency={summary.currency}
        />
      )}

      {/* Category Entry Dialog */}
      {isCategoryDialogOpen && (
        <CategoryDialog
          isOpen={isCategoryDialogOpen}
          categories={categories}
          error={errorMsg}
          onClose={() => {
            setErrorMsg("");
            setIsCategoryDialogOpen(false);
          }}
          onSave={handleSaveCategory}
          isPending={isPending}
        />
      )}

      {/* Budget Entry Dialog */}
      {isBudgetDialogOpen && (
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
      )}

      {/* Planned Purchase Dialog */}
      {isPlannedPurchaseDialogOpen && (
        <PlannedPurchaseDialog
          isOpen={isPlannedPurchaseDialogOpen}
          onClose={() => setIsPlannedPurchaseDialogOpen(false)}
          onSave={handleSavePlannedPurchase}
          currency={summary.currency}
          isPending={isPending}
        />
      )}

      {/* Statement Upload & Extraction Dialog */}
      {isStatementUploadOpen && (
        <StatementUploadDialog
          onClose={() => setIsStatementUploadOpen(false)}
          onManageAccounts={() => { setIsStatementUploadOpen(false); setActiveTab("accounts"); }}
          categories={categories}
          onImported={result => {
            setTotalTransactionsCount(previous => previous + result.imported);
            loadTransactionsPage(1);
            void fetch("/api/horizon", { cache: "no-store" }).then(async response => {
              if (response.ok) setSummary(await response.json() as HorizonSummary);
            }).catch(() => setErrorMsg("Refresh Financial Horizon to see your updated totals."));
          }}
        />
      )}
    </main>
  );
}
