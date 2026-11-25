import "@/app/fitness/workout-theme.css";

import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { getUserTimeZone } from "@/app/lib/user-time-zone";
import { redirect } from "next/navigation";
import { frame } from "@/app/components/design-system/page-frame";
import PulseCard from "@/app/components/design-system/pulse-card";
import TopAppBar from "@/app/components/top-app-bar";
import { Clock, Dumbbell, Flag, Flame, List, Timer, Watch } from "lucide-react";
import { AddExerciseButton, AddExerciseProvider } from "@/app/fitness/[id]/add-exercise-form";
import EditWorkoutButton from "@/app/fitness/[id]/edit-workout-button";
import { workoutLabel, type WorkoutClassification } from "@/app/fitness/workout-types";
import DeleteVisitButton from "@/app/fitness/[id]/delete-visit-button";
import ExerciseCard from "@/app/fitness/[id]/exercise-card";
import { formatDuration } from "@/app/fitness/[id]/format-duration.mjs";
import type { SavedExercise } from "@/app/fitness/[id]/actions";

export const metadata = {
  title: "Fitness Workout | PULSE",
};

type GymVisit = WorkoutClassification & {
  id: number;
  created_at: string;
  start_time: string | null;
  end_time: string | null;
  duration_seconds: number | null;
  calories_burned: number | null;
  synced_from_apple_watch: boolean;
};

const numberFormat = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 });

const weekdayClass = "block text-[clamp(1.4rem,3vw,2.2rem)] leading-[1.1] font-extrabold tracking-[-0.03em]";
const tagClass = "flex items-center gap-2 rounded-full border-[1.5px] px-3.5 py-[5px] text-[0.9rem] font-bold [&_svg]:size-4";
const exerciseNoticeClass = "grid justify-items-center gap-3.5 rounded-card border-[1.5px] border-dashed border-divider px-6 py-9 text-center";
const sectionDescriptionClass = "mt-1 text-control text-muted-foreground";

function WorkoutSummary({ visit, timeZone, error }: {
  visit: GymVisit | null;
  timeZone: string;
  error: string;
}) {
  const classification = visit && workoutLabel(visit);
  const date = visit ? new Date(visit.start_time ?? visit.created_at) : null;
  const dateParts = date && new Intl.DateTimeFormat("en-IN", {
    weekday: "long", day: "numeric", month: "long", year: "numeric", timeZone,
  }).formatToParts(date);
  const day = dateParts && dateParts.find((part) => part.type === "day")?.value;
  const weekday = dateParts && dateParts.find((part) => part.type === "weekday")?.value;
  const monthYear = dateParts && dateParts
    .filter((part) => part.type === "month" || part.type === "year")
    .map((part) => part.value).join(" ");
  const timeFormat = new Intl.DateTimeFormat("en-IN", { timeStyle: "short", timeZone });
  const metrics = visit ? [
    { label: "Start", Icon: Clock, value: visit.start_time && timeFormat.format(new Date(visit.start_time)) },
    { label: "End", Icon: Flag, value: visit.end_time && timeFormat.format(new Date(visit.end_time)) },
    { label: "Duration", Icon: Timer, value: visit.duration_seconds == null ? null : formatDuration(visit.duration_seconds, "short") },
    { label: "Calories burned", Icon: Flame, value: visit.calories_burned == null ? null : numberFormat.format(visit.calories_burned), unit: "kcal" },
  ] : [];

  return (
    <header className="relative isolate grid gap-[clamp(20px,3vw,30px)] overflow-hidden rounded-[40px] bg-(image:--workout-hero-gradient) p-[clamp(22px,3vw,36px)] text-white shadow-(--purple-hero-shadow)">
      <svg className="absolute -top-2.5 -right-6 -z-10 size-[300px] fill-none stroke-white opacity-10" strokeWidth={1} strokeLinecap="round" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M6.5 6.5v11M17.5 6.5v11M3.5 9v6M20.5 9v6M6.5 12h11" />
      </svg>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 flex-wrap items-center gap-2">
          <span className={`${tagClass} border-white/40 bg-white/20`}><Dumbbell aria-hidden="true" />Gym visit</span>
          {visit?.synced_from_apple_watch && (
            <span className={`${tagClass} border-transparent bg-white/95 text-(--workout-synced-text)`}>
              <Watch aria-hidden="true" />Synced from watch
            </span>
          )}
          {classification && <span className={`${tagClass} min-w-0 border-white/40 bg-white/20 wrap-anywhere`}>{classification}</span>}
        </div>
        {visit && (
          <EditWorkoutButton visitID={visit.id}
            classification={{ workout_type: visit.workout_type, workout_focus: visit.workout_focus }} />
        )}
      </div>
      {date ? (
        <h1 className="flex items-center gap-[clamp(14px,2.4vw,24px)]">
          <span className="text-[clamp(4rem,10vw,7rem)] leading-[0.85] font-extrabold tracking-[-0.06em] tabular-nums">{day}</span>
          <span>
            <span className={weekdayClass}>{weekday}</span>
            <span className="mt-1 block text-[clamp(1.05rem,2vw,1.4rem)] font-semibold opacity-90">{monthYear}</span>
          </span>
        </h1>
      ) : <h1 className={weekdayClass}>Workout</h1>}
      {metrics.some(({ value }) => value !== null) && (
        <dl className="grid grid-cols-4 rounded-[22px] border border-white/22 bg-white/14 max-[701px]:grid-cols-2">
          {metrics.map(({ label, Icon, value, unit }) => (
            <div key={label} className="border-white/22 px-[18px] py-3.5 not-first:border-s max-[701px]:nth-3:border-s-0 max-[701px]:nth-[n+3]:border-t">
              <dt className="flex items-center gap-1.5 text-caption font-bold tracking-[0.06em] uppercase opacity-85">
                <Icon size={14} className="shrink-0" aria-hidden="true" />{label}
              </dt>
              <dd className="mt-1 text-[clamp(1.15rem,2vw,1.5rem)] font-extrabold tracking-[-0.02em] tabular-nums">
                {value ?? <span aria-label="Not available">—</span>}
                {value !== null && unit && <small className="ms-1 text-[0.6em] font-semibold tracking-normal opacity-85">{unit}</small>}
              </dd>
            </div>
          ))}
        </dl>
      )}
      {error && <p role="alert">{error}</p>}
    </header>
  );
}

export default async function GymVisitPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: visitID } = await params;
  const timeZonePromise = getUserTimeZone();

  let exercises: SavedExercise[] = [];
  let visit: GymVisit | null = null;
  let error = "";
  let detailsError = "";
  let visitNotFound = false;
  let unauthorized = false;

  try {
    const [exercisesResponse, visitResponse] = await Promise.all([
      apiFetch(`${apiBaseURL}/api/gym-visits/${encodeURIComponent(visitID)}/exercises`, { signal: AbortSignal.timeout(10_000) }),
      apiFetch(`${apiBaseURL}/api/fitness-workouts/${encodeURIComponent(visitID)}`, { signal: AbortSignal.timeout(10_000) }),
    ]);
    unauthorized = exercisesResponse.status === 401 || visitResponse.status === 401;
    visitNotFound = exercisesResponse.status === 404 || visitResponse.status === 404;
    if (exercisesResponse.ok) {
      exercises = ((await exercisesResponse.json()) as SavedExercise[] | null) ?? [];
    } else {
      error = "We couldn't load this workout's exercises. Please try again.";
    }

    if (visitResponse.ok) {
      visit = (await visitResponse.json()) as GymVisit;
    } else {
      detailsError = "We couldn't load this workout's details. Please try again.";
    }
  } catch {
    error = "We couldn't reach the server. Please try again.";
  }

  if (unauthorized) redirect("/");
  if (visitNotFound) redirect("/fitness");

  const timeZone = await timeZonePromise;
  const sets = exercises.flatMap((exercise) => exercise.sets);
  const volume = sets.reduce((total, set) => total + set.reps * (set.weight ?? 0), 0);
  const exerciseCount = `${exercises.length} ${exercises.length === 1 ? "exercise" : "exercises"}`;
  const setCount = `${sets.length} ${sets.length === 1 ? "set" : "sets"}`;

  return (
    <main id="main-content" tabIndex={-1} className={`${frame.page} leading-snug`}>
      <div className={`${frame.content} grid min-w-0 gap-8`}>
        <TopAppBar backHref="/fitness" backLabel="Back to Fitness" />
        <WorkoutSummary visit={visit} timeZone={timeZone} error={detailsError} />

        <AddExerciseProvider visitID={visitID}>
          <section className="grid min-w-0 gap-5" aria-labelledby="exercises-heading">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-divider pb-5">
              <div>
                <h2 id="exercises-heading" tabIndex={-1} className="flex items-center gap-2.5 text-[clamp(1.3rem,2.2vw,1.65rem)] leading-[1.2] font-extrabold tracking-[-0.03em]">
                  <List size={26} className="shrink-0 text-brand" aria-hidden="true" />Exercises
                </h2>
                {!error && (
                  <p className={sectionDescriptionClass}>{exercises.length
                    ? [exerciseCount, setCount, volume > 0 && `${numberFormat.format(volume)} kg lifted`].filter(Boolean).join(" · ")
                    : "Nothing added to this workout yet"}
                  </p>
                )}
              </div>
              <AddExerciseButton />
            </div>
            {error ? (
              <p className={`${exerciseNoticeClass} text-destructive`} role="alert">{error}</p>
            ) : exercises.length ? (
              <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,440px),1fr))] gap-5">
                {exercises.map((exercise) => <ExerciseCard key={exercise.id} visitID={visitID} exercise={exercise} />)}
              </div>
            ) : (
              <div className={`${exerciseNoticeClass} text-muted-foreground`}>
                <p>No exercises yet. Add your first exercise to start logging sets.</p>
                <AddExerciseButton />
              </div>
            )}
          </section>
        </AddExerciseProvider>

        <PulseCard as="section" padding="md" className="flex flex-wrap items-center justify-between gap-4" aria-labelledby="delete-workout-heading">
          <div>
            <h2 id="delete-workout-heading" className="text-[1.15rem] font-extrabold text-destructive">Delete workout</h2>
            <p className={sectionDescriptionClass}>Permanently removes this workout and all of its exercises and sets.</p>
          </div>
          <DeleteVisitButton visitID={visitID} />
        </PulseCard>
      </div>
    </main>
  );
}
