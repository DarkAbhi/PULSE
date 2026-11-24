"use client";

import { createContext, useContext, useEffect, useId, useRef, useState, type ReactNode, type RefObject } from "react";

export interface PulseDropdownTriggerProps {
  ref: RefObject<HTMLButtonElement | null>;
  onClick: () => void;
  "aria-expanded": boolean;
  "aria-controls": string | undefined;
}

interface PulseDropdownProps {
  /** Render the trigger button and spread these props onto it. */
  trigger: (props: PulseDropdownTriggerProps & { isOpen: boolean }) => ReactNode;
  children: ReactNode;
  className?: string;
}

const DropdownContext = createContext<(() => void) | null>(null);

/** Shared popover panel of actions anchored to a trigger. Closes on outside click, Escape, or selecting an item. */
export default function PulseDropdown({ trigger, children, className = "" }: PulseDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const panelId = useId();
  const triggerRef = useRef<HTMLButtonElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setIsOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [isOpen]);

  const close = () => {
    triggerRef.current?.focus();
    setIsOpen(false);
  };

  return (
    <div
      ref={containerRef}
      className="relative inline-block text-left"
      onKeyDown={event => {
        if (isOpen && event.key === "Escape") {
          event.preventDefault();
          close();
        }
      }}
    >
      {trigger({
        ref: triggerRef,
        onClick: () => setIsOpen(open => !open),
        "aria-expanded": isOpen,
        "aria-controls": isOpen ? panelId : undefined,
        isOpen,
      })}
      {isOpen && (
        <div
          id={panelId}
          className={`absolute left-0 z-50 mt-2 w-64 space-y-1 rounded-card bg-popover p-2 shadow-card sm:right-0 sm:left-auto ${className}`}
        >
          <DropdownContext.Provider value={close}>{children}</DropdownContext.Provider>
        </div>
      )}
    </div>
  );
}

interface PulseDropdownItemProps {
  onSelect: () => void;
  icon?: ReactNode;
  description?: ReactNode;
  children: ReactNode;
}

/** A row inside PulseDropdown. Closes the dropdown, returns focus to the trigger, then runs onSelect. */
export function PulseDropdownItem({ onSelect, icon, description, children }: PulseDropdownItemProps) {
  const close = useContext(DropdownContext);

  return (
    <button
      type="button"
      className="flex min-h-10 w-full items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-foreground transition hover:bg-primary/10 hover:text-action motion-reduce:transition-none"
      onClick={() => {
        close?.();
        onSelect();
      }}
    >
      {icon}
      <span className="grid text-start">
        <span className="font-semibold leading-tight">{children}</span>
        {description && <span className="text-caption text-muted-foreground">{description}</span>}
      </span>
    </button>
  );
}

export function PulseDropdownSeparator() {
  return <div aria-hidden="true" className="my-1 border-t border-divider" />;
}
