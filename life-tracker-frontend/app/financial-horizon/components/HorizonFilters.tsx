import type { ComponentProps } from "react";
import PulseSelect from "@/app/components/design-system/pulse-select";
import { Icon } from "@/app/financial-horizon/components/HorizonIcon";

export function HorizonSelect({
  compact = false,
  className = "",
  ...props
}: Omit<ComponentProps<"select">, "aria-label"> & { compact?: boolean; "aria-label": string }) {
  return (
    <div className={`inline-flex max-w-full max-sm:flex-1 ${compact ? "min-w-30" : "min-w-37.5"} ${className}`}>
      <PulseSelect {...props} className={`w-full border-[1.5px] border-divider rounded-full bg-card text-foreground py-0 ps-4 text-control font-semibold ${compact ? "h-10" : "h-11"}`} />
    </div>
  );
}

export function HorizonSearch(props: Omit<ComponentProps<"input">, "type"> & { "aria-label": string }) {
  return (
    <div className="relative min-w-[min(200px,100%)] flex-1">
      <Icon name="search" className="pointer-events-none absolute start-3.5 top-3 size-5 text-muted-foreground" />
      <input {...props} type="search" className="h-11 min-w-0 border-[1.5px] border-divider rounded-full bg-card text-foreground w-full py-0 ps-11 pe-4" />
    </div>
  );
}
