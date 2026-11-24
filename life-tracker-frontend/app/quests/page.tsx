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
      className={`quest-page bg-(--quest-canvas) text-(--quest-text) ${frame.page}`}>
      <div className="mx-auto max-w-[1200px]">
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" />
        <div className="mt-5 grid gap-5 sm:mt-7">
          <header className="flex flex-wrap items-center justify-between gap-4">
            <h1 className="flex items-center gap-3.5 text-[clamp(2rem,4.4vw,3rem)] leading-none font-extrabold tracking-[-0.045em]">
              <span className="grid size-[clamp(44px,5vw,56px)] shrink-0 place-items-center rounded-2xl bg-(image:--quest-gradient) text-white shadow-[0_14px_28px_-14px_#5b2ce0e6]">
                <Gamepad2 className="size-3/5" aria-hidden="true" />
              </span>
              Pulse Quest
            </h1>
            {overview && (
              <div className="flex flex-wrap items-center gap-2.5">
                <div className="flex min-h-11 items-center gap-2 rounded-full bg-(--quest-panel) px-3.5 font-semibold shadow-(--quest-shadow) sm:pr-[18px]">
                  <Zap className="size-5 fill-current text-(--quest-gold)" aria-hidden="true" />
                  <span className="text-(--quest-muted)">XP</span>
                  <strong className="text-lg tabular-nums">{overview.progress.lifetime_xp.toLocaleString("en")}</strong>
                </div>
                <div className="flex min-h-11 items-center gap-2 rounded-full bg-(--quest-panel) px-3.5 font-semibold shadow-(--quest-shadow) sm:pr-[18px]">
                  <Heart className="size-5 fill-current text-(--quest-pink)" aria-hidden="true" />
                  <span className="text-(--quest-muted)">Hearts</span>
                  <strong className="text-lg tabular-nums">{overview.progress.hearts.toLocaleString("en")}</strong>
                </div>
                {overview.configured && <QuestSettingsButton {...overview} />}
              </div>
            )}
          </header>
          {overview ? (
            <>
              <RefreshQuests resetAt={overview.next_reset_at} />
              <div className="grid gap-5 min-[701px]:grid-cols-2 min-[1051px]:grid-cols-3">
                <StreakCard streak={overview.progress.streak} />
                <QuestWeek overview={overview} />
                <TodayXP progress={overview.progress} />
              </div>
              <div className="grid gap-5 min-[1051px]:grid-cols-[minmax(0,1.7fr)_minmax(0,1fr)]">
                <DailyQuests overview={overview} />
                <aside aria-label="Quest achievements" className="grid min-w-0 grid-rows-[auto_1fr] gap-5">
                  <StreakMedals bestStreak={overview.progress.best_streak} />
                  <QuestProgress overview={overview} />
                </aside>
              </div>
            </>
          ) : (
            <p role="alert" className="rounded-[28px] bg-(--quest-panel) p-6 shadow-(--quest-shadow)">
              We couldn’t load your quests. Refresh the page to try again.
            </p>
          )}
        </div>
      </div>
    </main>
  );
}
