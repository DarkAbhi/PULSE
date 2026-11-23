import { apiFetch } from "@/app/lib/server-api";
import { getUserTimeZone } from "@/app/lib/user-time-zone";
import { redirect } from "next/navigation";
import { frame } from "@/app/components/design-system/page-frame";
import TopAppBar from "@/app/components/top-app-bar";
import { Clock, Dumbbell, Flag, Flame, List, Timer, Watch } from "lucide-react";
import { AddExerciseButton, AddExerciseProvider } from "@/app/fitness/[id]/add-exercise-form";
import DeleteVisitButton from "@/app/fitness/[id]/delete-visit-button";
import ExerciseCard from "@/app/fitness/[id]/exercise-card";
import { formatDuration } from "@/app/fitness/[id]/format-duration.mjs";
import type { SavedExercise } from "@/app/fitness/[id]/actions";
import { workoutPalette } from "@/app/fitness/[id]/workout-detail-classes";

export const metadata = {
  title: "Fitness Workout | PULSE",
};

const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

type GymVisit = {
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
const exerciseNoticeClass = "grid justify-items-center gap-3.5 rounded-[28px] border-[1.5px] border-dashed border-(--workout-line) px-6 py-9 text-center";
const sectionDescriptionClass = "mt-1 text-[0.95rem] text-(--workout-muted)";

function WorkoutSummary({ visit, timeZone, error }: {
  visit: GymVisit | null;
  timeZone: string;
  error: string;
}) {
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
    <header className="relative isolate grid gap-[clamp(20px,3vw,30px)] overflow-hidden rounded-[40px] bg-[linear-gradient(135deg,#3a2cd9,#7b2ce0_55%,#d41fb0)] p-[clamp(22px,3vw,36px)] text-white shadow-[0_22px_44px_-26px_rgb(91_44_224/90%)]">
      <svg className="absolute -top-2.5 -right-6 -z-10 size-[300px] fill-none stroke-white opacity-10" strokeWidth={1} strokeLinecap="round" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M6.5 6.5v11M17.5 6.5v11M3.5 9v6M20.5 9v6M6.5 12h11" />
      </svg>
      <div className="flex flex-wrap items-center gap-2">
        <span className={`${tagClass} border-white/40 bg-white/20`}><Dumbbell aria-hidden="true" />Gym visit</span>
        {visit?.synced_from_apple_watch && (
          <span className={`${tagClass} border-transparent bg-white/95 text-[#4a2bd0]`}>
            <Watch aria-hidden="true" />Synced from watch
          </span>
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
              <dt className="flex items-center gap-1.5 text-[0.72rem] font-bold tracking-[0.06em] uppercase opacity-85">
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
  const [{ id: visitID }, timeZone] = await Promise.all([params, getUserTimeZone()]);
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) redirect("/");

  let exercises: SavedExercise[] = [];
  let visit: GymVisit | null = null;
  let error = "";
  let detailsError = "";
  let visitNotFound = false;

  try {
    const [exercisesResponse, visitResponse] = await Promise.all([
      apiFetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises`),
      apiFetch(`${apiBaseURL}/api/fitness-workouts/${visitID}`),
    ]);
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

  if (visitNotFound) redirect("/fitness");

  const sets = exercises.flatMap((exercise) => exercise.sets);
  const volume = sets.reduce((total, set) => total + set.reps * (set.weight ?? 0), 0);
  const exerciseCount = `${exercises.length} ${exercises.length === 1 ? "exercise" : "exercises"}`;
  const setCount = `${sets.length} ${sets.length === 1 ? "set" : "sets"}`;

  return (
    <main className={`${frame.page} ${workoutPalette} bg-background leading-[1.4] text-(--workout-ink)`}>
      <div className={`${frame.content} grid min-w-0 gap-[30px]`}>
        <TopAppBar backHref="/fitness" backLabel="Back to Fitness" />
        <WorkoutSummary visit={visit} timeZone={timeZone} error={detailsError} />

        <AddExerciseProvider visitID={visitID}>
          <section className="grid min-w-0 gap-5" aria-labelledby="exercises-heading">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-(--workout-line) pb-5">
              <div>
                <h2 id="exercises-heading" tabIndex={-1} className="flex items-center gap-2.5 text-[clamp(1.3rem,2.2vw,1.65rem)] leading-[1.2] font-extrabold tracking-[-0.03em]">
                  <List size={26} className="shrink-0 text-(--workout-accent)" aria-hidden="true" />Exercises
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
              <p className={`${exerciseNoticeClass} text-(--workout-danger)`} role="alert">{error}</p>
            ) : exercises.length ? (
              <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,440px),1fr))] gap-5">
                {exercises.map((exercise) => <ExerciseCard key={exercise.id} visitID={visitID} exercise={exercise} />)}
              </div>
            ) : (
              <div className={`${exerciseNoticeClass} text-(--workout-muted)`}>
                <p>No exercises yet. Add your first exercise to start logging sets.</p>
                <AddExerciseButton />
              </div>
            )}
          </section>
        </AddExerciseProvider>

        <section className="flex flex-wrap items-center justify-between gap-4 rounded-[28px] border-[1.5px] border-(--workout-danger)/40 bg-(--workout-card) p-6" aria-labelledby="delete-workout-heading">
          <div>
            <h2 id="delete-workout-heading" className="text-[1.15rem] font-extrabold text-(--workout-danger)">Delete workout</h2>
            <p className={sectionDescriptionClass}>Permanently removes this workout and all of its exercises and sets.</p>
          </div>
          <DeleteVisitButton visitID={visitID} />
        </section>
      </div>
    </main>
  );
}
