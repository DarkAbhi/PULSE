import "@/app/dashboard/dashboard-theme.css";
import { getSession } from "@/app/lib/session";
import { getDashboardData } from "@/app/dashboard/data";
import { frame } from "@/app/components/design-system/page-frame";
import { redirect } from "next/navigation";
import Link from "next/link";
import { Suspense } from "react";
import { ArrowRight, Gamepad2, UserRound } from "lucide-react";
import DashboardFrame from "@/app/dashboard/dashboard-frame";
import GreetingHeader from "@/app/dashboard/greeting-header";
import GymVisitCard from "@/app/dashboard/gym-visit-card";
import NotificationDropdown from "@/app/components/notification-dropdown";
import NamePromptDialog from "@/app/dashboard/name-prompt-dialog";
import FinancialHorizonCard from "@/app/dashboard/financial-horizon-card";
import SectionTiles from "@/app/dashboard/section-tiles";
import { FitnessAtAGlance, FitnessAtAGlanceSkeleton } from "@/app/dashboard/fitness-at-a-glance";
import { RecentActivity, RecentActivitySkeleton } from "@/app/dashboard/recent-activity";
import PulseCard from "@/app/components/design-system/pulse-card";
import QuestHeatmap from "@/app/dashboard/quest-heatmap";
import { getUserCurrency } from "@/app/lib/user-currency";
import { getUserTimeZone } from "@/app/lib/user-time-zone";

const dashboardClass =
  "bg-background text-foreground [&_:is(a,button)]:[-webkit-tap-highlight-color:transparent] " +
  "[&_:is(a,button):focus-visible]:outline-3 [&_:is(a,button):focus-visible]:outline-ring " +
  "[&_:is(a,button):focus-visible]:outline-offset-4";

export const metadata = {
  title: "Dashboard | PULSE",
};

type DashboardData = ReturnType<typeof getDashboardData>;

const cardPlaceholderClass = "min-h-50 rounded-card bg-card dashboard-compact:min-h-36";

async function DashboardGreeting({ username, profile, notifications }: {
  username: string;
  profile: DashboardData["profile"];
  notifications: DashboardData["notifications"];
}) {
  const data = await profile;
  return (
    <>
      <GreetingHeader name={data?.name || username}>
        <Suspense fallback={<Link href="/notifications" className="inline-flex min-h-11 items-center px-3">Notifications</Link>}>
          <DashboardNotifications notifications={notifications} />
        </Suspense>
        <Link className="inline-flex items-center justify-center size-11 rounded-full text-inherit hover:bg-white/15" href="/profile" aria-label="Profile">
          <UserRound className="h-5 w-5" aria-hidden="true" />
        </Link>
      </GreetingHeader>
      {data?.has_profile === false && <NamePromptDialog initialDisplayName={data.name ?? ""} />}
    </>
  );
}

async function DashboardNotifications({ notifications }: { notifications: DashboardData["notifications"] }) {
  return <NotificationDropdown initialNotifications={await notifications ?? []} />;
}

async function DashboardBalance({ horizon }: { horizon: DashboardData["horizon"] }) {
  return <FinancialHorizonCard summary={await horizon} />;
}

async function DashboardWorkout({ workout }: { workout: DashboardData["workout"] }) {
  return <GymVisitCard visited={(await workout)?.visited ?? null} />;
}

async function DashboardFitness({ fitness }: { fitness: DashboardData["fitness"] }) {
  return <FitnessAtAGlance data={await fitness} />;
}

async function DashboardRecentActivity({ activity }: { activity: DashboardData["recentActivity"] }) {
  const [entries, currency, timeZone] = await Promise.all([activity, getUserCurrency(), getUserTimeZone()]);
  return <RecentActivity entries={entries} currency={currency} timeZone={timeZone} />;
}

async function DashboardQuestActivity({ activity }: { activity: DashboardData["questActivity"] }) {
  const data = await activity;
  return data ? (
    <QuestHeatmap activity={data} />
  ) : (
    <p className="text-sm text-muted-foreground">Quest history is temporarily unavailable.</p>
  );
}

export default async function DashboardPage() {
  const session = await getSession();
  if (!session) redirect("/");

  const data = getDashboardData();

  return (
    <DashboardFrame className={`${dashboardClass} ${frame.page}`}>
      <div className={`grid gap-6 dashboard-compact:gap-4 ${frame.content}`}>
        <Suspense fallback={<GreetingHeader name={session.username} />}>
          <DashboardGreeting username={session.username} profile={data.profile} notifications={data.notifications} />
        </Suspense>
        <div className="@container grid gap-6 dashboard-compact:gap-3 dashboard-compact:md:auto-rows-fr dashboard-compact:xl:grid-cols-4">
          <section
            className="grid gap-6 items-stretch md:grid-cols-2 dashboard-compact:gap-3 dashboard-compact:xl:col-span-2 dashboard-compact:xl:grid-cols-subgrid dashboard-compact:xl:grid-rows-subgrid"
            aria-label="Your day at a glance"
          >
            <Suspense fallback={<div className={cardPlaceholderClass} aria-busy="true" />}>
              <DashboardBalance horizon={data.horizon} />
            </Suspense>
            <Suspense fallback={<div className={cardPlaceholderClass} aria-busy="true" />}>
              <DashboardWorkout workout={data.workout} />
            </Suspense>
          </section>
          <SectionTiles />
        </div>
        <Suspense fallback={<FitnessAtAGlanceSkeleton />}>
          <DashboardFitness fitness={data.fitness} />
        </Suspense>
        <PulseCard as="section" aria-labelledby="dashboard-quest-heading"
          className="grid min-w-0 gap-3 border border-border bg-card p-4 dashboard-compact:gap-2">
          <header className="flex flex-wrap items-center justify-between gap-x-3">
            <h2 id="dashboard-quest-heading" className="flex items-center gap-2.5 text-xl font-extrabold tracking-tight">
              <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-(--dashboard-metric-icon-surface) text-(--dashboard-metric-icon)">
                <Gamepad2 className="size-5" aria-hidden="true" />
              </span>
              Quest activity
            </h2>
            <Link href="/quests" prefetch={false}
              className="inline-flex min-h-11 items-center gap-1.5 rounded-md text-sm font-semibold text-muted-foreground hover:text-foreground">
              View quests <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          </header>
          <Suspense fallback={<div className="h-52 rounded-lg bg-foreground/5" role="status"><span className="sr-only">Loading quest activity</span></div>}>
            <DashboardQuestActivity activity={data.questActivity} />
          </Suspense>
        </PulseCard>
        <Suspense fallback={<RecentActivitySkeleton />}>
          <DashboardRecentActivity activity={data.recentActivity} />
        </Suspense>
      </div>
    </DashboardFrame>
  );
}
