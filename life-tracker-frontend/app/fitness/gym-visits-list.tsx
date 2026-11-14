import { PulseCardLink } from "@/app/components/design-system/pulse-card";
import { ChevronRight } from "lucide-react";

export type WorkoutGroup = {
  date: string;
  date_label: string;
  workouts: { id: number; time_label: string }[];
};

interface GymVisitsListProps {
  groups: WorkoutGroup[];
  hideDate?: boolean;
}

export default function GymVisitsList({ groups, hideDate = false }: GymVisitsListProps) {
  return (
    <ul className="space-y-2">
      {groups.flatMap((group) =>
        group.workouts.map((visit) => (
          <li key={visit.id}>
            <PulseCardLink
              className="flex min-h-12 items-center justify-between gap-3 px-4 py-2.5 text-sm hover:bg-secondary"
              href={`/fitness/${visit.id}`}
            >
              <span>
                <span className="block font-semibold">{hideDate ? visit.time_label : group.date_label}</span>
                <span className="block text-xs text-muted-foreground">{hideDate ? "View workout" : visit.time_label}</span>
              </span>
              <ChevronRight className="h-4 w-4 shrink-0 text-action" aria-hidden="true" />
            </PulseCardLink>
          </li>
        )),
      )}
    </ul>
  );
}
