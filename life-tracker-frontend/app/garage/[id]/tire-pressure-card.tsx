"use client";

import PulseCard from "../../components/design-system/pulse-card";

import Button from "../../components/design-system/button";

import { useState } from "react";
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
