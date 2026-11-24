import Link from "next/link";
import type { ComponentProps, HTMLAttributes } from "react";

export type PulseCardVariant = "surface" | "tile";
export type PulseCardElevation = "raised" | "flat";
export type PulseCardSize = "default" | "compact";

interface PulseCardStyleProps {
  /** `surface` for content cards; `tile` for the gradient feature tiles. */
  variant?: PulseCardVariant;
  /** `raised` lifts the card off the canvas; `flat` is for cards layered on another surface or inside a dialog. */
  elevation?: PulseCardElevation;
  /** `compact` uses the smaller radius for cards nested inside another card or dialog. */
  size?: PulseCardSize;
}

export interface PulseCardProps extends HTMLAttributes<HTMLElement>, PulseCardStyleProps {
  as?: "div" | "section" | "article" | "aside" | "fieldset" | "li" | "p";
}

export type PulseCardLinkProps = ComponentProps<typeof Link> & PulseCardStyleProps;

const fills: Record<PulseCardVariant, string> = {
  /** Solid themed surface. Tint a whole card with `--card-fill`. */
  surface: "bg-(--card-fill,var(--card))",
  /** 135° gradient from `--tile-start` to `--tile-end`. Text uses `--tile-foreground`, white by default. */
  tile: "[background:linear-gradient(135deg,var(--tile-start),var(--tile-end))] text-(--tile-foreground,white)",
};

const shadows: Record<PulseCardVariant, string> = {
  surface: "shadow-card",
  tile: "shadow-[0_20px_40px_-22px_var(--tile-start)] dark:shadow-[0_20px_40px_-24px_rgb(0_0_0/70%)]",
};

const radii: Record<PulseCardVariant, Record<PulseCardSize, string>> = {
  surface: { default: "rounded-card", compact: "rounded-control" },
  tile: { default: "rounded-hero", compact: "rounded-card" },
};

/** The full visual spec for a card. Callers pass layout and spacing only through `className`. */
function cardClassName({ variant = "surface", elevation = "raised", size = "default" }: PulseCardStyleProps, className: string) {
  const shadow = elevation === "raised" ? shadows[variant] : "";
  // `pulse-card` carries no styles; it is a stable hook for tests.
  return `pulse-card min-w-0 ${radii[variant][size]} ${fills[variant]} ${shadow} ${className}`;
}

/** Dashboard-style card: rounded, filled and raised by default. Callers control padding and layout; the design system owns its shape. */
export default function PulseCard({ as: Component = "div", variant, elevation, size, className = "", ...props }: PulseCardProps) {
  return <Component {...props} className={cardClassName({ variant, elevation, size }, className)} />;
}

/** The same card with native Next.js navigation and a keyboard focus ring. */
export function PulseCardLink({ variant, elevation, size, className = "", ...props }: PulseCardLinkProps) {
  return (
    <Link
      {...props}
      className={`${cardClassName({ variant, elevation, size }, "")} focus-visible:outline-3 focus-visible:outline-offset-4 focus-visible:outline-ring ${className}`}
    />
  );
}
