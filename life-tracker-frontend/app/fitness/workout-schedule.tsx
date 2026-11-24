"use client";

import { useState } from "react";
import { Pencil } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseCard from "@/app/components/design-system/pulse-card";
import { saveWorkoutScheduleDay } from "@/app/fitness/actions";
import { fitnessStyles as styles } from "@/app/fitness/fitness-classes";
import WorkoutTypeDialog from "@/app/fitness/workout-type-dialog";
import { workoutLabel, type WorkoutScheduleDay } from "@/app/fitness/workout-types";

const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

export default function WorkoutSchedule({ schedule, today }: { schedule: WorkoutScheduleDay[]; today: string }) {
  const [editing, setEditing] = useState<number | null>(null);
  const todayWeekday = new Date(`${today}T00:00:00Z`).getUTCDay() || 7;
  const editingDay = schedule.find((item) => item.weekday === editing);

  return (
    <PulseCard as="section" aria-labelledby="workout-schedule-heading" className={`${styles.surface} p-4 sm:p-6`}>
      <h2 id="workout-schedule-heading" className="text-2xl font-extrabold tracking-tight">Workout schedule</h2>
      <p className={`${styles.muted} mt-1 text-control`}>Plan your workouts and rest days for a repeating week.</p>
      <ul className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7" role="list">
        {weekdays.map((day, index) => {
          const weekday = index + 1;
          const entry = schedule.find((item) => item.weekday === weekday);
          const label = entry && workoutLabel(entry);
          return (
            <li key={day} className="min-w-0">
              <PulseButton type="button" variant="outline" aria-haspopup="dialog" aria-label={`Edit ${day} schedule, ${label ?? "no plan"}`}
                aria-current={weekday === todayWeekday ? "date" : undefined} onClick={() => setEditing(weekday)}
                className="h-full w-full rounded-control! items-start! justify-between! gap-2 px-3! py-3! text-left sm:min-h-20 aria-[current=date]:border-brand">
                <span className="min-w-0">
                  <span className="block font-bold">{day}</span>
                  <span className={`${entry && entry.workout_type !== "rest_day" ? "text-action" : styles.muted} mt-1 block wrap-anywhere`}>
                    {label ?? "No plan"}
                  </span>
                </span>
                <Pencil size={14} aria-hidden="true" className={`${styles.muted} mt-0.5 shrink-0`} />
              </PulseButton>
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
