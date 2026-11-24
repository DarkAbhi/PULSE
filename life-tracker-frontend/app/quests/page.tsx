import "@/app/quests/quest-theme.css";
import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { frame } from "@/app/components/design-system/page-frame";
import TopAppBar from "@/app/components/top-app-bar";
import QuestSettingsButton from "@/app/quests/quest-settings";
import RefreshQuests from "@/app/quests/refresh-quests";
import { DailyQuests, QuestProgress, QuestWeek, StreakCard, StreakMedals, TodayXP } from "@/app/quests/quest-panels";
import type { QuestOverview } from "@/app/quests/types";
import { Gamepad2, Heart, Zap } from "lucide-react";
import { redirect } from "next/navigation";

export const metadata = { title: "Pulse Quest | PULSE" };

export default async function QuestPage({ searchParams }: { searchParams: Promise<{ week?: string | string[] }> }) {
  const { week } = await searchParams;
  const requestWeek = typeof week === "string" && /^\d{4}-\d{2}-\d{2}$/.test(week) ? week : "";
  const endpoint = new URL("/api/quests/overview", apiBaseURL);
  if (requestWeek) endpoint.searchParams.set("week", requestWeek);

  let response = await apiFetch(endpoint.toString(), { signal: AbortSignal.timeout(10_000) }).catch(() => null);
  if (response?.status === 400 && requestWeek) {
    endpoint.searchParams.delete("week");
    response = await apiFetch(endpoint.toString(), { signal: AbortSignal.timeout(10_000) }).catch(() => null);
  }
  if (response?.status === 401) redirect("/");
  const overview: QuestOverview | null = response?.ok ? await response.json().catch(() => null) : null;

  return (
    <main id="main-content" tabIndex={-1}
      className={`quest-page bg-radial-[ellipse_at_top_left] from-(--quest-canvas-glow) to-(--quest-canvas) to-70% text-(--quest-text) scheme-dark ${frame.page}`}>
      <div className={frame.content}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" />
        <div className="mt-5 grid gap-5 sm:mt-7">
          <header className="flex flex-wrap items-center justify-between gap-x-5 gap-y-3">
            <h1 className="flex items-center gap-3 text-3xl font-extrabold tracking-tight sm:text-4xl">
              <Gamepad2 className="size-8 shrink-0 text-(--quest-violet) sm:size-10" aria-hidden="true" />
              Pulse Quest
            </h1>
            {overview && (
              <div className="flex flex-wrap items-center gap-2 sm:gap-3">
                <div className="flex min-h-11 items-center gap-2 rounded-full border border-(--quest-border) bg-(--quest-panel) px-3 sm:px-4">
                  <Zap className="size-5 fill-current text-(--quest-gold)" aria-hidden="true" />
                  <span className="text-sm text-(--quest-muted)">XP</span>
                  <strong className="text-lg tabular-nums">{overview.progress.lifetime_xp.toLocaleString("en")}</strong>
                </div>
                <div className="flex min-h-11 items-center gap-2 rounded-full border border-pink-400/30 bg-pink-500/15 px-3 sm:px-4">
                  <Heart className="size-5 fill-current text-(--quest-pink)" aria-hidden="true" />
                  <span className="text-sm text-(--quest-muted)">Hearts</span>
                  <strong className="text-lg tabular-nums">{overview.progress.hearts.toLocaleString("en")}</strong>
                </div>
                {overview.configured && <QuestSettingsButton {...overview} />}
              </div>
            )}
          </header>
          {overview ? (
            <>
              <RefreshQuests resetAt={overview.next_reset_at} />
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-[0.95fr_1.05fr_1.05fr]">
                <StreakCard streak={overview.progress.streak} />
                <QuestWeek overview={overview} />
                <TodayXP progress={overview.progress} />
              </div>
              <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1.55fr)_minmax(0,1fr)]">
                <DailyQuests overview={overview} />
                <aside className="grid gap-4">
                  <StreakMedals bestStreak={overview.progress.best_streak} />
                  <QuestProgress overview={overview} />
                </aside>
              </div>
            </>
          ) : (
            <p role="alert" className="rounded-card border border-(--quest-border) bg-(--quest-panel) p-5">
              We couldn’t load your quests. Refresh the page to try again.
            </p>
          )}
        </div>
      </div>
    </main>
  );
}
