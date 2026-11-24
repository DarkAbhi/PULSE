import type { ReactNode } from "react";
import Link from "next/link";
import {
  Award, BarChart3, BedDouble, CalendarDays, Check, ChevronLeft, ChevronRight, Circle, Dumbbell,
  Footprints, Heart, Medal, Star, Target, Trophy, Utensils, Zap, type LucideIcon,
} from "lucide-react";
import QuestSettingsButton from "@/app/quests/quest-settings";
import type { Quest, QuestDay, QuestOverview } from "@/app/quests/types";

const panel = "min-w-0 rounded-card border border-(--quest-border) bg-linear-to-br from-(--quest-panel) to-(--quest-panel-deep) p-4 sm:p-5";
const number = new Intl.NumberFormat("en");
const shortDate = new Intl.DateTimeFormat("en", { month: "short", day: "numeric", timeZone: "UTC" });
const fullDate = new Intl.DateTimeFormat("en", { dateStyle: "full", timeZone: "UTC" });
const weekday = new Intl.DateTimeFormat("en", { weekday: "narrow", timeZone: "UTC" });

function PanelHeading({ id, icon: Icon, children, subtitle }: {
  id: string;
  icon: LucideIcon;
  children: ReactNode;
  subtitle?: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <Icon className="size-7 shrink-0 text-(--quest-violet)" aria-hidden="true" />
      <div>
        <h2 id={id} className="text-xl leading-tight font-bold tracking-tight">{children}</h2>
        {subtitle && <p className="mt-0.5 text-sm text-(--quest-muted)">{subtitle}</p>}
      </div>
    </div>
  );
}

export function StreakCard({ streak }: { streak: number }) {
  const hexagon = "[clip-path:polygon(50%_0,94%_25%,94%_75%,50%_100%,6%_75%,6%_25%)]";
  return (
    <section aria-labelledby="quest-streak" className="relative isolate flex min-h-40 items-center justify-between gap-2 overflow-hidden rounded-card border border-amber-300/40 bg-linear-to-br from-yellow-200 via-amber-400 to-orange-600 px-5 py-4 text-amber-950 sm:px-6">
      <div aria-hidden="true" className="pointer-events-none absolute -right-8 -bottom-23 -z-10 h-32 w-[110%] rotate-[-8deg] rounded-[50%] bg-amber-100/35" />
      <div>
        <h2 id="quest-streak" className="text-lg leading-tight font-bold">Current streak</h2>
        <p className="mt-1 text-4xl leading-none font-extrabold tracking-tight tabular-nums xl:text-5xl">
          {number.format(streak)} {streak === 1 ? "day" : "days"}
        </p>
        <p className="mt-2 text-sm font-semibold">{streak ? "Keep it going!" : "Finish all quests on a scheduled day."}</p>
      </div>
      <div aria-hidden="true" className="relative isolate mr-0.5 grid size-19 shrink-0 place-items-center drop-shadow-[0_7px_13px_#78350f80] sm:size-24">
        <span className="absolute inset-x-6 -bottom-2 h-9 bg-linear-to-b from-fuchsia-800 to-pink-500 [clip-path:polygon(0_0,100%_0,100%_100%,50%_72%,0_100%)]" />
        <div className={`absolute inset-0 bg-linear-to-br from-yellow-50 via-amber-200 to-yellow-500 ${hexagon}`} />
        <div className={`absolute inset-2 bg-linear-to-br from-amber-300 to-orange-600 ${hexagon}`} />
        <Star className="relative size-9 fill-yellow-100 text-yellow-50 drop-shadow-sm sm:size-11" />
      </div>
    </section>
  );
}

const dayStyle: Record<QuestDay["status"], string> = {
  complete: "bg-(--quest-green-bg) text-(--quest-green)",
  pending: "bg-violet-500/25 text-white",
  missed: "bg-pink-500/14 text-(--quest-pink)",
  rest: "bg-(--quest-tile) text-(--quest-muted)",
  unavailable: "bg-white/4 text-(--quest-muted)",
  future: "bg-white/8 text-(--quest-muted)",
};
const dayLabel: Record<QuestDay["status"], string> = {
  complete: "All quests complete",
  pending: "Quests in progress",
  missed: "Quest day missed",
  rest: "No quests scheduled",
  unavailable: "Before quest setup",
  future: "Upcoming",
};

function QuestWeekArrow({ week, direction }: { week?: string; direction: "previous" | "next" }) {
  const Icon = direction === "previous" ? ChevronLeft : ChevronRight;
  const className = "grid size-10 place-items-center rounded-full";
  return week ? (
    <Link href={`/quests?week=${week}`} scroll={false} prefetch={false}
      aria-label={direction === "previous" ? "Previous week" : "Next week"}
      className={`${className} text-(--quest-muted) hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-(--quest-violet)`}>
      <Icon className="size-5" aria-hidden="true" />
    </Link>
  ) : <span className={`${className} text-white/25`} aria-hidden="true"><Icon className="size-5" /></span>;
}

export function QuestWeek({ overview }: { overview: QuestOverview }) {
  const { week, today, previous_week, next_week } = overview;
  const weekLabel = next_week
    ? shortDate.formatRange(new Date(week[0].date), new Date(week[6].date))
    : "This week";
  return (
    <section aria-labelledby="quest-week" className={panel}>
      <div className="-mt-1 mb-2 flex min-h-10 items-center justify-between gap-1">
        <h2 id="quest-week" className="text-xl font-bold tracking-tight">{weekLabel}</h2>
        <nav aria-label="Quest weeks" className="-mr-2 flex">
          <QuestWeekArrow week={previous_week} direction="previous" />
          <QuestWeekArrow week={next_week} direction="next" />
        </nav>
      </div>
      <ol className="grid grid-cols-7 gap-1.5 sm:gap-2">
        {week.map(day => {
          const date = new Date(day.date);
          const isToday = day.date === today;
          return (
            <li key={day.date} aria-current={isToday ? "date" : undefined} className="grid min-w-0 gap-2 text-center">
              <span className="text-xs font-bold text-(--quest-muted)" aria-hidden="true">{weekday.format(date)}</span>
              <span className={`relative grid min-h-10 place-items-center rounded-xl text-sm font-bold tabular-nums ${dayStyle[day.status]} ${isToday ? "ring-2 ring-inset ring-violet-400" : ""}`}
                title={`${dayLabel[day.status]}${day.total ? ` · ${day.completed}/${day.total}` : ""}`}>
                <span className="sr-only">{fullDate.format(date)}. {dayLabel[day.status]}. {day.total > 0 && `${day.completed} of ${day.total} complete.`}</span>
                {day.status === "complete"
                  ? <Check className="size-5 stroke-3" aria-hidden="true" />
                  : <span aria-hidden="true">{date.getUTCDate()}</span>}
              </span>
            </li>
          );
        })}
      </ol>
    </section>
  );
}

export function TodayXP({ progress }: { progress: QuestOverview["progress"] }) {
  const { level, today_xp, today_xp_target } = progress;
  const percent = today_xp_target ? today_xp / today_xp_target * 100 : 0;
  return (
    <section aria-labelledby="quest-xp" className={`${panel} grid content-between gap-4 md:max-lg:col-span-2`}>
      <div className="flex items-center gap-4">
        <div aria-label={`Level ${level}`} className="relative isolate grid size-17 shrink-0 place-items-center text-xs font-bold sm:size-20">
          <span className="absolute inset-0 -z-20 bg-linear-to-b from-sky-400 to-fuchsia-500 [clip-path:polygon(50%_0,94%_25%,94%_75%,50%_100%,6%_75%,6%_25%)]" />
          <span className="absolute inset-1.5 -z-10 bg-linear-to-br from-blue-700 to-violet-800 [clip-path:polygon(50%_0,94%_25%,94%_75%,50%_100%,6%_75%,6%_25%)]" />
          <span aria-hidden="true" className="text-center leading-none">Lv <strong className="text-xl tabular-nums">{level}</strong></span>
        </div>
        <div>
          <h2 id="quest-xp" className="text-sm font-semibold text-(--quest-muted)">Today’s XP</h2>
          <p className="text-3xl leading-tight font-extrabold tracking-tight tabular-nums">
            {number.format(today_xp)} <span className="text-base font-semibold text-(--quest-violet)">/ {number.format(today_xp_target)} XP</span>
          </p>
        </div>
      </div>
      <div className="relative mr-3 flex h-8 items-center" role="progressbar"
        aria-label="Today’s XP" aria-valuemin={0} aria-valuemax={Math.max(1, today_xp_target)} aria-valuenow={today_xp}
        aria-valuetext={`${today_xp} of ${today_xp_target} XP`}>
        <div className="absolute inset-x-0 h-1.5 overflow-hidden rounded-full bg-violet-500/35" aria-hidden="true">
          <div className="h-full rounded-full bg-linear-to-r from-fuchsia-400 to-violet-300" style={{ width: `${percent}%` }} />
        </div>
        {today_xp_target > 0 && [20, 40, 60, 80, 100].map(checkpoint => {
          const reached = percent >= checkpoint;
          return (
            <span key={checkpoint} aria-hidden="true" style={{ left: `${checkpoint}%` }}
              className={`absolute grid size-6 -translate-x-1/2 place-items-center rounded-lg border-2 ${reached
                ? "border-emerald-300 bg-(--quest-green) text-emerald-950" : "border-violet-300 bg-(--quest-tile)"}`}>
              {reached && <Check className="size-4 stroke-3" />}
            </span>
          );
        })}
      </div>
    </section>
  );
}

function sleepDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const remaining = minutes % 60;
  return hours ? `${hours} hr${remaining ? ` ${remaining} min` : ""}` : `${minutes} min`;
}

const questContent: Record<Quest["id"], { title: string; icon: LucideIcon; description: (quest: Quest) => string }> = {
  gym_workout: { title: "Gym workout", icon: Dumbbell, description: () => "Log an exercise set or sync a strength workout." },
  daily_steps: { title: "Daily step goal", icon: Footprints, description: q => `${number.format(q.progress)} / ${number.format(q.target)} steps` },
  activity_rings: { title: "Close activity rings", icon: Circle, description: q => `${q.progress} / 3 rings closed · Move, Exercise, and Stand` },
  sleep_goal: { title: "Sleep goal", icon: BedDouble, description: q => `${sleepDuration(q.progress)} of ${sleepDuration(q.target)} asleep` },
  daily_meals: { title: "Daily meal goal", icon: Utensils, description: q => `${q.progress} / ${q.target} meals marked eaten` },
};

function QuestRow({ quest }: { quest: Quest }) {
  const { title, description, icon: Icon } = questContent[quest.id];
  return (
    <li className={`grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-x-3 gap-y-2 rounded-2xl border px-3 py-2.5 sm:grid-cols-[auto_minmax(0,1fr)_auto_auto] sm:gap-x-4 ${quest.completed
      ? "border-(--quest-green-border) bg-(--quest-green-bg)"
      : "border-(--quest-border) bg-white/4"}`}>
      <span className={`grid size-10 place-items-center rounded-xl ${quest.completed ? "bg-emerald-400/15 text-(--quest-green)" : "bg-violet-500/25 text-(--quest-violet)"}`}>
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <div>
        <h3 className="font-bold leading-tight">{title}</h3>
        <p className="mt-0.5 text-sm text-(--quest-muted)">{description(quest)}</p>
      </div>
      <div className="col-start-2 row-start-2 flex items-center gap-4 text-sm font-semibold whitespace-nowrap tabular-nums sm:col-start-3 sm:row-start-1">
        <span className="flex items-center gap-1.5"><Zap className="size-4 fill-current text-(--quest-gold)" aria-hidden="true" />{quest.xp} XP</span>
        <span className="flex items-center gap-1.5"><Heart className="size-4 fill-current text-(--quest-pink)" aria-hidden="true" />{quest.hearts}<span className="sr-only"> hearts</span></span>
      </div>
      <span className={`col-start-3 row-start-1 grid size-6 place-items-center rounded-lg border-2 sm:col-start-4 ${quest.completed
        ? "border-emerald-300 bg-(--quest-green) text-emerald-950" : "border-violet-300/75"}`}>
        <span className="sr-only">{quest.completed ? "Completed" : "In progress"}</span>
        {quest.completed && <Check className="size-4 stroke-3" aria-hidden="true" />}
      </span>
    </li>
  );
}

export function DailyQuests({ overview }: { overview: QuestOverview }) {
  const { today, today_quests, configured, progress, settings, settings_effective, has_workout_days } = overview;
  return (
    <section aria-labelledby="daily-quests" className={panel}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PanelHeading id="daily-quests" icon={Target} subtitle="Quests reset at midnight in your time zone.">Today’s quests</PanelHeading>
        {today_quests.length > 0 && (
          <span className="rounded-full border border-(--quest-border) bg-(--quest-tile) px-3 py-1.5 text-sm font-bold tabular-nums">
            {progress.today_complete} / {today_quests.length} completed
          </span>
        )}
      </div>
      {today_quests.length > 0 ? (
        <ul className="mt-4 grid gap-2">
          {today_quests.map(quest => <QuestRow key={quest.id} quest={quest} />)}
        </ul>
      ) : (
        <div className="mt-4 grid justify-items-start gap-3 rounded-2xl border border-(--quest-border) bg-white/4 p-4">
          <p className="text-sm text-(--quest-muted)">
            {configured ? "No quests scheduled today." : "Set your goals to start earning XP and hearts from the activity you track in Pulse."}
          </p>
          {!configured && <QuestSettingsButton {...overview} />}
        </div>
      )}
      {configured && settings.gym_workout && !has_workout_days && (
        <p className="mt-3 text-sm text-(--quest-muted)">
          <Link href="/fitness" className="font-semibold text-(--quest-violet) underline underline-offset-2">Add workout days</Link> to schedule the Gym workout quest.
        </p>
      )}
      {settings_effective && settings_effective > today && (
        <p className="mt-3 text-sm text-(--quest-muted)" role="status">
          Updated goals start {shortDate.format(new Date(settings_effective))}.
        </p>
      )}
    </section>
  );
}

const milestones = [3, 7, 14, 30];

export function StreakMedals({ bestStreak }: { bestStreak: number }) {
  const next = milestones.find(milestone => milestone > bestStreak);
  return (
    <section aria-labelledby="quest-medals" className={panel}>
      <PanelHeading id="quest-medals" icon={Medal}
        subtitle={next ? `Best streak of ${bestStreak} ${bestStreak === 1 ? "day" : "days"} · Next medal at ${next} days` : "All streak medals earned"}>
        Streak medals
      </PanelHeading>
      <ul className="mt-4 grid grid-cols-4 gap-2">
        {milestones.map(milestone => {
          const earned = bestStreak >= milestone;
          return (
            <li key={milestone} className={`grid min-w-0 justify-items-center gap-1 rounded-2xl border p-2 text-center text-xs font-semibold sm:p-3 ${earned
              ? "border-amber-400/40 bg-amber-400/15 text-(--quest-gold)"
              : "border-(--quest-border) bg-(--quest-tile) text-(--quest-muted)"}`}>
              <Award className="size-6" aria-hidden="true" />
              <span>{milestone} days<span className="sr-only"> {earned ? "milestone earned" : "milestone locked"}</span></span>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

export function QuestProgress({ overview }: { overview: QuestOverview }) {
  const { week_xp, week_hearts, week_active, level } = overview.progress;
  const stats = [
    { label: "XP earned", value: number.format(week_xp), icon: Zap, color: "text-(--quest-gold)" },
    { label: "Hearts earned", value: number.format(week_hearts), icon: Heart, color: "text-(--quest-pink)" },
    { label: "Days active", value: `${week_active} / 7`, icon: CalendarDays, color: "text-(--quest-blue)" },
    { label: "Current level", value: `Lv ${number.format(level)}`, icon: Trophy, color: "text-(--quest-gold)" },
  ];
  return (
    <section aria-labelledby="quest-progress" className={panel}>
      <PanelHeading id="quest-progress" icon={BarChart3} subtitle={overview.next_week ? "Selected week" : "This week"}>Your progress</PanelHeading>
      <dl className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-2 xl:grid-cols-4">
        {stats.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="flex min-w-0 flex-col items-center gap-1 rounded-2xl border border-(--quest-border) bg-(--quest-tile) px-2 py-3 text-center">
            <dt className="order-2 text-xs leading-tight text-(--quest-muted)">{label}</dt>
            <dd className="contents">
              <Icon className={`size-6 ${color}`} aria-hidden="true" />
              <span className="text-lg leading-tight font-bold tabular-nums">{value}</span>
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
