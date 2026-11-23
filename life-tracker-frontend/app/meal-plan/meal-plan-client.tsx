"use client";

import Select from "@/app/components/design-system/select";
import type { MealKind, MealPreferences } from "@/app/meal-plan/onboarding/types";
import { isMealTimeEnabled, standardMealKind } from "@/app/meal-plan/onboarding/questionnaire";

import { useTimeZone } from "@/app/components/time-zone-provider";

import { dateKey } from "@/app/lib/time-zone.mjs";

import { useState, useEffect, useTransition, useId, type ReactNode } from "react";
import { frame } from "@/app/components/design-system/page-frame";
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  Utensils,
  Clock,
  Loader2,
  Check,
  Coffee,
  LocateFixed,
  Moon,
  SlidersHorizontal,
  Sun,
  Sunrise,
  X,
} from "lucide-react";
import Dialog, { DialogActions, DialogAction, DialogHeader } from "@/app/components/design-system/dialog";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import {
  MealPlan,
  MealTime,
  type RecipeSummary,
  addMealPlanAction,
  deleteMealPlanAction,
  getMealPlansAction,
  updateMealPlanConsumedAction,
} from "@/app/meal-plan/actions";
import Button from "@/app/components/design-system/button";
import { RecipePicker } from "@/app/meal-plan/recipe-picker";
import { mealPlanStyles as styles } from "@/app/meal-plan/meal-plan-classes";

interface MealPlanClientProps {
  appBar: ReactNode;
  preferences?: MealPreferences | null;
  onEditPreferences?: () => void;
  initialMeals: MealPlan[];
  initialMealTimes: MealTime[];
  initialTodayStr?: string;
}

const standardSlotIcons = {
  breakfast: Sunrise,
  lunch: Sun,
  evening_snacks: Coffee,
  dinner: Moon,
} satisfies Record<MealKind, typeof Clock>;
const standardSlots = Object.keys(standardSlotIcons) as MealKind[];
type CalendarSlot = MealKind | "custom";

const slotColors: Record<CalendarSlot, string> = {
  breakfast: "[--slot-color:#ffc83d] text-[#3a2a00]",
  lunch: "[--slot-color:#f0603a] text-[#3a2a00]",
  evening_snacks: "[--slot-color:#d91cab] text-white",
  dinner: "[--slot-color:#5b2ce0] text-white",
  custom: "[--slot-color:#3e59bc] text-white",
};

function MealSlotCard({ kind, icon, name, timeRange, usualTime, children, onAdd, addDisabled }: {
  kind: CalendarSlot;
  icon: ReactNode;
  name: string;
  timeRange?: string;
  usualTime?: string;
  children: ReactNode;
  onAdd?: () => void;
  addDisabled?: boolean;
}) {
  return (
    <article className={styles.slotCard}>
      <div className="flex items-start gap-3">
        <span className={`${styles.slotIcon} ${slotColors[kind]}`} aria-hidden="true">{icon}</span>
        <div className="min-w-0 self-center">
          <h3 className="text-lg leading-tight font-extrabold tracking-tight wrap-anywhere">{name}</h3>
          {timeRange && <p className={`${styles.muted} mt-1 text-sm font-semibold`}>{timeRange}</p>}
          {usualTime && <p className={`${styles.muted} mt-1 text-xs`}>Your usual time: {usualTime}</p>}
        </div>
      </div>
      {children}
      {onAdd && (
        <button className={styles.slotAdd} type="button" onClick={onAdd} disabled={addDisabled}
          aria-label={`Add meal to ${name}`}
        >
          <Plus className="size-5" aria-hidden="true" /> Add meal
        </button>
      )}
    </article>
  );
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
  appBar,
  preferences,
  onEditPreferences,
  initialMeals,
  initialMealTimes,
  initialTodayStr,
}: MealPlanClientProps) {
  const { timeZone } = useTimeZone();
  const [clock, setClock] = useState(() => ({
    today: initialTodayStr ?? dateKey(new Date(), timeZone),
  }));
  useEffect(() => {
    const update = () => {
      setClock({ today: dateKey(new Date(), timeZone) });
    };
    update();
    const timer = window.setInterval(update, 60000);
    window.addEventListener("focus", update);
    return () => { window.clearInterval(timer); window.removeEventListener("focus", update); };
  }, [timeZone]);
  const [selectedDate, setSelectedDate] = useState(
    () => initialTodayStr ?? dateKey(new Date(), timeZone)
  );
  const [meals, setMeals] = useState<MealPlan[]>(initialMeals);
  const mealTimes = initialMealTimes;
  const [isLoadingWeek, setIsLoadingWeek] = useState(false);

  // Meal form state
  const [isAddMealOpen, setIsAddMealOpen] = useState(false);
  const [selectedMealTimeID, setSelectedMealTimeID] = useState<number | null>(null);
  const [mealName, setMealName] = useState("");
  const [customMeal, setCustomMeal] = useState(false);
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeSummary | null>(null);
  const [mealModalError, setMealModalError] = useState("");

  const [mealToDelete, setMealToDelete] = useState<MealPlan | null>(null);

  const [isPending, startTransition] = useTransition();

  const [deleteError, setDeleteError] = useState("");
  const addMealTitleId = useId();

  const currentMonday = getMonday(parseYMD(selectedDate));
  const weekEnd = addDays(currentMonday, 6);
  const weekDays = Array.from({ length: 7 }, (_, i) => {
    const d = addDays(currentMonday, i);
    const dateStr = formatYMD(d);
    return {
      date: d,
      dateStr,
      dayShort: d.toLocaleDateString("en-IN", { timeZone: "UTC", weekday: "short" }),
      dayFull: d.toLocaleDateString("en-IN", { timeZone: "UTC", weekday: "long" }),
      monthDay: d.toLocaleDateString("en-IN", { timeZone: "UTC", month: "short", day: "numeric" }),
      isToday: dateStr === clock.today,
      isSelected: dateStr === selectedDate,
    };
  });

  const weekStartStr = formatYMD(currentMonday);
  const weekEndStr = formatYMD(weekEnd);

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
  const enabledMealTimes = orderedMealTimes.filter(time => isMealTimeEnabled(time, preferences));
  const otherMeals = dayMeals.filter((meal) => !mealTimes.some((time) => time.id === meal.meal_time_id));
  const eatenCount = dayMeals.filter((meal) => meal.is_consumed).length;
  const isToday = selectedDate === clock.today;
  const slotKindByID = new Map<number | undefined, MealKind | undefined>(
    mealTimes.map(time => [time.id, standardMealKind(time)])
  );
  const plannedByDay = weekDays.map(day => {
    const items = meals.filter(meal => meal.date === day.dateStr);
    const slots = new Set<CalendarSlot>(items.map(meal => slotKindByID.get(meal.meal_time_id) ?? "custom"));
    return { count: items.length, slots };
  });
  const calendarSlots: CalendarSlot[] = plannedByDay.some(day => day.slots.has("custom"))
    ? [...standardSlots, "custom"]
    : standardSlots;

  // Open add meal modal, optionally preselecting a meal time slot
  const handleOpenAddMeal = (mealTimeID?: number) => {
    setMealName("");
    setCustomMeal(false);
    setSelectedRecipe(null);
    setMealModalError("");
    setSelectedMealTimeID(enabledMealTimes.find(time => time.id === mealTimeID)?.id ?? enabledMealTimes[0]?.id ?? null);
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
    if (customMeal ? !trimmed : !selectedRecipe) {
      setMealModalError(customMeal ? "Please enter a meal name." : "Choose a recipe.");
      return;
    }
    if (!enabledMealTimes.some(time => time.id === selectedMealTimeID)) {
      setMealModalError("Choose an available meal time.");
      return;
    }

    startTransition(async () => {
      setMealModalError("");
      const res = await addMealPlanAction(
        selectedDate, customMeal ? trimmed : "", selectedMealTimeID ?? undefined, customMeal ? undefined : selectedRecipe?.id,
      );
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

  const weekRange = new Intl.DateTimeFormat("en-IN", {
    timeZone: "UTC",
    month: "short",
    day: "numeric",
    ...(currentMonday.getUTCFullYear() !== weekEnd.getUTCFullYear() && { year: "numeric" }),
  }).formatRange(currentMonday, weekEnd);

  const renderMeals = (items: MealPlan[]) => (
    <ul className="flex flex-col gap-2">
      {items.map((meal) => (
        <li key={meal.id} className={styles.mealRow}>
          <button
            type="button"
            role="checkbox"
            aria-checked={meal.is_consumed}
            aria-label={`Eaten: ${meal.name}`}
            onClick={() => handleToggleConsumed(meal)}
            disabled={isPending}
            className={styles.mealToggle}
          >
            <span className={styles.check} aria-hidden="true">
              {meal.is_consumed && <Check className="size-5" strokeWidth={3} />}
            </span>
            <span className={`min-w-0 wrap-anywhere text-sm font-semibold ${meal.is_consumed ? `${styles.muted} line-through` : ""}`}>
              {meal.name}
            </span>
          </button>
          <button
            type="button"
            onClick={() => { setDeleteError(""); setMealToDelete(meal); }}
            aria-label={`Delete ${meal.name}`}
            disabled={isPending}
            className={styles.removeMeal}
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </li>
      ))}
    </ul>
  );

  return (
    <main className={`${frame.page} ${styles.page}`}>
      <div className={frame.content} inert={isAddMealOpen || mealToDelete !== null}>
        {appBar}

        <div className={styles.layout}>
          <section className={styles.hero} aria-labelledby="meal-plan-title">
            <div className={styles.plate} aria-hidden="true">
              {["[animation-delay:-4s]", "[animation-delay:-2s]", ""].map(delay => (
                <span key={delay} className={`${styles.ripple} ${delay}`} />
              ))}
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="5.5" />
                <circle cx="12" cy="12" r="2" />
                <path d="M3 4.5v4a1.5 1.5 0 0 0 3 0v-4M4.5 10v9.5M21 4.5c-1.6 1-2.2 3-2.2 5.5h2.2v9.5" />
              </svg>
            </div>
            <div className="relative">
              <h1 id="meal-plan-title" className={styles.title}>Meal Plan</h1>
              <p className="mt-2 text-base font-medium sm:text-xl">Plan your week and track meals eaten</p>
              {onEditPreferences && (
                <button className={styles.preferences} type="button" onClick={onEditPreferences}>
                  <SlidersHorizontal className="size-5" aria-hidden="true" /> Meal preferences
                </button>
              )}
            </div>
          </section>

          <section className={styles.calendar} aria-label="Weekly calendar">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <h2 aria-live="polite" className={styles.weekRange}>
                {weekRange}
                {currentMonday.getUTCFullYear() === weekEnd.getUTCFullYear() && (
                  <span className={`${styles.muted} mt-1.5 block text-sm font-semibold tracking-normal`}>{weekEnd.getUTCFullYear()}</span>
                )}
              </h2>
              <div className="flex items-center gap-2">
                <button className={styles.weekArrow} type="button" onClick={() => moveWeek(-7)} aria-label="Previous week">
                  <ChevronLeft className="size-5" aria-hidden="true" />
                </button>
                <button className={`${styles.primaryAction} ${styles.todayAction}`} type="button" onClick={handleToday} disabled={isToday}>
                  <LocateFixed className="size-5" aria-hidden="true" /> Today
                </button>
                <button className={styles.weekArrow} type="button" onClick={() => moveWeek(7)} aria-label="Next week">
                  <ChevronRight className="size-5" aria-hidden="true" />
                </button>
              </div>
            </div>

            <div className={styles.weekDays} role="group" aria-label="Days of the week" aria-busy={isLoadingWeek}>
              {weekDays.map((day, index) => {
                const { count, slots } = plannedByDay[index];
                const mealCountLabel = count ? `, ${count} ${count === 1 ? "meal" : "meals"} planned` : "";
                return (
                  <button
                    key={day.dateStr}
                    className={styles.day}
                    type="button"
                    aria-label={`${day.dayFull}, ${day.monthDay}${mealCountLabel}`}
                    aria-pressed={day.isSelected}
                    aria-current={day.isToday ? "date" : undefined}
                    onClick={() => setSelectedDate(day.dateStr)}
                  >
                    <span className={styles.dayLabel}>{day.dayShort}</span>
                    <span className={styles.dayNumber}>{day.date.getUTCDate()}</span>
                    <span className={styles.dots} aria-hidden="true">
                      {calendarSlots.map(slot => (
                        <span key={slot} className={`${styles.dot} ${slotColors[slot]}`} data-planned={slots.has(slot)} />
                      ))}
                    </span>
                  </button>
                );
              })}
            </div>
          </section>

          <section aria-labelledby="meal-agenda-title">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h2 id="meal-agenda-title" className={styles.agendaTitle}>{selectedDay.dayFull}</h2>
                <div className={`${styles.muted} mt-1.5 flex flex-wrap items-center gap-x-2.5 gap-y-1 text-sm sm:text-base`}>
                  <time dateTime={selectedDate}>{selectedDay.monthDay} {selectedDay.date.getUTCFullYear()}</time>
                  {isToday && <span className={styles.todayBadge}>Today</span>}
                  <span role="status">
                    {isLoadingWeek ? "Loading meals…" : dayMeals.length ? `${eatenCount} of ${dayMeals.length} meals eaten` : "No meals planned yet"}
                  </span>
                </div>
              </div>
              <button className={`${styles.primaryAction} ${styles.addMeal}`} type="button"
                onClick={() => handleOpenAddMeal()} disabled={isLoadingWeek || !enabledMealTimes.length}
              >
                <Plus className="size-5" aria-hidden="true" /> Add meal
              </button>
            </div>

            <div className={styles.slotGrid} aria-busy={isLoadingWeek}>
              {orderedMealTimes.map((time) => {
                const items = dayMeals.filter(meal => meal.meal_time_id === time.id);
                const enabled = isMealTimeEnabled(time, preferences);
                if (!enabled && !items.length) return null;
                const kind = standardMealKind(time);
                const SlotIcon = kind ? standardSlotIcons[kind] : Clock;
                const usualTime = preferences?.meals.find(meal => meal.kind === kind)?.usual_time;

                return (
                  <MealSlotCard
                    key={time.id}
                    name={time.name}
                    kind={kind ?? "custom"}
                    icon={<SlotIcon className="size-6" />}
                    timeRange={`${formatTimeDisplay(time.start_time)} – ${formatTimeDisplay(time.end_time)}`}
                    usualTime={usualTime && formatTimeDisplay(usualTime)}
                    onAdd={enabled ? () => handleOpenAddMeal(time.id) : undefined}
                    addDisabled={isLoadingWeek}
                  >
                    {items.length ? renderMeals(items) : !isLoadingWeek && (
                      <p className={`${styles.muted} py-1 text-sm`}>Nothing planned</p>
                    )}
                  </MealSlotCard>
                );
              })}
              {otherMeals.length > 0 && (
                <MealSlotCard name="Other meals" kind="custom" icon={<Utensils className="size-6" />}>
                  {renderMeals(otherMeals)}
                </MealSlotCard>
              )}
              {!isLoadingWeek && !enabledMealTimes.length && !dayMeals.length && (
                <p className={styles.emptyState}>No meal times available.</p>
              )}
            </div>
          </section>
        </div>
      </div>

      {/* Add meal Modal Dialog */}
      {isAddMealOpen && (
        <Dialog
          labelledBy={addMealTitleId}
          onBackdropClick={(e) => {
            if (!isPending && e.target === e.currentTarget) handleCloseAddMeal();
          }}
          size="sm"
        >
          <form onSubmit={handleSubmitAddMeal}>
            <div className="mb-5">
              <DialogHeader
                id={addMealTitleId}
                icon={<Utensils className="size-5" />}
                description={`For ${selectedDay.dayFull}, ${parseYMD(selectedDate).toLocaleDateString("en-IN", {
                  timeZone: "UTC",
                  month: "short",
                  day: "numeric",
                })}`}
              >
                Add meal
              </DialogHeader>
            </div>

            {mealModalError && (
              <div
                className="mb-4 rounded-lg bg-destructive/10 p-3 text-xs text-destructive"
                role="alert"
              >
                {mealModalError}
              </div>
            )}

            <div className="mb-4 flex flex-wrap gap-2" aria-label="Meal source" role="group">
              <Button
                type="button"
                size="sm"
                variant={customMeal ? "secondary" : "primary"}
                aria-pressed={!customMeal}
                disabled={isPending}
                onClick={() => { setCustomMeal(false); setMealModalError(""); }}
              >
                Recipe catalog
              </Button>
              <Button
                type="button"
                size="sm"
                variant={customMeal ? "primary" : "secondary"}
                aria-pressed={customMeal}
                disabled={isPending}
                onClick={() => { setCustomMeal(true); setMealModalError(""); }}
              >
                Custom meal
              </Button>
            </div>

            <div className="mb-4" hidden={customMeal || !!selectedRecipe}>
              <RecipePicker onSelect={setSelectedRecipe} />
            </div>

            {customMeal ? (
              <div className="mb-4 space-y-1.5">
                <label htmlFor="meal-name-input" className="block text-sm font-semibold">Meal name</label>
                <input
                  id="meal-name-input"
                  type="text"
                  autoFocus
                  value={mealName}
                  onChange={(e) => setMealName(e.target.value)}
                  placeholder="e.g. Scrambled eggs on toast"
                  className="min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground"
                />
              </div>
            ) : selectedRecipe && (
              <div className="mb-4 flex items-center justify-between gap-2">
                <h3 className="text-base font-extrabold wrap-anywhere">{selectedRecipe.name}</h3>
                <Button type="button" size="sm" variant="secondary" autoFocus disabled={isPending} onClick={() => setSelectedRecipe(null)}>
                  Change
                </Button>
              </div>
            )}

            {/* Meal Time Selector */}
            <div className="space-y-1.5 mb-4">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="meal-time-select"
                  className="block text-sm font-semibold text-foreground"
                >
                  Meal time
                </label>
              </div>
              <Select
                id="meal-time-select"
                value={selectedMealTimeID ?? ""}
                onChange={(e) =>
                  setSelectedMealTimeID(e.target.value ? parseInt(e.target.value, 10) : null)
                }
                className="min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-sm text-foreground"
              >
                {enabledMealTimes.length === 0 && <option value="">No meal times available</option>}
                {enabledMealTimes.map((mt) => (
                  <option key={mt.id} value={mt.id}>
                    {mt.name} ({formatTimeDisplay(mt.start_time)} – {formatTimeDisplay(mt.end_time)})
                  </option>
                ))}
              </Select>
            </div>

            <DialogActions>
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
                disabled={isPending || (customMeal ? !mealName.trim() : !selectedRecipe)}
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

    </main>
  );
}
