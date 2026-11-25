import "@/app/dashboard/dashboard-theme.css";
import { getSession } from "@/app/lib/session";
import { getDashboardData } from "@/app/dashboard/data";
import { frame } from "@/app/components/design-system/page-frame";
import { redirect } from "next/navigation";
import Link from "next/link";
import { Suspense } from "react";
import { ArrowRight, UserRound } from "lucide-react";
import DashboardFrame from "@/app/dashboard/dashboard-frame";
import GreetingHeader from "@/app/dashboard/greeting-header";
import NotificationDropdown from "@/app/components/notification-dropdown";
import NamePromptDialog from "@/app/dashboard/name-prompt-dialog";
import FinanceCard from "@/app/dashboard/quick-actions/finance-card";
import FitnessCard from "@/app/dashboard/quick-actions/fitness-card";
import GarageCard from "@/app/dashboard/quick-actions/garage-card";
import MealPlanCard from "@/app/dashboard/quick-actions/meal-plan-card";
import { QuickActionCardSkeleton } from "@/app/dashboard/quick-actions/quick-action-card";
import { FitnessAtAGlance, FitnessAtAGlanceSkeleton } from "@/app/dashboard/fitness-at-a-glance";
import { RecentActivity, RecentActivitySkeleton } from "@/app/dashboard/recent-activity";
import PulseCard from "@/app/components/design-system/pulse-card";
import QuestHeatmap from "@/app/dashboard/quest-heatmap";
import { getUserCurrency } from "@/app/lib/user-currency";
import { getUserTimeZone } from "@/app/lib/user-time-zone";

export const metadata = {
  title: "Dashboard | PULSE",
};

type DashboardData = ReturnType<typeof getDashboardData>;

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

async function DashboardFitness({ fitness, questSettings }: {
  fitness: DashboardData["fitness"];
  questSettings: DashboardData["questSettings"];
}) {
  const [data, quest] = await Promise.all([fitness, questSettings]);
  const stepTarget = data && quest?.settings_effective && quest.settings_effective <= data.date
    ? quest.settings?.step_target ?? null
    : null;
  return <FitnessAtAGlance data={data} stepTarget={stepTarget} />;
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
    <DashboardFrame className={`bg-background text-foreground [&_:is(a,button)]:[-webkit-tap-highlight-color:transparent] [&_:is(a,button):focus-visible]:outline-3 [&_:is(a,button):focus-visible]:outline-ring [&_:is(a,button):focus-visible]:outline-offset-4 ${frame.page}`}>
      <div className={`grid gap-6 dashboard-compact:gap-4 ${frame.content}`}>
        <Suspense fallback={<GreetingHeader name={session.username} />}>
          <DashboardGreeting username={session.username} profile={data.profile} notifications={data.notifications} />
        </Suspense>
        <section
          className="grid gap-6 md:grid-cols-2 dashboard-compact:gap-3 dashboard-compact:md:auto-rows-fr dashboard-compact:xl:grid-cols-4"
          aria-label="Quick actions"
        >
          <Suspense fallback={<QuickActionCardSkeleton />}>
            <FinanceCard horizon={data.horizon} />
          </Suspense>
          <Suspense fallback={<QuickActionCardSkeleton />}>
            <FitnessCard workout={data.workout} />
          </Suspense>
          <GarageCard />
          <MealPlanCard />
        </section>
        <div className="grid min-w-0 gap-7.5 leading-[1.4]">
          <Suspense fallback={<FitnessAtAGlanceSkeleton />}>
            <DashboardFitness fitness={data.fitness} questSettings={data.questSettings} />
          </Suspense>
          <PulseCard as="section" aria-labelledby="dashboard-quest-heading" padding="md">
            <header className="mb-5 flex min-h-11 items-center gap-3.5 max-[360px]:gap-2">
              <span className="grid size-11 shrink-0 place-items-center rounded-[14px] bg-[linear-gradient(135deg,#3a2cd9,#b22bd8)] text-white">
                <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M6 11h4M8 9v4M7 6h10a4 4 0 0 1 4 4v3a3 3 0 0 1-5.4 1.8L14.5 14h-5l-1.1.8A3 3 0 0 1 3 13v-3a4 4 0 0 1 4-4Z" />
                  <circle cx={15.2} cy={10.2} r={0.7} />
                  <circle cx={17.6} cy={12.6} r={0.7} />
                </svg>
              </span>
              <h2 id="dashboard-quest-heading" className="flex-1 text-[clamp(1.25rem,2vw,1.5rem)] font-extrabold tracking-[-.03em]">
                Quest activity
              </h2>
              <Link href="/quests" prefetch={false}
                className="group -mr-3.5 inline-flex min-h-11 shrink-0 items-center gap-2 rounded-full px-3.5 font-bold text-muted-foreground transition-colors hover:bg-(--dashboard-activity-chip) hover:text-foreground max-[360px]:text-sm">
                View quests <ArrowRight className="size-5 motion-safe:transition-transform motion-safe:group-hover:translate-x-[3px]" aria-hidden="true" />
              </Link>
            </header>
            <Suspense fallback={<div className="h-40 rounded-lg bg-(--dashboard-activity-chip)" role="status"><span className="sr-only">Loading quest activity</span></div>}>
              <DashboardQuestActivity activity={data.questActivity} />
            </Suspense>
          </PulseCard>
          <Suspense fallback={<RecentActivitySkeleton />}>
            <DashboardRecentActivity activity={data.recentActivity} />
          </Suspense>
        </div>
      </div>
    </DashboardFrame>
  );
}
