"use client";

import Link from "next/link";
import { Dumbbell, ArrowRight } from "lucide-react";

export type WorkoutGroup = {
  date: string;
  date_label: string;
  workouts: { id: number; time_label: string }[];
};

interface GymVisitsListProps {
  groups: WorkoutGroup[];
}

export default function GymVisitsList({ groups }: GymVisitsListProps) {
  return (
    <div className="space-y-9">
      {groups.map((group) => (
        <section key={group.date}>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-[0.12em] text-primary">
            {group.date_label}
          </h2>
          <div className="space-y-3">
            {group.workouts.map((visit) => (
              <Link
                className="flex items-center justify-between rounded-2xl border border-border bg-card p-5 shadow-sm"
                href={`/fitness/${visit.id}`}
                key={visit.id}
              >
                <div className="flex items-center gap-4">
                  <div
                    className="flex h-11 w-11 items-center justify-center rounded-xl bg-secondary text-secondary-foreground"
                    aria-hidden="true"
                  >
                    <Dumbbell className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-foreground">
                      Workout
                    </h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {visit.time_label}
                    </p>
                  </div>
                </div>
                <span className="flex items-center gap-1 text-sm font-semibold text-primary">
                  Open <ArrowRight className="h-4 w-4" />
                </span>
              </Link>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
