import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { type GymVisit } from "./gym-visits-list";
import FitnessCalendar from "./fitness-calendar";
import { indiaDateKey } from "./calendar-utils.mjs";

export const metadata = {
  title: "Fitness | Life Tracker",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export default async function GymVisitsPage() {
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

  let visits: GymVisit[] = [];
  let error = "";

  try {
    const response = await fetch(`${apiBaseURL}/api/gym-visits`, {
      headers: {
        Cookie: cookieHeader,
      },
    });
    if (!response.ok) {
      error = "We couldn't load your workouts. Please try again.";
    } else {
      visits = (await response.json()) as GymVisit[];
    }
  } catch {
    error = "We couldn't reach the server. Please try again.";
  }

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        <Link
          className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit"
          href="/dashboard"
        >
          <ArrowLeft className="h-4 w-4" /> Dashboard
        </Link>
        <h1 className="mt-5 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
          Fitness
        </h1>
        <p className="mt-3 text-base text-muted-foreground">
          Track your workouts and revisit each session whenever you want.
        </p>

        <div className="mt-10">
          {error ? (
            <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
              {error}
            </p>
          ) : (
            <FitnessCalendar visits={visits} today={indiaDateKey(new Date())} />
          )}
        </div>
      </div>
    </main>
  );
}
