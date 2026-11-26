import { PulseCardLink } from "@/components/ui/pulse-card";
import { Clock } from "lucide-react";
import { fitnessStyles as styles } from "@/features/fitness/styles/fitness-classes";

import { workoutLabel, type WorkoutClassification } from "@/features/fitness/types/workout-types";

export type WorkoutGroup = {
  date: string;
  date_label: string;
  workouts: (WorkoutClassification & { id: number; time_label: string; duration_seconds?: number | null })[];
};

const monthFormatter = new Intl.DateTimeFormat("en-IN", { month: "short", timeZone: "UTC" });
const dateFormatter = new Intl.DateTimeFormat("en-IN", {
  weekday: "short", day: "numeric", month: "short", timeZone: "UTC",
});

export default function GymVisitsList({ groups }: { groups: WorkoutGroup[] }) {
  return (
    <ul className={styles.recentGrid}>
      {groups.flatMap((group) => {
        const date = new Date(`${group.date}T00:00:00Z`);
        return group.workouts.map((visit) => (
          <li key={visit.id} className="min-w-0">
            <PulseCardLink
              padding="md"
              className={styles.workoutCard}
              href={`/fitness/${visit.id}`}
            >
              <div aria-hidden="true" className={styles.dateBadge}>
                <span className={styles.dateBadgeDay}>{date.getUTCDate()}</span>
                <span className={styles.dateBadgeMonth}>{monthFormatter.format(date)}</span>
              </div>
              <div className="min-w-0">
                <h3 className={`${styles.workoutTitle} wrap-anywhere`}>{workoutLabel(visit) ?? "Gym visit"}</h3>
                <time dateTime={group.date} aria-label={group.date_label} className={styles.workoutDate}>{dateFormatter.format(date)}</time>
                <span className={styles.timeChip}>
                  <Clock size={15} className="shrink-0" aria-hidden="true" />{visit.time_label}
                </span>
              </div>
            </PulseCardLink>
          </li>
        ));
      })}
    </ul>
  );
}
