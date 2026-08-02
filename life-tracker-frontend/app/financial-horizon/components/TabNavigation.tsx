"use client";

import { LayoutDashboard, Receipt, Calendar, CreditCard, ShoppingBag } from "lucide-react";

export type HorizonTab = "overview" | "transactions" | "subscriptions" | "fixed" | "planner";

interface TabNavigationProps {
  activeTab: HorizonTab;
  onTabChange: (tab: HorizonTab) => void;
  transactionsCount: number;
  subscriptionsCount?: number;
  fixedObligationsCount: number;
  plannedPurchasesCount: number;
}

export default function TabNavigation({
  activeTab,
  onTabChange,
  transactionsCount,
  subscriptionsCount = 0,
  fixedObligationsCount,
  plannedPurchasesCount,
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
      count: transactionsCount,
    },
    {
      id: "subscriptions" as HorizonTab,
      label: "Subscriptions",
      icon: CreditCard,
      count: subscriptionsCount,
    },
    {
      id: "fixed" as HorizonTab,
      label: "Fixed Obligations",
      icon: Calendar,
      count: fixedObligationsCount,
    },
    {
      id: "planner" as HorizonTab,
      label: "Planner & Future Horizon",
      icon: ShoppingBag,
      count: plannedPurchasesCount,
    },
  ];

  return (
    <div className="border-b border-border/80 pb-px">
      <nav className="flex gap-2 overflow-x-auto pb-1 scrollbar-none" aria-label="Horizon navigation tabs">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              className={`group flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition duration-150 ${
                isActive
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "bg-card text-muted-foreground border border-border/60 hover:border-border hover:bg-secondary/70 hover:text-foreground"
              }`}
            >
              <Icon className={`h-4 w-4 transition-transform group-hover:scale-110 ${isActive ? "text-primary-foreground" : "text-muted-foreground"}`} />
              <span>{tab.label}</span>
              {typeof tab.count === "number" && (
                <span
                  className={`ml-1 rounded-full px-2 py-0.5 text-xs font-bold transition ${
                    isActive
                      ? "bg-primary-foreground/20 text-primary-foreground"
                      : "bg-secondary text-secondary-foreground"
                  }`}
                >
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </nav>
    </div>
  );
}
