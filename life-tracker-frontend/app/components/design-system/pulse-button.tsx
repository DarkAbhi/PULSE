"use client";

import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";

export type PulseButtonVariant =
  | "primary"
  | "secondary"
  | "tertiary"
  | "destructive"
  | "soft"
  | "chip"
  | "gradient"
  | "outline"
  | "outlineDanger"
  | "filledDanger"
  | "onHero"
  | "warning"
  | "icon"
  | "iconChip"
  | "iconChipDanger"
  | "iconOverlay"
  | "iconSecondary"
  | "iconDanger"
  | "menuItem";
export type PulseButtonSize = "sm" | "md" | "lg" | "xl" | "touch";
type PulseButtonShape = "default" | "control" | "pill";

export interface PulseButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: PulseButtonVariant;
  size?: PulseButtonSize;
  shape?: PulseButtonShape;
  labelStyle?: "default" | "strong";
  icon?: ReactNode;
  iconPosition?: "start" | "end";
}

const variants: Record<PulseButtonVariant, string> = {
  primary: "bg-primary text-primary-foreground hover:bg-primary-pressed",
  secondary: "bg-btn-cancel-bg text-btn-cancel-text hover:bg-btn-cancel-hover",
  tertiary: "text-action hover:bg-primary/10",
  destructive: "bg-destructive-soft text-destructive hover:bg-destructive-soft/80",
  soft: "bg-primary/10 text-action hover:bg-primary/20",
  chip: "bg-chip text-foreground enabled:hover:bg-divider",
  gradient: "[background:var(--button-fill,var(--primary-fill))] text-[color:var(--button-foreground,var(--primary-foreground))] enabled:hover:brightness-95",
  outline: "border-[1.5px] border-divider text-foreground enabled:hover:bg-chip",
  outlineDanger: "border border-destructive text-destructive enabled:hover:bg-destructive/10",
  filledDanger: "bg-destructive text-destructive-foreground enabled:hover:brightness-95",
  onHero: "bg-white text-(--on-hero-action) enabled:hover:bg-(--on-hero-hover)",
  warning: "bg-warning-soft text-warning hover:bg-warning-soft/80",
  icon: "text-muted-foreground hover:bg-secondary hover:text-foreground",
  iconChip: "text-muted-foreground enabled:hover:bg-chip enabled:hover:text-foreground",
  iconOverlay: "text-inherit enabled:hover:bg-white/15",
  iconChipDanger: "text-muted-foreground enabled:hover:bg-chip enabled:hover:text-destructive",
  iconSecondary: "bg-secondary text-foreground hover:bg-surface-strong",
  iconDanger: "text-destructive hover:bg-destructive/10",
  menuItem: "w-full text-foreground hover:bg-primary/10 hover:text-action",
};

const sizes: Record<PulseButtonSize, string> = {
  sm: "min-h-8 gap-1.5 px-3 py-1.5",
  md: "min-h-10 gap-2 px-4 py-2",
  lg: "min-h-11 gap-2 px-4 py-3",
  xl: "min-h-12 gap-2 px-4 py-2.5",
  touch: "min-h-12.5 gap-2 px-3.5 py-2",
};

const iconSizes: Record<PulseButtonSize, string> = {
  sm: "size-8",
  md: "size-10",
  lg: "size-11",
  xl: "size-12",
  touch: "size-12.5",
};

const shapes: Record<PulseButtonShape, string> = {
  default: "rounded-lg",
  control: "rounded-control",
  pill: "rounded-full",
};

/** Shared button for actions. Native button props, including onClick and type, pass through. */
const PulseButton = forwardRef<HTMLButtonElement, PulseButtonProps>(function PulseButton(
  { children, className = "", icon, iconPosition = "start", size = "md", shape = "default", labelStyle = "default", variant = "primary", ...props },
  ref,
) {
  const isIconButton = variant.startsWith("icon");
  const labelSize = labelStyle === "strong" ? "text-control" : size === "sm" ? "text-xs" : "text-sm";
  const alignment = variant === "menuItem" ? "justify-start font-medium" : "justify-center";
  const weight = variant === "menuItem" ? "" : labelStyle === "strong" ? "font-bold" : "font-semibold";

  return (
    <button
      {...props}
      ref={ref}
      className={`inline-flex items-center transition motion-reduce:transition-none disabled:cursor-not-allowed disabled:opacity-50 ${alignment} ${weight} ${isIconButton ? iconSizes[size] : `${sizes[size]} ${labelSize}`} ${shapes[shape]} ${variants[variant]} ${className}`}
    >
      {iconPosition === "start" && icon}
      {children}
      {iconPosition === "end" && icon}
    </button>
  );
});

export default PulseButton;
