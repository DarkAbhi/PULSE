import { type ComponentPropsWithRef } from "react";

export type PulseIconButtonVariant = "plain" | "secondary" | "overlay" | "danger";
export type PulseIconButtonSize = "sm" | "md" | "lg" | "xl";

export interface PulseIconButtonProps extends Omit<ComponentPropsWithRef<"button">, "aria-label"> {
  /** Icon-only buttons have no visible text, so an accessible name is required. */
  "aria-label": string;
  variant?: PulseIconButtonVariant;
  size?: PulseIconButtonSize;
}

const variants: Record<PulseIconButtonVariant, string> = {
  plain: "text-muted-foreground enabled:hover:bg-chip enabled:hover:text-foreground",
  secondary: "bg-secondary text-foreground enabled:hover:bg-surface-strong",
  overlay: "text-inherit enabled:hover:bg-white/15",
  danger: "text-muted-foreground enabled:hover:bg-chip enabled:hover:text-destructive",
};

const sizes: Record<PulseIconButtonSize, string> = {
  sm: "size-8",
  md: "size-10",
  lg: "size-11",
  xl: "size-12",
};

/** Shared circular icon-only button. Pass the icon as children. */
export default function PulseIconButton(
  { children, className = "", size = "md", variant = "plain", ...props }: PulseIconButtonProps,
) {
  return (
    <button
      {...props}
      className={`inline-flex shrink-0 items-center justify-center rounded-full transition motion-reduce:transition-none disabled:cursor-not-allowed disabled:opacity-50 ${sizes[size]} ${variants[variant]} ${className}`}
    >
      {children}
    </button>
  );
}
