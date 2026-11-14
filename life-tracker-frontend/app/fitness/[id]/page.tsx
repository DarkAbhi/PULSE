import { apiFetch } from "@/app/lib/server-api";
import PulseCard from "@/app/components/design-system/pulse-card";
import { getUserTimeZone } from "@/app/lib/user-time-zone";
import { redirect } from "next/navigation";
import BackLink from "@/app/components/design-system/back-link";
import { Watch } from "lucide-react";
import DeleteVisitButton from "./delete-visit-button";
import DeleteExerciseButton from "./delete-exercise-button";
import EditExerciseButton from "./edit-exercise-button";
import AddExerciseForm, { AddExerciseShortcut } from "./add-exercise-form";
import { formatDuration } from "./format-duration.mjs";
import type { SavedExercise } from "./actions";

export const metadata = {
  title: "Fitness Workout | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

type GymVisit = {
  id: number;
  created_at: string;
  start_time: string | null;
  end_time: string | null;
  duration_seconds: number | null;
  calories_burned: number | null;
  synced_from_apple_watch: boolean;
};

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function GymVisitPage({ params }: PageProps) {
  const timeZone = await getUserTimeZone();

  const visitDateFormatter = new Intl.DateTimeFormat("en-IN", {
    dateStyle: "full",
    timeZone,
  });

  const compactDateFormatter = new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone,
  });

  const workoutTimeFormatter = new Intl.DateTimeFormat("en-IN", {
    timeStyle: "short",
    timeZone,
  });
  const workoutNumberFormatter = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 3 });

  const visitDateKeyFormatter = new Intl.DateTimeFormat("en-CA", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    timeZone,
  });

  function isToday(date: Date) {
    return visitDateKeyFormatter.format(date) === visitDateKeyFormatter.format(new Date());
  }

  const { id: visitID } = await params;

  // Validate session on the server
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) {
    redirect("/");
  }

  let exercises: SavedExercise[] = [];
  let error = "";
  let workoutTitle = "Workout";
  let compactTitle = workoutTitle;
  let visit: GymVisit | null = null;
  let detailsError = "";
  let visitNotFound = false;

  try {
    const [exercisesResponse, visitResponse] = await Promise.all([
      apiFetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises`),
      apiFetch(`${apiBaseURL}/api/fitness-workouts/${visitID}`),
    ]);
    visitNotFound = exercisesResponse.status === 404 || visitResponse.status === 404;
    if (!exercisesResponse.ok) {
      error = "We couldn't load this workout's exercises. Please try again.";
    } else {
      exercises = (await exercisesResponse.json()) as SavedExercise[];
    }

    if (visitResponse.ok) {
      visit = (await visitResponse.json()) as GymVisit;
      const visitDate = new Date(visit.start_time ?? visit.created_at);
      const today = isToday(visitDate);
      workoutTitle = today
        ? "Today's workout"
        : visitDateFormatter.format(visitDate);
      compactTitle = today ? workoutTitle : compactDateFormatter.format(visitDate);
    } else {
      detailsError = "We couldn't load this workout's details. Please try again.";
    }
  } catch {
    error = "We couldn't reach the server. Please try again.";
    detailsError = error;
  }

  if (visitNotFound) redirect("/fitness");

  const hasWorkoutDetails = visit && [
    visit.start_time,
    visit.end_time,
    visit.duration_seconds,
    visit.calories_burned,
  ].some((value) => value != null);

  const showExerciseLog = Boolean(error) || exercises.length > 0;

  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-10 sm:py-10 lg:px-16">
      <div className="mx-auto grid max-w-6xl items-start gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)] lg:auto-rows-min lg:gap-x-10">
        <header className="min-w-0 lg:col-start-1 lg:row-start-1">
          <div className="flex items-start justify-between gap-4">
            <BackLink href="/fitness">Fitness</BackLink>
            <DeleteVisitButton visitID={visitID} />
          </div>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            <span className="sm:hidden">{compactTitle}</span>
            <span className="hidden sm:inline">{workoutTitle}</span>
          </h1>
          {visit?.synced_from_apple_watch && (
            <p className="mt-3 flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <Watch aria-hidden="true" className="h-4 w-4" />
              Synced from fitness watch
            </p>
          )}
          {showExerciseLog && <AddExerciseShortcut />}

          {(hasWorkoutDetails || (detailsError && detailsError !== error)) && <PulseCard as="section" aria-labelledby="workout-details-heading" className="mt-5 p-4 sm:p-6">
            <h2 id="workout-details-heading" className="text-base font-semibold">Workout details</h2>
            {detailsError ? (
              <p className="mt-4 text-sm text-destructive" role="alert">{detailsError}</p>
            ) : visit && (
              <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-3">
                {[
                  ["Start time", visit.start_time == null ? "Not available" : workoutTimeFormatter.format(new Date(visit.start_time))],
                  ["End time", visit.end_time == null ? "Not available" : workoutTimeFormatter.format(new Date(visit.end_time))],
                  ["Duration", formatDuration(visit.duration_seconds)],
                  ["Calories burned", visit.calories_burned == null ? "Not available" : `${workoutNumberFormatter.format(visit.calories_burned)} kcal`],
                ].map(([label, value]) => (
                  <div key={label}>
                    <dt className="text-sm text-muted-foreground">{label}</dt>
                    <dd className="mt-1 font-semibold text-foreground">{value}</dd>
                  </div>
                ))}
              </dl>
            )}
          </PulseCard>}

          {!showExerciseLog && (
            <p className="mt-4 text-sm text-muted-foreground">No exercises saved yet.</p>
          )}
        </header>

        {showExerciseLog && (
          <section className="min-w-0 space-y-3 lg:col-start-1 lg:row-start-2" aria-labelledby="exercises-heading">
            <h2 id="exercises-heading" className="text-xl font-semibold">Exercises</h2>
            {error ? (
              <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
                {error}
              </p>
            ) : (
              exercises.map((exercise) => (
                <PulseCard as="article"
                  className="p-4 sm:p-6"
                  key={exercise.id}
                >
                  <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1">
                    <h3 className="min-w-0 break-words text-lg font-semibold text-foreground">
                      {exercise.name}
                    </h3>
                    <div className="flex items-center gap-1">
                      <EditExerciseButton visitID={visitID} exercise={exercise} />
                      <DeleteExerciseButton visitID={visitID} exerciseID={exercise.id} exerciseName={exercise.name} />
                    </div>
                  </div>
                  {exercise.catalog_exercise && <div className="mt-2 text-sm text-muted-foreground">
                    <p>
                      {exercise.catalog_exercise.name !== exercise.name && `${exercise.catalog_exercise.name} · `}
                      {exercise.catalog_exercise.equipment ?? "Equipment unspecified"}
                    </p>
                    <p className="mt-1">{exercise.catalog_exercise.data.primaryMuscles.join(", ")}</p>
                    <details className="mt-3">
                      <summary className="cursor-pointer">Exercise instructions</summary>
                      <ol className="mt-2 list-decimal space-y-2 pl-5">
                        {exercise.catalog_exercise.data.instructions.map((instruction, index) => <li key={index}>{instruction}</li>)}
                      </ol>
                    </details>
                  </div>}
                  <div className="mt-4 overflow-x-auto rounded-lg">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-muted text-muted-foreground">
                        <tr>
                          <th className="px-2 py-2 sm:px-4 font-medium">Set</th>
                          <th className="px-2 py-2 sm:px-4 font-medium">Reps</th>
                          <th className="px-2 py-2 sm:px-4 font-medium">Weight</th>
                        </tr>
                      </thead>
                      <tbody>
                        {exercise.sets.map((set) => (
                          <tr key={set.id} className="border-t border-divider">
                            <td className="px-2 py-2 sm:px-4 text-muted-foreground">{set.set_number}</td>
                            <td className="px-2 py-2 sm:px-4 text-muted-foreground">{set.reps}</td>
                            <td className="px-2 py-2 sm:px-4 text-muted-foreground">
                              {set.weight === null
                                ? "Bodyweight"
                                : `${set.weight} kg`}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </PulseCard>
              ))
            )}
          </section>
        )}

        <AddExerciseForm visitID={visitID} />
      </div>
    </main>
  );
}
