"use client";

import Button from "../../components/design-system/button";

import { useState, useRef, useEffect } from "react";
import { Plus, ChevronDown, Receipt, Calendar, ShoppingBag, Target, Tag } from "lucide-react";

interface QuickActionDropdownProps {
  onLogTransaction: () => void;
  onAddFixedObligation: () => void;
  onAddPlannedPurchase: () => void;
  onAddBudget: () => void;
  onAddCategory: () => void;
  isPending?: boolean;
}

export default function QuickActionDropdown({
  onLogTransaction,
  onAddFixedObligation,
  onAddPlannedPurchase,
  onAddBudget,
  onAddCategory,
  isPending = false,
}: QuickActionDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("keydown", handleKeyDown);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  const handleAction = (actionFn: () => void) => {
    setIsOpen(false);
    actionFn();
  };

  return (
    <div className="relative inline-block text-left" ref={containerRef}>
      <Button variant="primary" size="md"
        onClick={() => setIsOpen((prev) => !prev)}
        disabled={isPending}
        aria-expanded={isOpen}
        aria-haspopup="true"
      >
        <Plus className="h-4 w-4 stroke-[2.5]" />
        <span>Quick Action</span>
        <ChevronDown className={`h-4 w-4 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
      </Button>

      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-64 origin-top-right rounded-2xl border border-border bg-card p-2 shadow-xl ring-1 ring-black/5 animate-in fade-in-50 zoom-in-95">
          <div className="px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
            Financial Actions
          </div>

          <div className="space-y-1">
            <Button variant="menuItem"
              onClick={() => handleAction(onLogTransaction)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                <Receipt className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Log Transaction</div>
                <div className="text-[11px] text-muted-foreground">Record daily expense/income</div>
              </div>
            </Button>

            <Button variant="menuItem"
              onClick={() => handleAction(onAddFixedObligation)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-500/10 text-blue-600 dark:text-blue-400">
                <Calendar className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Add Fixed Obligation</div>
                <div className="text-[11px] text-muted-foreground">Rent, EMI, SIP, subscriptions</div>
              </div>
            </Button>

            <Button variant="menuItem"
              onClick={() => handleAction(onAddPlannedPurchase)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400">
                <ShoppingBag className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Add Planned Purchase</div>
                <div className="text-[11px] text-muted-foreground">Next month target items</div>
              </div>
            </Button>

            <div className="my-1 border-t border-border/60" />

            <Button variant="menuItem"
              onClick={() => handleAction(onAddBudget)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-purple-500/10 text-purple-600 dark:text-purple-400">
                <Target className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Add Monthly Budget</div>
                <div className="text-[11px] text-muted-foreground">Allocate category target</div>
              </div>
            </Button>

            <Button variant="menuItem"
              onClick={() => handleAction(onAddCategory)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-500/10 text-slate-600 dark:text-slate-400">
                <Tag className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Add Category Tag</div>
                <div className="text-[11px] text-muted-foreground">Create custom tx category</div>
              </div>
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
