"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import { useTimeZone } from "@/app/components/time-zone-provider";

import { dateKey } from "@/app/lib/time-zone.mjs";

import Button from "@/app/components/design-system/button";

import { useState, useEffect, useTransition, useId } from "react";
import BackLink from "@/app/components/design-system/back-link";
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  Trash2,
  Utensils,
  Clock,
  Settings2,
  Loader2,
  Check,
} from "lucide-react";
import Dialog, { DialogActions, DialogAction } from "@/app/components/design-system/dialog";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import {
  MealPlan,
  MealTime,
  addMealPlanAction,
  deleteMealPlanAction,
  getMealPlansAction,
  createMealTimeAction,
  deleteMealTimeAction,
  updateMealPlanConsumedAction,
} from "./actions";

interface MealPlanClientProps {
  initialMeals: MealPlan[];
  initialMealTimes: MealTime[];
  initialTodayStr?: string;
}


function parseYMD(dateStr: string): Date {
  return new Date(`${dateStr}T00:00:00Z`);
}

function formatYMD(d: Date): string {
  const year = d.getUTCFullYear();
  const month = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getMonday(d: Date): Date {
  const date = new Date(d);
  const day = date.getUTCDay();
  const diff = (day === 0 ? -6 : 1) - day;
  date.setUTCDate(date.getUTCDate() + diff);
  date.setUTCHours(0, 0, 0, 0);
  return date;
}

function addDays(d: Date, days: number): Date {
  const res = new Date(d);
  res.setUTCDate(res.getUTCDate() + days);
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
  initialTodayStr,
}: MealPlanClientProps) {
  const { timeZone } = useTimeZone();
  const [todayStr, setTodayStr] = useState(() => initialTodayStr ?? dateKey(new Date(), timeZone));
  useEffect(() => {
    const update = () => setTodayStr(dateKey(new Date(), timeZone));
    update();
    const timer = window.setInterval(update, 60000);
    window.addEventListener("focus", update);
    return () => { window.clearInterval(timer); window.removeEventListener("focus", update); };
  }, [timeZone]);
  const [selectedDate, setSelectedDate] = useState(
    () => initialTodayStr ?? dateKey(new Date(), timeZone)
  );
  const [meals, setMeals] = useState<MealPlan[]>(initialMeals);
  const [mealTimes, setMealTimes] = useState<MealTime[]>(initialMealTimes);
  const [isLoadingWeek, setIsLoadingWeek] = useState(false);

  // Meal form state
  const [isAddMealOpen, setIsAddMealOpen] = useState(false);
  const [selectedMealTimeID, setSelectedMealTimeID] = useState<number | null>(null);
  const [mealName, setMealName] = useState("");
  const [mealModalError, setMealModalError] = useState("");

  // New meal time Meal Time Modal state
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

  const [deleteError, setDeleteError] = useState("");
  const addMealTitleId = useId();
  const addMealTimeTitleId = useId();
  const manageMealTimesTitleId = useId();

  const currentMonday = getMonday(parseYMD(selectedDate));
  const weekDays = Array.from({ length: 7 }, (_, i) => {
    const d = addDays(currentMonday, i);
    const dateStr = formatYMD(d);
    return {
      date: d,
      dateStr,
      dayShort: d.toLocaleDateString("en-IN", { timeZone: "UTC", weekday: "short" }),
      dayFull: d.toLocaleDateString("en-IN", { timeZone: "UTC", weekday: "long" }),
      monthDay: d.toLocaleDateString("en-IN", { timeZone: "UTC", month: "short", day: "numeric" }),
      isToday: dateStr === todayStr,
      isSelected: dateStr === selectedDate,
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

  const moveWeek = (days: number) => setSelectedDate((date) => formatYMD(addDays(parseYMD(date), days)));
  const handleToday = () => setSelectedDate(dateKey(new Date(), timeZone));

  const selectedDay = weekDays.find((day) => day.isSelected)!;
  const dayMeals = meals.filter((meal) => meal.date === selectedDate);
  const orderedMealTimes = [...mealTimes].sort((a, b) => a.start_time.localeCompare(b.start_time));
  const otherMeals = dayMeals.filter((meal) => !mealTimes.some((time) => time.id === meal.meal_time_id));
  const eatenCount = dayMeals.filter((meal) => meal.is_consumed).length;

  // Open add meal modal, optionally preselecting a meal time slot
  const handleOpenAddMeal = (mealTimeID?: number) => {
    setMealName("");
    setMealModalError("");
    setSelectedMealTimeID(mealTimeID ?? orderedMealTimes[0]?.id ?? null);
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
    setDeleteError("");
    startTransition(async () => {
      const res = await deleteMealPlanAction(mealToDelete.id);
      if (res.ok) {
        setMeals((prev) => prev.filter((m) => m.id !== mealToDelete.id));
        setMealToDelete(null);
      } else {
        setDeleteError(res.error ?? "Could not remove the meal.");
      }
    });
  };

  const handleToggleConsumed = (meal: MealPlan) => {
    const nextConsumed = !meal.is_consumed;
    // Optimistic update
    setMeals((prev) =>
      prev.map((m) => (m.id === meal.id ? { ...m, is_consumed: nextConsumed } : m))
    );

    startTransition(async () => {
      const res = await updateMealPlanConsumedAction(meal.id, nextConsumed);
      if (!res.ok) {
        // Revert on failure
        setMeals((prev) =>
          prev.map((m) => (m.id === meal.id ? { ...m, is_consumed: !nextConsumed } : m))
        );
      }
    });
  };

  // New meal time Meal Time handlers
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
    setDeleteError("");
    startTransition(async () => {
      const res = await deleteMealTimeAction(mealTimeToDelete.id);
      if (res.ok) {
        setMealTimes((prev) => prev.filter((mt) => mt.id !== mealTimeToDelete.id));
        setMealTimeToDelete(null);
      } else {
        setDeleteError(res.error ?? "Could not remove the meal time.");
      }
    });
  };

  const formattedWeekRange = `${currentMonday.toLocaleDateString("en-IN", { timeZone: "UTC",
    month: "short",
    day: "numeric",
  })} – ${addDays(currentMonday, 6).toLocaleDateString("en-IN", { timeZone: "UTC",
    month: "short",
    day: "numeric",
    year: "numeric",
  })}`;

  const renderMeals = (items: MealPlan[]) => (
    <ul className="divide-y divide-border/60">
      {items.map((meal) => (
        <li key={meal.id} className="flex items-center gap-2 py-2 first:pt-0 last:pb-0">
          <Button
            variant={meal.is_consumed ? "soft" : "tertiary"}
            size="sm"
            type="button"
            role="checkbox"
            aria-checked={meal.is_consumed}
            aria-label={`Eaten: ${meal.name}`}
            onClick={() => handleToggleConsumed(meal)}
            disabled={isPending}
            className="min-w-0 flex-1 justify-start! text-left"
          >
            <span className="flex size-4 shrink-0 items-center justify-center rounded border border-current">
              {meal.is_consumed && <Check className="size-3" aria-hidden="true" />}
            </span>
            <span className={`min-w-0 wrap-anywhere text-sm ${meal.is_consumed ? "text-muted-foreground line-through" : "text-foreground"}`}>
              {meal.name}
            </span>
          </Button>
          <Button variant="iconDanger" size="md" type="button"
            onClick={() => { setDeleteError(""); setMealToDelete(meal); }}
            aria-label={`Delete ${meal.name}`}
            disabled={isPending}
            className="shrink-0"
          >
            <Trash2 className="size-4" aria-hidden="true" />
          </Button>
        </li>
      ))}
    </ul>
  );

  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-8 sm:py-10">
      <div className="mx-auto max-w-5xl" inert={isAddMealOpen || isAddMealTimeOpen || isManageMealTimesOpen || mealToDelete !== null || mealTimeToDelete !== null}>
        <BackLink href="/dashboard">Dashboard</BackLink>

        <PulseCard as="section" aria-labelledby="meal-plan-title" className="mt-5 overflow-hidden">
          <div className="flex flex-wrap items-end justify-between gap-4 border-b border-border bg-accent px-4 py-6 text-accent-foreground sm:px-7 sm:py-8">
            <div>
              <h1 id="meal-plan-title" className="text-4xl font-bold tracking-tight sm:text-5xl">Meal Plan</h1>
              <p className="mt-2 text-sm">Plan your week, one meal at a time.</p>
            </div>
            <Button variant="secondary" onClick={() => setIsManageMealTimesOpen(true)}>
              <Settings2 className="size-4" aria-hidden="true" /> Meal times
            </Button>
          </div>

          <div className="p-3 sm:p-6">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2 sm:mb-4">
              <h2 className="text-sm font-semibold sm:text-base">{formattedWeekRange}</h2>
              <div className="flex items-center gap-1">
                <Button variant="tertiary" size="sm" onClick={handleToday}>Today</Button>
                <Button variant="iconSecondary" size="sm" onClick={() => moveWeek(-7)} aria-label="Previous week">
                  <ChevronLeft className="size-4" aria-hidden="true" />
                </Button>
                <Button variant="iconSecondary" size="sm" onClick={() => moveWeek(7)} aria-label="Next week">
                  <ChevronRight className="size-4" aria-hidden="true" />
                </Button>
              </div>
            </div>

            <div className="grid grid-cols-7 gap-1 sm:gap-2" role="group" aria-label="Days of the week">
              {weekDays.map((day) => (
                <Button
                  key={day.dateStr}
                  variant={day.isSelected ? "primary" : "iconSecondary"}
                  aria-label={`${day.dayFull}, ${day.monthDay}`}
                  aria-pressed={day.isSelected}
                  aria-current={day.isToday ? "date" : undefined}
                  onClick={() => setSelectedDate(day.dateStr)}
                  className="h-auto min-h-16 w-full min-w-0 flex-col px-0 py-2 sm:min-h-20"
                >
                  <span className="text-xs font-medium">{day.dayShort}</span>
                  <span className="text-lg leading-none tabular-nums sm:text-2xl">{day.date.getUTCDate()}</span>
                  <span aria-hidden="true" className={`size-1 rounded-full ${day.isToday ? "bg-current" : "invisible"}`} />
                </Button>
              ))}
            </div>
          </div>
        </PulseCard>

        <section aria-labelledby="meal-agenda-title" className="mt-7 sm:mt-9">
          <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 id="meal-agenda-title" className="text-2xl font-bold tracking-tight sm:text-3xl">
                {selectedDay.dayFull}, {selectedDay.monthDay}
              </h2>
              <p className="mt-1 text-sm text-muted-foreground" role="status">
                {isLoadingWeek ? "Loading meals…" : dayMeals.length ? `${eatenCount} of ${dayMeals.length} meals eaten` : "No meals planned yet"}
              </p>
            </div>
            <Button onClick={() => handleOpenAddMeal()} disabled={isLoadingWeek}>
              <Plus className="size-4" aria-hidden="true" /> Add meal
            </Button>
          </div>

          <PulseCard className="overflow-hidden" aria-busy={isLoadingWeek}>
            <div className="divide-y divide-border">
              {orderedMealTimes.map((time) => {
                const items = dayMeals.filter((meal) => meal.meal_time_id === time.id);
                return (
                  <section key={time.id} className="grid grid-cols-[minmax(0,1fr)_auto] gap-x-5 gap-y-3 px-4 py-4 sm:grid-cols-[12rem_minmax(0,1fr)_auto] sm:items-start sm:px-6">
                    <div className="col-start-1 row-start-1 flex min-w-0 items-start gap-3">
                      <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-accent text-accent-foreground">
                        <Clock className="size-4" aria-hidden="true" />
                      </span>
                      <div className="min-w-0">
                        <h3 className="font-semibold wrap-anywhere">{time.name}</h3>
                        <p className="text-xs text-muted-foreground">{formatTimeDisplay(time.start_time)} – {formatTimeDisplay(time.end_time)}</p>
                      </div>
                    </div>
                    <div className="col-span-2 row-start-2 min-w-0 sm:col-span-1 sm:col-start-2 sm:row-start-1">
                      {items.length ? renderMeals(items) : <p className="py-2 text-sm text-muted-foreground">Nothing planned</p>}
                    </div>
                    <Button variant="secondary" size="sm" className="col-start-2 row-start-1 justify-self-end sm:col-start-3"
                      onClick={() => handleOpenAddMeal(time.id)}
                      aria-label={`Add meal to ${time.name}`}
                      disabled={isLoadingWeek}
                    >
                      <Plus className="size-3.5" aria-hidden="true" /> Add
                    </Button>
                  </section>
                );
              })}
              {otherMeals.length > 0 && (
                <section className="grid gap-3 px-4 py-4 sm:grid-cols-[12rem_minmax(0,1fr)] sm:gap-5 sm:px-6">
                  <h3 className="flex items-center gap-3 font-semibold">
                    <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-secondary">
                      <Utensils className="size-4" aria-hidden="true" />
                    </span>
                    Other meals
                  </h3>
                  {renderMeals(otherMeals)}
                </section>
              )}
              {orderedMealTimes.length === 0 && otherMeals.length === 0 && (
                <p className="px-4 py-8 text-sm text-muted-foreground sm:px-6">
                  Add a meal to start planning this day.
                </p>
              )}
            </div>
          </PulseCard>
        </section>
      </div>

      {/* Add meal Modal Dialog */}
      {isAddMealOpen && !isAddMealTimeOpen && (
        <Dialog
          labelledBy={addMealTitleId}
          onBackdropClick={(e) => {
            if (!isPending && e.target === e.currentTarget) handleCloseAddMeal();
          }}
          size="sm"
        >
          <form onSubmit={handleSubmitAddMeal}>
            <div className="mb-5 flex items-start gap-3 border-b border-border pb-4">
              <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-accent text-accent-foreground">
                <Utensils className="h-5 w-5" />
              </div>
              <div>
                <h2 id={addMealTitleId} className="text-xl font-bold tracking-tight text-foreground">
                  Add meal
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  For {selectedDay.dayFull},{" "}
                  {parseYMD(selectedDate).toLocaleDateString("en-IN", { timeZone: "UTC",
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

            {/* Meal name Input */}
            <div className="mb-4 space-y-1.5">
              <label
                htmlFor="meal-name-input"
                className="block text-sm font-semibold text-foreground"
              >
                Meal name
              </label>
              <input
                id="meal-name-input"
                type="text"
                autoFocus
                value={mealName}
                onChange={(e) => setMealName(e.target.value)}
                placeholder="e.g. Scrambled eggs on toast"
                className="min-h-11 w-full min-w-0 rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
              />
            </div>

            {/* Meal Time Selector */}
            <div className="space-y-1.5 mb-4">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="meal-time-select"
                  className="block text-sm font-semibold text-foreground"
                >
                  Meal time
                </label>
                <Button variant="tertiary" size="sm"
                  icon={<Plus className="h-3 w-3" />}
                  type="button"
                  onClick={handleOpenAddMealTime}
                >
                  New meal time
                </Button>
              </div>
              <select
                id="meal-time-select"
                value={selectedMealTimeID ?? ""}
                onChange={(e) =>
                  setSelectedMealTimeID(e.target.value ? parseInt(e.target.value, 10) : null)
                }
                className="min-h-11 w-full min-w-0 rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
              >
                {orderedMealTimes.length === 0 && <option value="">Other meals</option>}
                {orderedMealTimes.map((mt) => (
                  <option key={mt.id} value={mt.id}>
                    {mt.name} ({formatTimeDisplay(mt.start_time)} – {formatTimeDisplay(mt.end_time)})
                  </option>
                ))}
              </select>
            </div>

            <DialogActions className="border-t border-border pt-4">
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
                  "Add meal"
                )}
              </DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}

      {/* New meal time dialog */}
      {isAddMealTimeOpen && (
        <Dialog
          labelledBy={addMealTimeTitleId}
          onBackdropClick={(e) => {
            if (!isPending && e.target === e.currentTarget) handleCloseAddMealTime();
          }}
          size="sm"
        >
          <form onSubmit={handleSubmitAddMealTime}>
            <div className="mb-5 flex items-start gap-3 border-b border-border pb-4">
              <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-accent text-accent-foreground">
                <Clock className="h-5 w-5" />
              </div>
              <div>
                <h2 id={addMealTimeTitleId} className="text-xl font-bold tracking-tight text-foreground">
                  New meal time
                </h2>
                <p className="text-xs text-muted-foreground">
                  Set the hours when this meal time appears in your daily plan.
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
                  className="block text-sm font-semibold text-foreground mb-1"
                >
                  Meal time name
                </label>
                <input
                  id="meal-time-name-input"
                  type="text"
                  autoFocus
                  value={newMealTimeName}
                  onChange={(e) => setNewMealTimeName(e.target.value)}
                  placeholder="e.g. Afternoon snack"
                  className="min-h-11 w-full min-w-0 rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label
                    htmlFor="start-time-input"
                    className="block text-sm font-semibold text-foreground mb-1"
                  >
                    Starts
                  </label>
                  <input
                    id="start-time-input"
                    type="time"
                    value={newStartTime}
                    onChange={(e) => setNewStartTime(e.target.value)}
                    className="min-h-11 w-full min-w-0 rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
                  />
                </div>
                <div>
                  <label
                    htmlFor="end-time-input"
                    className="block text-sm font-semibold text-foreground mb-1"
                  >
                    Ends
                  </label>
                  <input
                    id="end-time-input"
                    type="time"
                    value={newEndTime}
                    onChange={(e) => setNewEndTime(e.target.value)}
                    className="min-h-11 w-full min-w-0 rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-hidden focus:ring-2 focus:ring-primary/20"
                  />
                </div>
              </div>
            </div>

            <DialogActions className="border-t border-border pt-4">
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
                  "Create meal time"
                )}
              </DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}

      {/* Manage Meal Times Modal Dialog */}
      {isManageMealTimesOpen && !isAddMealTimeOpen && !mealTimeToDelete && (
        <Dialog
          labelledBy={manageMealTimesTitleId}
          onBackdropClick={(e) => {
            if (e.target === e.currentTarget) setIsManageMealTimesOpen(false);
          }}
          size="md"
        >
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3 border-b border-border pb-4">
            <div className="flex items-center gap-3">
              <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-accent text-accent-foreground">
                <Settings2 className="h-5 w-5" />
              </div>
              <div>
                <h2 id={manageMealTimesTitleId} className="text-xl font-bold tracking-tight text-foreground">
                  Meal times
                </h2>
                <p className="text-xs text-muted-foreground">
                  Set up the time slots in your daily plan.
                </p>
              </div>
            </div>
            <Button variant="primary" size="sm"
              onClick={handleOpenAddMealTime}
              autoFocus
            >
              <Plus className="h-3.5 w-3.5" />
              New meal time
            </Button>
          </div>

          {orderedMealTimes.length === 0 && <p className="text-sm text-muted-foreground">Add a meal time to organize your daily plan.</p>}
          <ul className="divide-y divide-border rounded-xl border border-border bg-background px-3 empty:hidden">
            {orderedMealTimes.map((mt) => (
              <li key={mt.id} className="flex items-center justify-between gap-2 py-3">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="wrap-anywhere font-semibold text-sm text-foreground">{mt.name}</span>
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
                  <Button variant="iconDanger" size="sm"
                    onClick={() => { setDeleteError(""); setMealTimeToDelete(mt); }}
                    aria-label={`Delete ${mt.name}`}
                    disabled={isPending}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </li>
            ))}
          </ul>

          <DialogActions className="border-t border-border pt-4 sm:flex sm:justify-end">
            <DialogAction
              type="button"
              variant="secondary"
              onClick={() => setIsManageMealTimesOpen(false)}
            >
              Done
            </DialogAction>
          </DialogActions>
        </Dialog>
      )}

      {/* Delete Meal Confirmation Dialog */}
      <ConfirmationDialog
        isOpen={mealToDelete !== null}
        onClose={() => setMealToDelete(null)}
        onConfirm={handleConfirmDeleteMeal}
        title="Remove this meal?"
        description={
          mealToDelete ? (
            <span>
              <strong>{mealToDelete.name}</strong> will be removed from your plan for {selectedDay.dayFull}, {selectedDay.monthDay}.
            </span>
          ) : null
        }
        confirmText="Remove meal"
        confirmLoadingText="Removing…"
        error={deleteError}
        isLoading={isPending}
        variant="destructive"
      />

      {/* Delete Meal Time Confirmation Dialog */}
      <ConfirmationDialog
        isOpen={mealTimeToDelete !== null}
        onClose={() => setMealTimeToDelete(null)}
        onConfirm={handleConfirmDeleteMealTime}
        title="Remove this meal time?"
        description={
          mealTimeToDelete ? (
            <span>
              Meals scheduled for <strong>{mealTimeToDelete.name}</strong> will move to Other meals.
            </span>
          ) : null
        }
        confirmText="Remove meal time"
        confirmLoadingText="Removing…"
        error={deleteError}
        isLoading={isPending}
        variant="destructive"
      />
    </main>
  );
}
