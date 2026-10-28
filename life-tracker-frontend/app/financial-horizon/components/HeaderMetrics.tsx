"use client";

import Button from "../../components/design-system/button";

import { Edit2, Sparkles, Wallet, Lock } from "lucide-react";
import { HorizonSummary } from "../../dashboard/financial-horizon-card";

interface HeaderMetricsProps {
  summary: HorizonSummary;
  uncommittedPool: number;
  totalCommitted: number;
  totalCommittedRatio: number;
  isEditingBase: boolean;
  setIsEditingBase: (val: boolean) => void;
  baseInput: string;
  setBaseInput: (val: string) => void;
  currencyInput: string;
  setCurrencyInput: (val: string) => void;
  handleSaveBaseConfig: (e: React.FormEvent) => void;
  isPending?: boolean;
}

export default function HeaderMetrics({
  summary,
  uncommittedPool,
  totalCommitted,
  totalCommittedRatio,
  isEditingBase,
  setIsEditingBase,
  baseInput,
  setBaseInput,
  currencyInput,
  setCurrencyInput,
  handleSaveBaseConfig,
  isPending = false,
}: HeaderMetricsProps) {
  return (
    <section className="grid gap-5 sm:grid-cols-3">
      {/* Card 1: Starting Pool (Base Income) */}
      <div className="relative overflow-hidden rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-action">
              <Wallet className="h-4 w-4" />
            </div>
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Starting Pool (Base Income)
            </span>
          </div>
          <Button variant="icon"
            onClick={() => setIsEditingBase(!isEditingBase)}
            title="Edit Base Income"
          >
            <Edit2 className="h-4 w-4" />
          </Button>
        </div>

        {isEditingBase ? (
          <form onSubmit={handleSaveBaseConfig} className="mt-3 space-y-2">
            <div className="flex gap-2">
              <input
                type="text"
                value={currencyInput}
                onChange={(e) => setCurrencyInput(e.target.value)}
                className="w-12 rounded-xl border border-border bg-background px-2 py-1 text-center text-xs font-bold"
                placeholder="₹"
              />
              <input
                type="number"
                step="0.01"
                min="0"
                value={baseInput}
                onChange={(e) => setBaseInput(e.target.value)}
                className="w-full rounded-xl border border-border bg-background px-3 py-1 text-sm font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="100000"
                autoFocus
              />
            </div>
            <div className="flex gap-2 justify-end">
              <Button variant="secondary" size="sm"
                type="button"
                onClick={() => setIsEditingBase(false)}
              >
                Cancel
              </Button>
              <Button variant="primary" size="sm"
                type="submit"
                disabled={isPending}
              >
                Save
              </Button>
            </div>
          </form>
        ) : (
          <div className="mt-3">
            <p className="text-2xl font-extrabold text-foreground tracking-tight">
              {summary.currency}
              {summary.base_amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">Baseline monthly revenue stream</p>
          </div>
        )}
      </div>

      {/* Card 2: Total Committed */}
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-warning/10 text-warning">
              <Lock className="h-4 w-4" />
            </div>
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Total Committed
            </span>
          </div>
          <span className="rounded-full bg-warning/10 border border-warning/20 px-2.5 py-0.5 text-xs font-bold text-warning">
            {totalCommittedRatio.toFixed(1)}% of pool
          </span>
        </div>
        <div className="mt-3">
          <p className="text-2xl font-extrabold text-foreground tracking-tight">
            {summary.currency}
            {totalCommitted.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
          </p>
          <p className="mt-1 text-xs text-muted-foreground flex items-center gap-2">
            <span>Fixed ({summary.currency}{summary.total_deductions.toLocaleString("en-IN")})</span>
            {(summary.total_subscription_burn ?? 0) > 0 && (
              <>
                <span>+</span>
                <span>Subscriptions ({summary.currency}{(summary.total_subscription_burn ?? 0).toLocaleString("en-IN")})</span>
              </>
            )}
          </p>
        </div>
      </div>

      {/* Card 3: Net Available Pool (Primary visual focus) */}
      <div className="rounded-2xl border border-success/30 bg-gradient-to-br from-card via-card to-success/10 p-5 shadow-md relative overflow-hidden">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-success/20 text-success">
              <Sparkles className="h-4.5 w-4.5" />
            </div>
            <span className="text-xs font-bold uppercase tracking-wider text-success">
              Net Available Pool
            </span>
          </div>
          <span
            className={`rounded-full px-2.5 py-0.5 text-xs font-bold border ${
              uncommittedPool >= 0
                ? "bg-success/10 border-success/30 text-success"
                : "bg-destructive/10 border-destructive/30 text-destructive"
            }`}
          >
            {uncommittedPool >= 0 ? "Surplus" : "Deficit"}
          </span>
        </div>
        <div className="mt-3">
          <p
            className={`text-2xl font-extrabold tracking-tight ${
              uncommittedPool >= 0
                ? "text-success dark:text-success"
                : "text-destructive dark:text-destructive"
            }`}
          >
            {summary.currency}
            {uncommittedPool.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
          </p>
          <p className="mt-1 text-xs text-muted-foreground font-medium">
            Uncommitted cash remaining after fixed obligations &amp; subscriptions
          </p>
        </div>
      </div>
    </section>
  );
}
