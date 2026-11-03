import { apiFetch } from "@/app/lib/server-api";
import { redirect } from "next/navigation";
import BackLink from "@/app/components/design-system/back-link";
import FitnessCalendar, { type FitnessOverview } from "./fitness-calendar";

export const metadata = {
  title: "Fitness | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export default async function GymVisitsPage() {
  // Validate session on the server
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) {
    redirect("/");
  }

  let overview: FitnessOverview | null = null;
  let error = "";

  try {
    const response = await apiFetch(`${apiBaseURL}/api/fitness/overview`, {
      cache: "no-store",
    });
    if (!response.ok) {
      error = "We couldn't load your workouts. Please try again.";
    } else {
      overview = (await response.json()) as FitnessOverview;
    }
  } catch {
    error = "We couldn't reach the server. Please try again.";
  }

  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-10 sm:py-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        <BackLink href="/dashboard">Dashboard</BackLink>
        <h1 className="mt-3 text-3xl sm:mt-5 font-bold tracking-tight text-foreground sm:text-4xl">
          Fitness
        </h1>
        <div className="mt-5 sm:mt-8">
          {error ? (
            <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
              {error}
            </p>
          ) : (
            overview && <FitnessCalendar overview={overview} />
          )}
        </div>
      </div>
    </main>
  );
}
