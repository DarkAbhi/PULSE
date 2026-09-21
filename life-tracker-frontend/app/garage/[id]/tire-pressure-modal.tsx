"use client";

import { useState, useEffect, useTransition } from "react";
import Dialog, { DialogAction, DialogActions } from "../../components/design-system/dialog";
import { saveTirePressure } from "./actions";
import { Gauge } from "lucide-react";

interface TirePressureModalProps {
  isOpen: boolean;
  onClose: () => void;
  vehicleId: string;
  currentFront?: number | null;
  currentRear?: number | null;
}

export default function TirePressureModal({
  isOpen,
  onClose,
  vehicleId,
  currentFront = null,
  currentRear = null,
}: TirePressureModalProps) {
  const [front, setFront] = useState<string>(currentFront != null ? String(currentFront) : "");
  const [rear, setRear] = useState<string>(currentRear != null ? String(currentRear) : "");
  const [error, setError] = useState<string>("");
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    if (isOpen) {
      setFront(currentFront != null ? String(currentFront) : "");
      setRear(currentRear != null ? String(currentRear) : "");
      setError("");
    }
  }, [isOpen, currentFront, currentRear]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const parsedFront = front.trim() === "" ? null : Number(front.trim());
    const parsedRear = rear.trim() === "" ? null : Number(rear.trim());

    if (parsedFront !== null && (isNaN(parsedFront) || parsedFront < 0 || parsedFront > 150)) {
      setError("Please enter a valid front tire pressure between 0 and 150 PSI.");
      return;
    }
    if (parsedRear !== null && (isNaN(parsedRear) || parsedRear < 0 || parsedRear > 150)) {
      setError("Please enter a valid rear tire pressure between 0 and 150 PSI.");
      return;
    }

    startTransition(async () => {
      const res = await saveTirePressure(vehicleId, {
        front_tire_pressure: parsedFront,
        rear_tire_pressure: parsedRear,
      });

      if (!res.ok) {
        setError(res.error ?? "Failed to save tire pressure.");
        return;
      }

      onClose();
    });
  };

  return (
    <Dialog labelledBy="tire-pressure-title" size="sm">
      <form onSubmit={handleSubmit}>
        <div className="flex items-center gap-2">
          <div className="rounded-xl bg-primary/10 p-2 text-primary">
            <Gauge className="h-5 w-5" />
          </div>
          <h2 className="text-xl font-bold tracking-tight text-foreground" id="tire-pressure-title">
            Recommended tire pressure
          </h2>
        </div>
        <p className="mt-2 text-sm text-muted-foreground">
          Set the target tire pressure values (in PSI) for the front and rear tires of this vehicle.
        </p>

        <div className="mt-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground" htmlFor="front-pressure">
              Front tire pressure (PSI)
            </label>
            <input
              className="mt-1.5 w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-1 focus:ring-primary"
              id="front-pressure"
              max="150"
              min="0"
              placeholder="e.g. 32"
              step="0.1"
              type="number"
              value={front}
              onChange={(e) => setFront(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground" htmlFor="rear-pressure">
              Rear tire pressure (PSI)
            </label>
            <input
              className="mt-1.5 w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-1 focus:ring-primary"
              id="rear-pressure"
              max="150"
              min="0"
              placeholder="e.g. 35"
              step="0.1"
              type="number"
              value={rear}
              onChange={(e) => setRear(e.target.value)}
            />
          </div>
        </div>

        {error && (
          <p className="mt-4 text-sm font-medium text-destructive" role="alert">
            {error}
          </p>
        )}

        <DialogActions>
          <DialogAction
            disabled={isPending}
            type="button"
            variant="secondary"
            onClick={onClose}
          >
            Cancel
          </DialogAction>
          <DialogAction disabled={isPending} type="submit">
            {isPending ? "Saving…" : "Save"}
          </DialogAction>
        </DialogActions>
      </form>
    </Dialog>
  );
}
