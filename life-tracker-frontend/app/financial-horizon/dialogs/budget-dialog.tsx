"use client";

import { currencySymbol } from "@/app/lib/currency.mjs";


import { useState, useEffect, useId } from "react";
import { Target } from "lucide-react";
import type { BudgetItem } from "@/app/financial-horizon/horizon-types";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

export interface BudgetDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: { id?: number; name: string; allocatedAmount: number }) => Promise<void> | void;
  editingBudget?: BudgetItem | null;
  currency: string;
  isPending?: boolean;
}

export default function BudgetDialog({
  isOpen,
  onClose,
  onSave,
  editingBudget = null,
  currency,
  isPending = false,
}: BudgetDialogProps) {
  const titleId = useId();

  const [name, setName] = useState("");
  const [allocatedAmount, setAllocatedAmount] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    if (!isOpen) return;
    if (editingBudget) {
      setName(editingBudget.name);
      setAllocatedAmount(editingBudget.allocated_amount.toString());
    } else {
      setName("");
      setAllocatedAmount("");
    }
    setErrorMsg("");
  }, [isOpen, editingBudget]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");

    const parsedAllocated = parseFloat(allocatedAmount);
    if (!name.trim()) {
      setErrorMsg("Budget name is required.");
      return;
    }
    if (isNaN(parsedAllocated) || parsedAllocated < 0) {
      setErrorMsg("Please enter a valid non-negative allocated amount.");
      return;
    }

    try {
      await onSave({
        id: editingBudget?.id,
        name: name.trim(),
        allocatedAmount: parsedAllocated,
      });
    } catch {
      setErrorMsg("Failed to save budget.");
    }
  };

  return (
    <PulseDialog
      labelledBy={titleId}
      onDismiss={isPending ? undefined : onClose}
    >
        <PulseDialogHeader id={titleId} icon={<Target className="size-5" />} onClose={onClose} closeDisabled={isPending}>
          {editingBudget ? "Edit Monthly Budget" : "Create Monthly Budget"}
        </PulseDialogHeader>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {errorMsg && (
            <div className="rounded-xl bg-destructive/10 p-3 text-xs font-semibold text-destructive">
              {errorMsg}
            </div>
          )}

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Budget Name</label>
            <input
              type="text"
              required
              placeholder="e.g. Housing & Utilities, Subscriptions, Shopping"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3.5 py-2.5 text-sm text-foreground"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">
              Monthly Allocated Amount ({currencySymbol(currency)})
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              required
              placeholder="0.00"
              value={allocatedAmount}
              onChange={(e) => setAllocatedAmount(e.target.value)}
              className="w-full px-3.5 py-2.5 text-sm text-foreground"
            />
          </div>

          <PulseDialogActions>
            <PulseDialogAction
              type="button"
              variant="secondary"
              disabled={isPending}
              onClick={onClose}
            >
              Cancel
            </PulseDialogAction>
            <PulseDialogAction
              type="submit"
              disabled={isPending}
            >
              {isPending ? "Saving…" : editingBudget ? "Update Budget" : "Save Budget"}
            </PulseDialogAction>
          </PulseDialogActions>
        </form>
    </PulseDialog>
  );
}
