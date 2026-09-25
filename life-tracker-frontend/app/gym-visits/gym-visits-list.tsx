"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Dumbbell, ArrowRight } from "lucide-react";

export type GymVisit = {
  id: number;
  created_at: string;
};

interface GymVisitsListProps {
  visits: GymVisit[];
}

const dateFormatter = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "full",
});

const timeFormatter = new Intl.DateTimeFormat("en-IN", {
  hour: "numeric",
  minute: "2-digit",
});

export default function GymVisitsList({ visits }: GymVisitsListProps) {
  const [visitsByDate, setVisitsByDate] = useState<[string, GymVisit[]][]>([]);

  useEffect(() => {
    const grouped = new Map<string, GymVisit[]>();
    for (const visit of visits) {
      const date = new Date(visit.created_at);
      const dateLabel = dateFormatter.format(date);
      grouped.set(dateLabel, [...(grouped.get(dateLabel) ?? []), visit]);
    }
    setVisitsByDate([...grouped.entries()]);
  }, [visits]);

  return (
    <div className="space-y-9">
      {visitsByDate.map(([date, dateVisits]) => (
        <section key={date}>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-[0.12em] text-primary">
            {date}
          </h2>
          <div className="space-y-3">
            {dateVisits.map((visit, index) => (
              <Link
                className="flex items-center justify-between rounded-2xl border border-border bg-card p-5 shadow-sm"
                href={`/gym-visits/${visit.id}`}
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
                      Gym visit{" "}
                      {dateVisits.length > 1
                        ? `#${dateVisits.length - index}`
                        : ""}
                    </h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {timeFormatter.format(new Date(visit.created_at))}
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
