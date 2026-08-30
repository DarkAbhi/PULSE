"use client";

import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { addVehicleAction } from "./actions";

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
        <div
          className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/60 p-4 backdrop-blur-xs sm:p-6"
          role="dialog"
          aria-labelledby="add-vehicle-title"
          aria-modal="true"
        >
          <section className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-2xl sm:p-8">
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
              <div className="flex gap-3 pt-2">
                <button
                  className="flex-1 rounded-lg border border-btn-cancel-border bg-btn-cancel-bg text-btn-cancel-text hover:bg-btn-cancel-hover px-4 py-3 text-sm font-semibold transition"
                  disabled={isPending}
                  onClick={() => setIsAddOpen(false)}
                  type="button"
                >
                  Cancel
                </button>
                <button
                  className="flex-1 rounded-lg bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:bg-muted disabled:text-muted-foreground"
                  disabled={isPending}
                  type="submit"
                >
                  {isPending ? "Adding…" : "Add vehicle"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </>
  );
}
