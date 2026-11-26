import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { frame } from "@/components/layout/page-frame";
import { TopAppBar } from "@/features/notifications/server";
import ComponentLibrary from "@/features/profile/debug-settings/component-library";
import { getDebugSettingsStatus } from "@/features/profile/server/debug-settings";
import QuestHistorySeed from "@/features/profile/debug-settings/quest-history-seed";

const tabs = [
  { id: "history", label: "Seed history", href: "/profile/debug-settings" },
  { id: "components", label: "Component library", href: "/profile/debug-settings?tab=components" },
] as const;

export async function DebugSettingsScreen({ tab }: { tab?: string | string[] }) {
  const status = await getDebugSettingsStatus();
  if (status === 401) redirect("/");
  if (status !== 204) notFound();

  const activeTab = tab === "components" ? "components" : "history";

  return (
    <main id="main-content" tabIndex={-1} className={frame.page}>
      <div className={`${frame.content} grid gap-6 md:gap-8 [&>*]:min-w-0`}>
        <TopAppBar backHref="/profile" backLabel="Back to Profile" />
        <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Debug settings</h1>

        <nav aria-label="Debug settings sections" className="flex w-fit max-w-full gap-1 rounded-full bg-card p-1.5 shadow-card">
          {tabs.map(({ id, href, label }) => (
            <Link
              key={id}
              href={href}
              aria-current={id === activeTab ? "page" : undefined}
              scroll={false}
              className="flex min-h-10 items-center justify-center rounded-full px-3 py-2 text-center text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted hover:text-foreground aria-[current=page]:bg-accent aria-[current=page]:text-accent-foreground sm:px-5 motion-reduce:transition-none"
            >
              {label}
            </Link>
          ))}
        </nav>

        {activeTab === "components" ? <ComponentLibrary /> : <QuestHistorySeed />}
      </div>
    </main>
  );
}
