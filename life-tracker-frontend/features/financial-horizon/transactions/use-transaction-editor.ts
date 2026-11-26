"use client";

import { useState, type Dispatch, type SetStateAction } from "react";
import { addTransactionAction, deleteTransactionAction, updateTransactionAction } from "@/features/financial-horizon/api/horizon-actions";
import type { HorizonMutations } from "@/features/financial-horizon/types/horizon-mutations";
import type { SubscriptionItem, TransactionItem, TransactionPage } from "@/features/financial-horizon/types/horizon-types";

export interface TransactionFields {
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
}

interface TransactionList {
  transactionPage: TransactionPage;
  loadTransactionsPage: (page: number, pageSize?: number) => void;
  setTotalTransactionsCount: Dispatch<SetStateAction<number>>;
}

export function useTransactionEditor(
  { setErrorMsg, startTransition, recalculateSummary, updateTransactions, updateSubscriptionsState }: HorizonMutations,
  { transactionPage, loadTransactionsPage, setTotalTransactionsCount }: TransactionList,
) {
  const [isTransactionDialogOpen, setIsTransactionDialogOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<TransactionItem | null>(null);
  const [paymentSubscription, setPaymentSubscription] = useState<SubscriptionItem | null>(null);
  const [txToDelete, setTxToDelete] = useState<TransactionItem | null>(null);

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

  /** Opens a new transaction prefilled as a payment for the subscription. */
  const handleOpenSubscriptionPayment = (subscription: SubscriptionItem) => {
    setPaymentSubscription(subscription);
    setEditingTransaction(null);
    setIsTransactionDialogOpen(true);
  };

  const handleCloseTransactionDialog = () => {
    setIsTransactionDialogOpen(false);
    setEditingTransaction(null);
    setPaymentSubscription(null);
  };

  const handleSaveTransaction = async (data: TransactionFields) => {
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

  return {
    isTransactionDialogOpen,
    editingTransaction,
    paymentSubscription,
    txToDelete,
    setTxToDelete,
    handleOpenAddTransaction,
    handleOpenEditTransaction,
    handleOpenSubscriptionPayment,
    handleCloseTransactionDialog,
    handleSaveTransaction,
    handleDeleteTransaction,
  };
}
