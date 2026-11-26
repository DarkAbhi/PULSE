import type { ReactNode } from "react";
import { ArrowRight } from "lucide-react";
import { PulseCardLink } from "@/components/ui/pulse-card";

export type QuickActionTone = "fitness" | "garage" | "meal" | "finance";

export type QuickActionCardProps = {
  /** Tile colours, defined in `@/app/dashboard/dashboard-theme.css`. */
  tone: QuickActionTone;
  href: string;
  title: string;
  description: ReactNode;
  icon: ReactNode;
  footer?: ReactNode;
  decor?: ReactNode;
  backdrop?: ReactNode;
  actionLabel?: string;
};

/**
 * Dashboard feature tile. The card is the size container: type and decor step up at container
 * breakpoints, and spacing uses Tailwind's spacing scale.
 */
export default function QuickActionCard({
  tone,
  href,
  title,
  description,
  icon,
  footer,
  decor,
  backdrop,
  actionLabel,
}: QuickActionCardProps) {
  return (
    <PulseCardLink
      href={href}
      variant="tile"
      data-tone={tone}
      className="group @container relative isolate flex min-h-50 overflow-hidden motion-safe:transition-transform motion-safe:duration-200 motion-safe:active:scale-99 dashboard-compact:min-h-36"
    >
      {backdrop && (
        <span className="pointer-events-none absolute inset-0 -z-1" aria-hidden="true">{backdrop}</span>
      )}

      <div className="flex min-w-0 flex-1 flex-col gap-4 p-6 dashboard-compact:gap-2 dashboard-compact:p-4">
        <div className="flex items-center justify-between gap-4" aria-hidden="true">
          <span className="grid size-11 place-items-center rounded-full border-2 border-white/40 bg-white/20 text-white *:size-5 @md:size-12 @md:*:size-6 dashboard-compact:size-10 dashboard-compact:*:size-5">
            {icon}
          </span>
          <span className="grid size-9 place-items-center rounded-full bg-white text-tile-action shadow-tile-object *:size-4 motion-safe:transition-transform motion-safe:group-hover:translate-x-1 @md:size-11 @md:*:size-5 dashboard-compact:size-9 dashboard-compact:*:size-4">
            <ArrowRight strokeWidth={2.4} />
          </span>
        </div>

        <div className="min-w-0 wrap-anywhere">
          <h2 className="text-xl leading-tight font-extrabold tracking-tight @sm:text-2xl @lg:text-3xl dashboard-compact:text-lg">
            {title}
          </h2>
          <span className="mt-1 block line-clamp-2 max-w-xs text-sm leading-snug font-medium opacity-85 @sm:text-base dashboard-compact:mt-0 dashboard-compact:text-sm">
            {description}
          </span>
        </div>

        {(footer || decor) && (
          <div className="mt-auto flex min-h-10 min-w-0 items-end justify-between gap-4 @sm:min-h-12 @lg:min-h-16 dashboard-compact:min-h-9">
            <div className="min-w-0 flex-1">{footer}</div>
            {decor && <div className="flex-none" aria-hidden="true">{decor}</div>}
          </div>
        )}

        {actionLabel && <span className="sr-only">{actionLabel}</span>}
      </div>
    </PulseCardLink>
  );
}

export function QuickActionCardSkeleton() {
  return <div className="min-h-50 rounded-hero bg-card dashboard-compact:min-h-36" aria-busy="true" />;
}
