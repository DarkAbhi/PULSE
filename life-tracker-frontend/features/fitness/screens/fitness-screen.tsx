import "@/features/fitness/styles/fitness-theme.css";
import { apiBaseURL, apiFetch } from "@/lib/api/server-api";
import { redirect } from "next/navigation";
import { TopAppBar } from "@/features/notifications/server";
import { frame } from "@/components/layout/page-frame";
import FitnessCalendar from "@/features/fitness/components/fitness-calendar";

import { FitnessSummary, FitnessStats, RecentWorkouts } from "@/features/fitness/components/fitness-overview";
import WorkoutSchedule from "@/features/fitness/components/workout-schedule";
import type { WorkoutScheduleDay } from "@/features/fitness/types/workout-types";
import type { FitnessOverview } from "@/features/fitness/types/fitness-types";

export async function FitnessScreen() {
  const [response, scheduleResponse] = await Promise.all([
    apiFetch(`${apiBaseURL}/api/fitness/overview`, { signal: AbortSignal.timeout(10_000) }).catch(() => null),
    apiFetch(`${apiBaseURL}/api/fitness/workout-schedule`, { signal: AbortSignal.timeout(10_000) }).catch(() => null),
  ]);
  if (response?.status === 401) redirect("/");

  const overview: FitnessOverview | null = response?.ok ? await response.json().catch(() => null) : null;
  const schedule: WorkoutScheduleDay[] | null = scheduleResponse?.ok ? await scheduleResponse.json().catch(() => null) : null;
  const error = response
    ? "We couldn't load your workouts. Please try again."
    : "We couldn't reach the server. Please try again.";

  return (
    <main id="main-content" tabIndex={-1} className={frame.page}>
      <div className={frame.content}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" />
        <div className="mt-3">
          <h1 className={overview ? "sr-only" : "mb-4 text-3xl font-bold tracking-tight sm:text-4xl"}>Fitness</h1>
          {overview ? (
            <FitnessCalendar
              today={overview.today}
              workoutsByDate={overview.calendar_workouts}
              schedule={schedule ?? []}
              summary={<FitnessSummary
                last={overview.cards.find(({ id }) => id === "last_workout")}
                streak={overview.cards.find(({ id }) => id === "streak")}
                today={overview.today}
                workoutDates={new Set(Object.keys(overview.calendar_workouts))}
              />}
            >
              {schedule ? <WorkoutSchedule schedule={schedule} today={overview.today} /> : (
                <section aria-labelledby="schedule-error-heading">
                  <h2 id="schedule-error-heading" className="mb-3 text-2xl font-extrabold tracking-tight">Workout schedule</h2>
                  <p className="text-sm text-destructive" role="alert">We couldn't load your workout schedule. Please try again.</p>
                </section>
              )}
              <RecentWorkouts recentWorkouts={overview.recent_workouts} />
              <FitnessStats cards={overview.cards.filter(({ id, value }) => id !== "last_workout" && id !== "streak" && value !== "Not available")} />
            </FitnessCalendar>
          ) : (
            <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
              {error}
            </p>
          )}
        </div>
      </div>
    </main>
  );
}
