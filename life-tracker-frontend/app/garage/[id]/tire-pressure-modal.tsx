"use client";

import { useState, useEffect, useTransition } from "react";
import Dialog, { DialogAction, DialogActions } from "../../components/design-system/dialog";
import { saveTirePressure } from "./actions";
import { Gauge, User, Users } from "lucide-react";

interface TirePressureModalProps {
  isOpen: boolean;
  onClose: () => void;
  vehicleId: string;
  currentFrontSolo?: number | null;
  currentRearSolo?: number | null;
  currentFrontPillion?: number | null;
  currentRearPillion?: number | null;
}

export default function TirePressureModal({
  isOpen,
  onClose,
  vehicleId,
  currentFrontSolo = null,
  currentRearSolo = null,
  currentFrontPillion = null,
  currentRearPillion = null,
}: TirePressureModalProps) {
  const [frontSolo, setFrontSolo] = useState<string>(currentFrontSolo != null ? String(currentFrontSolo) : "");
  const [rearSolo, setRearSolo] = useState<string>(currentRearSolo != null ? String(currentRearSolo) : "");
  const [frontPillion, setFrontPillion] = useState<string>(currentFrontPillion != null ? String(currentFrontPillion) : "");
  const [rearPillion, setRearPillion] = useState<string>(currentRearPillion != null ? String(currentRearPillion) : "");
  const [error, setError] = useState<string>("");
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    if (isOpen) {
      setFrontSolo(currentFrontSolo != null ? String(currentFrontSolo) : "");
      setRearSolo(currentRearSolo != null ? String(currentRearSolo) : "");
      setFrontPillion(currentFrontPillion != null ? String(currentFrontPillion) : "");
      setRearPillion(currentRearPillion != null ? String(currentRearPillion) : "");
      setError("");
    }
  }, [isOpen, currentFrontSolo, currentRearSolo, currentFrontPillion, currentRearPillion]);

  if (!isOpen) return null;

  const parsePressure = (val: string) => {
    const trimmed = val.trim();
    if (trimmed === "") return null;
    return Number(trimmed);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const parsedFrontSolo = parsePressure(frontSolo);
    const parsedRearSolo = parsePressure(rearSolo);
    const parsedFrontPillion = parsePressure(frontPillion);
    const parsedRearPillion = parsePressure(rearPillion);

    const validate = (val: number | null, name: string) => {
      if (val !== null && (isNaN(val) || val < 0 || val > 150)) {
        return `Please enter a valid ${name} tire pressure between 0 and 150 PSI.`;
      }
      return null;
    };

    const err =
      validate(parsedFrontSolo, "solo front") ||
      validate(parsedRearSolo, "solo rear") ||
      validate(parsedFrontPillion, "pillion front") ||
      validate(parsedRearPillion, "pillion rear");

    if (err) {
      setError(err);
      return;
    }

    startTransition(async () => {
      const res = await saveTirePressure(vehicleId, {
        front_tire_pressure_solo: parsedFrontSolo,
        rear_tire_pressure_solo: parsedRearSolo,
        front_tire_pressure_pillion: parsedFrontPillion,
        rear_tire_pressure_pillion: parsedRearPillion,
      });

      if (!res.ok) {
        setError(res.error ?? "Failed to save tire pressure.");
        return;
      }

      onClose();
    });
  };

  return (
    <Dialog labelledBy="tire-pressure-title" size="md">
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
          Set the recommended tire pressure values (in PSI) for solo riding and when riding with a pillion.
        </p>

        <div className="mt-6 space-y-4">
          {/* Solo Riding */}
          <div className="rounded-xl border border-border/70 bg-card p-4">
            <h3 className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
              <User className="h-4 w-4 text-primary" /> Solo riding
            </h3>
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-muted-foreground" htmlFor="front-pressure-solo">
                  Front tire (PSI)
                </label>
                <input
                  className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-1 focus:ring-primary"
                  id="front-pressure-solo"
                  max="150"
                  min="0"
                  placeholder="e.g. 29"
                  step="0.1"
                  type="number"
                  value={frontSolo}
                  onChange={(e) => setFrontSolo(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-muted-foreground" htmlFor="rear-pressure-solo">
                  Rear tire (PSI)
                </label>
                <input
                  className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-1 focus:ring-primary"
                  id="rear-pressure-solo"
                  max="150"
                  min="0"
                  placeholder="e.g. 33"
                  step="0.1"
                  type="number"
                  value={rearSolo}
                  onChange={(e) => setRearSolo(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* With Pillion */}
          <div className="rounded-xl border border-border/70 bg-card p-4">
            <h3 className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
              <Users className="h-4 w-4 text-primary" /> With pillion
            </h3>
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-muted-foreground" htmlFor="front-pressure-pillion">
                  Front tire (PSI)
                </label>
                <input
                  className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-1 focus:ring-primary"
                  id="front-pressure-pillion"
                  max="150"
                  min="0"
                  placeholder="e.g. 32"
                  step="0.1"
                  type="number"
                  value={frontPillion}
                  onChange={(e) => setFrontPillion(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-muted-foreground" htmlFor="rear-pressure-pillion">
                  Rear tire (PSI)
                </label>
                <input
                  className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-1 focus:ring-primary"
                  id="rear-pressure-pillion"
                  max="150"
                  min="0"
                  placeholder="e.g. 36"
                  step="0.1"
                  type="number"
                  value={rearPillion}
                  onChange={(e) => setRearPillion(e.target.value)}
                />
              </div>
            </div>
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
