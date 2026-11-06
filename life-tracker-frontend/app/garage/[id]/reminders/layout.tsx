import type { ReactNode } from "react";

export default function RemindersLayout({ children }: { children: ReactNode }) {
  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-8 sm:py-10">
      <div className="mx-auto max-w-5xl">{children}</div>
    </main>
  );
}
