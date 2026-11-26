"use client";

import { useId, useState, type TransitionStartFunction } from "react";
import dynamic from "next/dynamic";
import { Loader2, Utensils } from "lucide-react";
import PulseButton from "@/components/ui/pulse-button";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/components/ui/pulse-dialog";
import PulseSelect from "@/components/ui/pulse-select";
import { addMealPlanAction, type MealPlan, type MealTime, type RecipeSummary } from "@/features/meal-plan/api/meal-plan-actions";
import { formatTimeDisplay } from "@/features/meal-plan/utils/format-time-display";
import { parseYMD } from "@/features/meal-plan/utils/meal-week";

const RecipePicker = dynamic(() => import("@/features/meal-plan/components/recipe-picker").then(module => module.RecipePicker), {
  loading: () => <p role="status">Loading recipes…</p>,
});

/**
 * Mounted only while open, so every opening starts from a fresh form. It shares the planner's
 * transition so the planner's meal rows stay disabled while a meal is being added.
 */
export default function AddMealDialog({
  date, dayFull, mealTimes, initialMealTimeId, isPending, startTransition, onAdded, onClose,
}: {
  date: string;
  dayFull: string;
  mealTimes: MealTime[];
  initialMealTimeId: number | null;
  isPending: boolean;
  startTransition: TransitionStartFunction;
  onAdded: (meal: MealPlan) => void;
  onClose: () => void;
}) {
  const titleId = useId();
  const [selectedMealTimeID, setSelectedMealTimeID] = useState<number | null>(initialMealTimeId);
  const [mealName, setMealName] = useState("");
  const [customMeal, setCustomMeal] = useState(false);
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeSummary | null>(null);
  const [error, setError] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = mealName.trim();
    if (customMeal ? !trimmed : !selectedRecipe) {
      setError(customMeal ? "Please enter a meal name." : "Choose a recipe.");
      return;
    }
    if (!mealTimes.some(time => time.id === selectedMealTimeID)) {
      setError("Choose an available meal time.");
      return;
    }

    startTransition(async () => {
      setError("");
      const res = await addMealPlanAction(
        date, customMeal ? trimmed : "", selectedMealTimeID ?? undefined, customMeal ? undefined : selectedRecipe?.id,
      );
      if (!res.ok || !res.meal) {
        setError(res.error ?? "Failed to add meal.");
        return;
      }

      onAdded(res.meal);
      onClose();
    });
  };

  return (
    <PulseDialog
      labelledBy={titleId}
      onDismiss={isPending ? undefined : onClose}
      size="sm"
    >
      <form onSubmit={handleSubmit}>
        <div className="mb-5">
          <PulseDialogHeader
            id={titleId}
            icon={<Utensils className="size-5" />}
            description={`For ${dayFull}, ${parseYMD(date).toLocaleDateString("en-IN", {
              timeZone: "UTC",
              month: "short",
              day: "numeric",
            })}`}
          >
            Add meal
          </PulseDialogHeader>
        </div>

        {error && (
          <div
            className="mb-4 rounded-lg bg-destructive/10 p-3 text-xs text-destructive"
            role="alert"
          >
            {error}
          </div>
        )}

        <div className="mb-4 flex flex-wrap gap-2" aria-label="Meal source" role="group">
          <PulseButton
            type="button"
            size="sm"
            variant={customMeal ? "secondary" : "primary"}
            aria-pressed={!customMeal}
            disabled={isPending}
            onClick={() => { setCustomMeal(false); setError(""); }}
          >
            Recipe catalog
          </PulseButton>
          <PulseButton
            type="button"
            size="sm"
            variant={customMeal ? "primary" : "secondary"}
            aria-pressed={customMeal}
            disabled={isPending}
            onClick={() => { setCustomMeal(true); setError(""); }}
          >
            Custom meal
          </PulseButton>
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
            <PulseButton type="button" size="sm" variant="secondary" autoFocus disabled={isPending} onClick={() => setSelectedRecipe(null)}>
              Change
            </PulseButton>
          </div>
        )}

        <div className="space-y-1.5 mb-4">
          <div className="flex items-center justify-between">
            <label
              htmlFor="meal-time-select"
              className="block text-sm font-semibold text-foreground"
            >
              Meal time
            </label>
          </div>
          <PulseSelect
            id="meal-time-select"
            value={selectedMealTimeID ?? ""}
            onChange={(e) =>
              setSelectedMealTimeID(e.target.value ? parseInt(e.target.value, 10) : null)
            }
            className="min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-sm text-foreground"
          >
            {mealTimes.length === 0 && <option value="">No meal times available</option>}
            {mealTimes.map((mt) => (
              <option key={mt.id} value={mt.id}>
                {mt.name} ({formatTimeDisplay(mt.start_time)} – {formatTimeDisplay(mt.end_time)})
              </option>
            ))}
          </PulseSelect>
        </div>

        <PulseDialogActions>
          <PulseDialogAction
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={isPending}
          >
            Cancel
          </PulseDialogAction>
          <PulseDialogAction
            type="submit"
            variant="primary"
            disabled={isPending || (customMeal ? !mealName.trim() : !selectedRecipe)}
          >
            {isPending ? (
              <span className="flex items-center justify-center gap-2">
                <Loader2 className="h-4 w-4 motion-safe:animate-spin" /> Adding…
              </span>
            ) : (
              "Add meal"
            )}
          </PulseDialogAction>
        </PulseDialogActions>
      </form>
    </PulseDialog>
  );
}
