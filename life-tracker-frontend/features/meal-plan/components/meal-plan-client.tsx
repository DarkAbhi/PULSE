"use client";

import { useState, useTransition, type ReactNode } from "react";
import { Clock, Coffee, Moon, Plus, SlidersHorizontal, Sun, Sunrise, Utensils } from "lucide-react";
import { frame } from "@/components/layout/page-frame";
import PulseButton from "@/components/ui/pulse-button";
import PulseConfirmationDialog from "@/components/ui/pulse-confirmation-dialog";
import {
  type MealPlan,
  type MealTime,
  deleteMealPlanAction,
  updateMealPlanConsumedAction,
} from "@/features/meal-plan/api/meal-plan-actions";
import AddMealDialog from "@/features/meal-plan/components/add-meal-dialog";
import { MealList, MealSlotCard } from "@/features/meal-plan/components/meal-slot-card";
import type { MealSlot } from "@/features/meal-plan/components/meal-visuals";
import MealWeekCalendar from "@/features/meal-plan/components/meal-week-calendar";
import { useMealWeek } from "@/features/meal-plan/hooks/use-meal-week";
import type { MealKind, MealPreferences } from "@/features/meal-plan/onboarding/onboarding-types";
import { isMealTimeEnabled, standardMealKind } from "@/features/meal-plan/onboarding/questionnaire";
import { mealPlanStyles as styles } from "@/features/meal-plan/styles/meal-plan-classes";
import { formatTimeDisplay } from "@/features/meal-plan/utils/format-time-display";

interface MealPlanClientProps {
  appBar: ReactNode;
  preferences?: MealPreferences | null;
  onEditPreferences?: () => void;
  initialMeals: MealPlan[] | null;
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

export default function MealPlanClient({
  appBar,
  preferences,
  onEditPreferences,
  initialMeals,
  initialMealTimes,
  initialTodayStr,
}: MealPlanClientProps) {
  const {
    selectedDate, setSelectedDate, selectedDay, isToday, currentMonday, weekEnd, weekDays,
    weekMeals, setMeals, isLoadingWeek, weekError, retryWeek, moveWeek, goToToday,
  } = useMealWeek(initialMeals, initialTodayStr);
  const mealTimes = initialMealTimes;

  // The add dialog holds its own form; the planner only knows whether it is open and which slot opened it.
  const [addMealTimeID, setAddMealTimeID] = useState<number | null | undefined>(undefined);
  const isAddMealOpen = addMealTimeID !== undefined;
  const [mealToDelete, setMealToDelete] = useState<MealPlan | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [isPending, startTransition] = useTransition();

  const dayMeals = weekMeals.filter((meal) => meal.date === selectedDate);
  const orderedMealTimes = [...mealTimes].sort((a, b) => a.start_time.localeCompare(b.start_time));
  const enabledMealTimes = orderedMealTimes.filter(time => isMealTimeEnabled(time, preferences));
  const otherMeals = dayMeals.filter((meal) => !mealTimes.some((time) => time.id === meal.meal_time_id));
  const eatenCount = dayMeals.filter((meal) => meal.is_consumed).length;
  const slotKindByID = new Map<number | undefined, MealKind | undefined>(
    mealTimes.map(time => [time.id, standardMealKind(time)])
  );
  const plannedByDay = weekDays.map(day => {
    const items = weekMeals.filter(meal => meal.date === day.dateStr);
    const slots = new Set<MealSlot>(items.map(meal => slotKindByID.get(meal.meal_time_id) ?? "custom"));
    return { count: items.length, slots };
  });
  const calendarSlots: MealSlot[] = plannedByDay.some(day => day.slots.has("custom"))
    ? [...standardSlots, "custom"]
    : standardSlots;

  // Open the add meal dialog, preselecting the slot when it is available.
  const handleOpenAddMeal = (mealTimeID?: number) => {
    setAddMealTimeID(enabledMealTimes.find(time => time.id === mealTimeID)?.id ?? enabledMealTimes[0]?.id ?? null);
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

  const renderMeals = (items: MealPlan[]) => (
    <MealList
      meals={items}
      disabled={isPending}
      onToggleConsumed={handleToggleConsumed}
      onDelete={(meal) => { setDeleteError(""); setMealToDelete(meal); }}
    />
  );

  return (
    <main id="main-content" tabIndex={-1} className={`${frame.page} [&_:is(button,a):focus-visible]:outline-brand`}>
      <div className={frame.content} inert={isAddMealOpen || mealToDelete !== null}>
        {appBar}

        <div className={styles.layout}>
          <h1 tabIndex={0} className="sr-only focus:not-sr-only">Meal Plan</h1>

          <MealWeekCalendar
            weekDays={weekDays}
            plannedByDay={plannedByDay}
            calendarSlots={calendarSlots}
            currentMonday={currentMonday}
            weekEnd={weekEnd}
            isToday={isToday}
            isLoadingWeek={isLoadingWeek}
            onMoveWeek={moveWeek}
            onToday={goToToday}
            onSelectDate={setSelectedDate}
          />

          <section aria-labelledby="meal-agenda-title">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h2 id="meal-agenda-title" className={styles.agendaTitle}>{selectedDay.dayFull}</h2>
                <div className={`${styles.muted} mt-1.5 flex flex-wrap items-center gap-x-2.5 gap-y-1 text-sm sm:text-base`}>
                  <time dateTime={selectedDate}>{selectedDay.monthDay} {selectedDay.date.getUTCFullYear()}</time>
                  {isToday && <span className={styles.todayBadge}>Today</span>}
                  <span role="status">
                    {isLoadingWeek ? "Loading meals…" : weekError ? "Meals unavailable" : dayMeals.length ? `${eatenCount} of ${dayMeals.length} meals eaten` : "No meals planned yet"}
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {onEditPreferences && (
                  <PulseButton variant="secondary" size="xl" labelStyle="strong" className="px-3 sm:px-4" type="button"
                    onClick={onEditPreferences} icon={<SlidersHorizontal className="size-5" aria-hidden="true" />}
                  >
                    <span className="sr-only sm:not-sr-only">Meal preferences</span>
                  </PulseButton>
                )}
                <PulseButton variant="primary" size="xl" labelStyle="strong" className={`${styles.primaryAction} ${styles.addMeal}`} type="button"
                  onClick={() => handleOpenAddMeal()} disabled={isLoadingWeek || weekError || !enabledMealTimes.length} icon={<Plus className="size-5" aria-hidden="true" />}
                >
                  Add meal
                </PulseButton>
              </div>
            </div>

            {weekError && (
              <div className="flex flex-wrap items-center gap-3">
                <p role="alert" className="text-sm text-destructive">We couldn’t load meals for this week.</p>
                <PulseButton type="button" variant="secondary" onClick={retryWeek}>Retry</PulseButton>
              </div>
            )}
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
                    addDisabled={isLoadingWeek || weekError}
                  >
                    {items.length ? renderMeals(items) : !isLoadingWeek && !weekError && (
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
              {!isLoadingWeek && !weekError && !enabledMealTimes.length && !dayMeals.length && (
                <p className={styles.emptyState}>No meal times available.</p>
              )}
            </div>
          </section>
        </div>
      </div>

      {isAddMealOpen && (
        <AddMealDialog
          date={selectedDate}
          dayFull={selectedDay.dayFull}
          mealTimes={enabledMealTimes}
          initialMealTimeId={addMealTimeID}
          isPending={isPending}
          startTransition={startTransition}
          onAdded={(meal) => setMeals((prev) => [...prev, meal])}
          onClose={() => setAddMealTimeID(undefined)}
        />
      )}

      <PulseConfirmationDialog
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
