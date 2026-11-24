"use client";

import { Icon, type IconName } from "@/app/financial-horizon/components/horizon-icon";
import HorizonAction from "@/app/financial-horizon/components/horizon-action";
import PulseDropdown, { PulseDropdownItem, PulseDropdownSeparator } from "@/app/components/design-system/pulse-dropdown";

interface QuickActionDropdownProps {
  onLogTransaction: () => void;
  onAddFixedObligation: () => void;
  onAddPlannedPurchase: () => void;
  onAddBudget: () => void;
  onAddCategory: () => void;
  isPending?: boolean;
}

const actionIconTones = {
  success: "bg-success/10 text-success",
  info: "bg-info/10 text-info",
  warning: "bg-warning/10 text-warning",
  lavender: "bg-lavender/10 text-foreground",
  muted: "bg-muted/10 text-muted-foreground",
};

function ActionIcon({ name, tone }: { name: IconName; tone: keyof typeof actionIconTones }) {
  return (
    <span className={`grid size-8 shrink-0 place-items-center rounded-lg ${actionIconTones[tone]}`}>
      <Icon name={name} className="size-4" />
    </span>
  );
}

export default function QuickActionDropdown({
  onLogTransaction,
  onAddFixedObligation,
  onAddPlannedPurchase,
  onAddBudget,
  onAddCategory,
  isPending = false,
}: QuickActionDropdownProps) {
  return (
    <PulseDropdown
      trigger={({ isOpen, ...triggerProps }) => (
        <HorizonAction {...triggerProps} disabled={isPending}>
          <Icon name="plus" strokeWidth={2.5} />
          <span>Quick Action</span>
          <Icon name="chevronDown" className={`size-5 transition-transform duration-200 motion-reduce:transition-none ${isOpen ? "rotate-180" : ""}`} />
        </HorizonAction>
      )}
    >
      <PulseDropdownItem icon={<ActionIcon name="receipt" tone="success" />} description="Record daily expense/income" onSelect={onLogTransaction}>
        Log Transaction
      </PulseDropdownItem>
      <PulseDropdownItem icon={<ActionIcon name="calendar" tone="info" />} description="Rent, EMI, SIP, subscriptions" onSelect={onAddFixedObligation}>
        Add Fixed Obligation
      </PulseDropdownItem>
      <PulseDropdownItem icon={<ActionIcon name="bag" tone="warning" />} description="Next month target items" onSelect={onAddPlannedPurchase}>
        Add Planned Purchase
      </PulseDropdownItem>
      <PulseDropdownSeparator />
      <PulseDropdownItem icon={<ActionIcon name="target" tone="lavender" />} description="Allocate category target" onSelect={onAddBudget}>
        Add Monthly Budget
      </PulseDropdownItem>
      <PulseDropdownItem icon={<ActionIcon name="tag" tone="muted" />} description="Create custom tx category" onSelect={onAddCategory}>
        Add Category Tag
      </PulseDropdownItem>
    </PulseDropdown>
  );
}
