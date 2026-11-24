"use client";

import CategoryOptions from "@/app/financial-horizon/components/category-options";

import PulseSelect from "@/app/components/design-system/pulse-select";
import { currencySymbol } from "@/app/lib/currency.mjs";
import { useTimeZone } from "@/app/components/profile-preferences-provider";
import { toDateTimeInput, fromDateTimeInput } from "@/app/lib/time-zone.mjs";

import PulseTabs, { type PulseTabItem } from "@/app/components/design-system/pulse-tabs";

import { useState, useEffect, useId } from "react";
import { Receipt, Wallet, ArrowDownRight, ArrowUpRight } from "lucide-react";
import type { TransactionItem, CategoryItem, BudgetItem, SubscriptionItem } from "@/app/financial-horizon/horizon-types";
import { listAccounts } from "@/app/financial-horizon/accounts";
import type { Account } from "@/app/financial-horizon/account-types";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

const transactionTypeTabs = [
  { value: "debit", label: "Debit (Expense)", icon: <ArrowDownRight className="text-destructive" /> },
  { value: "credit", label: "Credit (Income / Refund)", icon: <ArrowUpRight className="text-success" /> },
] as const satisfies readonly PulseTabItem<"debit" | "credit">[];

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
    accountId?: number | null;
  }) => Promise<void> | void;
  editingTransaction?: TransactionItem | null;
  paymentSubscription?: SubscriptionItem | null;
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
  paymentSubscription,
  categories,
  budgets,
  subscriptions = [],
  currency,
  isPending = false,
}: TransactionDialogProps) {
  const { timeZone } = useTimeZone();
  const titleId = useId();
  const [entryTimeZone, setEntryTimeZone] = useState(timeZone);

  const [txName, setTxName] = useState("");
  const [txAmount, setTxAmount] = useState("");
  const [txType, setTxType] = useState<"debit" | "credit">("debit");
  const [txDate, setTxDate] = useState("");
  const [txCategoryId, setTxCategoryId] = useState<number | null>(null);
  const [txBudgetId, setTxBudgetId] = useState<number | null>(null);
  const [txSubscriptionId, setTxSubscriptionId] = useState<number | null>(null);
  const [txNotes, setTxNotes] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountId, setAccountId] = useState<number | null>(null);
  const accountEditable = editingTransaction?.source !== "statement";

  useEffect(() => {
    if (!isOpen || !accountEditable) return;
    let current = true;
    void listAccounts(true).then(response => {
      if (current && response.ok) setAccounts(response.data.accounts);
    });
    return () => { current = false; };
  }, [isOpen, accountEditable]);

  // Synchronize state when modal opens or editingTransaction changes
  useEffect(() => {
    if (!isOpen) return;

    setErrorMsg("");
    setAccountId(editingTransaction?.account_id ?? null);
    setEntryTimeZone(timeZone);
    if (editingTransaction) {
      setTxName(editingTransaction.name);
      setTxAmount(editingTransaction.amount.toString());
      setTxType(editingTransaction.type || "debit");
      const d = editingTransaction.transaction_date ? new Date(editingTransaction.transaction_date) : new Date();
      const localIso = toDateTimeInput(d, timeZone);
      setTxDate(localIso);
      setTxCategoryId(editingTransaction.category_id ?? null);
      setTxBudgetId(editingTransaction.budget_id ?? null);
      setTxSubscriptionId(editingTransaction.subscription_id ?? null);
      setTxNotes(editingTransaction.notes ?? "");
    } else {
      setTxName(paymentSubscription ? `${paymentSubscription.name} Payment` : "");
      setTxAmount(paymentSubscription ? String(paymentSubscription.amount) : "");
      setTxType("debit");
      const now = new Date();
      const localIso = toDateTimeInput(now, timeZone);
      setTxDate(localIso);
      setTxCategoryId(paymentSubscription?.category_id ?? null);
      setTxBudgetId(paymentSubscription?.budget_id ?? null);
      setTxSubscriptionId(paymentSubscription?.id ?? null);
      setTxNotes(paymentSubscription ? `Recurring payment for ${paymentSubscription.name}` : "");
    }
  }, [isOpen, editingTransaction, paymentSubscription]);

  if (!isOpen) return null;

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

    try {
      const isoDate = txDate ? fromDateTimeInput(txDate, entryTimeZone, editingTransaction?.transaction_date) : new Date().toISOString();
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
        accountId: accountEditable && accountId !== (editingTransaction?.account_id ?? null) ? accountId : undefined,
      });
    } catch (error) {
      setErrorMsg(error instanceof RangeError ? error.message : "Failed to save transaction.");
    }
  };

  return (
    <PulseDialog
      labelledBy={titleId}
      onDismiss={isPending ? undefined : onClose}
      size="md"
    >
        <PulseDialogHeader id={titleId} icon={<Receipt className="size-5" />} onClose={onClose} closeDisabled={isPending}>
          {editingTransaction ? "Edit Transaction" : "Log New Transaction"}
        </PulseDialogHeader>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {errorMsg && (
            <div className="rounded-xl bg-destructive/10 p-3 text-xs font-semibold text-destructive">
              {errorMsg}
            </div>
          )}

          {/* Transaction Type Segment */}
          <div className="space-y-1">
            <p aria-hidden="true" className="text-xs font-semibold text-muted-foreground">Transaction Type</p>
            <PulseTabs
              variant="segmented"
              label="Transaction type"
              items={transactionTypeTabs}
              value={txType}
              onChange={setTxType}
              className="flex w-full [&>button]:min-w-0 [&>button]:flex-1 [&>button]:text-center"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Transaction Title / Description</label>
            <input
              type="text"
              required
              placeholder="e.g. Netflix Monthly Payment, Spotify"
              value={txName}
              onChange={(e) => setTxName(e.target.value)}
              className="w-full px-3.5 py-2.5 text-sm text-foreground"
            />
          </div>

          {editingTransaction?.original_narration && (
            <details className="min-w-0 text-sm text-muted-foreground">
              <summary className="w-fit cursor-pointer rounded py-2 focus-visible:outline-2 focus-visible:outline-ring">Original statement narration</summary>
              <p className="whitespace-pre-wrap [overflow-wrap:anywhere]">{editingTransaction.original_narration}</p>
            </details>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Amount ({currencySymbol(currency)})</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                value={txAmount}
                onChange={(e) => setTxAmount(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm text-foreground"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Date & Time</label>
              <input
                type="datetime-local"
                required
                value={txDate}
                onChange={(e) => setTxDate(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm text-foreground"
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Category</label>
              <PulseSelect
                aria-label="Category"
                value={txCategoryId ?? ""}
                onChange={(e) => setTxCategoryId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full px-3 py-2.5 text-sm text-foreground"
              >
                <option value="">Uncategorized</option>
                <CategoryOptions categories={categories} />
              </PulseSelect>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Link to Budget</label>
              <PulseSelect
                value={txBudgetId ?? ""}
                onChange={(e) => setTxBudgetId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full px-3 py-2.5 text-sm text-foreground"
              >
                <option value="">-- Unlinked --</option>
                {budgets.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </PulseSelect>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-muted-foreground">Link to Subscription</label>
              <PulseSelect
                value={txSubscriptionId ?? ""}
                onChange={(e) => setTxSubscriptionId(e.target.value ? parseInt(e.target.value, 10) : null)}
                className="w-full px-3 py-2.5 text-sm text-foreground"
              >
                <option value="">-- Unlinked --</option>
                {subscriptions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({currencySymbol(currency)}{s.amount})
                  </option>
                ))}
              </PulseSelect>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Notes / Remarks (Optional)</label>
            <input
              type="text"
              placeholder="e.g. Paid via UPI, invoice #1234"
              value={txNotes}
              onChange={(e) => setTxNotes(e.target.value)}
              className="w-full px-3.5 py-2.5 text-sm text-foreground"
            />
          </div>


          {accountEditable && accounts.some(account => !account.archived_at || account.id === accountId) && (
            <label className="block text-sm font-medium">
              Financial account (optional)
              <PulseSelect
                value={accountId ?? ""}
                onChange={event => setAccountId(event.target.value ? Number(event.target.value) : null)}
                disabled={isPending}
                className="mt-1"
              >
                <option value="">No account</option>
                {accounts.filter(account => !account.archived_at || account.id === editingTransaction?.account_id)
                  .map(account => (
                    <option key={account.id} value={account.id} disabled={Boolean(account.archived_at) && account.id !== accountId}>
                      {account.display_name}{account.archived_at ? " · Archived" : ""}
                    </option>
                  ))}
              </PulseSelect>
            </label>
          )}
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
              {isPending ? "Saving…" : editingTransaction ? "Update Transaction" : "Save Transaction"}
            </PulseDialogAction>
          </PulseDialogActions>
        </form>
    </PulseDialog>
  );
}
