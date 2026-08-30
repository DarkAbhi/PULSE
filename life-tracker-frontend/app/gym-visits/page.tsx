import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import GymVisitsList, { GymVisit } from "./gym-visits-list";

export const metadata = {
  title: "Gym History | Life Tracker",
};

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
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
      error = "We couldn't load your gym visits. Please try again.";
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
        <p className="mt-5 text-sm font-semibold tracking-[0.18em] text-primary uppercase">
          Gym Visits
        </p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
          Your gym history
        </h1>
        <p className="mt-3 text-base text-muted-foreground">
          Every visit, ready whenever you want to revisit or complete its workout log.
        </p>

        <div className="mt-10">
          {error ? (
            <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
              {error}
            </p>
          ) : visits.length === 0 ? (
            <section className="rounded-2xl border border-dashed border-border bg-card p-10 text-center">
              <p className="text-lg font-semibold text-foreground">No gym visits yet.</p>
              <p className="mt-2 text-sm text-muted-foreground">
                Mark a gym visit from the dashboard when you&apos;re ready.
              </p>
            </section>
          ) : (
            <GymVisitsList visits={visits} />
          )}
        </div>
      </div>
    </main>
  );
}
