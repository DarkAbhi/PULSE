"use client";

import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "tertiary"
  | "destructive"
  | "destructiveOutline"
  | "soft"
  | "warning"
  | "icon"
  | "iconSecondary"
  | "iconDanger"
  | "menuItem";
export type ButtonSize = "sm" | "md" | "lg";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: ReactNode;
  iconPosition?: "start" | "end";
}

const variants: Record<ButtonVariant, string> = {
  primary: "bg-primary text-primary-foreground hover:bg-primary/90 focus-visible:ring-primary/20",
  secondary: "border border-btn-cancel-border bg-btn-cancel-bg text-btn-cancel-text hover:bg-btn-cancel-hover focus-visible:ring-ring/15",
  tertiary: "text-primary hover:bg-primary/10 focus-visible:ring-primary/20",
  destructive: "bg-destructive text-destructive-foreground hover:bg-destructive/90 focus-visible:ring-destructive/20",
  destructiveOutline: "border border-destructive/40 text-destructive hover:bg-destructive/10 focus-visible:ring-destructive/20",
  soft: "border border-primary/20 bg-primary/10 text-primary hover:bg-primary/20 focus-visible:ring-primary/20",
  warning: "bg-amber-500 text-white hover:bg-amber-600 focus-visible:ring-amber-500/20",
  icon: "text-muted-foreground hover:bg-secondary hover:text-foreground focus-visible:ring-primary/20",
  iconSecondary: "border border-border bg-card text-foreground hover:bg-secondary focus-visible:ring-primary/20",
  iconDanger: "text-destructive hover:bg-destructive/10 focus-visible:ring-destructive/20",
  menuItem: "w-full text-foreground hover:bg-primary/10 hover:text-primary focus-visible:ring-primary/20",
};

const sizes: Record<ButtonSize, string> = {
  sm: "min-h-8 gap-1.5 rounded-lg px-3 py-1.5 text-xs",
  md: "min-h-10 gap-2 rounded-lg px-4 py-2 text-sm",
  lg: "min-h-11 gap-2 rounded-lg px-4 py-3 text-sm",
};

const iconSizes: Record<ButtonSize, string> = {
  sm: "h-8 w-8 rounded-lg",
  md: "h-10 w-10 rounded-lg",
  lg: "h-11 w-11 rounded-lg",
};

/** Shared button for actions. Native button props, including onClick and type, pass through. */
const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { children, className = "", icon, iconPosition = "start", size = "md", variant = "primary", ...props },
  ref,
) {
  const isIconButton = variant === "icon" || variant === "iconSecondary" || variant === "iconDanger";

  return (
    <button
      {...props}
      ref={ref}
      className={`inline-flex items-center ${variant === "menuItem" ? "justify-start font-medium" : "justify-center font-semibold"} transition focus-visible:outline-none focus-visible:ring-4 disabled:cursor-not-allowed disabled:opacity-50 ${isIconButton ? iconSizes[size] : sizes[size]} ${variants[variant]} ${className}`}
    >
      {iconPosition === "start" && icon}
      {children}
      {iconPosition === "end" && icon}
    </button>
  );
});

export default Button;
