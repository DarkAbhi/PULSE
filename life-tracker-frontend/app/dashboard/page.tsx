import "./dashboard.css";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { Car, ArrowRight, Utensils, UserRound } from "lucide-react";
import GreetingHeader from "./greeting-header";
import GymVisitCard from "./gym-visit-card";
import NotificationDropdown from "./notification-dropdown";
import NamePromptDialog from "./name-prompt-dialog";
import FinancialHorizonCard, { HorizonSummary } from "./financial-horizon-card";
import { AppNotification } from "../components/notification-list";

export const metadata = {
  title: "Dashboard | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
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
  let notifications: AppNotification[] = [];
  let horizonSummary: HorizonSummary | null = null;
  let horizonError = "";

  try {
    const [profileRes, gymRes, notificationsRes, horizonRes] =
      await Promise.all([
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
      const gym = (await gymRes.json()) as { visited: boolean };
      gymVisited = gym.visited;
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
    <main className="pulse-dashboard">
      <div className="pulse-dashboard-content">
        <GreetingHeader username={username} displayName={displayName}>
          <NotificationDropdown initialNotifications={notifications} />
          <Link className="pulse-profile-link" href="/profile" aria-label="Profile">
            <UserRound className="h-5 w-5" />
          </Link>
        </GreetingHeader>
        <section className="pulse-dashboard-cards" aria-label="Your day at a glance">
          <FinancialHorizonCard summary={horizonSummary} error={horizonError} />
          <GymVisitCard visited={gymVisited} />
        </section>
        <nav className="pulse-spaces" aria-label="Your spaces">
          <Link className="pulse-space-link" href="/garage">
            <span className="pulse-space-icon"><Car className="h-5 w-5" /></span>
            <span><strong>Garage</strong><span>Vehicles, fuel & maintenance</span></span>
            <ArrowRight className="h-4 w-4" />
          </Link>
          <Link className="pulse-space-link" href="/meal-plan">
            <span className="pulse-space-icon"><Utensils className="h-5 w-5" /></span>
            <span><strong>Meal Plan</strong><span>Weekly meals</span></span>
            <ArrowRight className="h-4 w-4" />
          </Link>
        </nav>
      </div>

      {needsProfile && <NamePromptDialog initialDisplayName={displayName} />}
    </main>
  );
}
