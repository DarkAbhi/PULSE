"use client";

import { useState } from "react";
import { Gauge, Pencil } from "lucide-react";
import TirePressureModal from "./tire-pressure-modal";

interface TirePressureCardProps {
  vehicleId: string;
  frontTirePressure: number | null | undefined;
  rearTirePressure: number | null | undefined;
}

export default function TirePressureCard({
  vehicleId,
  frontTirePressure,
  rearTirePressure,
}: TirePressureCardProps) {
  const [isOpen, setIsOpen] = useState(false);

  const hasPressure = frontTirePressure != null || rearTirePressure != null;

  return (
    <>
      <aside className="mt-4 rounded-2xl border border-primary/20 bg-primary/5 p-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="flex items-center gap-2 font-semibold text-foreground">
              <Gauge className="h-5 w-5 text-primary" /> Target tire pressure
            </h3>
            {hasPressure ? (
              <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2">
                <p className="text-sm text-muted-foreground">
                  Front:{" "}
                  <span className="font-semibold text-foreground">
                    {frontTirePressure != null ? `${frontTirePressure} PSI` : "—"}
                  </span>
                </p>
                <p className="text-sm text-muted-foreground">
                  Rear:{" "}
                  <span className="font-semibold text-foreground">
                    {rearTirePressure != null ? `${rearTirePressure} PSI` : "—"}
                  </span>
                </p>
              </div>
            ) : (
              <p className="mt-2 text-sm text-muted-foreground">
                No tire pressure values configured yet.
              </p>
            )}
          </div>
          <button
            type="button"
            className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-border bg-card px-3 py-1.5 text-xs font-semibold text-foreground shadow-xs hover:bg-accent"
            onClick={() => setIsOpen(true)}
          >
            {hasPressure ? (
              <>
                <Pencil className="h-3.5 w-3.5" /> Edit
              </>
            ) : (
              "Set pressure"
            )}
          </button>
        </div>
      </aside>

      <TirePressureModal
        currentFront={frontTirePressure}
        currentRear={rearTirePressure}
        isOpen={isOpen}
        vehicleId={vehicleId}
        onClose={() => setIsOpen(false)}
      />
    </>
  );
}
