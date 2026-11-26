"use client";

import { useEffect, useRef, type ReactNode, type RefObject } from "react";

/**
 * - `primary`: large page-level section navigation (card container, filled active pill, optional icons).
 * - `segmented`: compact in-content switcher for filters/sub-views (chip track, raised active segment).
 */
export type PulseTabsVariant = "primary" | "segmented";

export interface PulseTabItem<T extends string> {
  value: T;
  label: ReactNode;
  /** Decorative icon shown before the label. Hidden on very narrow screens in the primary variant. */
  icon?: ReactNode;
}

export interface PulseTabsProps<T extends string> {
  /** Accessible name for the tab group. */
  label: string;
  items: readonly PulseTabItem<T>[];
  value: T;
  onChange: (value: T) => void;
  variant?: PulseTabsVariant;
  /** Layout classes for the container, e.g. grid columns for `primary` or `flex-wrap` for `segmented`. */
  className?: string;
}

const containers: Record<PulseTabsVariant, string> = {
  primary: "grid grid-cols-1 gap-1 rounded-card bg-card p-2 shadow-card max-sm:max-h-46 max-sm:overflow-y-auto max-sm:scroll-py-2",
  segmented: "inline-flex items-center gap-0.5 rounded-full bg-chip p-1",
};

const tabs: Record<PulseTabsVariant, { base: string; active: string; inactive: string }> = {
  primary: {
    base: "flex min-h-11 min-w-0 w-full items-center gap-2 rounded-full px-3 py-2 text-start text-control font-bold wrap-anywhere xl:justify-center xl:text-center",
    active: "[background:var(--primary-fill)] text-primary-foreground forced-colors:outline-1 forced-colors:outline-[ButtonText]",
    inactive: "text-muted-foreground hover:bg-chip hover:text-foreground",
  },
  segmented: {
    base: "inline-flex min-h-9 items-center justify-center gap-1.5 rounded-full px-4 text-control font-bold transition motion-reduce:transition-none",
    active: "bg-card text-foreground shadow-card forced-colors:outline-1 forced-colors:outline-[ButtonText]",
    inactive: "text-muted-foreground hover:bg-chip hover:text-foreground",
  },
};

/** Keeps the active primary tab visible when the mobile tab list scrolls. */
function useRevealActive(
  containerRef: RefObject<HTMLElement | null>,
  activeRef: RefObject<HTMLButtonElement | null>,
  enabled: boolean,
  activeValue: string,
) {
  useEffect(() => {
    if (!enabled || !containerRef.current || !activeRef.current) return;
    const list: HTMLElement = containerRef.current;
    const button: HTMLButtonElement = activeRef.current;

    function reveal() {
      if (list.scrollHeight <= list.clientHeight) return;
      const box = list.getBoundingClientRect();
      const target = button.getBoundingClientRect();
      const padding = parseFloat(getComputedStyle(list).paddingBlockStart);
      const above = target.top - box.top - padding;
      const below = target.bottom - box.bottom + padding;
      if (above < 0) list.scrollTop += above;
      else if (below > 0) list.scrollTop += below;
    }

    const observer = new ResizeObserver(reveal);
    observer.observe(list);
    return () => observer.disconnect();
  }, [containerRef, activeRef, enabled, activeValue]);
}

/** Shared tab switcher. `primary` renders page navigation (`aria-current`); `segmented` renders a toggle group (`aria-pressed`). */
export default function PulseTabs<T extends string>({
  label, items, value, onChange, variant = "primary", className = "",
}: PulseTabsProps<T>) {
  const containerRef = useRef<HTMLElement & HTMLDivElement>(null);
  const activeRef = useRef<HTMLButtonElement>(null);
  const isPrimary = variant === "primary";
  const style = tabs[variant];

  useRevealActive(containerRef, activeRef, isPrimary, value);

  const buttons = items.map(item => {
    const isActive = item.value === value;
    return (
      <button
        key={item.value}
        ref={isActive ? activeRef : undefined}
        type="button"
        aria-current={isPrimary && isActive ? "true" : undefined}
        aria-pressed={isPrimary ? undefined : isActive}
        onClick={() => onChange(item.value)}
        className={`${style.base} ${isActive ? style.active : style.inactive}`}
      >
        {item.icon && (
          <span aria-hidden="true" className={`shrink-0 [&>svg]:size-5 ${isPrimary ? "hidden min-[360px]:inline-flex" : "inline-flex"}`}>
            {item.icon}
          </span>
        )}
        <span>{item.label}</span>
      </button>
    );
  });

  const classes = `${containers[variant]} ${className}`;
  return isPrimary
    ? <nav ref={containerRef} aria-label={label} className={classes}>{buttons}</nav>
    : <div ref={containerRef} role="group" aria-label={label} className={classes}>{buttons}</div>;
}
