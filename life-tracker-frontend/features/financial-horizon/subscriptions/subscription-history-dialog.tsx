"use client";

import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/components/ui/pulse-dialog";
import type { SubscriptionItem, TransactionItem } from "@/features/financial-horizon/types/horizon-types";
import { formatMoney } from "@/lib/currency/currency.mjs";
import { formatInstant } from "@/lib/time-zone/time-zone.mjs";
import { useTimeZone } from "@/providers/profile-preferences-provider";

export default function SubscriptionHistoryDialog({ subscription, transactions, isLoading, currency, onClose }: {
  subscription: SubscriptionItem;
  transactions: TransactionItem[];
  isLoading: boolean;
  currency: string;
  onClose: () => void;
}) {
  const { timeZone } = useTimeZone();

  return (
    <PulseDialog size="md" labelledBy="subscription-payment-history-title" onDismiss={onClose}>
        <PulseDialogHeader
          id="subscription-payment-history-title"
          description={`${transactions.length} recorded payments • Total spent: ${formatMoney(transactions.reduce((sum, t) => sum + t.amount, 0), currency)}`}
        >
          {subscription.name} Payment History
        </PulseDialogHeader>

        <div className="mt-4 max-h-96 overflow-y-auto space-y-2 pr-1">
          {isLoading ? (
            <div className="py-8 text-center text-xs text-muted-foreground">
              Loading linked transactions...
            </div>
          ) : transactions.length === 0 ? (
            <div className="py-8 text-center text-xs text-muted-foreground">
              No linked transactions recorded for this subscription yet.
            </div>
          ) : (
            transactions.map((tx) => (
              <div
                key={tx.id}
                className="flex items-center justify-between rounded-xl bg-background p-3 text-sm"
              >
                <div>
                  <span className="font-semibold text-foreground">{tx.name}</span>
                  <span className="block text-xs text-muted-foreground">
                    {formatInstant(tx.transaction_date, timeZone, {
                      year: "numeric",
                      month: "short",
                      day: "numeric",
                    })}
                  </span>
                </div>
                <span className="font-bold text-foreground">
                  {formatMoney(tx.amount, currency)}
                </span>
              </div>
            ))
          )}
        </div>

        <PulseDialogActions>
          <PulseDialogAction
            type="button"
            variant="secondary"
            onClick={onClose}
          >
            Close
          </PulseDialogAction>
        </PulseDialogActions>
    </PulseDialog>
  );
}
