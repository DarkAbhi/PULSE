"use client";

import { useState } from "react";
import { CalendarDays, Dumbbell, Moon, Pencil, Plus } from "lucide-react";
import PulseCard from "@/components/ui/pulse-card";
import { saveWorkoutScheduleDay } from "@/features/fitness/api/fitness-actions";
import { fitnessStyles as styles } from "@/features/fitness/styles/fitness-classes";
import WorkoutTypeDialog from "@/features/fitness/components/workout-type-dialog";
import { workoutLabel, workoutTypes, type WorkoutScheduleDay } from "@/features/fitness/types/workout-types";

const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

type DayKind = "workout" | "rest" | "none";

function dayKind(entry?: WorkoutScheduleDay): DayKind {
  if (!entry) return "none";
  return entry.workout_type === "rest_day" ? "rest" : "workout";
}

const kindIcons = { workout: Dumbbell, rest: Moon, none: Plus } satisfies Record<DayKind, typeof Dumbbell>;

export default function WorkoutSchedule({ schedule, today }: { schedule: WorkoutScheduleDay[]; today: string }) {
  const [editing, setEditing] = useState<number | null>(null);
  const todayWeekday = new Date(`${today}T00:00:00Z`).getUTCDay() || 7;
  const editingDay = schedule.find((item) => item.weekday === editing);
  const workoutDays = schedule.filter((item) => item.workout_type !== "rest_day").length;
  const restDays = schedule.length - workoutDays;

  return (
    <PulseCard as="section" aria-labelledby="workout-schedule-heading" padding="md">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-3">
        <span aria-hidden="true" className={styles.scheduleIcon}><CalendarDays size={22} /></span>
        <div className="min-w-48 flex-1">
          <h2 id="workout-schedule-heading" className="text-xl leading-tight font-extrabold tracking-tight sm:text-2xl">Workout schedule</h2>
          <p className={`${styles.muted} mt-0.5 text-control`}>Plan your workouts and rest days for a repeating week.</p>
        </div>
        {schedule.length > 0 && (
          <p className={`${styles.timeChip} tabular-nums max-sm:ml-15`}>
            {workoutDays} workout {workoutDays === 1 ? "day" : "days"} · {restDays} rest
          </p>
        )}
      </div>
      <ul className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-4 sm:gap-3 xl:grid-cols-7" role="list">
        {weekdays.map((day, index) => {
          const weekday = index + 1;
          const entry = schedule.find((item) => item.weekday === weekday);
          const label = entry && workoutLabel(entry);
          const kind = dayKind(entry);
          const Icon = kindIcons[kind];
          const isToday = weekday === todayWeekday;
          const title = kind === "workout" && entry ? workoutTypes[entry.workout_type as keyof typeof workoutTypes] : kind === "rest" ? "Rest day" : "No plan";
          const focus = kind === "workout" ? entry?.workout_focus : null;
          return (
            <li key={day} className="min-w-0">
              <button type="button" aria-haspopup="dialog" aria-label={`Edit ${day} schedule, ${label ?? "no plan"}`}
                aria-current={isToday ? "date" : undefined} data-kind={kind} onClick={() => setEditing(weekday)}
                className={`${styles.scheduleDay} min-h-36`}>
                <span className="flex w-full items-center justify-between gap-2">
                  <span className={`${styles.muted} text-sm font-bold`}>{day.slice(0, 3)}</span>
                  {isToday ? <span className={styles.scheduleToday}>Today</span> : (
                    <Pencil size={14} aria-hidden="true" className={`${styles.muted} opacity-0 transition-opacity group-focus-visible:opacity-100`} />
                  )}
                </span>
                <span aria-hidden="true" className={styles.scheduleType}><Icon size={20} /></span>
                <span className="mt-auto block min-w-0">
                  <span className={`block leading-tight font-extrabold tracking-tight wrap-anywhere ${kind === "none" ? styles.muted : ""}`}>{title}</span>
                  {focus && <span className="sr-only"> · </span>}
                  {focus && <span className={`${styles.muted} mt-0.5 block text-sm font-semibold wrap-anywhere`}>{focus}</span>}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
      {editing !== null && (
        <WorkoutTypeDialog
          title={`Edit ${weekdays[editing - 1]}`}
          weekday={weekdays[editing - 1]}
          initialType={editingDay?.workout_type}
          initialFocus={editingDay?.workout_focus}
          onClose={() => setEditing(null)}
          onSave={(type, focus) => saveWorkoutScheduleDay(editing, type, focus)}
        />
      )}
    </PulseCard>
  );
}
