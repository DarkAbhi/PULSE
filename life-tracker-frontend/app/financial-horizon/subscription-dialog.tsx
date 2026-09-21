"use client";

import { useState, useEffect } from "react";
import { X, CreditCard, Calendar, AlertCircle } from "lucide-react";
import { SubscriptionItem, BudgetItem, CategoryItem } from "../dashboard/financial-horizon-card";
import Dialog, { DialogAction, DialogActions } from "../components/design-system/dialog";

interface SubscriptionDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: {
    name: string;
    amount: number;
    billing_cycle: string;
    billing_day?: number | null;
    renewal_date?: string | null;
    status: string;
    category_id?: number | null;
    budget_id?: number | null;
    notes?: string | null;
  }) => Promise<void>;
  editingSubscription?: SubscriptionItem | null;
  categories: CategoryItem[];
  budgets: BudgetItem[];
  currency?: string;
}

export default function SubscriptionDialog({
  isOpen,
  onClose,
  onSave,
  editingSubscription,
  categories,
  budgets,
  currency = "₹",
}: SubscriptionDialogProps) {
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [billingCycle, setBillingCycle] = useState<string>("monthly");
  const [billingDay, setBillingDay] = useState<string>("1");
  const [renewalDate, setRenewalDate] = useState<string>(new Date().toISOString().slice(0, 10));
  const [status, setStatus] = useState<string>("active");
  const [categoryId, setCategoryId] = useState<string>("");
  const [budgetId, setBudgetId] = useState<string>("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (editingSubscription) {
      setName(editingSubscription.name);
      setAmount(editingSubscription.amount.toString());
      const cycle = editingSubscription.billing_cycle === "yearly" ? "yearly" : "monthly";
      setBillingCycle(cycle);
      setBillingDay(
        editingSubscription.billing_day
          ? editingSubscription.billing_day.toString()
          : "1"
      );
      setRenewalDate(
        editingSubscription.renewal_date
          ? editingSubscription.renewal_date.slice(0, 10)
          : editingSubscription.next_renewal_date
          ? editingSubscription.next_renewal_date.slice(0, 10)
          : new Date().toISOString().slice(0, 10)
      );
      setStatus(editingSubscription.status || "active");
      setCategoryId(editingSubscription.category_id ? editingSubscription.category_id.toString() : "");
      setBudgetId(editingSubscription.budget_id ? editingSubscription.budget_id.toString() : "");
      setNotes(editingSubscription.notes || "");
    } else {
      setName("");
      setAmount("");
      setBillingCycle("monthly");
      setBillingDay(new Date().getDate().toString());
      setRenewalDate(new Date().toISOString().slice(0, 10));
      setStatus("active");
      
      const defaultSubCategory = categories.find(c => c.name.toLowerCase().includes("subscription"));
      setCategoryId(defaultSubCategory ? defaultSubCategory.id.toString() : "");
      
      setBudgetId("");
      setNotes("");
    }
    setError("");
  }, [editingSubscription, isOpen, categories]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const trimmedName = name.trim();
    if (!trimmedName) {
      setError("Subscription name is required");
      return;
    }

    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount < 0) {
      setError("Please enter a valid non-negative amount");
      return;
    }

    let parsedBillingDay: number | null = null;
    let parsedRenewalDate: string | null = null;

    if (billingCycle === "monthly") {
      const numBillingDay = parseInt(billingDay, 10);
      if (isNaN(numBillingDay) || numBillingDay < 1 || numBillingDay > 31) {
        setError("Billing day of month must be between 1 and 31");
        return;
      }
      parsedBillingDay = numBillingDay;
    } else {
      if (!renewalDate) {
        setError("Annual renewal date is required");
        return;
      }
      parsedRenewalDate = renewalDate;
      const parsedDate = new Date(renewalDate);
      if (!isNaN(parsedDate.getDate())) {
        parsedBillingDay = parsedDate.getDate();
      }
    }

    setIsSubmitting(true);
    try {
      await onSave({
        name: trimmedName,
        amount: numAmount,
        billing_cycle: billingCycle,
        billing_day: parsedBillingDay,
        renewal_date: parsedRenewalDate,
        status,
        category_id: categoryId ? parseInt(categoryId, 10) : null,
        budget_id: budgetId ? parseInt(budgetId, 10) : null,
        notes: notes.trim() ? notes.trim() : null,
      });
      onClose();
    } catch (err: any) {
      setError(err?.message || "Failed to save subscription");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog panelClassName="border-border/80 p-6 sm:p-6 transition-all" size="md">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border/60 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-foreground">
                {editingSubscription ? "Edit Subscription" : "Add Subscription"}
              </h2>
              <p className="text-xs text-muted-foreground">
                Track recurring monthly or annual payments
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-muted-foreground hover:bg-secondary hover:text-foreground transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Error alert */}
        {error && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-destructive/10 p-3 text-sm text-destructive border border-destructive/20">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {/* Subscription Name */}
          <div>
            <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
              Subscription Name *
            </label>
            <input
              type="text"
              placeholder="e.g. Netflix, Spotify, AWS, Gym"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
              required
            />
          </div>

          {/* Amount & Billing Cycle */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
                Amount ({currency}) *
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
                Billing Cycle *
              </label>
              <select
                value={billingCycle}
                onChange={(e) => setBillingCycle(e.target.value)}
                className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
              >
                <option value="monthly">Monthly</option>
                <option value="yearly">Yearly (Annual)</option>
              </select>
            </div>
          </div>

          {/* Conditional Field: Billing Day (Monthly) OR Annual Renewal Date (Yearly) */}
          <div className="grid grid-cols-2 gap-3">
            {billingCycle === "monthly" ? (
              <div>
                <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
                  Billing Day of Month (1 - 31) *
                </label>
                <input
                  type="number"
                  min={1}
                  max={31}
                  placeholder="1 - 31"
                  value={billingDay}
                  onChange={(e) => setBillingDay(e.target.value)}
                  className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
                  required
                />
              </div>
            ) : (
              <div>
                <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
                  Annual Renewal Date *
                </label>
                <div className="relative">
                  <input
                    type="date"
                    value={renewalDate}
                    onChange={(e) => setRenewalDate(e.target.value)}
                    className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
                Status *
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
              >
                <option value="active">Active</option>
                <option value="paused">Paused</option>
                <option value="cancelled">Cancelled</option>
              </select>
            </div>
          </div>

          {/* Category & Budget */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
                Category
              </label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
              >
                <option value="">None (Uncategorized)</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
                Link to Budget
              </label>
              <select
                value={budgetId}
                onChange={(e) => setBudgetId(e.target.value)}
                className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary"
              >
                <option value="">None (No Budget)</option>
                {budgets.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({currency}{b.allocated_amount.toLocaleString()})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Notes */}
          <div>
            <label className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1.5">
              Notes / Renewal Link (Optional)
            </label>
            <textarea
              rows={2}
              placeholder="e.g. Shared plan with family, renewal URL, payment card used"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full rounded-xl border border-border/80 bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary resize-none"
            />
          </div>

          {/* Footer Actions */}
          <DialogActions className="border-t border-border/60 pt-3">
            <DialogAction
              type="button"
              variant="secondary"
              onClick={onClose}
            >
              Cancel
            </DialogAction>
            <DialogAction
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Saving..." : editingSubscription ? "Save Changes" : "Add Subscription"}
            </DialogAction>
          </DialogActions>
        </form>
    </Dialog>
  );
}
