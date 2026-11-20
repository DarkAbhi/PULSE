"use client";

import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "tertiary"
  | "destructive"
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
  primary: "bg-primary text-primary-foreground hover:bg-primary-pressed",
  secondary: "bg-btn-cancel-bg text-btn-cancel-text hover:bg-btn-cancel-hover",
  tertiary: "text-action hover:bg-primary/10",
  destructive: "bg-destructive-soft text-destructive hover:bg-destructive-soft/80",
  soft: "bg-primary/10 text-action hover:bg-primary/20",
  warning: "bg-warning-soft text-warning hover:bg-warning-soft/80",
  icon: "text-muted-foreground hover:bg-secondary hover:text-foreground",
  iconSecondary: "bg-secondary text-foreground hover:bg-surface-strong",
  iconDanger: "text-destructive hover:bg-destructive/10",
  menuItem: "w-full text-foreground hover:bg-primary/10 hover:text-action",
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
      className={`inline-flex items-center ${variant === "menuItem" ? "justify-start font-medium" : "justify-center font-semibold"} transition disabled:cursor-not-allowed disabled:opacity-50 ${isIconButton ? iconSizes[size] : sizes[size]} ${variants[variant]} ${className}`}
    >
      {iconPosition === "start" && icon}
      {children}
      {iconPosition === "end" && icon}
    </button>
  );
});

export default Button;
