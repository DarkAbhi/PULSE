"use client";

import { formatMoney } from "@/app/lib/currency.mjs";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/profile-preferences-provider";

import { useEffect, useId, useState } from "react";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import type { SubscriptionItem, TransactionItem } from "@/app/financial-horizon/horizon-types";
import { getTransactionsPageAction } from "@/app/financial-horizon/actions";

export default function LinkSubscriptionPaymentDialog({
  subscription,
  currency,
  transactionCount,
  onClose,
  onCreateNew,
  onLink,
}: {
  subscription: SubscriptionItem;
  currency: string;
  transactionCount: number;
  onClose: () => void;
  onCreateNew: () => void;
  onLink: (transaction: TransactionItem) => Promise<void>;
}) {
  const { timeZone } = useTimeZone();
  const titleId = useId();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(transactionCount > 0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (transactionCount === 0) {
      setLoading(false);
      return;
    }
    let current = true;
    const timer = setTimeout(async () => {
      setLoading(true);
      let result: Awaited<ReturnType<typeof getTransactionsPageAction>>;
      try {
        result = await getTransactionsPageAction(page, 20, "debit", search);
      } catch {
        if (current) {
          setError("Unable to load transactions. Please try again.");
          setLoading(false);
        }
        return;
      }
      if (!current) return;
      if (result.ok) {
        setTransactions(result.data.transactions as TransactionItem[]);
        setTotalPages(result.data.total_pages);
        setError("");
      } else {
        setError(result.error);
      }
      setLoading(false);
    }, search ? 250 : 0);
    return () => { current = false; clearTimeout(timer); };
  }, [page, search, transactionCount]);

  const selected = transactions.find((transaction) => transaction.id === selectedId);

  const link = async () => {
    if (!selected) return;
    setSaving(true);
    setError("");
    try {
      await onLink(selected);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Failed to link transaction.");
      setSaving(false);
    }
  };

  return (
    <PulseDialog labelledBy={titleId} size="wide" onDismiss={saving ? undefined : onClose}>
      <PulseDialogHeader
        id={titleId}
        description="Select an existing debit transaction, including one imported from a statement."
        onClose={onClose}
        closeDisabled={saving}
      >
        Log payment for {subscription.name}
      </PulseDialogHeader>
      <label className="mt-5 block text-xs font-semibold text-muted-foreground" htmlFor="payment-transaction-search">Search transactions</label>
      <input
        id="payment-transaction-search"
        type="search"
        value={search}
        onChange={(event) => { setSearch(event.target.value); setPage(1); setSelectedId(null); }}
        placeholder="Search by name or notes"
        className="mt-1 w-full px-3.5 py-2.5 text-sm text-foreground"
      />
      {error && <p className="mt-3 text-sm text-destructive" role="alert">{error}</p>}
      <div className="mt-4 max-h-72 space-y-2 overflow-y-auto" aria-busy={loading}>
        {loading ? <p className="text-sm text-muted-foreground">Loading transactions…</p> : transactions.length === 0 ? (
          <p className="text-sm text-muted-foreground">{transactionCount === 0 ? "No transactions yet. Create one to log this payment." : "No matching transactions found."}</p>
        ) : transactions.map((transaction) => (
          <label key={transaction.id} className={`flex items-center gap-3 rounded-xl bg-secondary/50 p-3 text-sm has-checked:bg-accent ${transaction.subscription_id ? "cursor-not-allowed opacity-50" : "cursor-pointer hover:bg-secondary"}`}>
            <input
              type="radio"
              name="payment-transaction"
              value={transaction.id}
              checked={selectedId === transaction.id}
              disabled={!!transaction.subscription_id || saving}
              onChange={() => setSelectedId(transaction.id)}
            />
            <span className="min-w-0 flex-1 truncate">{transaction.name}<span className="block text-xs text-muted-foreground">{formatInstant(transaction.transaction_date, timeZone, { dateStyle: "medium" })} · {transaction.category_name}{transaction.subscription_id ? " · Already linked" : ""}</span></span>
            <span className="font-semibold">{formatMoney(transaction.amount, currency)}</span>
          </label>
        ))}
      </div>
      {totalPages > 1 && <div className="mt-3 flex items-center justify-between text-sm">
        <PulseButton variant="secondary" size="sm" disabled={page === 1 || loading} onClick={() => { setPage(page - 1); setSelectedId(null); }}>Previous</PulseButton>
        <span>Page {page} of {totalPages}</span>
        <PulseButton variant="secondary" size="sm" disabled={page === totalPages || loading} onClick={() => { setPage(page + 1); setSelectedId(null); }}>Next</PulseButton>
      </div>}
      <PulseDialogActions className="sm:grid-cols-[1fr_auto_auto]">
        <PulseDialogAction type="button" variant="secondary" onClick={onCreateNew} disabled={saving}>Create new transaction</PulseDialogAction>
        <PulseDialogAction type="button" variant="secondary" onClick={onClose} disabled={saving}>Cancel</PulseDialogAction>
        <PulseDialogAction type="button" onClick={link} disabled={!selected || saving}>{saving ? "Linking…" : "Link payment"}</PulseDialogAction>
      </PulseDialogActions>
    </PulseDialog>
  );
}
