import type { HTMLAttributes } from "react";

interface HorizonEmptyProps extends HTMLAttributes<HTMLElement> {
  as?: "div" | "p";
}

/** Dashed placeholder for an empty list. It marks where cards will appear, so it is not a card itself. */
export default function HorizonEmpty({ as: Component = "div", className = "", ...props }: HorizonEmptyProps) {
  return (
    <Component
      {...props}
      className={`min-w-0 rounded-card border border-dashed border-divider p-8 text-center ${className}`}
    />
  );
}
