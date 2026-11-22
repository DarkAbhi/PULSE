import { apiFetch } from "@/app/lib/server-api";
import { redirect } from "next/navigation";
import BackLink from "@/app/components/design-system/back-link";
import frame from "@/app/components/design-system/page-frame.module.css";
import FitnessCalendar, { type FitnessOverview } from "./fitness-calendar";

export const metadata = {
  title: "Fitness | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export default async function FitnessPage() {
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
    <main className={frame.page}>
      <div className={frame.content}>
        <BackLink href="/dashboard">Dashboard</BackLink>
        <div className="mt-3">
          {overview ? <FitnessCalendar overview={overview} /> : (
            <>
              <h1 className="mb-4 text-3xl font-bold tracking-tight sm:text-4xl">Fitness</h1>
              <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
                {error}
              </p>
            </>
          )}
        </div>
      </div>
    </main>
  );
}
