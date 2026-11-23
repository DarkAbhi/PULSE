"use client";

import { createContext, ReactNode, useContext, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import Button, { type ButtonProps } from "@/app/components/design-system/button";
import { horizonPalette } from "@/app/components/design-system/horizon-palette";

let openDialogs = 0;
let previousBodyOverflow = "";

const DialogFooterContext = createContext<HTMLElement | null>(null);
const DialogFormContext = createContext<string | undefined>(undefined);

const widths = {
  sm: "max-w-[520px]",
  md: "max-w-lg",
  wide: "max-w-xl",
  lg: "max-w-2xl",
  xl: "max-w-3xl",
} as const;

export interface DialogProps {
  children: ReactNode;
  labelledBy?: string;
  onDismiss?: () => void;
  onBackdropClick?: (event: React.MouseEvent<HTMLDivElement>) => void;
  panelClassName?: string;
  size?: keyof typeof widths;
}

/**
 * Centered dialog on desktop and a bottom sheet on narrow screens.
 * Content scrolls independently of the actions.
 */
export default function Dialog({
  children,
  labelledBy,
  onDismiss,
  onBackdropClick,
  panelClassName = "",
  size = "sm",
}: DialogProps) {
  const [footer, setFooter] = useState<HTMLElement | null>(null);

  useEffect(() => {
    if (openDialogs++ === 0) {
      previousBodyOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
    }
    return () => {
      if (--openDialogs === 0) document.body.style.overflow = previousBodyOverflow;
    };
  }, []);

  useEffect(() => {
    if (!onDismiss) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !event.defaultPrevented) {
        event.preventDefault();
        onDismiss?.();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onDismiss]);

  return (
    <div
      aria-labelledby={labelledBy}
      aria-modal="true"
      className="fixed inset-0 z-50 overflow-y-auto overscroll-contain bg-[#0d0a1f]/55 p-3 backdrop-blur-[4px] max-[761px]:p-0"
      role="dialog"
    >
      <div
        className="flex min-h-full items-center justify-center max-[761px]:items-end"
        onClick={event => {
          onBackdropClick?.(event);
          if (event.target === event.currentTarget && !event.defaultPrevented) onDismiss?.();
        }}
      >
        <section
          className={`${horizonPalette} dialog-fields relative flex max-h-[calc(100dvh-24px)] w-full ${widths[size]} flex-col overflow-hidden rounded-[32px] bg-card p-7 pb-[max(1.75rem,env(safe-area-inset-bottom))] shadow-[0_30px_80px_-20px_rgb(0_0_0/55%)] motion-safe:animate-dialog-in max-[761px]:min-w-full max-[761px]:rounded-t-[28px] max-[761px]:rounded-b-none ${panelClassName}`}
        >
          <DialogFooterContext.Provider value={footer}>
            <div
              className={`min-h-0 overflow-y-auto ${
                panelClassName.includes("p-0") ? "" : "-mx-1.5 -my-1 px-1.5 py-1"
              }`}
            >
              {children}
            </div>
            <div className="shrink-0" ref={setFooter} />
          </DialogFooterContext.Provider>
        </section>
      </div>
    </div>
  );
}

type DialogHeaderProps = {
  id: string;
  children: ReactNode;
  description?: ReactNode;
  icon?: ReactNode;
  onClose?: () => void;
  closeDisabled?: boolean;
  closeLabel?: string;
  closeAutoFocus?: boolean;
};

export function DialogHeader({
  id,
  children,
  description,
  icon,
  onClose,
  closeDisabled,
  closeLabel = "Close",
  closeAutoFocus,
}: DialogHeaderProps) {
  return (
    <header className="flex items-center gap-2.5">
      {icon && <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-accent text-action" aria-hidden="true">{icon}</span>}
      <div className="min-w-0 flex-1 wrap-anywhere">
        <h2 className="text-[1.6rem] leading-[1.15] font-extrabold tracking-[-0.03em]" id={id}>{children}</h2>
        {description && <p className="mt-1.5 text-[0.9rem] text-muted-foreground">{description}</p>}
      </div>
      {onClose && (
        <Button
          aria-label={closeLabel}
          autoFocus={closeAutoFocus}
          className="size-11! shrink-0 rounded-full! enabled:hover:bg-(--horizon-chip)! motion-reduce:transition-none"
          disabled={closeDisabled}
          onClick={onClose}
          type="button"
          variant="icon"
        >
          <X className="size-5" aria-hidden="true" />
        </Button>
      )}
    </header>
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
      <div ref={actionsRef} className={`mt-5 grid grid-cols-2 gap-2.5 [&>:only-child]:col-span-full max-sm:[&>:first-child:nth-last-child(3)]:col-span-full ${className}`}>
        {children}
      </div>
    </DialogFormContext.Provider>
  );

  return footer && ready ? createPortal(actions, footer) : actions;
}

type DialogActionProps = ButtonProps & {
  variant?: "primary" | "secondary" | "destructive";
};

const actionVariants: Record<NonNullable<DialogActionProps["variant"]>, string> = {
  primary: "[background:var(--horizon-button)]! enabled:hover:brightness-[0.93]",
  secondary: "border-[1.5px] border-divider bg-transparent! enabled:hover:bg-(--horizon-chip)!",
  destructive: "bg-destructive! text-destructive-foreground! enabled:hover:brightness-[0.93]",
};

export function DialogAction({ className = "", variant = "primary", ...props }: DialogActionProps) {
  const formId = useContext(DialogFormContext);
  return (
    <Button
      className={`min-h-[50px]! rounded-full! px-3.5! py-2! text-[0.95rem]! leading-[1.2]! font-bold! wrap-anywhere motion-safe:enabled:active:scale-[0.96] motion-reduce:transition-none ${actionVariants[variant]} ${className}`}
      data-variant={variant}
      size="lg"
      variant={variant}
      {...props}
      form={props.form ?? (props.type !== "button" && props.type !== "reset" ? formId : undefined)}
      type={props.type ?? "submit"}
    />
  );
}
