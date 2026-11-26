import type { ReactNode } from "react";
import { frame } from "@/components/layout/page-frame";

export default function RemindersLayout({ children }: { children: ReactNode }) {
  return (
    <main id="main-content" tabIndex={-1} className={`${frame.page} bg-background text-foreground`}>
      <div className={frame.content}>{children}</div>
    </main>
  );
}
