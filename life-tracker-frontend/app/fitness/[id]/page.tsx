import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Watch } from "lucide-react";
import DeleteVisitButton from "./delete-visit-button";
import DeleteExerciseButton from "./delete-exercise-button";
import AddExerciseForm from "./add-exercise-form";
import { formatDuration } from "./format-duration.mjs";
import type { CatalogExercise } from "./actions";

export const metadata = {
  title: "Fitness Workout | Life Tracker",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

type SavedExercise = {
  id: number;
  name: string;
  exercise_catalog_id: string | null;
  catalog_exercise: CatalogExercise | null;
  sets: Array<{
    id: number;
    set_number: number;
    reps: number;
    weight: number | null;
  }>;
};

type GymVisit = {
  id: number;
  created_at: string;
  start_time: string | null;
  end_time: string | null;
  duration_seconds: number | null;
  calories_burned: number | null;
  synced_from_apple_watch: boolean;
};

const indiaTimeZone = "Asia/Kolkata";

const visitDateFormatter = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "full",
  timeZone: indiaTimeZone,
});

const workoutTimeFormatter = new Intl.DateTimeFormat("en-IN", {
  timeStyle: "short",
  timeZone: indiaTimeZone,
});
const workoutNumberFormatter = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 3 });

const visitDateKeyFormatter = new Intl.DateTimeFormat("en-CA", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  timeZone: indiaTimeZone,
});

function isToday(date: Date) {
  return visitDateKeyFormatter.format(date) === visitDateKeyFormatter.format(new Date());
}

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function GymVisitPage({ params }: PageProps) {
  const { id: visitID } = await params;

  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on the server
  const sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!sessionResponse.ok) {
    redirect("/");
  }

  let exercises: SavedExercise[] = [];
  let error = "";
  let workoutTitle = "Workout";
  let visit: GymVisit | null = null;
  let detailsError = "";
  let visitNotFound = false;

  try {
    const [exercisesResponse, visitResponse] = await Promise.all([
      fetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises`, {
        headers: {
          Cookie: cookieHeader,
        },
      }),
      fetch(`${apiBaseURL}/api/fitness-workouts/${visitID}`, {
        headers: {
          Cookie: cookieHeader,
        },
      }),
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
      workoutTitle = isToday(visitDate)
        ? "Today's workout"
        : visitDateFormatter.format(visitDate);
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

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto grid max-w-6xl gap-10 lg:grid-cols-[1.15fr_0.85fr]">
        <section>
          <div className="flex items-start justify-between gap-4">
            <Link
              className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit"
              href="/fitness"
            >
              <ArrowLeft className="h-4 w-4" /> Fitness
            </Link>
            <DeleteVisitButton visitID={visitID} />
          </div>
          <p className="mt-5 text-sm font-semibold tracking-[0.18em] text-primary uppercase">
            Fitness Workout
          </p>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            {workoutTitle}
          </h1>
          {visit?.synced_from_apple_watch && (
            <p className="mt-3 flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <Watch aria-hidden="true" className="h-4 w-4" />
              Synced from Apple Watch
            </p>
          )}
          <p className="mt-3 text-base text-muted-foreground">
            Capture what you did, one exercise and set at a time.
          </p>

          {hasWorkoutDetails && <section aria-labelledby="workout-details-heading" className="mt-8 rounded-2xl border border-border bg-card p-6 shadow-sm">
            <h2 id="workout-details-heading" className="text-xl font-semibold">Workout details</h2>
            <p className="mt-1 text-xs text-muted-foreground">Times shown in India time (IST).</p>
            {detailsError ? (
              <p className="mt-4 text-sm text-destructive" role="alert">{detailsError}</p>
            ) : visit && (
              <>
                <dl className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-2">
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
                {visit.start_time == null && (
                  <p className="mt-5 text-sm text-muted-foreground">No timing or calorie data is linked to this workout yet.</p>
                )}
              </>
            )}
          </section>}

          <div className="mt-8 space-y-4">
            {error ? (
              <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
                {error}
              </p>
            ) : exercises.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-border bg-card p-8">
                <p className="font-semibold text-foreground">
                  No exercises saved yet.
                </p>
                <p className="mt-2 text-sm text-muted-foreground">
                  Add your first exercise using the form.
                </p>
              </div>
            ) : (
              exercises.map((exercise) => (
                <article
                  className="rounded-2xl border border-border bg-card p-6 shadow-sm"
                  key={exercise.id}
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <h2 className="min-w-0 break-words text-xl font-semibold text-foreground">
                      {exercise.name}
                    </h2>
                    <DeleteExerciseButton visitID={visitID} exerciseID={exercise.id} exerciseName={exercise.name} />
                  </div>
                  {exercise.catalog_exercise && <div className="mt-2 text-sm text-muted-foreground">
                    <p>{exercise.catalog_exercise.name} · {exercise.catalog_exercise.equipment ?? "Equipment unspecified"}</p>
                    <p className="mt-1">{exercise.catalog_exercise.data.primaryMuscles.join(", ")}</p>
                    <details className="mt-3">
                      <summary className="cursor-pointer">Exercise instructions</summary>
                      <ol className="mt-2 list-decimal space-y-2 pl-5">
                        {exercise.catalog_exercise.data.instructions.map((instruction, index) => <li key={index}>{instruction}</li>)}
                      </ol>
                    </details>
                  </div>}
                  <div className="mt-4 overflow-hidden rounded-lg border border-border">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-muted text-muted-foreground">
                        <tr>
                          <th className="px-4 py-3 font-medium">Set</th>
                          <th className="px-4 py-3 font-medium">Reps</th>
                          <th className="px-4 py-3 font-medium">Weight</th>
                        </tr>
                      </thead>
                      <tbody>
                        {exercise.sets.map((set) => (
                          <tr
                            className="border-t border-border"
                            key={set.id}
                          >
                            <td className="px-4 py-3 text-muted-foreground">{set.set_number}</td>
                            <td className="px-4 py-3 text-muted-foreground">{set.reps}</td>
                            <td className="px-4 py-3 text-muted-foreground">
                              {set.weight === null
                                ? "Bodyweight"
                                : `${set.weight} kg`}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </article>
              ))
            )}
          </div>
        </section>

        <AddExerciseForm visitID={visitID} />
      </div>
    </main>
  );
}
