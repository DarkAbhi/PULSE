"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import Button from "@/app/components/design-system/button";

import { markAirFillAction } from "../actions";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { Pencil } from "lucide-react";
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

  const pressureRows = [
    { label: "Solo riding", readings: [frontTirePressureSolo, rearTirePressureSolo] },
    { label: "With pillion", readings: [frontTirePressurePillion, rearTirePressurePillion] },
  ];
  const hasAnyPressure = pressureRows.some(({ readings }) => readings.some((pressure) => pressure != null));

  return (
    <>
      <PulseCard as="section" aria-labelledby="pressure-heading" className="min-w-0 p-4 sm:p-5 lg:row-span-2">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 id="pressure-heading" className="font-semibold">Target tire pressure</h3>
            <p className="mt-1 text-sm text-muted-foreground">Saved settings by riding load.</p>
          </div>
          <Button variant="secondary" size="sm" type="button" onClick={() => setIsOpen(true)}>
            {hasAnyPressure ? (
              <>
                <Pencil className="size-3.5" aria-hidden="true" /> Edit pressure
              </>
            ) : (
              "Set pressure"
            )}
          </Button>
        </div>

        <div className="mt-5 rounded-xl bg-secondary px-3 sm:px-5">
          <table aria-labelledby="pressure-heading" className="w-full table-fixed text-left">
            <thead>
              <tr className="border-b border-border">
                <th scope="col" className="w-[38%] py-4 text-xs font-medium text-muted-foreground">Riding load</th>
                {["Front", "Rear"].map((position) => (
                  <th key={position} scope="col" className="py-4 text-center text-sm font-semibold">
                    <svg aria-hidden="true" viewBox="0 0 24 32" fill="none" className="mx-auto mb-2 h-8 w-6 text-action">
                      <rect x="4" y="2" width="16" height="28" rx="5" fill="currentColor" fillOpacity=".12" stroke="currentColor" strokeWidth="1.5" />
                      <path d="m8 8 4 3 4-3m-8 7 4 3 4-3m-8 7 4 3 4-3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                    {position}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {pressureRows.map(({ label, readings }) => (
                <tr key={label}>
                  <th scope="row" className="py-4 pr-2 text-sm font-medium">{label}</th>
                  {readings.map((pressure, index) => (
                    <td key={index} className="py-4 text-center">
                      {pressure == null ? (
                        <span className="text-xs text-muted-foreground">Not set</span>
                      ) : (
                        <span className="inline-flex flex-wrap items-baseline justify-center gap-x-1">
                          <span className="text-2xl font-semibold tracking-tight tabular-nums sm:text-3xl">{pressure}</span>
                          <span className="text-xs text-muted-foreground">PSI</span>
                        </span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </PulseCard>

      <PulseCard as="section" aria-labelledby="air-fill-heading" className="min-w-0 p-4 sm:p-5">
        <h3 id="air-fill-heading" className="font-semibold">Record an air fill</h3>
        <form className="mt-4 space-y-3" onSubmit={(event) => {
          event.preventDefault(); setError(""); startTransition(async () => {
            const result = await markAirFillAction(Number(vehicleId), odometer === "" ? null : Number(odometer));
            if (!result.ok) { setError(result.error ?? "Could not record air fill."); return; }
            setOdometer(""); router.refresh();
          });
        }}>
          <label className="block text-sm">
            Odometer <span className="text-muted-foreground">(km, optional)</span>
            <input type="number" min="0" max="99999999999.9" step="0.1" value={odometer} onChange={(e) => setOdometer(e.target.value)} className="mt-1.5 w-full rounded-lg border border-input bg-background px-3 py-2 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring" />
          </label>
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
