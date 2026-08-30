"use client";

import { ButtonHTMLAttributes, ReactNode } from "react";

const widths = {
  sm: "max-w-md",
  md: "max-w-lg",
  wide: "max-w-xl",
  lg: "max-w-2xl",
  xl: "max-w-3xl",
} as const;

export interface DialogProps {
  children: ReactNode;
  labelledBy?: string;
  onBackdropClick?: (event: React.MouseEvent<HTMLDivElement>) => void;
  panelClassName?: string;
  size?: keyof typeof widths;
}

/**
 * Shared dialog frame that keeps every modal clear of the viewport edges.
 * Content taller than the available space scrolls inside the panel instead
 * of pushing the panel flush against the browser window.
 */
export default function Dialog({
  children,
  labelledBy,
  onBackdropClick,
  panelClassName = "",
  size = "sm",
}: DialogProps) {
  return (
    <div
      aria-labelledby={labelledBy}
      aria-modal="true"
      className="fixed inset-0 z-50 overflow-y-auto bg-overlay-bg p-4 backdrop-blur-xs sm:p-6 lg:p-8"
      role="dialog"
    >
      <div
        className="flex min-h-full items-center justify-center"
        onClick={onBackdropClick}
      >
        <section
          className={`w-full ${widths[size]} max-h-[calc(100dvh-2rem)] overflow-y-auto rounded-2xl border border-border bg-card p-6 shadow-2xl sm:max-h-[calc(100dvh-3rem)] sm:p-8 lg:max-h-[calc(100dvh-4rem)] ${panelClassName}`}
        >
          {children}
        </section>
      </div>
    </div>
  );
}

export function DialogActions({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`mt-6 grid gap-3 sm:grid-cols-2 ${className}`}>{children}</div>;
}

type DialogActionProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "destructive";
};

export function DialogAction({ className = "", variant = "primary", ...props }: DialogActionProps) {
  const variants = {
    primary: "bg-primary text-primary-foreground hover:bg-primary/90 focus:ring-primary/20",
    secondary: "border border-btn-cancel-border bg-btn-cancel-bg text-btn-cancel-text hover:bg-btn-cancel-hover focus:ring-ring/15",
    destructive: "bg-destructive text-destructive-foreground hover:bg-destructive/90 focus:ring-destructive/20",
  };

  return (
    <button
      className={`inline-flex min-h-11 items-center justify-center rounded-lg px-4 py-3 text-sm font-semibold transition focus:outline-none focus:ring-4 disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    />
  );
}
