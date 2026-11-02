"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import Button from "@/app/components/design-system/button";

import { markAirFillAction } from "../actions";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Gauge, Pencil, User, Users } from "lucide-react";
import TirePressureModal from "./tire-pressure-modal";

interface TirePressureCardProps {
  vehicleId: string;
  frontTirePressureSolo?: number | null;
  rearTirePressureSolo?: number | null;
  frontTirePressurePillion?: number | null;
  rearTirePressurePillion?: number | null;
}

export default function TirePressureCard({
  vehicleId,
  frontTirePressureSolo,
  rearTirePressureSolo,
  frontTirePressurePillion,
  rearTirePressurePillion,
}: TirePressureCardProps) {
  const [odometer, setOdometer] = useState("");
  const [error, setError] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);

  const hasSolo = frontTirePressureSolo != null || rearTirePressureSolo != null;
  const hasPillion = frontTirePressurePillion != null || rearTirePressurePillion != null;
  const hasAnyPressure = hasSolo || hasPillion;

  return (
    <>
      <PulseCard as="aside" className="mt-4 bg-primary/5 p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="space-y-3 flex-1">
            <h3 className="flex items-center gap-2 font-semibold text-foreground">
              <Gauge className="h-5 w-5 text-action" /> Target tire pressure
            </h3>
            {hasAnyPressure ? (
              <div className="grid gap-2 sm:grid-cols-2">
                {/* Solo */}
                <PulseCard className="p-3">
                  <p className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                    <User className="h-3.5 w-3.5 text-action" /> Solo riding
                  </p>
                  <p className="mt-1.5 text-xs text-muted-foreground">
                    Front:{" "}
                    <span className="font-semibold text-foreground">
                      {frontTirePressureSolo != null ? `${frontTirePressureSolo} PSI` : "—"}
                    </span>
                    {"  ·  "}
                    Rear:{" "}
                    <span className="font-semibold text-foreground">
                      {rearTirePressureSolo != null ? `${rearTirePressureSolo} PSI` : "—"}
                    </span>
                  </p>
                </PulseCard>

                {/* With Pillion */}
                <PulseCard className="p-3">
                  <p className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                    <Users className="h-3.5 w-3.5 text-action" /> With pillion
                  </p>
                  <p className="mt-1.5 text-xs text-muted-foreground">
                    Front:{" "}
                    <span className="font-semibold text-foreground">
                      {frontTirePressurePillion != null ? `${frontTirePressurePillion} PSI` : "—"}
                    </span>
                    {"  ·  "}
                    Rear:{" "}
                    <span className="font-semibold text-foreground">
                      {rearTirePressurePillion != null ? `${rearTirePressurePillion} PSI` : "—"}
                    </span>
                  </p>
                </PulseCard>
              </div>
            ) : (
              <p className="mt-2 text-sm text-muted-foreground">
                No tire pressure values configured yet.
              </p>
            )}
          </div>
          <Button variant="secondary" size="sm"
            type="button"
            className="shrink-0"
            onClick={() => setIsOpen(true)}
          >
            {hasAnyPressure ? (
              <>
                <Pencil className="h-3.5 w-3.5" /> Edit
              </>
            ) : (
              "Set pressure"
            )}
          </Button>
        </div>
        <form className="mt-4 space-y-3" onSubmit={(event) => {
          event.preventDefault(); setError(""); startTransition(async () => {
            const result = await markAirFillAction(Number(vehicleId), odometer === "" ? null : Number(odometer));
            if (!result.ok) { setError(result.error ?? "Could not record air fill."); return; }
            setOdometer(""); router.refresh();
          });
        }}>
          <label className="block text-sm">Odometer (km, optional)<input type="number" min="0" max="99999999999.9" step="0.1" value={odometer} onChange={(e) => setOdometer(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2" /></label>
          <Button type="submit" disabled={pending}>{pending ? "Saving…" : "Record air fill"}</Button>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        </form>
      </PulseCard>

      <TirePressureModal
        currentFrontPillion={frontTirePressurePillion}
        currentFrontSolo={frontTirePressureSolo}
        currentRearPillion={rearTirePressurePillion}
        currentRearSolo={rearTirePressureSolo}
        isOpen={isOpen}
        vehicleId={vehicleId}
        onClose={() => setIsOpen(false)}
      />
    </>
  );
}
