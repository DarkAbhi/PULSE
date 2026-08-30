import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import DeleteVisitButton from "./delete-visit-button";
import AddExerciseForm from "./add-exercise-form";

export const metadata = {
  title: "Gym Visit Detail | Life Tracker",
};

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

type SavedExercise = {
  id: number;
  name: string;
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
};

const indiaTimeZone = "Asia/Kolkata";

const visitDateFormatter = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "full",
  timeZone: indiaTimeZone,
});

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

  try {
    const [exercisesResponse, visitsResponse] = await Promise.all([
      fetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises`, {
        headers: {
          Cookie: cookieHeader,
        },
      }),
      fetch(`${apiBaseURL}/api/gym-visits`, {
        headers: {
          Cookie: cookieHeader,
        },
      }),
    ]);
    if (exercisesResponse.status === 404) {
      redirect("/dashboard");
    }
    if (!exercisesResponse.ok) {
      error = "We couldn't load this gym visit. Please try again.";
    } else {
      exercises = (await exercisesResponse.json()) as SavedExercise[];
    }

    if (visitsResponse.ok) {
      const visit = ((await visitsResponse.json()) as GymVisit[]).find(
        ({ id }) => id === Number(visitID)
      );
      if (visit) {
        const visitDate = new Date(visit.created_at);
        workoutTitle = isToday(visitDate)
          ? "Today's workout"
          : visitDateFormatter.format(visitDate);
      }
    }
  } catch {
    error = "We couldn't reach the server. Please try again.";
  }

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto grid max-w-6xl gap-10 lg:grid-cols-[1.15fr_0.85fr]">
        <section>
          <div className="flex items-start justify-between gap-4">
            <Link
              className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit"
              href="/dashboard"
            >
              <ArrowLeft className="h-4 w-4" /> Dashboard
            </Link>
            <DeleteVisitButton visitID={visitID} />
          </div>
          <p className="mt-5 text-sm font-semibold tracking-[0.18em] text-primary uppercase">
            Gym Visit
          </p>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            {workoutTitle}
          </h1>
          <p className="mt-3 text-base text-muted-foreground">
            Capture what you did, one exercise and set at a time.
          </p>

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
                  <h2 className="text-xl font-semibold text-foreground">
                    {exercise.name}
                  </h2>
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
