import type { ComponentProps } from "react";
import { Icon } from "@/app/financial-horizon/components/HorizonIcon";
import { horizonClasses as theme } from "@/app/financial-horizon/horizon-theme-classes";

export function HorizonSelect({
  compact = false,
  className = "",
  ...props
}: Omit<ComponentProps<"select">, "aria-label"> & { compact?: boolean; "aria-label": string }) {
  return (
    <div className={`relative inline-flex max-w-full max-sm:flex-1 ${className}`}>
      <select {...props} className={`${theme.select} ${compact ? "h-10 min-w-30" : "h-11 min-w-37.5"}`} />
      <Icon name="chevronDown" className="pointer-events-none absolute top-1/2 right-3.5 size-5 -translate-y-1/2 text-muted-foreground" />
    </div>
  );
}

export function HorizonSearch(props: Omit<ComponentProps<"input">, "type"> & { "aria-label": string }) {
  return (
    <div className="relative min-w-[min(200px,100%)] flex-1">
      <Icon name="search" className="pointer-events-none absolute top-3 left-3.5 size-5 text-muted-foreground" />
      <input {...props} type="search" className={theme.search} />
    </div>
  );
}
