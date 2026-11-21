"use client";

import { useEffect, useState } from "react";
import { useTimeZone } from "@/app/components/time-zone-provider";

const DAYS = [
  ["Mon", "M"],
  ["Tue", "T"],
  ["Wed", "W"],
  ["Thu", "T"],
  ["Fri", "F"],
  ["Sat", "S"],
  ["Sun", "S"],
];

export default function MealPlanWeekStrip() {
  const { timeZone } = useTimeZone();
  const [today, setToday] = useState<string>();

  useEffect(() => {
    const weekday = new Intl.DateTimeFormat("en-US", { weekday: "short", timeZone });
    const update = () => setToday(weekday.format(new Date()));
    update();
    const timer = window.setInterval(update, 60_000);
    return () => window.clearInterval(timer);
  }, [timeZone]);

  return (
    <span className="pulse-meal-week">
      {DAYS.map(([day, label]) => (
        <span key={day} className="pulse-meal-day" data-today={day === today}>
          {label}
        </span>
      ))}
    </span>
  );
}
