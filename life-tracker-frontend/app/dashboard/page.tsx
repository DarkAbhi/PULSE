import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { Car, ArrowRight } from "lucide-react";
import GreetingHeader from "./greeting-header";
import GymVisitCard from "./gym-visit-card";
import NotificationCenter from "./notification-center";
import NamePromptDialog from "./name-prompt-dialog";
import FinancialHorizonCard, { HorizonSummary } from "./financial-horizon-card";
import { AppNotification } from "../components/notification-list";

export const metadata = {
  title: "Dashboard | Life Tracker",
};

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

type ProfileResponse = {
  has_profile: boolean;
  name?: string;
};

export default async function DashboardPage() {
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
  const session = (await sessionResponse.json()) as { username: string };

  let profile: ProfileResponse = { has_profile: false };
  let gymVisited = false;
  let gymVisitID: number | null = null;
  let notifications: AppNotification[] = [];
  let notificationsError = "";
  let horizonSummary: HorizonSummary | null = null;
  let horizonError = "";

  try {
    const [profileRes, gymRes, notificationsRes, horizonRes] = await Promise.all([
      fetch(`${apiBaseURL}/api/profile`, {
        headers: { Cookie: cookieHeader },
      }),
      fetch(`${apiBaseURL}/api/workout/today`, {
        headers: { Cookie: cookieHeader },
      }),
      fetch(`${apiBaseURL}/api/notifications?limit=5`, {
        headers: { Cookie: cookieHeader },
      }),
      fetch(`${apiBaseURL}/api/horizon`, {
        headers: { Cookie: cookieHeader },
      }),
    ]);

    if (!profileRes.ok) {
      redirect("/");
    }
    profile = (await profileRes.json()) as ProfileResponse;

    if (gymRes.ok) {
      const gym = (await gymRes.json()) as { visited: boolean; id?: number };
      gymVisited = gym.visited;
      gymVisitID = gym.id ?? null;
    }

    if (notificationsRes.ok) {
      notifications = (await notificationsRes.json()) as AppNotification[];
    } else {
      notificationsError = "We couldn't load your notifications.";
    }

    if (horizonRes.ok) {
      horizonSummary = (await horizonRes.json()) as HorizonSummary;
    } else {
      horizonError = "We couldn't load your financial horizon.";
    }
  } catch {
    redirect("/");
  }

  const username = session.username;
  const displayName = profile.name ?? "";
  const needsProfile = !profile.has_profile;

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        <header className="mb-10 flex items-start justify-between gap-4">
          <GreetingHeader username={username} displayName={displayName} />
          <Link
            className="group shrink-0 rounded-full border border-border bg-card p-1 shadow-sm transition hover:shadow-md hover:border-primary/50"
            href="/profile"
            aria-label="Profile"
          >
            <img
              alt="Profile placeholder"
              className="h-12 w-12 rounded-full object-cover transition duration-200 group-hover:scale-105"
              src="/avatar-placeholder.jpg"
            />
          </Link>
        </header>

        <section aria-labelledby="categories-heading">
          <div className="mb-4 flex items-center justify-between">
            <h2
              className="text-lg font-semibold text-foreground"
              id="categories-heading"
            >
              Your spaces
            </h2>
            <span className="text-sm text-muted-foreground">More coming soon</span>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <Link
              className="group rounded-2xl border border-border bg-card p-6 shadow-sm transition duration-200 hover:-translate-y-1 hover:shadow-lg"
              href="/garage"
            >
              <div
                className="flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-secondary-foreground"
                aria-hidden="true"
              >
                <Car className="h-6 w-6" />
              </div>
              <h3 className="mt-5 text-xl font-semibold text-foreground">
                Garage
              </h3>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Keep the details of your vehicles close at hand.
              </p>
              <span className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-primary transition group-hover:opacity-80">
                Explore garage{" "}
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </span>
            </Link>

            <GymVisitCard initialVisited={gymVisited} initialVisitID={gymVisitID} />

            <FinancialHorizonCard summary={horizonSummary} error={horizonError} />
          </div>
        </section>

        <section className="mt-12" aria-labelledby="notifications-heading">
          <div className="mb-4 flex items-center justify-between gap-4">
            <div>
              <h2
                className="text-lg font-semibold text-foreground"
                id="notifications-heading"
              >
                Notification center
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                Recent updates from your spaces.
              </p>
            </div>
            <Link
              className="shrink-0 flex items-center gap-1.5 text-sm font-semibold text-primary transition hover:opacity-85"
              href="/notifications"
            >
              View all <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
          {notificationsError ? (
            <p
              className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive"
              role="alert"
            >
              {notificationsError}
            </p>
          ) : (
            <NotificationCenter initialNotifications={notifications} />
          )}
        </section>
      </div>

      {needsProfile && (
        <NamePromptDialog initialDisplayName={displayName} />
      )}
    </main>
  );
}
