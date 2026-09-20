"use client";

import { useState, useEffect, useTransition, useId } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
  Plus,
  Trash2,
  Utensils,
  Clock,
  Settings2,
  Loader2,
} from "lucide-react";
import Dialog, { DialogActions, DialogAction } from "../components/design-system/dialog";
import ConfirmationDialog from "../components/design-system/confirmation-dialog";
import {
  MealPlan,
  MealTime,
  addMealPlanAction,
  deleteMealPlanAction,
  getMealPlansAction,
  createMealTimeAction,
  deleteMealTimeAction,
} from "./actions";

interface MealPlanClientProps {
  initialMeals: MealPlan[];
  initialMealTimes: MealTime[];
}

function formatYMD(d: Date): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getMonday(d: Date): Date {
  const date = new Date(d);
  const day = date.getDay();
  const diff = (day === 0 ? -6 : 1) - day;
  date.setDate(date.getDate() + diff);
  date.setHours(0, 0, 0, 0);
  return date;
}

function addDays(d: Date, days: number): Date {
  const res = new Date(d);
  res.setDate(res.getDate() + days);
  return res;
}

function formatTimeDisplay(timeStr?: string): string {
  if (!timeStr) return "";
  const parts = timeStr.split(":");
  if (parts.length < 2) return timeStr;
  const hours = parseInt(parts[0], 10);
  const minutes = parts[1];
  const ampm = hours >= 12 ? "PM" : "AM";
  const formattedHours = hours % 12 === 0 ? 12 : hours % 12;
  return `${formattedHours}:${minutes} ${ampm}`;
}

export default function MealPlanClient({
  initialMeals,
  initialMealTimes,
}: MealPlanClientProps) {
  const [todayStr] = useState(() => formatYMD(new Date()));
  const [currentMonday, setCurrentMonday] = useState(() => getMonday(new Date()));
  const [selectedDate, setSelectedDate] = useState(() => formatYMD(new Date()));
  const [meals, setMeals] = useState<MealPlan[]>(initialMeals);
  const [mealTimes, setMealTimes] = useState<MealTime[]>(initialMealTimes);
  const [isLoadingWeek, setIsLoadingWeek] = useState(false);

  // Add Meal Modal state
  const [isAddMealOpen, setIsAddMealOpen] = useState(false);
  const [selectedMealTimeID, setSelectedMealTimeID] = useState<number | null>(null);
  const [mealName, setMealName] = useState("");
  const [mealModalError, setMealModalError] = useState("");

  // Add Custom Meal Time Modal state
  const [isAddMealTimeOpen, setIsAddMealTimeOpen] = useState(false);
  const [newMealTimeName, setNewMealTimeName] = useState("");
  const [newStartTime, setNewStartTime] = useState("16:00");
  const [newEndTime, setNewEndTime] = useState("17:00");
  const [mealTimeModalError, setMealTimeModalError] = useState("");

  // Manage Meal Times Modal state
  const [isManageMealTimesOpen, setIsManageMealTimesOpen] = useState(false);

  // Deletion confirmation state
  const [mealToDelete, setMealToDelete] = useState<MealPlan | null>(null);
  const [mealTimeToDelete, setMealTimeToDelete] = useState<MealTime | null>(null);

  const [isPending, startTransition] = useTransition();

  const addMealTitleId = useId();
  const addMealTimeTitleId = useId();
  const manageMealTimesTitleId = useId();

  const weekDays = Array.from({ length: 7 }, (_, i) => {
    const d = addDays(currentMonday, i);
    const dateStr = formatYMD(d);
    return {
      date: d,
      dateStr,
      dayShort: d.toLocaleDateString(undefined, { weekday: "short" }),
      dayFull: d.toLocaleDateString(undefined, { weekday: "long" }),
      monthDay: d.toLocaleDateString(undefined, { month: "short", day: "numeric" }),
      isToday: dateStr === todayStr,
      isSelected: dateStr === selectedDate,
      mealsCount: meals.filter((m) => m.date === dateStr).length,
    };
  });

  const weekStartStr = formatYMD(currentMonday);
  const weekEndStr = formatYMD(addDays(currentMonday, 6));

  useEffect(() => {
    let cancelled = false;
    setIsLoadingWeek(true);

    getMealPlansAction(weekStartStr, weekEndStr).then((res) => {
      if (cancelled) return;
      setIsLoadingWeek(false);
      if (res.ok && res.meals) {
        setMeals(res.meals);
      }
    });

    return () => {
      cancelled = true;
    };
  }, [weekStartStr, weekEndStr]);

  const handlePrevWeek = () => setCurrentMonday((prev) => addDays(prev, -7));
  const handleNextWeek = () => setCurrentMonday((prev) => addDays(prev, 7));
  const handleCurrentWeek = () => {
    const today = new Date();
    setCurrentMonday(getMonday(today));
    setSelectedDate(formatYMD(today));
  };

  const selectedDayObj = weekDays.find((d) => d.dateStr === selectedDate) ?? {
    dayFull: new Date(selectedDate + "T00:00:00").toLocaleDateString(undefined, {
      weekday: "long",
    }),
    monthDay: new Date(selectedDate + "T00:00:00").toLocaleDateString(undefined, {
      month: "long",
      day: "numeric",
      year: "numeric",
    }),
    isToday: selectedDate === todayStr,
  };

  const dayMeals = meals.filter((m) => m.date === selectedDate);

  // Open add meal modal, optionally preselecting a meal time slot
  const handleOpenAddMeal = (mealTimeID?: number) => {
    setMealName("");
    setMealModalError("");
    setSelectedMealTimeID(mealTimeID ?? (mealTimes.length > 0 ? mealTimes[0].id : null));
    setIsAddMealOpen(true);
  };

  const handleCloseAddMeal = () => {
    setIsAddMealOpen(false);
    setMealName("");
    setMealModalError("");
  };

  const handleSubmitAddMeal = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = mealName.trim();
    if (!trimmed) {
      setMealModalError("Please enter a meal name.");
      return;
    }

    startTransition(async () => {
      setMealModalError("");
      const res = await addMealPlanAction(selectedDate, trimmed, selectedMealTimeID ?? undefined);
      if (!res.ok || !res.meal) {
        setMealModalError(res.error ?? "Failed to add meal.");
        return;
      }

      setMeals((prev) => [...prev, res.meal!]);
      handleCloseAddMeal();
    });
  };

  const handleConfirmDeleteMeal = () => {
    if (!mealToDelete) return;
    startTransition(async () => {
      const res = await deleteMealPlanAction(mealToDelete.id);
      if (res.ok) {
        setMeals((prev) => prev.filter((m) => m.id !== mealToDelete.id));
        setMealToDelete(null);
      }
    });
  };

  // Add Custom Meal Time handlers
  const handleOpenAddMealTime = () => {
    setNewMealTimeName("");
    setNewStartTime("16:00");
    setNewEndTime("17:00");
    setMealTimeModalError("");
    setIsAddMealTimeOpen(true);
  };

  const handleCloseAddMealTime = () => {
    setIsAddMealTimeOpen(false);
    setNewMealTimeName("");
    setMealTimeModalError("");
  };

  const handleSubmitAddMealTime = (e: React.FormEvent) => {
    e.preventDefault();
    const name = newMealTimeName.trim();
    if (!name) {
      setMealTimeModalError("Please enter a meal time name.");
      return;
    }
    if (!newStartTime || !newEndTime) {
      setMealTimeModalError("Please provide both start and end times.");
      return;
    }

    startTransition(async () => {
      setMealTimeModalError("");
      const res = await createMealTimeAction(name, newStartTime, newEndTime);
      if (!res.ok || !res.mealTime) {
        setMealTimeModalError(res.error ?? "Failed to create meal time.");
        return;
      }

      setMealTimes((prev) => [...prev, res.mealTime!]);
      setSelectedMealTimeID(res.mealTime!.id);
      handleCloseAddMealTime();
    });
  };

  const handleConfirmDeleteMealTime = () => {
    if (!mealTimeToDelete) return;
    startTransition(async () => {
      const res = await deleteMealTimeAction(mealTimeToDelete.id);
      if (res.ok) {
        setMealTimes((prev) => prev.filter((mt) => mt.id !== mealTimeToDelete.id));
        setMealTimeToDelete(null);
      }
    });
  };

  const formattedWeekRange = `${currentMonday.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
  })} – ${addDays(currentMonday, 6).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  })}`;

  // Check for any meals with no assigned meal time
  const unassignedMeals = dayMeals.filter((m) => !m.meal_time_id);

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        {/* Top Header */}
        <header className="mb-10 flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <Link
              className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit"
              href="/dashboard"
            >
              <ArrowLeft className="h-4 w-4" /> Dashboard
            </Link>
            <p className="mt-5 text-sm font-semibold tracking-[0.18em] text-primary uppercase">
              Life Tracker
            </p>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              Meal Plan
            </h1>
            <p className="mt-3 text-base text-muted-foreground">
              Plan and track your daily meals.
            </p>
          </div>
          <div className="flex items-center gap-2.5 shrink-0 sm:pt-2">
            <button
              onClick={() => setIsManageMealTimesOpen(true)}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-card px-3.5 py-2 text-xs font-semibold text-foreground shadow-xs transition hover:bg-secondary"
            >
              <Settings2 className="h-3.5 w-3.5 text-muted-foreground" />
              Meal Times
            </button>
            <button
              onClick={handleCurrentWeek}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-card px-3.5 py-2 text-xs font-semibold text-foreground shadow-xs transition hover:bg-secondary"
            >
              <CalendarIcon className="h-3.5 w-3.5 text-muted-foreground" />
              Current Week
            </button>
          </div>
        </header>

        {/* Week Navigator & Calendar Bar */}
        <section className="rounded-2xl border border-border bg-card p-5 sm:p-6 mb-10 shadow-sm">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2.5">
              <span className="text-base font-semibold text-foreground">
                {formattedWeekRange}
              </span>
              {isLoadingWeek && (
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
              )}
            </div>
            <div className="flex items-center gap-1.5">
              <button
                onClick={handlePrevWeek}
                aria-label="Previous week"
                className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-card text-foreground transition hover:bg-secondary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <button
                onClick={handleNextWeek}
                aria-label="Next week"
                className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-card text-foreground transition hover:bg-secondary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-7 gap-2 sm:gap-3">
            {weekDays.map((day) => {
              const active = day.isSelected;
              return (
                <button
                  key={day.dateStr}
                  onClick={() => setSelectedDate(day.dateStr)}
                  className={`group relative flex flex-col items-center justify-center rounded-xl p-3 sm:p-3.5 text-center transition focus:outline-hidden focus:ring-2 focus:ring-primary/30 ${
                    active
                      ? "bg-primary text-primary-foreground shadow-md font-semibold"
                      : "border border-border/70 bg-background/50 hover:bg-secondary/60 text-foreground"
                  }`}
                >
                  <span
                    className={`text-xs uppercase tracking-wider ${
                      active
                        ? "text-primary-foreground/90 font-bold"
                        : "text-muted-foreground font-medium"
                    }`}
                  >
                    {day.dayShort}
                  </span>
                  <span
                    className={`mt-1 text-sm sm:text-base ${
                      active ? "font-bold text-primary-foreground" : "font-semibold"
                    }`}
                  >
                    {day.date.getDate()}
                  </span>

                  <div className="mt-1.5 flex items-center gap-1 min-h-[6px]">
                    {day.isToday && (
                      <span
                        className={`h-1.5 w-1.5 rounded-full ${
                          active ? "bg-primary-foreground" : "bg-primary"
                        }`}
                        title="Today"
                      />
                    )}
                    {day.mealsCount > 0 && (
                      <span
                        className={`text-[10px] leading-none px-1 rounded-full ${
                          active
                            ? "bg-primary-foreground/25 text-primary-foreground"
                            : "bg-secondary text-secondary-foreground font-medium"
                        }`}
                      >
                        {day.mealsCount}
                      </span>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </section>

        {/* Selected Day Header */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
          <div>
            <div className="flex items-center gap-2.5">
              <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
                {selectedDayObj.dayFull}
              </h2>
              {selectedDayObj.isToday && (
                <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                  Today
                </span>
              )}
            </div>
            <p className="text-sm text-muted-foreground mt-1">
              {new Date(selectedDate + "T00:00:00").toLocaleDateString(undefined, {
                month: "long",
                day: "numeric",
                year: "numeric",
              })}
            </p>
          </div>

          {dayMeals.length > 0 && (
            <button
              onClick={() => handleOpenAddMeal()}
              className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-xs transition hover:bg-primary/90 focus:outline-hidden focus:ring-2 focus:ring-primary/20"
            >
              <Plus className="h-4 w-4" />
              Add meal
            </button>
          )}
        </div>

        {/* Meal Times Sections or Empty State */}
        {dayMeals.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border bg-card/60 p-12 sm:p-16 text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-secondary text-secondary-foreground mb-4">
              <Utensils className="h-7 w-7 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold text-foreground">
              No meals planned for this day yet
            </h3>
            <p className="mt-1.5 text-sm text-muted-foreground max-w-sm mx-auto">
              Start planning your meals for {selectedDayObj.dayFull}.
            </p>
            <div className="mt-6">
              <button
                onClick={() => handleOpenAddMeal()}
                className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-sm transition hover:bg-primary/90"
              >
                <Plus className="h-4 w-4" />
                Add meal
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="grid gap-5 sm:grid-cols-2">
              {mealTimes.map((mt) => {
                const slotMeals = dayMeals.filter((m) => m.meal_time_id === mt.id);
                return (
                  <div
                    key={mt.id}
                    className="flex flex-col justify-between rounded-2xl border border-border bg-card p-6 shadow-xs"
                  >
                    <div>
                      <div className="flex items-center justify-between gap-2 border-b border-border/70 pb-3 mb-3">
                        <div className="min-w-0">
                          <h3 className="font-semibold text-foreground truncate text-base">
                            {mt.name}
                          </h3>
                          <div className="flex items-center gap-1 text-xs text-muted-foreground mt-0.5">
                            <Clock className="h-3 w-3 shrink-0" />
                            <span>
                              {formatTimeDisplay(mt.start_time)} – {formatTimeDisplay(mt.end_time)}
                            </span>
                          </div>
                        </div>
                        <button
                          onClick={() => handleOpenAddMeal(mt.id)}
                          title={`Add to ${mt.name}`}
                          className="inline-flex items-center gap-1 rounded-md bg-secondary/80 hover:bg-secondary px-2.5 py-1 text-xs font-semibold text-foreground transition"
                        >
                          <Plus className="h-3.5 w-3.5" />
                          Add
                        </button>
                      </div>

                      {slotMeals.length === 0 ? (
                        <p className="text-xs text-muted-foreground italic py-3">
                          Nothing planned yet.
                        </p>
                      ) : (
                        <ul className="divide-y divide-border/50">
                          {slotMeals.map((meal) => (
                            <li
                              key={meal.id}
                              className="flex items-center justify-between py-2.5 group"
                            >
                              <div className="flex items-center gap-2.5 min-w-0">
                                <span className="h-1.5 w-1.5 rounded-full bg-primary shrink-0" />
                                <span className="text-sm font-medium text-foreground truncate">
                                  {meal.name}
                                </span>
                              </div>
                              <button
                                onClick={() => setMealToDelete(meal)}
                                aria-label={`Delete ${meal.name}`}
                                disabled={isPending}
                                className="opacity-70 hover:opacity-100 p-1 text-muted-foreground hover:text-destructive transition"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Unassigned meals if any */}
            {unassignedMeals.length > 0 && (
              <div className="mt-6 rounded-2xl border border-border bg-card p-5 shadow-xs">
                <h3 className="font-semibold text-foreground text-base mb-3 border-b border-border/70 pb-2">
                  Other Meals
                </h3>
                <ul className="divide-y divide-border/50">
                  {unassignedMeals.map((meal) => (
                    <li key={meal.id} className="flex items-center justify-between py-2.5">
                      <span className="text-sm font-medium text-foreground">{meal.name}</span>
                      <button
                        onClick={() => setMealToDelete(meal)}
                        aria-label={`Delete ${meal.name}`}
                        disabled={isPending}
                        className="p-1 text-muted-foreground hover:text-destructive transition"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </>
        )}
      </div>

      {/* Add Meal Modal Dialog */}
      {isAddMealOpen && (
        <Dialog
          labelledBy={addMealTitleId}
          onBackdropClick={(e) => {
            if (e.target === e.currentTarget) handleCloseAddMeal();
          }}
          size="sm"
        >
          <form onSubmit={handleSubmitAddMeal}>
            <div className="flex items-center gap-3 mb-4">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Utensils className="h-5 w-5" />
              </div>
              <div>
                <h2 id={addMealTitleId} className="text-lg font-bold text-foreground">
                  Add Meal
                </h2>
                <p className="text-xs text-muted-foreground">
                  For {selectedDayObj.dayFull},{" "}
                  {new Date(selectedDate + "T00:00:00").toLocaleDateString(undefined, {
                    month: "short",
                    day: "numeric",
                  })}
                </p>
              </div>
            </div>

            {mealModalError && (
              <div
                className="mb-4 rounded-lg bg-destructive/10 p-3 text-xs text-destructive"
                role="alert"
              >
                {mealModalError}
              </div>
            )}

            {/* Meal Time Selector */}
            <div className="space-y-1.5 mb-4">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="meal-time-select"
                  className="block text-xs font-semibold text-foreground uppercase tracking-wider"
                >
                  Time of Day
                </label>
                <button
                  type="button"
                  onClick={handleOpenAddMealTime}
                  className="text-xs text-primary hover:underline inline-flex items-center gap-1"
                >
                  <Plus className="h-3 w-3" /> New meal time
                </button>
              </div>
              <select
                id="meal-time-select"
                value={selectedMealTimeID ?? ""}
                onChange={(e) =>
                  setSelectedMealTimeID(e.target.value ? parseInt(e.target.value, 10) : null)
                }
                className="w-full rounded-lg border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
              >
                {mealTimes.map((mt) => (
                  <option key={mt.id} value={mt.id}>
                    {mt.name} ({formatTimeDisplay(mt.start_time)} – {formatTimeDisplay(mt.end_time)})
                  </option>
                ))}
              </select>
            </div>

            {/* Meal Name Input */}
            <div className="space-y-1.5">
              <label
                htmlFor="meal-name-input"
                className="block text-xs font-semibold text-foreground uppercase tracking-wider"
              >
                Meal Name
              </label>
              <input
                id="meal-name-input"
                type="text"
                autoFocus
                value={mealName}
                onChange={(e) => setMealName(e.target.value)}
                placeholder="e.g., Scrambled Eggs with Avocado"
                className="w-full rounded-lg border border-border bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
              />
            </div>

            <DialogActions className="mt-6">
              <DialogAction
                type="button"
                variant="secondary"
                onClick={handleCloseAddMeal}
                disabled={isPending}
              >
                Cancel
              </DialogAction>
              <DialogAction
                type="submit"
                variant="primary"
                disabled={isPending || !mealName.trim()}
              >
                {isPending ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin" /> Adding...
                  </span>
                ) : (
                  "Add Meal"
                )}
              </DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}

      {/* Add Custom Meal Time Modal Dialog */}
      {isAddMealTimeOpen && (
        <Dialog
          labelledBy={addMealTimeTitleId}
          onBackdropClick={(e) => {
            if (e.target === e.currentTarget) handleCloseAddMealTime();
          }}
          size="sm"
        >
          <form onSubmit={handleSubmitAddMealTime}>
            <div className="flex items-center gap-3 mb-4">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Clock className="h-5 w-5" />
              </div>
              <div>
                <h2 id={addMealTimeTitleId} className="text-lg font-bold text-foreground">
                  New Meal Time
                </h2>
                <p className="text-xs text-muted-foreground">
                  Add a custom time of day and its time range
                </p>
              </div>
            </div>

            {mealTimeModalError && (
              <div
                className="mb-4 rounded-lg bg-destructive/10 p-3 text-xs text-destructive"
                role="alert"
              >
                {mealTimeModalError}
              </div>
            )}

            <div className="space-y-3">
              <div>
                <label
                  htmlFor="meal-time-name-input"
                  className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1"
                >
                  Meal Time Name
                </label>
                <input
                  id="meal-time-name-input"
                  type="text"
                  autoFocus
                  value={newMealTimeName}
                  onChange={(e) => setNewMealTimeName(e.target.value)}
                  placeholder="e.g., Pre-workout Snack"
                  className="w-full rounded-lg border border-border bg-background px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label
                    htmlFor="start-time-input"
                    className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1"
                  >
                    Start Time
                  </label>
                  <input
                    id="start-time-input"
                    type="time"
                    value={newStartTime}
                    onChange={(e) => setNewStartTime(e.target.value)}
                    className="w-full rounded-lg border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
                  />
                </div>
                <div>
                  <label
                    htmlFor="end-time-input"
                    className="block text-xs font-semibold text-foreground uppercase tracking-wider mb-1"
                  >
                    End Time
                  </label>
                  <input
                    id="end-time-input"
                    type="time"
                    value={newEndTime}
                    onChange={(e) => setNewEndTime(e.target.value)}
                    className="w-full rounded-lg border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
                  />
                </div>
              </div>
            </div>

            <DialogActions className="mt-6">
              <DialogAction
                type="button"
                variant="secondary"
                onClick={handleCloseAddMealTime}
                disabled={isPending}
              >
                Cancel
              </DialogAction>
              <DialogAction
                type="submit"
                variant="primary"
                disabled={isPending || !newMealTimeName.trim()}
              >
                {isPending ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin" /> Saving...
                  </span>
                ) : (
                  "Create Meal Time"
                )}
              </DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}

      {/* Manage Meal Times Modal Dialog */}
      {isManageMealTimesOpen && (
        <Dialog
          labelledBy={manageMealTimesTitleId}
          onBackdropClick={(e) => {
            if (e.target === e.currentTarget) setIsManageMealTimesOpen(false);
          }}
          size="md"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Settings2 className="h-5 w-5" />
              </div>
              <div>
                <h2 id={manageMealTimesTitleId} className="text-lg font-bold text-foreground">
                  Meal Times & Ranges
                </h2>
                <p className="text-xs text-muted-foreground">
                  Configured times of day for your meals
                </p>
              </div>
            </div>
            <button
              onClick={() => {
                setIsManageMealTimesOpen(false);
                handleOpenAddMealTime();
              }}
              className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground transition hover:bg-primary/90"
            >
              <Plus className="h-3.5 w-3.5" />
              Add Custom
            </button>
          </div>

          <ul className="divide-y divide-border/60 my-4 max-h-80 overflow-y-auto pr-1">
            {mealTimes.map((mt) => (
              <li key={mt.id} className="flex items-center justify-between py-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-sm text-foreground">{mt.name}</span>
                    {mt.is_default && (
                      <span className="rounded-full bg-secondary px-2 py-0.5 text-[10px] font-medium text-secondary-foreground">
                        Default
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-1 text-xs text-muted-foreground mt-0.5">
                    <Clock className="h-3 w-3" />
                    <span>
                      {formatTimeDisplay(mt.start_time)} – {formatTimeDisplay(mt.end_time)}
                    </span>
                  </div>
                </div>

                {!mt.is_default && (
                  <button
                    onClick={() => setMealTimeToDelete(mt)}
                    aria-label={`Delete ${mt.name}`}
                    disabled={isPending}
                    className="p-1.5 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded-lg transition disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </li>
            ))}
          </ul>

          <div className="mt-4 flex justify-end">
            <DialogAction
              type="button"
              variant="secondary"
              onClick={() => setIsManageMealTimesOpen(false)}
            >
              Close
            </DialogAction>
          </div>
        </Dialog>
      )}

      {/* Delete Meal Confirmation Dialog */}
      <ConfirmationDialog
        isOpen={mealToDelete !== null}
        onClose={() => setMealToDelete(null)}
        onConfirm={handleConfirmDeleteMeal}
        title="Delete Meal?"
        description={
          mealToDelete ? (
            <span>
              Are you sure you want to remove <strong>{mealToDelete.name}</strong> from your meal plan for {selectedDayObj.dayFull}?
            </span>
          ) : null
        }
        confirmText="Yes, delete"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        variant="destructive"
      />

      {/* Delete Meal Time Confirmation Dialog */}
      <ConfirmationDialog
        isOpen={mealTimeToDelete !== null}
        onClose={() => setMealTimeToDelete(null)}
        onConfirm={handleConfirmDeleteMealTime}
        title="Delete Meal Time?"
        description={
          mealTimeToDelete ? (
            <span>
              Are you sure you want to delete the custom meal time <strong>{mealTimeToDelete.name}</strong>? Any meals assigned to it will remain in your plan under Other Meals.
            </span>
          ) : null
        }
        confirmText="Yes, delete"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        variant="destructive"
      />
    </main>
  );
}
