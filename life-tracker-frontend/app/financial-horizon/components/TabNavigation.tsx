"use client";

import Button from "@/app/components/design-system/button";

import { LayoutDashboard, Receipt, Calendar, CreditCard, ShoppingBag } from "lucide-react";

export type HorizonTab = "overview" | "transactions" | "subscriptions" | "fixed" | "planner";

interface TabNavigationProps {
  activeTab: HorizonTab;
  onTabChange: (tab: HorizonTab) => void;
}

export default function TabNavigation({
  activeTab,
  onTabChange,
}: TabNavigationProps) {
  const tabs = [
    {
      id: "overview" as HorizonTab,
      label: "Overview",
      icon: LayoutDashboard,
    },
    {
      id: "transactions" as HorizonTab,
      label: "Transactions",
      icon: Receipt,
    },
    {
      id: "subscriptions" as HorizonTab,
      label: "Subscriptions",
      icon: CreditCard,
    },
    {
      id: "fixed" as HorizonTab,
      label: "Fixed Obligations",
      icon: Calendar,
    },
    {
      id: "planner" as HorizonTab,
      label: "Planner & Future Horizon",
      icon: ShoppingBag,
    },
  ];

  return (
    <nav
      aria-label="Financial Horizon sections"
      className="grid grid-cols-2 gap-1 rounded-2xl bg-card p-1.5 lg:flex"
    >
      {tabs.map(({ id, label, icon: Icon }) => {
        const isActive = activeTab === id;

        return (
          <Button
            key={id}
            variant={isActive ? "primary" : "menuItem"}
            size="lg"
            aria-current={isActive ? "true" : undefined}
            onClick={() => onTabChange(id)}
            className="min-w-0 flex-auto text-left last:col-span-2 lg:w-auto"
          >
            <span className="flex w-full items-center gap-2 lg:justify-center">
              <Icon aria-hidden="true" className="hidden h-4 w-4 shrink-0 min-[400px]:block" />
              {label}
            </span>
          </Button>
        );
      })}
    </nav>
  );
}
