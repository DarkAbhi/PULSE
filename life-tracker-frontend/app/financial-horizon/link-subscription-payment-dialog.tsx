"use client";

import { useEffect, useId, useState } from "react";
import { X } from "lucide-react";
import Button from "../components/design-system/button";
import Dialog from "../components/design-system/dialog";
import type { SubscriptionItem, TransactionItem } from "../dashboard/financial-horizon-card";
import { getTransactionsPageAction } from "./actions";

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
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !saving) onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose, saving]);

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
    <Dialog labelledBy={titleId} size="wide" onBackdropClick={(event) => {
      if (event.target === event.currentTarget && !saving) onClose();
    }}>
      <div className="flex items-center justify-between gap-3">
        <h2 id={titleId} className="text-xl font-bold text-foreground">Log payment for {subscription.name}</h2>
        <Button variant="icon" onClick={onClose} disabled={saving} aria-label="Close"><X className="h-5 w-5" /></Button>
      </div>
      <p className="mt-2 text-sm text-muted-foreground">Select an existing debit transaction, including one imported from a statement.</p>
      <label className="mt-5 block text-xs font-semibold text-muted-foreground" htmlFor="payment-transaction-search">Search transactions</label>
      <input
        id="payment-transaction-search"
        type="search"
        value={search}
        onChange={(event) => { setSearch(event.target.value); setPage(1); setSelectedId(null); }}
        placeholder="Search by name or notes"
        className="mt-1 w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
      />
      {error && <p className="mt-3 text-sm text-destructive" role="alert">{error}</p>}
      <div className="mt-4 max-h-72 space-y-2 overflow-y-auto" aria-busy={loading}>
        {loading ? <p className="text-sm text-muted-foreground">Loading transactions…</p> : transactions.length === 0 ? (
          <p className="text-sm text-muted-foreground">{transactionCount === 0 ? "No transactions yet. Create one to log this payment." : "No matching transactions found."}</p>
        ) : transactions.map((transaction) => (
          <label key={transaction.id} className={`flex items-center gap-3 rounded-xl border p-3 text-sm ${transaction.subscription_id ? "cursor-not-allowed opacity-50" : "cursor-pointer hover:border-primary"}`}>
            <input
              type="radio"
              name="payment-transaction"
              value={transaction.id}
              checked={selectedId === transaction.id}
              disabled={!!transaction.subscription_id || saving}
              onChange={() => setSelectedId(transaction.id)}
            />
            <span className="min-w-0 flex-1 truncate">{transaction.name}<span className="block text-xs text-muted-foreground">{new Date(transaction.transaction_date).toLocaleDateString()} · {transaction.category_name}{transaction.subscription_id ? " · Already linked" : ""}</span></span>
            <span className="font-semibold">{currency}{transaction.amount.toLocaleString()}</span>
          </label>
        ))}
      </div>
      {totalPages > 1 && <div className="mt-3 flex items-center justify-between text-sm">
        <Button variant="secondary" size="sm" disabled={page === 1 || loading} onClick={() => { setPage(page - 1); setSelectedId(null); }}>Previous</Button>
        <span>Page {page} of {totalPages}</span>
        <Button variant="secondary" size="sm" disabled={page === totalPages || loading} onClick={() => { setPage(page + 1); setSelectedId(null); }}>Next</Button>
      </div>}
      <div className="mt-5 flex flex-wrap justify-between gap-3 border-t border-border pt-4">
        <Button variant="secondary" onClick={onCreateNew} disabled={saving}>Create new transaction</Button>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button onClick={link} disabled={!selected || saving}>{saving ? "Linking…" : "Link payment"}</Button>
        </div>
      </div>
    </Dialog>
  );
}
