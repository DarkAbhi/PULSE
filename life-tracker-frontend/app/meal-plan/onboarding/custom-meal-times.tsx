"use client";

import { useEffect, useId, useState, useTransition } from "react";
import { createPortal } from "react-dom";
import { Clock, Loader2, Plus, Trash2 } from "lucide-react";
import Button from "@/app/components/design-system/button";
import Dialog, { DialogAction, DialogActions } from "@/app/components/design-system/dialog";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import { createMealTimeAction, deleteMealTimeAction, type MealTime } from "../actions";

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
    <Button variant="secondary" type="button" onClick={handleOpenAddMealTime} icon={<Plus className="size-4" aria-hidden="true" />}>
      Add custom meal time
    </Button>
    <ul className="divide-y divide-divider empty:hidden">
      {times.filter(time => !time.is_default).map(time => (
        <li key={time.id} className="flex items-center justify-between gap-3 py-2">
          <div className="min-w-0">
            <p className="wrap-anywhere font-semibold">{time.name}</p>
            <p className="text-sm text-muted-foreground">{time.start_time}–{time.end_time}</p>
          </div>
          <Button type="button" variant="iconDanger" size="sm" aria-label={`Delete ${time.name}`}
            onClick={() => { setDeleteError(""); setMealTimeToDelete(time); }}>
            <Trash2 className="size-4" aria-hidden="true" />
          </Button>
        </li>
      ))}
    </ul>
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
            <div className="mb-5 flex items-start gap-3 border-b border-divider pb-4">
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

            <DialogActions className="border-t border-divider pt-4">
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
