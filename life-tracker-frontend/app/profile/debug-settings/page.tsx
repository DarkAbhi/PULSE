import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { frame } from "@/app/components/design-system/page-frame";
import TopAppBar from "@/app/components/top-app-bar";
import ComponentLibrary from "@/app/profile/debug-settings/component-library";
import { getDebugSettingsStatus } from "@/app/profile/debug-settings/debug-settings";
import QuestHistorySeed from "@/app/profile/debug-settings/quest-history-seed";

export const metadata = { title: "Debug settings | PULSE" };

const tabs = [
  { id: "history", label: "Seed history", href: "/profile/debug-settings" },
  { id: "components", label: "Component library", href: "/profile/debug-settings?tab=components" },
] as const;

export default async function DebugSettingsPage({ searchParams }: {
  searchParams: Promise<{ tab?: string | string[] }>;
}) {
  const [status, { tab }] = await Promise.all([getDebugSettingsStatus(), searchParams]);
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
