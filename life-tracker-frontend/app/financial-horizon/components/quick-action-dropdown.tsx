"use client";

import { Icon, type IconName } from "@/app/financial-horizon/components/horizon-icon";
import HorizonAction from "@/app/financial-horizon/components/horizon-action";
import PulseButton from "@/app/components/design-system/pulse-button";

import { useState, useRef, useEffect, useId } from "react";

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
  const panelId = useId();
  const trigger = useRef<HTMLButtonElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    if (!isOpen) return;
    document.addEventListener("pointerdown", handleClickOutside);
    return () => document.removeEventListener("pointerdown", handleClickOutside);
  }, [isOpen]);

  const handleAction = (actionFn: () => void) => {
    trigger.current?.focus();
    setIsOpen(false);
    actionFn();
  };

  return (
    <div className="relative inline-block text-left" ref={containerRef} onKeyDown={event => {
      if (isOpen && event.key === "Escape") {
        event.preventDefault();
        setIsOpen(false);
        trigger.current?.focus();
      }
    }}>
      <HorizonAction
        ref={trigger}
        onClick={() => setIsOpen((prev) => !prev)}
        disabled={isPending}
        aria-expanded={isOpen}
        aria-controls={isOpen ? panelId : undefined}
      >
        <Icon name="plus" strokeWidth={2.5} />
        <span>Quick Action</span>
        <Icon name="chevronDown" className={`size-5 transition-transform duration-200 motion-reduce:transition-none ${isOpen ? "rotate-180" : ""}`} />
      </HorizonAction>

      {isOpen && (
        <div id={panelId} className="absolute left-0 z-50 mt-2 w-64 rounded-card bg-popover p-2 shadow-card sm:right-0 sm:left-auto">
          <div className="space-y-1">
            <QuickActionItem icon="receipt" tone="success" label="Log Transaction" description="Record daily expense/income" onClick={() => handleAction(onLogTransaction)} />
            <QuickActionItem icon="calendar" tone="info" label="Add Fixed Obligation" description="Rent, EMI, SIP, subscriptions" onClick={() => handleAction(onAddFixedObligation)} />
            <QuickActionItem icon="bag" tone="warning" label="Add Planned Purchase" description="Next month target items" onClick={() => handleAction(onAddPlannedPurchase)} />
            <div aria-hidden="true" className="my-1 border-t border-divider" />
            <QuickActionItem icon="target" tone="lavender" label="Add Monthly Budget" description="Allocate category target" onClick={() => handleAction(onAddBudget)} />
            <QuickActionItem icon="tag" tone="muted" label="Add Category Tag" description="Create custom tx category" onClick={() => handleAction(onAddCategory)} />
          </div>
        </div>
      )}
    </div>
  );
}


const actionIconTones = {
  success: "bg-success/10 text-success",
  info: "bg-info/10 text-info",
  warning: "bg-warning/10 text-warning",
  lavender: "bg-lavender/10 text-foreground",
  muted: "bg-muted/10 text-muted-foreground",
};

function QuickActionItem({
  icon,
  tone,
  label,
  description,
  onClick,
}: {
  icon: IconName;
  tone: keyof typeof actionIconTones;
  label: string;
  description: string;
  onClick: () => void;
}) {
  return (
    <PulseButton type="button" variant="menuItem" onClick={onClick}>
      <span className={`grid size-8 shrink-0 place-items-center rounded-lg ${actionIconTones[tone]}`}>
        <Icon name={icon} className="size-4" />
      </span>
      <span className="grid text-start">
        <span className="font-semibold leading-tight">{label}</span>
        <span className="text-caption text-muted-foreground">{description}</span>
      </span>
    </PulseButton>
  );
}
