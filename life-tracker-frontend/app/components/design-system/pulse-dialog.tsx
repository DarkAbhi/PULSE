"use client";

import { createContext, ReactNode, useContext, useEffect, useLayoutEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import PulseButton, { type PulseButtonProps, type PulseButtonVariant } from "@/app/components/design-system/pulse-button";
import PulseIconButton from "@/app/components/design-system/pulse-icon-button";

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

export interface PulseDialogProps {
  children: ReactNode;
  labelledBy?: string;
  onDismiss?: () => void;
  panelClassName?: string;
  size?: keyof typeof widths;
}

/**
 * Centered dialog on desktop and a bottom sheet on narrow screens.
 * Content scrolls independently of the actions.
 */
export default function PulseDialog({
  children,
  labelledBy,
  onDismiss,
  panelClassName = "",
  size = "sm",
}: PulseDialogProps) {
  const [footer, setFooter] = useState<HTMLElement | null>(null);

  const dialogRef = useRef<HTMLDialogElement>(null);
  const backdropPress = useRef(false);

  useLayoutEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const trigger = document.activeElement;
    dialog.showModal();

    if (openDialogs++ === 0) {
      previousBodyOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
    }
    return () => {
      dialog.close();
      if (trigger instanceof HTMLElement && trigger.isConnected) trigger.focus({ preventScroll: true });
      if (--openDialogs === 0) document.body.style.overflow = previousBodyOverflow;
    };
  }, []);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={labelledBy}
      aria-modal="true"
      className="fixed inset-0 m-0 h-dvh max-h-none w-full max-w-none overflow-y-auto overscroll-contain bg-transparent p-0 text-inherit backdrop:bg-scrim/55 backdrop:backdrop-blur-xs sheet:p-3"
      onCancel={event => {
        event.stopPropagation();
        event.preventDefault();
        onDismiss?.();
      }}
    >
      <div
        className="flex min-h-full items-end justify-center sheet:items-center"
        onPointerDown={event => { backdropPress.current = event.target === event.currentTarget; }}
        onClick={event => {
          if (backdropPress.current && event.target === event.currentTarget) onDismiss?.();
          backdropPress.current = false;
        }}
      >
        <section
          className={`dialog-fields relative flex max-h-[calc(100dvh-1.5rem)] w-full ${widths[size]} flex-col overflow-hidden rounded-t-card bg-card p-7 pb-[max(1.75rem,env(safe-area-inset-bottom))] text-card-foreground shadow-dialog motion-safe:animate-dialog-in max-sheet:min-w-full sheet:rounded-hero ${panelClassName}`}
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
    </dialog>
  );
}

type PulseDialogHeaderProps = {
  id: string;
  children: ReactNode;
  description?: ReactNode;
  icon?: ReactNode;
  onClose?: () => void;
  closeDisabled?: boolean;
  closeLabel?: string;
  closeAutoFocus?: boolean;
};

export function PulseDialogHeader({
  id,
  children,
  description,
  icon,
  onClose,
  closeDisabled,
  closeLabel = "Close",
  closeAutoFocus,
}: PulseDialogHeaderProps) {
  return (
    <header className="flex items-center gap-2.5">
      {icon && <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-accent text-action" aria-hidden="true">{icon}</span>}
      <div className="min-w-0 flex-1 wrap-anywhere">
        <h2 className="text-[1.6rem] leading-[1.15] font-extrabold tracking-[-0.03em]" id={id}>{children}</h2>
        {description && <p className="mt-1.5 text-[0.9rem] text-muted-foreground">{description}</p>}
      </div>
      {onClose && (
        <PulseIconButton
          aria-label={closeLabel}
          autoFocus={closeAutoFocus}
          className="shrink-0"
          disabled={closeDisabled}
          onClick={onClose}
          type="button"
          size="lg"
        >
          <X className="size-5" aria-hidden="true" />
        </PulseIconButton>
      )}
    </header>
  );
}

export function PulseDialogActions({ children, className = "" }: { children: ReactNode; className?: string }) {
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

type PulseDialogActionProps = Omit<PulseButtonProps, "variant"> & {
  variant?: "primary" | "secondary" | "destructive";
};

const actionVariants: Record<NonNullable<PulseDialogActionProps["variant"]>, PulseButtonVariant> = {
  primary: "primary",
  secondary: "outline",
  destructive: "filledDanger",
};

export function PulseDialogAction({ className = "", variant = "primary", ...props }: PulseDialogActionProps) {
  const formId = useContext(DialogFormContext);
  return (
    <PulseButton
      className={`wrap-anywhere motion-safe:enabled:active:scale-95 ${className}`}
      data-variant={variant}
      {...props}
      size="touch"
      labelStyle="strong"
      variant={actionVariants[variant]}
      form={props.form ?? (props.type !== "button" && props.type !== "reset" ? formId : undefined)}
      type={props.type ?? "submit"}
    />
  );
}
