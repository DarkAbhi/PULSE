"use client";

import { useState, useTransition, useEffect, useMemo, type ReactNode } from "react";
import dynamic from "next/dynamic";
import { useSearchParams } from "next/navigation";
import { Compass, X } from "lucide-react";
import { frame } from "@/components/layout/page-frame";
import PulseConfirmationDialog from "@/components/ui/pulse-confirmation-dialog";
import PulseIconButton from "@/components/ui/pulse-icon-button";
import { useCurrency } from "@/providers/profile-preferences-provider";
import { addHorizonCategoryAction, updateHorizonConfigAction } from "@/features/financial-horizon/api/horizon-actions";
import HeaderMetrics from "@/features/financial-horizon/components/header-metrics";
import QuickActionDropdown from "@/features/financial-horizon/components/quick-action-dropdown";
import TabNavigation, { horizonTab, type HorizonTab } from "@/features/financial-horizon/components/tab-navigation";
import { useBudgets } from "@/features/financial-horizon/budgets/use-budgets";
import { useFixedObligations } from "@/features/financial-horizon/obligations/use-fixed-obligations";
import OverviewTab from "@/features/financial-horizon/overview/overview-tab";
import { usePlannedPurchases } from "@/features/financial-horizon/planned-purchases/use-planned-purchases";
import SubscriptionHistoryDialog from "@/features/financial-horizon/subscriptions/subscription-history-dialog";
import { useSubscriptions } from "@/features/financial-horizon/subscriptions/use-subscriptions";
import { useTransactionEditor } from "@/features/financial-horizon/transactions/use-transaction-editor";
import { useTransactions } from "@/features/financial-horizon/transactions/use-transactions";
import type { HorizonMutations } from "@/features/financial-horizon/types/horizon-mutations";
import type { HorizonSummary, NextMonthPurchaseItem, SubscriptionItem, TransactionPage } from "@/features/financial-horizon/types/horizon-types";
import { calculateHorizonSummary, updateHorizonSubscriptions, type HorizonUpdates } from "@/features/financial-horizon/utils/horizon-summary";

const TransactionDialog = dynamic(() => import("@/features/financial-horizon/transactions/transaction-dialog"));
const LinkSubscriptionPaymentDialog = dynamic(() => import("@/features/financial-horizon/subscriptions/link-subscription-payment-dialog"));
const CategoryDialog = dynamic(() => import("@/features/financial-horizon/components/category-dialog"));
const BudgetDialog = dynamic(() => import("@/features/financial-horizon/budgets/budget-dialog"));
const SubscriptionDialog = dynamic(() => import("@/features/financial-horizon/subscriptions/subscription-dialog"));
const PlannedPurchaseDialog = dynamic(() => import("@/features/financial-horizon/planned-purchases/planned-purchase-dialog"));
const StatementUploadDialog = dynamic(() => import("@/features/financial-horizon/transactions/statement-upload-dialog"));
const AccountsTab = dynamic(() => import("@/features/financial-horizon/accounts/accounts-tab"));
const BudgetsTab = dynamic(() => import("@/features/financial-horizon/budgets/budgets-tab"));
const TransactionsTab = dynamic(() => import("@/features/financial-horizon/transactions/transactions-tab"));
const SubscriptionsTab = dynamic(() => import("@/features/financial-horizon/subscriptions/subscriptions-tab"));
const FixedObligationsTab = dynamic(() => import("@/features/financial-horizon/obligations/fixed-obligations-tab"));
const PlannedPurchasesTab = dynamic(() => import("@/features/financial-horizon/planned-purchases/planned-purchases-tab"));

interface FinancialHorizonClientProps {
  appBar: ReactNode;
  initialSummary: HorizonSummary;
  initialLoadError: boolean;
  initialPurchases: NextMonthPurchaseItem[];
  initialTransactionPage: TransactionPage;
}

/**
 * Owns the state every tab shares: the horizon summary, the error banner and one transition.
 * Each subdomain's dialogs and mutations live in its own hook (budgets/, obligations/, …).
 */
export default function FinancialHorizonClient({
  appBar,
  initialSummary,
  initialLoadError,
  initialPurchases,
  initialTransactionPage,
}: FinancialHorizonClientProps) {
  const { currency } = useCurrency();
  const activeTab = horizonTab(useSearchParams().get("tab"));

  function setActiveTab(tab: HorizonTab) {
    const url = new URL(window.location.href);
    if (tab === "overview") url.searchParams.delete("tab");
    else url.searchParams.set("tab", tab);
    if (url.href !== window.location.href) window.history.pushState(null, "", url);
  }

  const [summaryData, setSummary] = useState<HorizonSummary>({
    ...initialSummary,
    budgets: initialSummary.budgets ?? [],
  });
  const summary = useMemo(() => ({ ...summaryData, currency }), [summaryData, currency]);
  const categories = summary.categories ?? [];
  const subscriptions = summary.subscriptions ?? [];

  useEffect(() => {
    setSummary({ ...initialSummary, budgets: initialSummary.budgets ?? [] });
  }, [initialSummary]);

  const [isEditingBase, setIsEditingBase] = useState(false);
  const [baseInput, setBaseInput] = useState(initialSummary.base_amount.toString());
  const [isStatementUploadOpen, setIsStatementUploadOpen] = useState(false);
  const [isCategoryDialogOpen, setIsCategoryDialogOpen] = useState(false);

  const [errorMsg, setErrorMsg] = useState("");
  const [isPending, startTransition] = useTransition();
  const {
    page: transactionPage, total: totalTransactionsCount, setTotal: setTotalTransactionsCount,
    type: txTypeFilter, setType: setTxTypeFilter, search: txSearchQuery, setSearch: setTxSearchQuery,
    category: txCategoryFilter, setCategory: setTxCategoryFilter, updateTransactions,
    loadPage: loadTransactionsPage, isPending: isLoadingTransactions,
  } = useTransactions(initialTransactionPage, setErrorMsg);

  const mutations: HorizonMutations = {
    setErrorMsg,
    startTransition,
    recalculateSummary: (updates: HorizonUpdates = {}) => setSummary(previous => calculateHorizonSummary(previous, updates)),
    updateTransactions,
    updateSubscriptionsState: (update: (items: SubscriptionItem[]) => SubscriptionItem[]) =>
      setSummary(previous => updateHorizonSubscriptions(previous, update)),
  };
  const budgets = useBudgets(mutations);
  const obligations = useFixedObligations(summary.deductions, mutations);
  const planned = usePlannedPurchases(initialPurchases, mutations);
  const subs = useSubscriptions(mutations);
  const transactions = useTransactionEditor(mutations, { transactionPage, loadTransactionsPage, setTotalTransactionsCount });

  // Calculated metrics
  const subscriptionBurnTotal = summary.total_subscription_burn ?? 0;
  const totalFixedObligations = summary.total_deductions + subscriptionBurnTotal;
  const uncommittedPool = summary.base_amount - totalFixedObligations;
  const netRemainingPool = uncommittedPool - planned.purchasesTotal;

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

  return (
    <main id="main-content" tabIndex={-1} className={`${frame.page}`}>
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
                onLogTransaction={transactions.handleOpenAddTransaction}
                onAddFixedObligation={() => {
                  setActiveTab("fixed");
                  obligations.handleOpenAddDeductionForm();
                }}
                onAddPlannedPurchase={() => planned.setIsPlannedPurchaseDialogOpen(true)}
                onAddBudget={budgets.handleOpenAddBudgetForm}
                onAddCategory={openCategoryDialog}
                isPending={isPending}
              />
            </div>
          </div>
        </header>

        {initialLoadError && (
          <p className="rounded-xl bg-warning-soft p-4 text-sm text-warning" role="status">
            Some transaction or purchase data couldn’t load. Refresh this page for up-to-date totals.
          </p>
        )}
        {errorMsg && !(activeTab === "fixed" && (obligations.isAddingDeduction || obligations.editingDeductionId !== null)) && (
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

        <TabNavigation activeTab={activeTab} onTabChange={setActiveTab} />

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
            onOpenAddBudget={budgets.handleOpenAddBudgetForm}
            onOpenEditBudget={budgets.handleOpenEditBudgetForm}
            onConfirmDeleteBudget={budgets.setBudgetToDelete}
            isPending={isPending}
          />
        )}

        {activeTab === "transactions" && (
          <TransactionsTab
            summary={summary}
            transactions={transactionPage.transactions}
            transactionPage={transactionPage}
            categories={categories}
            onOpenAddTransaction={transactions.handleOpenAddTransaction}
            onOpenEditTransaction={transactions.handleOpenEditTransaction}
            onConfirmDeleteTransaction={transactions.setTxToDelete}
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
            onAddSubscription={subs.handleOpenAddSubscription}
            onEditSubscription={subs.handleOpenEditSubscription}
            onDeleteSubscription={subs.setSubToDelete}
            onToggleStatus={subs.handleToggleSubscriptionStatus}
            onLogPayment={subs.setLinkingPaymentSubscription}
            onViewTransactions={subs.handleViewSubscriptionTransactions}
          />
        )}

        {activeTab === "fixed" && (
          <FixedObligationsTab
            summary={summary}
            subscriptions={subscriptions}
            activeCategory={obligations.activeCategory}
            setActiveCategory={obligations.setActiveCategory}
            isAddingDeduction={obligations.isAddingDeduction}
            editingDeductionId={obligations.editingDeductionId}
            {...obligations.form}
            onOpenAddDeduction={obligations.handleOpenAddDeductionForm}
            onOpenEditDeduction={obligations.handleOpenEditDeductionForm}
            onCancelDeductionForm={obligations.handleCancelDeductionForm}
            onSaveDeduction={obligations.handleSaveDeduction}
            error={errorMsg}
            onToggleDeductionActive={obligations.handleToggleDeductionActive}
            onDeleteDeduction={obligations.setDeductionToDelete}
            onEditSubscription={subs.handleOpenEditSubscription}
            onNavigateToSubscriptions={() => setActiveTab("subscriptions")}
            isPending={isPending}
          />
        )}

        {activeTab === "planner" && (
          <PlannedPurchasesTab
            summary={summary}
            purchases={planned.purchases}
            purchasesTotal={planned.purchasesTotal}
            uncommittedPool={uncommittedPool}
            netRemainingPool={netRemainingPool}
            onOpenAddPurchase={() => planned.setIsPlannedPurchaseDialogOpen(true)}
            onConfirmDeletePurchase={planned.setPurchaseToDelete}
            isPending={isPending}
          />
        )}
      </div>

      <PulseConfirmationDialog
        isOpen={!!obligations.deductionToDelete}
        onClose={() => { obligations.setDeductionToDelete(null); setErrorMsg(""); }}
        onConfirm={obligations.handleConfirmDeleteDeduction}
        title={obligations.deductionToDelete ? `Delete "${obligations.deductionToDelete.name}"?` : ""}
        description="This will permanently delete this fixed obligation. This action cannot be undone."
        confirmText="Delete obligation"
        confirmLoadingText="Deleting…"
        isLoading={obligations.deletingDeductionId !== null}
        error={errorMsg}
        variant="destructive"
      />

      <PulseConfirmationDialog
        isOpen={!!budgets.budgetToDelete}
        onClose={() => budgets.setBudgetToDelete(null)}
        onConfirm={() => {
          if (budgets.budgetToDelete) budgets.handleDeleteBudget(budgets.budgetToDelete.id);
        }}
        title={budgets.budgetToDelete ? `Delete ${budgets.budgetToDelete.name}?` : ""}
        description="This will permanently delete this monthly budget. Any linked obligations will remain intact but become unbudgeted."
        confirmText="Delete budget"
        confirmLoadingText="Deleting…"
        isLoading={budgets.deletingBudgetId !== null}
        error={errorMsg}
        variant="destructive"
      />

      <PulseConfirmationDialog
        isOpen={!!planned.purchaseToDelete}
        onClose={() => planned.setPurchaseToDelete(null)}
        onConfirm={() => {
          if (planned.purchaseToDelete) planned.handleDeletePurchase(planned.purchaseToDelete);
        }}
        title={planned.purchaseToDelete ? `Delete ${planned.purchaseToDelete.name}?` : ""}
        description="This permanently removes it from your next month purchases."
        confirmText="Delete purchase"
        confirmLoadingText="Deleting…"
        isLoading={planned.deletingPurchaseID !== null}
        error={errorMsg}
        variant="destructive"
      />

      <PulseConfirmationDialog
        isOpen={!!transactions.txToDelete}
        onClose={() => transactions.setTxToDelete(null)}
        onConfirm={transactions.handleDeleteTransaction}
        title={transactions.txToDelete ? `Delete "${transactions.txToDelete.name}"?` : ""}
        description="This will permanently delete this transaction record."
        confirmText="Delete transaction"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={errorMsg}
        variant="destructive"
      />

      <PulseConfirmationDialog
        isOpen={!!subs.subToDelete}
        onClose={() => subs.setSubToDelete(null)}
        onConfirm={() => {
          if (subs.subToDelete) subs.handleDeleteSubscription(subs.subToDelete.id);
        }}
        title={subs.subToDelete ? `Delete ${subs.subToDelete.name}?` : ""}
        description="This will permanently delete this subscription. Linked past transactions will remain intact."
        confirmText="Delete subscription"
        confirmLoadingText="Deleting…"
        isLoading={subs.deletingSubId !== null}
        error={errorMsg}
        variant="destructive"
      />

      {subs.viewingSubTransactions && (
        <SubscriptionHistoryDialog
          subscription={subs.viewingSubTransactions}
          transactions={subs.subTransactionsList}
          isLoading={subs.isLoadingSubTx}
          currency={summary.currency}
          onClose={() => subs.setViewingSubTransactions(null)}
        />
      )}

      {subs.linkingPaymentSubscription && <LinkSubscriptionPaymentDialog
        subscription={subs.linkingPaymentSubscription}
        currency={summary.currency}
        transactionCount={totalTransactionsCount}
        onClose={() => subs.setLinkingPaymentSubscription(null)}
        onCreateNew={() => {
          if (subs.linkingPaymentSubscription) transactions.handleOpenSubscriptionPayment(subs.linkingPaymentSubscription);
          subs.setLinkingPaymentSubscription(null);
        }}
        onLink={subs.handleLinkSubscriptionPayment}
      />}

      {transactions.isTransactionDialogOpen && (
        <TransactionDialog
          isOpen={transactions.isTransactionDialogOpen}
          onClose={transactions.handleCloseTransactionDialog}
          onSave={transactions.handleSaveTransaction}
          editingTransaction={transactions.editingTransaction}
          paymentSubscription={transactions.paymentSubscription}
          categories={categories}
          budgets={summary.budgets ?? []}
          subscriptions={subscriptions}
          currency={summary.currency}
          isPending={isPending}
        />
      )}

      {subs.isSubscriptionDialogOpen && (
        <SubscriptionDialog
          isOpen={subs.isSubscriptionDialogOpen}
          onClose={subs.handleCloseSubscriptionDialog}
          onSave={subs.handleSaveSubscription}
          editingSubscription={subs.editingSubscription}
          categories={categories}
          budgets={summary.budgets ?? []}
          currency={summary.currency}
        />
      )}

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

      {budgets.isBudgetDialogOpen && (
        <BudgetDialog
          isOpen={budgets.isBudgetDialogOpen}
          onClose={budgets.handleCloseBudgetDialog}
          onSave={budgets.handleSaveBudget}
          editingBudget={budgets.editingBudget}
          currency={summary.currency}
          isPending={isPending}
        />
      )}

      {planned.isPlannedPurchaseDialogOpen && (
        <PlannedPurchaseDialog
          isOpen={planned.isPlannedPurchaseDialogOpen}
          onClose={() => planned.setIsPlannedPurchaseDialogOpen(false)}
          onSave={planned.handleSavePlannedPurchase}
          currency={summary.currency}
          isPending={isPending}
        />
      )}

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
