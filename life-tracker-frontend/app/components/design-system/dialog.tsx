"use client";

import { createContext, ReactNode, useContext, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import Button, { type ButtonProps } from "./button";

const DialogFooterContext = createContext<HTMLElement | null>(null);
const DialogFormContext = createContext<string | undefined>(undefined);

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
 * Content scrolls inside the panel while DialogActions remain in its footer.
 */
export default function Dialog({
  children,
  labelledBy,
  onBackdropClick,
  panelClassName = "",
  size = "sm",
}: DialogProps) {
  const [footer, setFooter] = useState<HTMLElement | null>(null);

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
          className={`flex w-full ${widths[size]} max-h-[calc(100dvh-2rem)] flex-col overflow-hidden rounded-2xl border border-border bg-card p-6 shadow-2xl sm:max-h-[calc(100dvh-3rem)] sm:p-8 lg:max-h-[calc(100dvh-4rem)] ${panelClassName}`}
        >
          <DialogFooterContext.Provider value={footer}>
            <div className="min-h-0 overflow-y-auto">{children}</div>
            <div className="shrink-0" ref={setFooter} />
          </DialogFooterContext.Provider>
        </section>
      </div>
    </div>
  );
}

export function DialogActions({ children, className = "" }: { children: ReactNode; className?: string }) {
  const footer = useContext(DialogFooterContext);
  const actionsRef = useRef<HTMLDivElement>(null);
  const generatedFormId = useId();
  const [formId, setFormId] = useState<string>();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const form = actionsRef.current?.closest("form");
    if (form) {
      if (!form.id) form.id = generatedFormId;
      setFormId(form.id);
    }
    setReady(true);
  }, [generatedFormId]);

  const actions = (
    <DialogFormContext.Provider value={formId}>
      <div ref={actionsRef} className={`mt-6 grid gap-3 sm:grid-cols-2 ${className}`}>
        {children}
      </div>
    </DialogFormContext.Provider>
  );

  return footer && ready ? createPortal(actions, footer) : actions;
}

type DialogActionProps = ButtonProps & {
  variant?: "primary" | "secondary" | "destructive";
};

export function DialogAction({ className = "", variant = "primary", ...props }: DialogActionProps) {
  const formId = useContext(DialogFormContext);
  return (
    <Button
      className={className}
      size="lg"
      variant={variant}
      {...props}
      form={props.form ?? (props.type !== "button" && props.type !== "reset" ? formId : undefined)}
      type={props.type ?? "submit"}
    />
  );
}
