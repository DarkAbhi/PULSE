import { type ComponentPropsWithRef, type ReactNode } from "react";

export type PulseButtonVariant =
  | "primary"
  | "secondary"
  | "outline"
  | "ghost"
  | "destructive"
  | "link";
export type PulseButtonSize = "sm" | "md" | "lg" | "xl" | "touch";

export interface PulseButtonProps extends ComponentPropsWithRef<"button"> {
  variant?: PulseButtonVariant;
  size?: PulseButtonSize;
  labelStyle?: "default" | "strong";
  icon?: ReactNode;
  iconPosition?: "start" | "end";
}

const variants: Record<PulseButtonVariant, string> = {
  /** Main action on a page or within a dialog. Gradient fill; override per feature with `--button-fill` / `--button-foreground`. */
  primary: "[background:var(--button-fill,var(--primary-fill))] text-(--button-foreground,var(--primary-foreground)) enabled:hover:brightness-95",
  /** Supporting action with less visual emphasis. Override per surface with `--btn-cancel-bg` / `--btn-cancel-text` / `--btn-cancel-hover`. */
  secondary: "bg-btn-cancel-bg text-btn-cancel-text enabled:hover:bg-btn-cancel-hover",
  /** Alternative action without a filled background. */
  outline: "border-[1.5px] border-divider text-foreground enabled:hover:bg-chip",
  /** Low-emphasis contextual action. */
  ghost: "text-action enabled:hover:bg-primary/10",
  /** Deleting or performing irreversible actions. */
  destructive: "bg-destructive text-destructive-foreground enabled:hover:brightness-95",
  /** Inline actions and lightweight navigation. */
  link: "text-action underline-offset-4 enabled:hover:underline",
};

const sizes: Record<PulseButtonSize, string> = {
  sm: "min-h-8 gap-1.5 px-3 py-1.5",
  md: "min-h-10 gap-2 px-4 py-2",
  lg: "min-h-11 gap-2 px-4 py-3",
  xl: "min-h-12 gap-2 px-4 py-2.5",
  touch: "min-h-12.5 gap-2 px-3.5 py-2",
};

/** Shared pill-shaped button for labelled actions; use PulseIconButton for icon-only actions. Native button props, including onClick and type, pass through. */
export default function PulseButton(
  { children, className = "", icon, iconPosition = "start", size = "md", labelStyle = "default", variant = "primary", ...props }: PulseButtonProps,
) {
  const labelSize = labelStyle === "strong" ? "text-control" : size === "sm" ? "text-xs" : "text-sm";
  const weight = labelStyle === "strong" ? "font-bold" : "font-semibold";

  return (
    <button
      {...props}
      className={`inline-flex items-center justify-center rounded-full transition motion-reduce:transition-none disabled:cursor-not-allowed disabled:opacity-50 ${weight} ${sizes[size]} ${labelSize} ${variants[variant]} ${className}`}
    >
      {iconPosition === "start" && icon}
      {children}
      {iconPosition === "end" && icon}
    </button>
  );
}
