import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { Car, ArrowRight } from "lucide-react";
import GreetingHeader from "./greeting-header";
import GymVisitCard from "./gym-visit-card";
import NotificationDropdown from "./notification-dropdown";
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
          <div className="flex shrink-0 items-center gap-3">
            <NotificationDropdown initialNotifications={notifications} />
            <Link
              className="group rounded-full border border-border bg-card p-1 shadow-sm transition hover:shadow-md hover:border-primary/50"
              href="/profile"
              aria-label="Profile"
            >
              <img
                alt="Profile placeholder"
                className="h-12 w-12 rounded-full object-cover transition duration-200 group-hover:scale-105"
                src="/avatar-placeholder.jpg"
              />
            </Link>
          </div>
        </header>

        <section aria-labelledby="categories-heading">
          <h2
            id="categories-heading"
            className="mb-4 text-lg font-semibold text-foreground"
          >
            Your spaces
          </h2>
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


      </div>

      {needsProfile && (
        <NamePromptDialog initialDisplayName={displayName} />
      )}
    </main>
  );
}
