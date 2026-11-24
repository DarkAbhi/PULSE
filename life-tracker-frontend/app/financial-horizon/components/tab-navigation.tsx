"use client";

import PulseTabs from "@/app/components/design-system/pulse-tabs";
import { Icon, type IconName } from "@/app/financial-horizon/components/horizon-icon";

export type HorizonTab = "overview" | "budgets" | "transactions" | "subscriptions" | "fixed" | "planner" | "accounts";

interface TabNavigationProps {
  activeTab: HorizonTab;
  onTabChange: (tab: HorizonTab) => void;
}

const tabs: { id: HorizonTab; label: string; icon: IconName }[] = [
  {
    id: "overview",
    label: "Overview",
    icon: "grid",
  },
  {
    id: "budgets",
    label: "Budgets",
    icon: "target",
  },
  {
    id: "transactions",
    label: "Transactions",
    icon: "receipt",
  },
  {
    id: "subscriptions",
    label: "Subscriptions",
    icon: "card",
  },
  {
    id: "fixed",
    label: "Obligations",
    icon: "calendar",
  },
  {
    id: "planner",
    label: "Planner",
    icon: "bag",
  },
  { id: "accounts", label: "Accounts", icon: "bank" },
];

const items = tabs.map(({ id, label, icon }) => ({ value: id, label, icon: <Icon name={icon} /> }));

export function horizonTab(value: string | null): HorizonTab {
  return tabs.find(tab => tab.id === value)?.id ?? "overview";
}

export default function TabNavigation({
  activeTab,
  onTabChange,
}: TabNavigationProps) {
  return (
    <PulseTabs
      label="Financial Horizon sections"
      items={items}
      value={activeTab}
      onChange={onTabChange}
      className="sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-7"
    />
  );
}
