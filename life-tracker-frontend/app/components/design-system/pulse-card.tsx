import Link from "next/link";
import type { ComponentProps, HTMLAttributes } from "react";

export type PulseCardVariant = "surface" | "tile";
export type PulseCardElevation = "raised" | "flat";
export type PulseCardSize = "default" | "compact";
export type PulseCardPadding = "none" | "sm" | "md" | "lg";

interface PulseCardStyleProps {
  /** `surface` for content cards; `tile` for the gradient feature tiles. */
  variant?: PulseCardVariant;
  /** `raised` lifts the card off the canvas; `flat` is for cards layered on another surface or inside a dialog. */
  elevation?: PulseCardElevation;
  /** `compact` uses the smaller radius for cards nested inside another card or dialog. */
  size?: PulseCardSize;
  /** Inner padding on the 4px spacing scale: `sm` 16px, `md` 24px, `lg` 32px. `none` leaves padding to the caller. */
  padding?: PulseCardPadding;
}

export interface PulseCardProps extends HTMLAttributes<HTMLElement>, PulseCardStyleProps {
  as?: "div" | "section" | "article" | "aside" | "fieldset" | "li" | "p";
}

export type PulseCardLinkProps = ComponentProps<typeof Link> & PulseCardStyleProps;

const fills: Record<PulseCardVariant, string> = {
  /** Solid themed surface. Tint a whole card with `--card-fill`. */
  surface: "bg-card-fill",
  /** 135° gradient from `--tile-start` to `--tile-end`. Text uses `--tile-foreground`, white by default. */
  tile: "bg-linear-135/srgb from-tile-start to-tile-end text-tile-foreground",
};

const shadows: Record<PulseCardVariant, string> = {
  surface: "shadow-card",
  tile: "shadow-tile dark:shadow-tile-dark",
};

const radii: Record<PulseCardVariant, Record<PulseCardSize, string>> = {
  surface: { default: "rounded-card", compact: "rounded-control" },
  tile: { default: "rounded-hero", compact: "rounded-card" },
};

const paddings: Record<PulseCardPadding, string> = {
  none: "",
  sm: "p-4",
  md: "p-6",
  lg: "p-8",
};

/** The full visual spec for a card. Callers pass layout through `className`. */
function cardClassName(
  { variant = "surface", elevation = "raised", size = "default", padding = "none" }: PulseCardStyleProps,
  className: string,
) {
  const shadow = elevation === "raised" ? shadows[variant] : "";
  // `pulse-card` carries no styles; it is a stable hook for tests.
  return `pulse-card min-w-0 ${radii[variant][size]} ${fills[variant]} ${shadow} ${paddings[padding]} ${className}`;
}

/** Dashboard-style card: rounded, filled and raised by default. The design system owns its shape and padding scale. */
export default function PulseCard({ as: Component = "div", variant, elevation, size, padding, className = "", ...props }: PulseCardProps) {
  return <Component {...props} className={cardClassName({ variant, elevation, size, padding }, className)} />;
}

/** The same card with native Next.js navigation and a keyboard focus ring. */
export function PulseCardLink({ variant, elevation, size, padding, className = "", ...props }: PulseCardLinkProps) {
  return (
    <Link
      {...props}
      className={`${cardClassName({ variant, elevation, size, padding }, "")} focus-visible:outline-3 focus-visible:outline-offset-4 focus-visible:outline-ring ${className}`}
    />
  );
}
