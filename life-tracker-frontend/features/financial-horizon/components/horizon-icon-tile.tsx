import type { ReactNode } from "react";

export default function HorizonIconTile({ children }: { children: ReactNode }) {
  return (
    <span className="grid size-11 shrink-0 place-items-center rounded-control bg-brand-blue/15 text-brand-blue">
      {children}
    </span>
  );
}
