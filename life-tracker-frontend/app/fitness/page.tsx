import "@/app/fitness/fitness-theme.css";
import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { redirect } from "next/navigation";
import TopAppBar from "@/app/components/top-app-bar";
import { frame } from "@/app/components/design-system/page-frame";
import FitnessCalendar from "@/app/fitness/fitness-calendar";

import { FitnessSummary, FitnessStats, RecentWorkouts } from "@/app/fitness/fitness-overview";
import type { FitnessOverview } from "@/app/fitness/types";

export const metadata = {
  title: "Fitness | PULSE",
};

export default async function FitnessPage() {
  const response = await apiFetch(`${apiBaseURL}/api/fitness/overview`, {
    signal: AbortSignal.timeout(10_000),
  }).catch(() => null);
  if (response?.status === 401) redirect("/");

  const overview: FitnessOverview | null = response?.ok ? await response.json().catch(() => null) : null;
  const error = response
    ? "We couldn't load your workouts. Please try again."
    : "We couldn't reach the server. Please try again.";

  return (
    <main id="main-content" tabIndex={-1} className={frame.page}>
      <div className={frame.content}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" />
        <div className="mt-3">
          {overview ? (
            <FitnessCalendar
              today={overview.today}
              workoutsByDate={overview.calendar_workouts}
              summary={<FitnessSummary
                last={overview.cards.find(({ id }) => id === "last_workout")}
                streak={overview.cards.find(({ id }) => id === "streak")}
                today={overview.today}
                workoutDates={new Set(Object.keys(overview.calendar_workouts))}
              />}
            >
              <RecentWorkouts recentWorkouts={overview.recent_workouts} />
              <FitnessStats cards={overview.cards.filter(({ id, value }) => id !== "last_workout" && id !== "streak" && value !== "Not available")} />
            </FitnessCalendar>
          ) : (
            <>
              <h1 className="mb-4 text-3xl font-bold tracking-tight sm:text-4xl">Fitness</h1>
              <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
                {error}
              </p>
            </>
          )}
        </div>
      </div>
    </main>
  );
}
