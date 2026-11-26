"use client";

import { useState } from "react";
import {
  addSubscriptionAction,
  deleteSubscriptionAction,
  getSubscriptionTransactionsAction,
  linkSubscriptionTransactionAction,
  updateSubscriptionAction,
} from "@/features/financial-horizon/api/horizon-actions";
import type { HorizonMutations } from "@/features/financial-horizon/types/horizon-mutations";
import type { SubscriptionItem, TransactionItem } from "@/features/financial-horizon/types/horizon-types";

export interface SubscriptionFields {
  name: string;
  amount: number;
  billing_cycle: string;
  billing_day?: number | null;
  renewal_date?: string | null;
  status: string;
  category_id?: number | null;
  budget_id?: number | null;
  notes?: string | null;
}

export function useSubscriptions({ setErrorMsg, startTransition, updateTransactions, updateSubscriptionsState }: HorizonMutations) {
  const [isSubscriptionDialogOpen, setIsSubscriptionDialogOpen] = useState(false);
  const [editingSubscription, setEditingSubscription] = useState<SubscriptionItem | null>(null);
  const [subToDelete, setSubToDelete] = useState<SubscriptionItem | null>(null);
  const [deletingSubId, setDeletingSubId] = useState<number | null>(null);
  const [viewingSubTransactions, setViewingSubTransactions] = useState<SubscriptionItem | null>(null);
  const [subTransactionsList, setSubTransactionsList] = useState<TransactionItem[]>([]);
  const [isLoadingSubTx, setIsLoadingSubTx] = useState(false);
  const [linkingPaymentSubscription, setLinkingPaymentSubscription] = useState<SubscriptionItem | null>(null);

  const handleOpenAddSubscription = () => {
    setEditingSubscription(null);
    setIsSubscriptionDialogOpen(true);
  };

  const handleOpenEditSubscription = (sub: SubscriptionItem) => {
    setEditingSubscription(sub);
    setIsSubscriptionDialogOpen(true);
  };

  const handleCloseSubscriptionDialog = () => {
    setIsSubscriptionDialogOpen(false);
    setEditingSubscription(null);
  };

  const handleSaveSubscription = async (data: SubscriptionFields) => {
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

  return {
    isSubscriptionDialogOpen,
    editingSubscription,
    subToDelete,
    setSubToDelete,
    deletingSubId,
    viewingSubTransactions,
    setViewingSubTransactions,
    subTransactionsList,
    isLoadingSubTx,
    linkingPaymentSubscription,
    setLinkingPaymentSubscription,
    handleOpenAddSubscription,
    handleOpenEditSubscription,
    handleCloseSubscriptionDialog,
    handleSaveSubscription,
    handleDeleteSubscription,
    handleToggleSubscriptionStatus,
    handleLinkSubscriptionPayment,
    handleViewSubscriptionTransactions,
  };
}
