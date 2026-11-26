"use client";

import { currencySymbol } from "@/lib/currency/currency.mjs";


import { useState, useEffect, useId } from "react";
import { ShoppingBag } from "lucide-react";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/components/ui/pulse-dialog";

export interface PlannedPurchaseDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: { name: string; price: number; url: string | null }) => Promise<void> | void;
  currency: string;
  isPending?: boolean;
}

export default function PlannedPurchaseDialog({
  isOpen,
  onClose,
  onSave,
  currency,
  isPending = false,
}: PlannedPurchaseDialogProps) {
  const titleId = useId();

  const [purchaseName, setPurchaseName] = useState("");
  const [purchasePrice, setPurchasePrice] = useState("");
  const [purchaseURL, setPurchaseURL] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    if (!isOpen) return;
    setPurchaseName("");
    setPurchasePrice("");
    setPurchaseURL("");
    setErrorMsg("");
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");

    const priceNum = parseFloat(purchasePrice);
    if (!purchaseName.trim()) {
      setErrorMsg("Item name is required.");
      return;
    }
    if (isNaN(priceNum) || priceNum < 0) {
      setErrorMsg("Please enter a valid non-negative price.");
      return;
    }

    try {
      await onSave({
        name: purchaseName.trim(),
        price: priceNum,
        url: purchaseURL.trim() || null,
      });
      onClose();
    } catch {
      setErrorMsg("Failed to save planned purchase.");
    }
  };

  return (
    <PulseDialog
      labelledBy={titleId}
      onDismiss={isPending ? undefined : onClose}
    >
        <PulseDialogHeader id={titleId} icon={<ShoppingBag className="size-5" />}>
          Add Planned Purchase
        </PulseDialogHeader>

        {errorMsg && (
          <div className="mt-4 rounded-xl bg-destructive/10 p-3 text-xs text-destructive">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Item Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Noise Cancelling Headphones"
              value={purchaseName}
              onChange={(e) => setPurchaseName(e.target.value)}
              className="w-full px-3.5 py-2 text-sm text-foreground"
              autoFocus
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Price ({currencySymbol(currency)}) *</label>
            <input
              type="number"
              step="0.01"
              min="0"
              required
              placeholder="0.00"
              value={purchasePrice}
              onChange={(e) => setPurchasePrice(e.target.value)}
              className="w-full px-3.5 py-2 text-sm text-foreground"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Product URL (Optional)</label>
            <input
              type="url"
              placeholder="https://..."
              value={purchaseURL}
              onChange={(e) => setPurchaseURL(e.target.value)}
              className="w-full px-3.5 py-2 text-sm text-foreground"
            />
          </div>

          <PulseDialogActions>
            <PulseDialogAction
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={isPending}
            >
              Cancel
            </PulseDialogAction>
            <PulseDialogAction
              type="submit"
              disabled={isPending}
            >
              {isPending ? "Adding…" : "Add Purchase"}
            </PulseDialogAction>
          </PulseDialogActions>
        </form>
    </PulseDialog>
  );
}
