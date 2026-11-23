"use client";

import { useEffect, useRef } from "react";
import { Icon, type IconName } from "@/app/financial-horizon/components/HorizonIcon";

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

export default function TabNavigation({
  activeTab,
  onTabChange,
}: TabNavigationProps) {
  const navigationRef = useRef<HTMLElement>(null);
  const activeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const navigation = navigationRef.current;
    const button = activeButtonRef.current;
    if (!navigation || !button) return;

    function revealActiveTab() {
      if (!navigation || !button || navigation.scrollHeight <= navigation.clientHeight) return;

      const container = navigation.getBoundingClientRect();
      const active = button.getBoundingClientRect();
      const padding = parseFloat(getComputedStyle(navigation).paddingBlockStart);
      const above = active.top - container.top - padding;
      const below = active.bottom - container.bottom + padding;
      if (above < 0) navigation.scrollTop += above;
      else if (below > 0) navigation.scrollTop += below;
    }

    const observer = new ResizeObserver(revealActiveTab);
    observer.observe(navigation);
    return () => observer.disconnect();
  }, [activeTab]);

  return (
    <nav
      ref={navigationRef}
      aria-label="Financial Horizon sections"
      className="grid grid-cols-1 gap-1 rounded-card bg-card p-2 shadow-card max-sm:max-h-46 max-sm:overflow-y-auto max-sm:scroll-py-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-7"
    >
      {tabs.map(({ id, label, icon }) => {
        const isActive = activeTab === id;

        return (
          <button
            key={id}
            ref={isActive ? activeButtonRef : undefined}
            type="button"
            aria-current={isActive ? "true" : undefined}
            onClick={() => onTabChange(id)}
            className={`flex min-h-11 min-w-0 w-full items-center gap-2 rounded-full px-3 py-2 text-start text-control font-bold wrap-anywhere xl:justify-center xl:text-center ${isActive
              ? "[background:var(--primary-fill)] text-primary-foreground forced-colors:outline-1 forced-colors:outline-[ButtonText]"
              : "text-muted-foreground hover:bg-chip hover:text-foreground"
            }`}
          >
            <Icon name={icon} className="hidden size-5 min-[360px]:block" />
            <span>{label}</span>
          </button>
        );
      })}
    </nav>
  );
}
