"use client";

import { useState, useEffect, useId } from "react";
import { X, Target } from "lucide-react";
import { BudgetItem } from "../dashboard/financial-horizon-card";
import Dialog, { DialogAction, DialogActions } from "../components/design-system/dialog";

export interface BudgetDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: { id?: number; name: string; allocatedAmount: number }) => Promise<void> | void;
  editingBudget?: BudgetItem | null;
  currency?: string;
  isPending?: boolean;
}

export default function BudgetDialog({
  isOpen,
  onClose,
  onSave,
  editingBudget = null,
  currency = "₹",
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

  // Escape key listener to close modal
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isPending) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose, isPending]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget && !isPending) {
      onClose();
    }
  };

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
    <Dialog
      labelledBy={titleId}
      onBackdropClick={handleBackdropClick}
      panelClassName="scale-100 transition-all duration-200 sm:p-6"
    >
        <div className="flex items-center justify-between border-b border-border pb-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Target className="h-5 w-5" />
            </div>
            <h2 className="text-xl font-bold text-foreground" id={titleId}>
              {editingBudget ? "Edit Monthly Budget" : "Create Monthly Budget"}
            </h2>
          </div>
          <button
            onClick={onClose}
            disabled={isPending}
            className="cursor-pointer rounded-lg p-1.5 text-muted-foreground hover:bg-secondary hover:text-foreground transition disabled:opacity-50"
            title="Close dialog"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {errorMsg && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs font-semibold text-destructive">
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
              className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">
              Monthly Allocated Amount ({currency})
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              required
              placeholder="0.00"
              value={allocatedAmount}
              onChange={(e) => setAllocatedAmount(e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <DialogActions className="border-t border-border pt-4">
            <DialogAction
              type="button"
              variant="secondary"
              disabled={isPending}
              onClick={onClose}
            >
              Cancel
            </DialogAction>
            <DialogAction
              type="submit"
              disabled={isPending}
            >
              {isPending ? "Saving…" : editingBudget ? "Update Budget" : "Save Budget"}
            </DialogAction>
          </DialogActions>
        </form>
    </Dialog>
  );
}
