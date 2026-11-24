import type { ReactNode } from "react";
import Link from "next/link";
import {
  Award, BarChart3, BedDouble, CalendarDays, Check, ChevronLeft, ChevronRight, Circle, Dumbbell,
  Footprints, Heart, Medal, Target, Trophy, Utensils, Zap, type LucideIcon,
} from "lucide-react";
import QuestSettingsButton from "@/app/quests/quest-settings";
import type { Quest, QuestDay, QuestOverview } from "@/app/quests/types";

const panel = "flex min-w-0 flex-col gap-[18px] rounded-[28px] bg-(--quest-panel) p-4 shadow-(--quest-shadow) sm:p-6";
const panelTitle = "text-[clamp(1.2rem,2vw,1.45rem)] leading-tight font-extrabold tracking-tight";
const number = new Intl.NumberFormat("en");
const shortDate = new Intl.DateTimeFormat("en", { month: "short", day: "numeric", timeZone: "UTC" });
const fullDate = new Intl.DateTimeFormat("en", { dateStyle: "full", timeZone: "UTC" });
const weekday = new Intl.DateTimeFormat("en", { weekday: "narrow", timeZone: "UTC" });

function PanelHeading({ id, icon: Icon, children, subtitle }: {
  id: string;
  icon: LucideIcon;
  children: ReactNode;
  subtitle: string;
}) {
  return (
    <div className="flex min-w-0 items-center gap-3.5">
      <span className="grid size-11 shrink-0 place-items-center rounded-[14px] bg-[color-mix(in_srgb,var(--quest-accent)_14%,transparent)] text-(--quest-blue)">
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <div>
        <h2 id={id} className={panelTitle}>{children}</h2>
        <p className="mt-0.5 text-sm text-(--quest-muted)">{subtitle}</p>
      </div>
    </div>
  );
}

export function StreakCard({ streak }: { streak: number }) {
  return (
    <section aria-labelledby="quest-streak" className="flex min-h-[210px] items-stretch justify-between gap-2 rounded-[28px] bg-linear-[135deg,#ffe9a0,#ffc83d_55%,#ff9b3d] p-4 text-[#3a2a00] shadow-[0_20px_40px_-22px_#ff9b3de6] sm:p-6 min-[701px]:max-[1050px]:col-span-2">
      <div className="flex flex-col justify-between gap-3.5">
        <div>
          <h2 id="quest-streak" className="text-base font-bold">Current streak</h2>
          <p className="mt-1 text-[clamp(2.6rem,5vw,4rem)] leading-none font-extrabold tracking-[-0.05em] tabular-nums">
            {number.format(streak)} {streak === 1 ? "day" : "days"}
          </p>
        </div>
        <p className="max-w-[20ch] leading-snug font-semibold">Finish all quests on a scheduled day.</p>
      </div>
      <svg viewBox="0 0 120 140" aria-hidden="true" className="w-[clamp(92px,11vw,124px)] shrink-0 self-center drop-shadow-[0_12px_14px_#783c0059] motion-safe:animate-[quest-medal-float_5s_ease-in-out_infinite]">
        <defs>
          <linearGradient id="quest-medal-rim" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0" stopColor="#fff3b0" />
            <stop offset="1" stopColor="#f7b500" />
          </linearGradient>
          <linearGradient id="quest-medal-center" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0" stopColor="#ffb02e" />
            <stop offset="1" stopColor="#e8590c" />
          </linearGradient>
        </defs>
        <path d="M42 96l-8 32 26-13 26 13-8-32z" fill="#c026d3" />
        <path d="M60 115l26 13-8-32-18 8z" fill="#9b1fb0" />
        <polygon points="60,6 106,32 106,84 60,110 14,84 14,32" fill="url(#quest-medal-rim)" stroke="white" strokeOpacity=".7" strokeWidth="3" strokeLinejoin="round" />
        <polygon points="60,22 92,40 92,76 60,94 28,76 28,40" fill="url(#quest-medal-center)" />
        <path d="M60 38l7 14.5 16 2.2-11.6 11.2 2.8 15.8L60 74l-14.2 7.7 2.8-15.8L37 54.7l16-2.2z" fill="white" />
      </svg>
    </section>
  );
}

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
  const className = "grid size-11 shrink-0 place-items-center rounded-full text-(--quest-muted)";
  return week ? (
    <Link href={`/quests?week=${week}`} scroll={false} prefetch={false}
      aria-label={direction === "previous" ? "Previous week" : "Next week"}
      className={`${className} hover:bg-(--quest-chip) focus-visible:outline-2 focus-visible:outline-(--quest-accent)`}>
      <Icon className="size-5" aria-hidden="true" />
    </Link>
  ) : <span className={`${className} opacity-35`} aria-hidden="true"><Icon className="size-5" /></span>;
}

export function QuestWeek({ overview }: { overview: QuestOverview }) {
  const { week, today, previous_week, next_week } = overview;
  const weekLabel = next_week
    ? shortDate.formatRange(new Date(week[0].date), new Date(week[6].date))
    : "This week";
  return (
    <section aria-labelledby="quest-week" className={panel}>
      <div className="flex min-h-11 items-center justify-between gap-2">
        <h2 id="quest-week" className={panelTitle}>{weekLabel}</h2>
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
            <li key={day.date} aria-current={isToday ? "date" : undefined} className="flex min-w-0 flex-col items-center gap-2 text-center">
              <span className="text-sm font-bold text-(--quest-muted)" aria-hidden="true">{weekday.format(date)}</span>
              <span className={`relative grid aspect-square w-full max-w-15 place-items-center rounded-xl pb-1 text-base font-extrabold tabular-nums sm:rounded-[18px] ${isToday
                ? "bg-(image:--quest-gradient) text-white shadow-[0_12px_22px_-12px_#5b2ce0e6]"
                : day.date < today ? "bg-(--quest-chip) text-(--quest-muted)" : "bg-(--quest-chip)"}`}
                title={`${dayLabel[day.status]}${day.total ? ` · ${day.completed}/${day.total}` : ""}`}>
                <span className="sr-only">{fullDate.format(date)}. {dayLabel[day.status]}. {day.total > 0 && `${day.completed} of ${day.total} complete.`}</span>
                <span aria-hidden="true">{date.getUTCDate()}</span>
                {day.completed > 0 && <span aria-hidden="true" className={`absolute bottom-1 size-1.5 rounded-full ${isToday ? "bg-white" : "bg-(--quest-green)"}`} />}
                {day.status === "complete" && <span aria-hidden="true" className="absolute -top-0.5 -right-0.5 size-2.5 rounded-full bg-(--quest-gold) ring-2 ring-(--quest-panel) sm:size-3" />}
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
  const percent = today_xp_target > 0 ? Math.min(100, today_xp / today_xp_target * 100) : 0;
  return (
    <section aria-labelledby="quest-xp" className={panel}>
      <div className="flex items-center gap-[18px]">
        <div className="relative grid size-[92px] shrink-0 place-items-center text-center font-extrabold text-white drop-shadow-[0_12px_16px_#5b2ce073]">
          <svg viewBox="0 0 100 100" aria-hidden="true" className="absolute inset-0 size-full">
            <defs>
              <linearGradient id="quest-level-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0" stopColor="#3a2cd9" />
                <stop offset="1" stopColor="#c026d3" />
              </linearGradient>
            </defs>
            <polygon points="50,4 90,27 90,73 50,96 10,73 10,27" fill="url(#quest-level-gradient)" stroke="white" strokeOpacity=".55" strokeWidth="3" strokeLinejoin="round" />
            <polygon points="50,16 80,33 80,67 50,84 20,67 20,33" fill="none" stroke="white" strokeOpacity=".28" strokeWidth="2" strokeLinejoin="round" />
          </svg>
          <span className="relative leading-none">
            <span className="sr-only">Level </span>
            <span aria-hidden="true" className="block text-xs font-bold">Lv</span>
            <strong className="text-3xl tracking-tight tabular-nums">{level}</strong>
          </span>
        </div>
        <div className="min-w-0">
          <h2 id="quest-xp" className="font-semibold text-(--quest-muted)">Today’s XP</h2>
          <p className="text-[clamp(2.2rem,4vw,3rem)] leading-none font-extrabold tracking-tight tabular-nums">
            {number.format(today_xp)} <span className="inline-block text-[0.45em] font-bold tracking-normal text-(--quest-muted)">/ {number.format(today_xp_target)} XP</span>
          </p>
        </div>
      </div>
      {today_xp_target > 0 && (
        <div className="relative mx-3 mb-2 h-10" role="progressbar" aria-labelledby="quest-xp"
          aria-valuemin={0} aria-valuemax={today_xp_target} aria-valuenow={Math.min(today_xp, today_xp_target)}
          aria-valuetext={`${today_xp} of ${today_xp_target} XP`}>
          <div className="absolute inset-x-0 top-2.5 h-2 rounded-full bg-(--quest-chip)" aria-hidden="true">
            <div className="h-full rounded-full bg-linear-to-r from-[#19d3f3] via-[#4c6bff] to-[#b22bd8] motion-safe:transition-[width] motion-safe:duration-500" style={{ width: `${percent}%` }} />
          </div>
          {[20, 40, 60, 80, 100].map(checkpoint => (
            <span key={checkpoint} aria-hidden="true" style={{ left: `${checkpoint}%` }}
              className="absolute top-0.5 grid -translate-x-1/2 justify-items-center gap-1 text-xs font-bold text-(--quest-muted) tabular-nums">
              <span className={`size-[22px] rounded-lg border-2 ${percent >= checkpoint
                ? "border-transparent bg-(image:--quest-gradient)" : "border-(--quest-border) bg-(--quest-panel)"}`} />
              {number.format(Math.round(today_xp_target * checkpoint / 100))}
            </span>
          ))}
        </div>
      )}
    </section>
  );
}

function sleepDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const remaining = minutes % 60;
  return `${hours} hr${remaining ? ` ${remaining} min` : ""}`;
}

const questContent: Record<Quest["id"], { title: string; icon: LucideIcon; description: (quest: Quest) => string }> = {
  gym_workout: { title: "Gym workout", icon: Dumbbell, description: q => q.completed ? "Workout completed." : "Log an exercise set or sync a strength workout." },
  daily_steps: { title: "Daily step goal", icon: Footprints, description: q => `${number.format(q.progress)} / ${number.format(q.target)} steps` },
  activity_rings: { title: "Close activity rings", icon: Circle, description: q => `${q.progress} / 3 rings closed · Move, Exercise, and Stand` },
  sleep_goal: { title: "Sleep goal", icon: BedDouble, description: q => `${sleepDuration(q.progress)} of ${sleepDuration(q.target)} asleep` },
  daily_meals: { title: "Daily meal goal", icon: Utensils, description: q => `${q.progress} / ${q.target} planned meals marked eaten` },
};

function QuestRow({ quest, upcomingGoal }: { quest: Quest; upcomingGoal?: string }) {
  const { title, description, icon } = questContent[quest.id];
  const Icon = quest.completed ? Check : icon;
  return (
    <li className={`grid min-h-[76px] grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1.5 rounded-[20px] border px-3 py-2.5 min-[701px]:grid-cols-[auto_minmax(0,1fr)_auto_44px] min-[701px]:gap-x-4 min-[701px]:px-3.5 ${quest.completed
      ? "border-[color-mix(in_srgb,var(--quest-green)_55%,transparent)] bg-[color-mix(in_srgb,var(--quest-green)_9%,var(--quest-inset))]"
      : "border-transparent bg-(--quest-inset)"}`}>
      <span className={`grid size-11 place-items-center rounded-[14px] ${quest.completed ? "bg-(--quest-green) text-(--quest-check)" : "bg-(--quest-panel) text-(--quest-blue)"}`}>
        <Icon className={`size-5 ${quest.completed ? "stroke-3" : ""}`} aria-hidden="true" />
      </span>
      <div>
        <h3 className="font-extrabold leading-tight tracking-tight">{title}</h3>
        <p className="mt-0.5 text-sm text-(--quest-muted)">{description(quest)}</p>
        {upcomingGoal && <p className="mt-1 text-sm font-semibold text-(--quest-blue)">{upcomingGoal}</p>}
      </div>
      <div className="col-start-2 row-start-2 flex items-center gap-3.5 text-sm font-extrabold whitespace-nowrap tabular-nums min-[701px]:col-start-3 min-[701px]:row-start-1 min-[701px]:text-base">
        <span className="inline-flex items-center gap-1.5"><Zap className="size-5 fill-current text-(--quest-gold)" aria-hidden="true" />{quest.xp} XP</span>
        <span className="inline-flex items-center gap-1.5"><Heart className="size-5 fill-current text-(--quest-pink)" aria-hidden="true" />{quest.hearts}<span className="sr-only"> hearts</span></span>
      </div>
      <span className={`col-start-3 row-span-2 row-start-1 grid size-7 place-items-center justify-self-center rounded-[10px] border-2 min-[701px]:col-start-4 min-[701px]:row-span-1 ${quest.completed
        ? "border-transparent bg-(--quest-green) text-(--quest-check)" : "border-(--quest-border) bg-(--quest-panel)"}`}>
        <span className="sr-only">{quest.completed ? "Completed" : "In progress"}</span>
        {quest.completed && <Check className="size-4 stroke-3" aria-hidden="true" />}
      </span>
    </li>
  );
}

export function DailyQuests({ overview }: { overview: QuestOverview }) {
  const { today, today_quests, configured, progress, settings, settings_effective, has_workout_days } = overview;
  const updatedGoalsDate = settings_effective && settings_effective > today
    ? shortDate.format(new Date(settings_effective))
    : undefined;
  const upcomingSleepTarget = settings.sleep_minutes === null ? null : settings.sleep_minutes * 60;
  const todaySleep = today_quests.find(quest => quest.id === "sleep_goal");
  const upcomingSleepGoal = updatedGoalsDate && todaySleep && todaySleep.target !== upcomingSleepTarget
    ? upcomingSleepTarget === null
      ? `Sleep quest turns off ${updatedGoalsDate}.`
      : `New goal: ${sleepDuration(upcomingSleepTarget)} starting ${updatedGoalsDate}.`
    : undefined;

  return (
    <section aria-labelledby="daily-quests" className={panel}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PanelHeading id="daily-quests" icon={Target} subtitle="Quests reset at midnight in your time zone.">Today’s quests</PanelHeading>
        {today_quests.length > 0 && (
          <span className="rounded-full bg-(--quest-chip) px-3.5 py-1.5 text-sm font-bold tabular-nums">
            {progress.today_complete} / {today_quests.length} completed
          </span>
        )}
      </div>
      {today_quests.length > 0 ? (
        <ul className="grid gap-2.5">
          {today_quests.map(quest => (
            <QuestRow key={quest.id} quest={quest} upcomingGoal={quest.id === "sleep_goal" ? upcomingSleepGoal : undefined} />
          ))}
        </ul>
      ) : (
        <div className="grid justify-items-start gap-3 rounded-[20px] bg-(--quest-inset) p-4">
          <p className="text-sm text-(--quest-muted)">
            {configured ? "No quests scheduled today." : "Set your goals to start earning XP and hearts from the activity you track in Pulse."}
          </p>
          {!configured && <QuestSettingsButton {...overview} />}
        </div>
      )}
      {configured && settings.gym_workout && !has_workout_days && (
        <p className="text-sm text-(--quest-muted)">
          <Link href="/fitness" className="font-semibold text-(--quest-blue) underline underline-offset-2">Add workout days</Link> to schedule the Gym workout quest.
        </p>
      )}
      {updatedGoalsDate && (
        <p className="text-sm text-(--quest-muted)" role="status">
          Updated goals start {updatedGoalsDate}.
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
      <ul className="grid grid-cols-2 gap-2.5 min-[701px]:grid-cols-4">
        {milestones.map(milestone => {
          const earned = bestStreak >= milestone;
          const isNext = milestone === next;
          return (
            <li key={milestone} className={`flex min-h-[108px] min-w-0 flex-col items-center justify-center gap-2 rounded-[20px] border px-1.5 py-3 text-center text-sm font-bold ${earned
              ? "border-transparent bg-linear-[160deg,#ffe9a0,#ffc83d] text-[#3a2a00]"
              : `bg-(--quest-inset) ${isNext ? "border-(--quest-accent)" : "border-transparent"}`}`}>
              <span className={`grid size-10 place-items-center rounded-full ${earned ? "bg-white/60 text-[#9a5a00]" : "bg-(--quest-chip) text-(--quest-muted)"}`}>
                <Award className="size-5" aria-hidden="true" />
              </span>
              <span>{milestone} days<span className="sr-only"> {earned ? "milestone earned" : "milestone locked"}</span></span>
              {isNext && <span className="-mt-1 text-xs font-semibold text-(--quest-blue)">Next</span>}
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
    { label: "XP earned", value: number.format(week_xp), icon: Zap, color: "fill-current text-(--quest-gold)" },
    { label: "Hearts earned", value: number.format(week_hearts), icon: Heart, color: "fill-current text-(--quest-pink)" },
    { label: "Days active", value: `${week_active} / 7`, icon: CalendarDays, color: "text-(--quest-blue)" },
    { label: "Current level", value: `Lv ${number.format(level)}`, icon: Trophy, color: "text-(--quest-amber)" },
  ];
  return (
    <section aria-labelledby="quest-progress" className={panel}>
      <PanelHeading id="quest-progress" icon={BarChart3} subtitle={overview.next_week ? "Selected week" : "This week"}>Your progress</PanelHeading>
      <dl className="grid flex-1 grid-cols-2 gap-2.5 min-[701px]:grid-cols-4">
        {stats.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="flex min-h-[124px] min-w-0 flex-col items-center justify-center gap-0.5 rounded-[20px] bg-(--quest-inset) px-1.5 py-3.5 text-center">
            <dt className="order-last text-sm leading-tight font-semibold text-(--quest-muted)">{label}</dt>
            <dd className="flex min-w-0 flex-col items-center gap-1.5">
              <Icon className={`size-5 ${color}`} aria-hidden="true" />
              <span className="max-w-full break-words text-lg leading-tight font-extrabold tracking-tight tabular-nums sm:text-2xl">{value}</span>
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
