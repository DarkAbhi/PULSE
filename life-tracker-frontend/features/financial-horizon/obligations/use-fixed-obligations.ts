"use client";

import { useState } from "react";
import { addDeductionAction, deleteDeductionAction, updateDeductionAction } from "@/features/financial-horizon/api/horizon-actions";
import type { HorizonMutations } from "@/features/financial-horizon/types/horizon-mutations";
import type { DeductionItem } from "@/features/financial-horizon/types/horizon-types";

export function useFixedObligations(
  deductions: DeductionItem[],
  { setErrorMsg, startTransition, recalculateSummary }: HorizonMutations,
) {
  const [activeCategory, setActiveCategory] = useState<string>("all");
  const [isAddingDeduction, setIsAddingDeduction] = useState(false);
  const [editingDeductionId, setEditingDeductionId] = useState<number | null>(null);
  const [deductionToDelete, setDeductionToDelete] = useState<DeductionItem | null>(null);
  const [deletingDeductionId, setDeletingDeductionId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState<string>("housing");
  const [dueDay, setDueDay] = useState<string>("");
  const [selectedBudgetId, setSelectedBudgetId] = useState<number | null>(null);

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
        const existing = deductions.find((d) => d.id === editingDeductionId);
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

  return {
    activeCategory,
    setActiveCategory,
    isAddingDeduction,
    editingDeductionId,
    deductionToDelete,
    setDeductionToDelete,
    deletingDeductionId,
    form: { name, setName, amount, setAmount, category, setCategory, dueDay, setDueDay, selectedBudgetId, setSelectedBudgetId },
    handleOpenAddDeductionForm,
    handleOpenEditDeductionForm,
    handleCancelDeductionForm,
    handleSaveDeduction,
    handleToggleDeductionActive,
    handleConfirmDeleteDeduction,
  };
}
