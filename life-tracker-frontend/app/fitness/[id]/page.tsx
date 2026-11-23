import { apiFetch } from "@/app/lib/server-api";
import { getUserTimeZone } from "@/app/lib/user-time-zone";
import { redirect } from "next/navigation";
import TopAppBar from "@/app/components/top-app-bar";
import { Clock, Dumbbell, Flag, Flame, List, Timer, Watch } from "lucide-react";
import { AddExerciseButton, AddExerciseProvider } from "./add-exercise-form";
import DeleteVisitButton from "./delete-visit-button";
import ExerciseCard from "./exercise-card";
import { formatDuration } from "./format-duration.mjs";
import type { SavedExercise } from "./actions";
import styles from "./workout-detail.module.css";

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
    <header className={styles.hero}>
      <svg className={styles.watermark} viewBox="0 0 24 24" aria-hidden="true">
        <path d="M6.5 6.5v11M17.5 6.5v11M3.5 9v6M20.5 9v6M6.5 12h11" />
      </svg>
      <div className={styles.tags}>
        <span className={styles.tag}><Dumbbell aria-hidden="true" />Gym visit</span>
        {visit?.synced_from_apple_watch && (
          <span className={`${styles.tag} ${styles.synced}`}>
            <Watch aria-hidden="true" />Synced from watch
          </span>
        )}
      </div>
      {date ? (
        <h1 className={styles.date}>
          <span className={styles.day}>{day}</span>
          <span>
            <span className={styles.weekday}>{weekday}</span>
            <span className={styles.monthYear}>{monthYear}</span>
          </span>
        </h1>
      ) : <h1 className={styles.weekday}>Workout</h1>}
      {metrics.some(({ value }) => value !== null) && (
        <dl className={styles.metrics}>
          {metrics.map(({ label, Icon, value, unit }) => (
            <div key={label}>
              <dt><Icon aria-hidden="true" />{label}</dt>
              <dd>
                {value ?? <span aria-label="Not available">—</span>}
                {value !== null && unit && <small>{unit}</small>}
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
    <main className={`${styles.palette} ${styles.page}`}>
      <div className={styles.content}>
        <TopAppBar backHref="/fitness" backLabel="Back to Fitness" />
        <WorkoutSummary visit={visit} timeZone={timeZone} error={detailsError} />

        <AddExerciseProvider visitID={visitID}>
          <section className={styles.section} aria-labelledby="exercises-heading">
            <div className={styles.sectionHeader}>
              <div>
                <h2 id="exercises-heading" tabIndex={-1}><List size={26} aria-hidden="true" />Exercises</h2>
                {!error && (
                  <p>{exercises.length
                    ? [exerciseCount, setCount, volume > 0 && `${numberFormat.format(volume)} kg lifted`].filter(Boolean).join(" · ")
                    : "Nothing added to this workout yet"}
                  </p>
                )}
              </div>
              <AddExerciseButton />
            </div>
            {error ? (
              <p className={styles.error} role="alert">{error}</p>
            ) : exercises.length ? (
              <div className={styles.exerciseGrid}>
                {exercises.map((exercise) => <ExerciseCard key={exercise.id} visitID={visitID} exercise={exercise} />)}
              </div>
            ) : (
              <div className={styles.empty}>
                <p>No exercises yet. Add your first exercise to start logging sets.</p>
                <AddExerciseButton />
              </div>
            )}
          </section>
        </AddExerciseProvider>

        <section className={styles.danger} aria-labelledby="delete-workout-heading">
          <div>
            <h2 id="delete-workout-heading">Delete workout</h2>
            <p>Permanently removes this workout and all of its exercises and sets.</p>
          </div>
          <DeleteVisitButton visitID={visitID} />
        </section>
      </div>
    </main>
  );
}
