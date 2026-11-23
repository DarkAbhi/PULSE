"use client";

import { useEffect, useId, useState, useTransition } from "react";
import { createPortal } from "react-dom";
import { Clock, Loader2, Plus, X } from "lucide-react";
import Dialog, { DialogAction, DialogActions, DialogHeader } from "@/app/components/design-system/dialog";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import { createMealTimeAction, deleteMealTimeAction, type MealTime } from "../actions";
import { gradientClass, pillClass } from "./meal-preference-classes";

export default function CustomMealTimes({ times, onCreated, onDeleted }: {
  times: MealTime[];
  onCreated: (time: MealTime) => void;
  onDeleted: (id: number) => void;
}) {
  const [isAddMealTimeOpen, setIsAddMealTimeOpen] = useState(false);
  const [newMealTimeName, setNewMealTimeName] = useState("");
  const [newStartTime, setNewStartTime] = useState("16:00");
  const [newEndTime, setNewEndTime] = useState("17:00");
  const [mealTimeModalError, setMealTimeModalError] = useState("");
  const [mealTimeToDelete, setMealTimeToDelete] = useState<MealTime | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [isPending, startTransition] = useTransition();
  const addMealTimeTitleId = useId();

  useEffect(() => {
    if (!isAddMealTimeOpen && !mealTimeToDelete) return;
    const journey = document.getElementById("meal-setup-journey");
    const trigger = document.activeElement;
    if (journey) journey.inert = true;
    return () => {
      if (journey) journey.inert = false;
      if (trigger instanceof HTMLElement && journey?.contains(trigger)) trigger.focus();
    };
  }, [isAddMealTimeOpen, mealTimeToDelete]);

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
    e.stopPropagation();
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

      onCreated(res.mealTime);
      handleCloseAddMealTime();
    });
  };

  const handleConfirmDeleteMealTime = () => {
    if (!mealTimeToDelete) return;
    setDeleteError("");
    startTransition(async () => {
      const res = await deleteMealTimeAction(mealTimeToDelete.id);
      if (res.ok) {
        onDeleted(mealTimeToDelete.id);
        setMealTimeToDelete(null);
      } else {
        setDeleteError(res.error ?? "Could not remove the meal time.");
      }
    });
  };

  return <>
    {times.map(time => (
      <span key={time.id} className={`${pillClass} border-transparent pr-0.5 pl-[18px] font-semibold wrap-anywhere ${gradientClass}`}>
        <span>{time.name}</span>
        <button className="grid h-10 w-11 shrink-0 place-items-center rounded-full" type="button" aria-label={`Delete ${time.name}`}
          onClick={() => { setDeleteError(""); setMealTimeToDelete(time); }}>
          <X className="size-[26px] rounded-full bg-white/22 p-1.5 stroke-3" aria-hidden="true" />
        </button>
      </span>
    ))}
    <button className={`${pillClass} gap-2 border-dashed border-(--meal-line) px-[18px] text-left font-bold text-(--meal-muted) hover:border-(--meal-selected) hover:text-(--meal-ink)`} type="button" onClick={handleOpenAddMealTime}>
      <Plus className="size-4 shrink-0" aria-hidden="true" />
      Add custom meal time
    </button>
      {/* New meal time dialog */}
      {isAddMealTimeOpen && createPortal(
        <Dialog
          labelledBy={addMealTimeTitleId}
          onBackdropClick={(e) => {
            if (!isPending && e.target === e.currentTarget) handleCloseAddMealTime();
          }}
          size="sm"
        >
          <form onSubmit={handleSubmitAddMealTime}>
            <div className="mb-5">
              <DialogHeader
                id={addMealTimeTitleId}
                icon={<Clock className="size-5" />}
                description="Set the hours when this meal time appears in your daily plan."
              >
                New meal time
              </DialogHeader>
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
                  maxLength={100}
                  value={newMealTimeName}
                  onChange={(e) => setNewMealTimeName(e.target.value)}
                  placeholder="e.g. Afternoon snack"
                  className="min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground"
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
                    className="min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-sm text-foreground"
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
                    className="min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-sm text-foreground"
                  />
                </div>
              </div>
            </div>

            <DialogActions>
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
        </Dialog>,
        document.body,
      )}

      {/* Delete Meal Time Confirmation Dialog */}
      {mealTimeToDelete && createPortal(<ConfirmationDialog
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
      />, document.body)}
  </>;
}
