"use client";

import { useEffect, useState } from "react";
import { getMealPlansAction, type MealPlan } from "@/features/meal-plan/api/meal-plan-actions";
import { addDays, formatYMD, getMonday, mealWeekBounds, parseYMD } from "@/features/meal-plan/utils/meal-week";
import { dateKey } from "@/lib/time-zone/time-zone.mjs";
import { useTimeZone } from "@/providers/profile-preferences-provider";

export interface MealWeekDay {
  date: Date;
  dateStr: string;
  dayShort: string;
  dayFull: string;
  monthDay: string;
  isToday: boolean;
  isSelected: boolean;
}

/** The selected day and its Monday-to-Sunday week, loading each week's meals once. */
export function useMealWeek(initialMeals: MealPlan[] | null, initialTodayStr?: string) {
  const { timeZone } = useTimeZone();
  const [today, setToday] = useState(() => initialTodayStr ?? dateKey(new Date(), timeZone));
  useEffect(() => {
    const update = () => {
      setToday(dateKey(new Date(), timeZone));
    };
    update();
    const timer = window.setInterval(update, 60000);
    window.addEventListener("focus", update);
    return () => { window.clearInterval(timer); window.removeEventListener("focus", update); };
  }, [timeZone]);
  const [selectedDate, setSelectedDate] = useState(
    () => initialTodayStr ?? dateKey(new Date(), timeZone)
  );
  const [meals, setMeals] = useState<MealPlan[]>(initialMeals ?? []);
  const [loadedWeek, setLoadedWeek] = useState(() => initialMeals && mealWeekBounds(initialTodayStr ?? today).start);
  const [failedWeek, setFailedWeek] = useState<string | null>(null);

  const currentMonday = getMonday(parseYMD(selectedDate));
  const weekEnd = addDays(currentMonday, 6);
  const weekDays: MealWeekDay[] = Array.from({ length: 7 }, (_, i) => {
    const d = addDays(currentMonday, i);
    const dateStr = formatYMD(d);
    return {
      date: d,
      dateStr,
      dayShort: d.toLocaleDateString("en-IN", { timeZone: "UTC", weekday: "short" }),
      dayFull: d.toLocaleDateString("en-IN", { timeZone: "UTC", weekday: "long" }),
      monthDay: d.toLocaleDateString("en-IN", { timeZone: "UTC", month: "short", day: "numeric" }),
      isToday: dateStr === today,
      isSelected: dateStr === selectedDate,
    };
  });

  const { start: weekStartStr, end: weekEndStr } = mealWeekBounds(selectedDate);
  const weekError = failedWeek === weekStartStr;
  const isLoadingWeek = loadedWeek !== weekStartStr && !weekError;

  useEffect(() => {
    if (loadedWeek === weekStartStr || weekError) return;
    let cancelled = false;

    getMealPlansAction(weekStartStr, weekEndStr).then((res) => {
      if (cancelled) return;
      if (res.ok && res.meals) {
        setMeals(res.meals);
        setLoadedWeek(weekStartStr);
      } else {
        setFailedWeek(weekStartStr);
      }
    });

    return () => {
      cancelled = true;
    };
  }, [loadedWeek, weekError, weekStartStr, weekEndStr]);

  return {
    selectedDate,
    setSelectedDate,
    selectedDay: weekDays.find((day) => day.isSelected)!,
    isToday: selectedDate === today,
    currentMonday,
    weekEnd,
    weekDays,
    weekMeals: loadedWeek === weekStartStr ? meals : [],
    setMeals,
    isLoadingWeek,
    weekError,
    retryWeek: () => setFailedWeek(null),
    moveWeek: (days: number) => setSelectedDate((date) => formatYMD(addDays(parseYMD(date), days))),
    goToToday: () => setSelectedDate(dateKey(new Date(), timeZone)),
  };
}
