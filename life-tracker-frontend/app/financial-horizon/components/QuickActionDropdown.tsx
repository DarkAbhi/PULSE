"use client";

import { Icon } from "./HorizonIcon";
import Button from "@/app/components/design-system/button";

import { useState, useRef, useEffect } from "react";
import { Tag } from "lucide-react";
import theme from "../horizon-theme.module.css";

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
        className={theme.primary}
        onClick={() => setIsOpen((prev) => !prev)}
        disabled={isPending}
        aria-expanded={isOpen}
        aria-haspopup="true"
      >
        <Icon name="plus" className="h-4 w-4 stroke-[2.5]" />
        <span>Quick Action</span>
        <Icon name="chevronDown" className={`h-4 w-4 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
      </Button>

      {isOpen && (
        <div className="absolute left-0 z-50 mt-2 w-64 rounded-[28px] bg-popover p-2 shadow-(--horizon-shadow) sm:right-0 sm:left-auto">
          <div className="space-y-1">
            <Button variant="menuItem"
              onClick={() => handleAction(onLogTransaction)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-success/10 text-success">
                <Icon name="receipt" className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Log Transaction</div>
                <div className="text-[11px] text-muted-foreground">Record daily expense/income</div>
              </div>
            </Button>

            <Button variant="menuItem"
              onClick={() => handleAction(onAddFixedObligation)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-info/10 text-info">
                <Icon name="calendar" className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Add Fixed Obligation</div>
                <div className="text-[11px] text-muted-foreground">Rent, EMI, SIP, subscriptions</div>
              </div>
            </Button>

            <Button variant="menuItem"
              onClick={() => handleAction(onAddPlannedPurchase)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-warning/10 text-warning">
                <Icon name="bag" className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Add Planned Purchase</div>
                <div className="text-[11px] text-muted-foreground">Next month target items</div>
              </div>
            </Button>

            <div className="my-1 border-t border-divider" />

            <Button variant="menuItem"
              onClick={() => handleAction(onAddBudget)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-lavender/10 text-foreground">
                <Icon name="target" className="h-4 w-4" />
              </div>
              <div className="text-left">
                <div className="font-semibold leading-tight">Add Monthly Budget</div>
                <div className="text-[11px] text-muted-foreground">Allocate category target</div>
              </div>
            </Button>

            <Button variant="menuItem"
              onClick={() => handleAction(onAddCategory)}
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-muted/10 text-muted-foreground">
                <Icon name="tag" className="h-4 w-4" />
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
