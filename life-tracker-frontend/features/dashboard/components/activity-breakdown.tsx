"use client";

import { useEffect, useId, useState } from "react";
import type { MouseEvent, ReactNode } from "react";

export default function ActivityBreakdown({ children, breakdown, className }: {
  children: ReactNode;
  breakdown: ReactNode;
  className: string;
}) {
  const [open, setOpen] = useState(false);
  const id = useId();

  useEffect(() => {
    if (!open) return;

    function dismiss(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", dismiss);
    return () => document.removeEventListener("keydown", dismiss);
  }, [open]);

  function activate(event: MouseEvent<HTMLButtonElement>) {
    if (event.detail === 0) {
      setOpen(true);
      return;
    }

    const pointerType = event.nativeEvent instanceof PointerEvent ? event.nativeEvent.pointerType : undefined;
    if (pointerType === "touch" || (!pointerType && window.matchMedia("(hover: none)").matches)) {
      setOpen((current) => !current);
    }
  }

  return (
    <div
      onPointerEnter={(event) => {
        if (event.pointerType !== "touch") setOpen(true);
      }}
      onPointerLeave={(event) => {
        if (event.pointerType !== "touch") setOpen(false);
      }}
    >
      <button
        type="button"
        className={className}
        aria-expanded={open}
        aria-controls={id}
        onFocus={(event) => {
          if (event.currentTarget.matches(":focus-visible")) setOpen(true);
        }}
        onBlur={() => setOpen(false)}
        onClick={activate}
      >
        {children}
      </button>
      <div id={id} hidden={!open} className="absolute inset-x-0 bottom-full z-10 pb-2">
        {breakdown}
      </div>
    </div>
  );
}
