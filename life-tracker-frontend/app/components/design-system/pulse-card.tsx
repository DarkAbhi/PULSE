import Link from "next/link";
import type { ComponentProps, HTMLAttributes } from "react";

export interface PulseCardProps extends HTMLAttributes<HTMLElement> {
  as?: "div" | "section" | "article" | "aside" | "li" | "p";
}

/** Dashboard-style surface. Callers control padding and layout; the design system owns its shape. */
export default function PulseCard({ as: Component = "div", className = "", ...props }: PulseCardProps) {
  return <Component {...props} className={`pulse-card ${className}`} />;
}

/** The same surface with native Next.js navigation and keyboard focus behavior. */
export function PulseCardLink({ className = "", ...props }: ComponentProps<typeof Link>) {
  return <Link {...props} className={`pulse-card ${className}`} />;
}
