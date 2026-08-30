"use client";

import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { addVehicleAction } from "./actions";
import Dialog, { DialogAction, DialogActions } from "../components/design-system/dialog";

export default function AddVehicleButton() {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [vehicleName, setVehicleName] = useState("");
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleAdd(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    startTransition(async () => {
      const res = await addVehicleAction(vehicleName.trim());
      if (!res.ok) {
        setError(res.error ?? "We couldn't add that vehicle.");
      } else {
        setVehicleName("");
        setIsAddOpen(false);
      }
    });
  }

  return (
    <>
      <button
        className="shrink-0 flex items-center gap-1.5 rounded-lg bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-sm transition hover:bg-primary/90 focus:outline-none focus:ring-4 focus:ring-primary/20"
        onClick={() => {
          setError("");
          setIsAddOpen(true);
        }}
        type="button"
      >
        <Plus className="h-4 w-4" /> Add vehicle
      </button>

      {isAddOpen && (
        <Dialog labelledBy="add-vehicle-title">
            <h2
              className="text-2xl font-bold tracking-tight text-foreground"
              id="add-vehicle-title"
            >
              Add a vehicle
            </h2>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Give it a name you&apos;ll recognize right away.
            </p>
            <form className="mt-6 space-y-4" onSubmit={handleAdd}>
              <label
                className="block text-sm font-medium text-muted-foreground"
                htmlFor="vehicle-name"
              >
                Vehicle name
              </label>
              <input
                autoFocus
                className="w-full rounded-lg border border-border bg-background text-foreground px-4 py-3 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/15"
                id="vehicle-name"
                maxLength={48}
                onChange={(event) => setVehicleName(event.target.value)}
                placeholder="e.g. Honda City"
                required
                value={vehicleName}
              />
              {error && (
                <p className="text-sm text-destructive" role="alert">
                  {error}
                </p>
              )}
              <DialogActions>
                <DialogAction
                  variant="secondary"
                  disabled={isPending}
                  onClick={() => setIsAddOpen(false)}
                  type="button"
                >
                  Cancel
                </DialogAction>
                <DialogAction
                  disabled={isPending}
                  type="submit"
                >
                  {isPending ? "Adding…" : "Add vehicle"}
                </DialogAction>
              </DialogActions>
            </form>
        </Dialog>
      )}
    </>
  );
}
