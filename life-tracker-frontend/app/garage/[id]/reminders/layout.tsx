import type { ReactNode } from "react";
import frame from "@/app/components/design-system/page-frame.module.css";

export default function RemindersLayout({ children }: { children: ReactNode }) {
  return (
    <main className={`${frame.page} bg-background text-foreground`}>
      <div className={frame.content}>{children}</div>
    </main>
  );
}
