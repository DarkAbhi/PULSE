import type { ReactNode } from "react";
import { Icon, type IconName } from "@/app/financial-horizon/components/horizon-icon";

interface SectionHeaderProps {
  title: string;
  icon: IconName;
  id?: string;
  description?: string;
  badges?: ReactNode;
  children?: ReactNode;
}

export default function SectionHeader({
  title,
  icon,
  id,
  description,
  badges,
  children,
}: SectionHeaderProps) {
  return (
    <header className="flex flex-wrap items-center justify-between gap-4 border-b border-divider pb-5">
      <div>
        <div className="flex flex-wrap items-center gap-2.5">
          <Icon name={icon} className="size-6 text-brand-blue" />
          <h2 id={id} className="text-(length:--finance-section-heading) leading-tight font-extrabold tracking-tight">
            {title}
          </h2>
          {badges}
        </div>
        {description && <p className="mt-1 max-w-prose text-control text-muted-foreground">{description}</p>}
      </div>
      {children}
    </header>
  );
}
