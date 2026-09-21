"use client";

import { useState, useEffect, useId } from "react";
import { X, Receipt, Wallet, ArrowDownRight, ArrowUpRight } from "lucide-react";
import { TransactionItem, CategoryItem, BudgetItem, SubscriptionItem } from "../dashboard/financial-horizon-card";
import Dialog, { DialogAction, DialogActions } from "../components/design-system/dialog";

export interface TransactionDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: {
    id?: number;
    name: string;
    amount: number;
    type?: "debit" | "credit";
    transactionDate: string;
    categoryId?: number | null;
    budgetId?: number | null;
    subscriptionId?: number | null;
    notes?: string | null;
  }) => Promise<void> | void;
  editingTransaction?: TransactionItem | null;
  categories: CategoryItem[];
  budgets: BudgetItem[];
  subscriptions?: SubscriptionItem[];
  currency: string;
  isPending?: boolean;
}

export default function TransactionDialog({
  isOpen,
  onClose,
  onSave,
  editingTransaction,
  categories,
  budgets,
  subscriptions = [],
  currency,
  isPending = false,
}: TransactionDialogProps) {
  const titleId = useId();

  const [txName, setTxName] = useState("");
  const [txAmount, setTxAmount] = useState("");
  const [txType, setTxType] = useState<"debit" | "credit">("debit");
  const [txDate, setTxDate] = useState("");
  const [txCategoryId, setTxCategoryId] = useState<number | null>(null);
  const [txBudgetId, setTxBudgetId] = useState<number | null>(null);
  const [txSubscriptionId, setTxSubscriptionId] = useState<number | null>(null);
  const [txNotes, setTxNotes] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  // Synchronize state when modal opens or editingTransaction changes
  useEffect(() => {
    if (!isOpen) return;

    setErrorMsg("");
    if (editingTransaction) {
      setTxName(editingTransaction.name);
      setTxAmount(editingTransaction.amount.toString());
      setTxType(editingTransaction.type || "debit");
      const d = editingTransaction.transaction_date ? new Date(editingTransaction.transaction_date) : new Date();
      const localIso = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
      setTxDate(localIso);
      setTxCategoryId(editingTransaction.category_id ?? null);
      setTxBudgetId(editingTransaction.budget_id ?? null);
      setTxSubscriptionId(editingTransaction.subscription_id ?? null);
      setTxNotes(editingTransaction.notes ?? "");
    } else {
      setTxName("");
      setTxAmount("");
      setTxType("debit");
      const now = new Date();
      const localIso = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
      setTxDate(localIso);
      setTxCategoryId(categories.length > 0 ? categories[0].id : null);
      setTxBudgetId(null);
      setTxSubscriptionId(null);
      setTxNotes("");
    }
  }, [isOpen, editingTransaction, categories]);

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

    const parsedAmount = parseFloat(txAmount);
    if (!txName.trim()) {
      setErrorMsg("Transaction name is required.");
      return;
    }
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setErrorMsg("Please enter a valid positive amount.");
      return;
    }

    const isoDate = txDate ? new Date(txDate).toISOString() : new Date().toISOString();

    try {
      await onSave({
        id: editingTransaction?.id,
        name: txName.trim(),
        amount: parsedAmount,
        type: txType,
        transactionDate: isoDate,
        categoryId: txCategoryId,
        budgetId: txBudgetId,
        subscriptionId: txSubscriptionId,
        notes: txNotes.trim() || null,
      });
    } catch {
      setErrorMsg("Failed to save transaction.");
    }
  };

  return (
    <Dialog
      labelledBy={titleId}
      onBackdropClick={handleBackdropClick}
      panelClassName="scale-100 transition-all duration-200 sm:p-6"
      size="wide"
    >
        <div className="flex items-center justify-between border-b border-border pb-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Receipt className="h-5 w-5" />
            </div>
            <h2 className="text-xl font-bold text-foreground" id={titleId}>
              {editingTransaction ? "Edit Transaction" : "Log New Transaction"}
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

          {/* Transaction Type Segment */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Transaction Type</label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setTxType("debit")}
                className={`cursor-pointer flex items-center justify-center gap-2 rounded-xl border py-2.5 text-xs font-semibold transition ${
                  txType === "debit"
                    ? "border-rose-500/50 bg-rose-500/10 text-rose-600 dark:text-rose-400 font-bold"
                    : "border-border bg-background text-muted-foreground hover:bg-secondary hover:text-foreground"
                }`}
              >
                <ArrowDownRight className="h-4 w-4 text-rose-500" />
                Debit (Expense)
              </button>
              <button
                type="button"
                onClick={() => setTxType("credit")}
                className={`cursor-pointer flex items-center justify-center gap-2 rounded-xl border py-2.5 text-xs font-semibold transition ${
                  txType === "credit"
                    ? "border-emerald-500/50 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-bold"
                    : "border-border bg-background text-muted-foreground hover:bg-secondary hover:text-foreground"
                }`}
              >
                <ArrowUpRight className="h-4 w-4 text-emerald-500" />
                Credit (Income / Refund)
              </button>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Transaction Title / Description</label>
            <input
              type="text"
              required
              placeholder="e.g. Netflix Monthly Payment, Spotify"
              value={txName}
              onChange={(e) => setTxName(e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Amount ({currency})</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                value={txAmount}
                onChange={(e) => setTxAmount(e.target.value)}
                className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Date & Time</label>
              <input
                type="datetime-local"
                required
                value={txDate}
                onChange={(e) => setTxDate(e.target.value)}
                className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Category</label>
              <select
                value={txCategoryId ?? ""}
                onChange={(e) => setTxCategoryId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Link to Budget</label>
              <select
                value={txBudgetId ?? ""}
                onChange={(e) => setTxBudgetId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">-- Unlinked --</option>
                {budgets.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Link to Subscription</label>
              <select
                value={txSubscriptionId ?? ""}
                onChange={(e) => setTxSubscriptionId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">-- Unlinked --</option>
                {subscriptions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({currency}{s.amount})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Notes / Remarks (Optional)</label>
            <input
              type="text"
              placeholder="e.g. Paid via UPI, invoice #1234"
              value={txNotes}
              onChange={(e) => setTxNotes(e.target.value)}
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
              {isPending ? "Saving…" : editingTransaction ? "Update Transaction" : "Save Transaction"}
            </DialogAction>
          </DialogActions>
        </form>
    </Dialog>
  );
}
