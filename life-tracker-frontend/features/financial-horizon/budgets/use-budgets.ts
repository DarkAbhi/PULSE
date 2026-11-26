"use client";

import { useState } from "react";
import { addBudgetAction, deleteBudgetAction, updateBudgetAction } from "@/features/financial-horizon/api/horizon-actions";
import type { HorizonMutations } from "@/features/financial-horizon/types/horizon-mutations";
import type { BudgetItem, TransactionItem } from "@/features/financial-horizon/types/horizon-types";

export function useBudgets({ setErrorMsg, startTransition, recalculateSummary, updateTransactions }: HorizonMutations) {
  const [isBudgetDialogOpen, setIsBudgetDialogOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState<BudgetItem | null>(null);
  const [budgetToDelete, setBudgetToDelete] = useState<BudgetItem | null>(null);
  const [deletingBudgetId, setDeletingBudgetId] = useState<number | null>(null);

  const handleOpenAddBudgetForm = () => {
    setEditingBudget(null);
    setIsBudgetDialogOpen(true);
  };

  const handleOpenEditBudgetForm = (b: BudgetItem) => {
    setEditingBudget(b);
    setIsBudgetDialogOpen(true);
  };

  const handleCloseBudgetDialog = () => {
    setIsBudgetDialogOpen(false);
    setEditingBudget(null);
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

  return {
    isBudgetDialogOpen,
    editingBudget,
    budgetToDelete,
    setBudgetToDelete,
    deletingBudgetId,
    handleOpenAddBudgetForm,
    handleOpenEditBudgetForm,
    handleCloseBudgetDialog,
    handleSaveBudget,
    handleDeleteBudget,
  };
}
