import type { ReactNode } from "react";
import { ArrowRight } from "lucide-react";
import { PulseCardLink } from "@/app/components/design-system/pulse-card";

export type QuickActionCardProps = {
  href: string;
  title: string;
  description: ReactNode;
  icon: ReactNode;
  footer?: ReactNode;
  decor?: ReactNode;
  backdrop?: ReactNode;
  actionLabel?: string;
  className: string;
};

export default function QuickActionCard({
  href,
  title,
  description,
  icon,
  footer,
  decor,
  backdrop,
  actionLabel,
  className,
}: QuickActionCardProps) {
  return (
    <PulseCardLink
      href={href}
      variant="tile"
      className={`group @container relative isolate grid grid-cols-[auto_minmax(0,1fr)_auto] grid-rows-[auto_auto_1fr] gap-x-3 gap-y-[clamp(12px,4cqi,20px)] min-h-50 overflow-hidden p-[clamp(18px,5cqi,28px)] motion-safe:transition-transform motion-safe:duration-200 motion-safe:ease-[ease] motion-safe:active:transform-[scale(.985)] dashboard-compact:min-h-36 dashboard-compact:gap-y-2.5 dashboard-compact:p-4 ${className}`}
    >
      {backdrop && (
        <span className="pointer-events-none absolute inset-0 -z-1" aria-hidden="true">{backdrop}</span>
      )}

      <span
        className="col-start-1 row-start-1 grid place-items-center self-center aspect-square rounded-full w-[clamp(42px,12cqi,52px)] border-[1.5px] border-white/50 bg-white/22 text-white [&_svg]:size-[46%] dashboard-compact:w-10"
        aria-hidden="true"
      >
        {icon}
      </span>

      <div className="col-span-3 row-start-2 min-w-0 wrap-anywhere">
        <h2 className="text-[clamp(1.25rem,6.2cqi,1.75rem)] leading-[1.1] font-extrabold tracking-[-.03em] dashboard-compact:text-lg">
          {title}
        </h2>
        <span className="mt-1 block line-clamp-2 max-w-[34ch] text-[clamp(.875rem,3.6cqi,1rem)] leading-snug font-medium opacity-85 dashboard-compact:mt-0.5 dashboard-compact:text-sm">
          {description}
        </span>
      </div>

      {(footer || decor) && (
        <div className="col-span-3 row-start-3 flex items-end justify-between gap-3 self-end min-w-0 min-h-[clamp(40px,12cqi,64px)] dashboard-compact:min-h-9">
          <div className="min-w-0 flex-1">{footer}</div>
          {decor && <div className="flex-none" aria-hidden="true">{decor}</div>}
        </div>
      )}

      <span className="col-start-3 row-start-1 grid place-items-center self-center aspect-square rounded-full w-[clamp(36px,10cqi,44px)] bg-white text-(--tile-action-fg,var(--tile-start)) shadow-[0_6px_14px_-6px_rgb(0_0_0/35%)] motion-safe:transition-transform motion-safe:group-hover:translate-x-0.5 dashboard-compact:w-9">
        <ArrowRight className="size-[48%]" strokeWidth={2.4} aria-hidden="true" />
        {actionLabel && <span className="sr-only">{actionLabel}</span>}
      </span>
    </PulseCardLink>
  );
}

export function QuickActionCardSkeleton() {
  return <div className="min-h-50 rounded-hero bg-card dashboard-compact:min-h-36" aria-busy="true" />;
}
