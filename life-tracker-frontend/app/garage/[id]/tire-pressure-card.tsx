"use client";

import Button from "@/app/components/design-system/button";
import { markAirFillAction } from "../actions";
import { useRouter } from "next/navigation";
import { useId, useState, useTransition, type ReactNode } from "react";
import { CircleDot, Pencil } from "lucide-react";
import TirePressureModal from "./tire-pressure-modal";
import { detail } from "./vehicle-detail-classes";

interface TirePressureCardProps {
  vehicleId: string;
  frontTirePressureSolo?: number | null;
  rearTirePressureSolo?: number | null;
  frontTirePressurePillion?: number | null;
  rearTirePressurePillion?: number | null;
  children: ReactNode;
}

export default function TirePressureCard({
  vehicleId,
  frontTirePressureSolo,
  rearTirePressureSolo,
  frontTirePressurePillion,
  rearTirePressurePillion,
  children,
}: TirePressureCardProps) {
  const odometerId = useId();
  const [odometer, setOdometer] = useState("");
  const [error, setError] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);

  const pressureRows = [
    { label: "Riding solo", readings: [{ position: "Front", pressure: frontTirePressureSolo }, { position: "Rear", pressure: rearTirePressureSolo }] },
    { label: "With pillion", readings: [{ position: "Front", pressure: frontTirePressurePillion }, { position: "Rear", pressure: rearTirePressurePillion }] },
  ];
  const hasAnyPressure = pressureRows.some(({ readings }) => readings.some(({ pressure }) => pressure != null));

  return (
    <section id="tires-air" aria-labelledby="tires-heading" className={detail.section}>
      <div className={detail.sectionHeader}>
        <h2 id="tires-heading" className={detail.sectionTitle}><CircleDot className="size-6" aria-hidden="true" /> Tires &amp; air</h2>
        <Button className={detail.secondaryButton} variant="secondary" type="button" aria-haspopup="dialog" onClick={() => setIsOpen(true)}>
          <Pencil className="size-4" aria-hidden="true" /> {hasAnyPressure ? "Edit pressure" : "Set pressure"}
        </Button>
      </div>

      <section aria-labelledby="pressure-heading" className={detail.card}>
        <h3 id="pressure-heading" className={`${detail.subheading} mb-4`}>Target tire pressure</h3>
        <div className="grid grid-cols-2 gap-5 max-[761px]:grid-cols-1">
          {pressureRows.map(({ label, readings }) => (
            <div key={label} className="border-(--detail-line) [&+div]:border-s [&+div]:ps-5 max-[761px]:[&+div]:border-s-0 max-[761px]:[&+div]:border-t max-[761px]:[&+div]:ps-0 max-[761px]:[&+div]:pt-5">
              <h4 className="mb-3 text-base font-extrabold">{label}</h4>
              <dl className="grid grid-cols-2 gap-3">
                {readings.map(({ position, pressure }) => (
                  <div key={position} className="rounded-[20px] bg-(--detail-inset) p-[clamp(10px,2vw,18px)]">
                    <dt className={detail.label}>{position}</dt>
                    <dd className={`${detail.metric} mt-1 tabular-nums`}>
                      {pressure == null ? <span className={`${detail.muted} text-sm font-semibold`}>Not set</span> : <>{pressure}<small className={detail.metricUnit}>PSI</small></>}
                    </dd>
                  </div>
                ))}
              </dl>
            </div>
          ))}
        </div>
      </section>

      <div className="grid grid-cols-2 gap-5 max-[1001px]:grid-cols-1">
        <section aria-labelledby="air-fill-heading" className={detail.card}>
          <h3 id="air-fill-heading" className={detail.subheading}>Record an air fill</h3>
          <form className="mt-4 grid gap-4" onSubmit={(event) => {
            event.preventDefault();
            setError("");
            startTransition(async () => {
              const result = await markAirFillAction(Number(vehicleId), odometer === "" ? null : Number(odometer));
              if (!result.ok) {
                setError(result.error ?? "Could not record air fill.");
                return;
              }
              setOdometer("");
              router.refresh();
            });
          }}>
            <div>
              <label htmlFor={odometerId} className={`${detail.muted} mb-1.5 block text-sm font-bold`}>Odometer (km, optional)</label>
              <input id={odometerId} type="number" min="0" max="99999999999.9" step="0.1" value={odometer} onChange={(event) => setOdometer(event.target.value)} className={`${detail.field} rounded-[14px] px-3.5`} />
            </div>
            <Button className={`${detail.primaryButton} justify-self-start`} type="submit" disabled={pending}>
              <CircleDot className="size-4" aria-hidden="true" /> {pending ? "Saving…" : "Record air fill"}
            </Button>
            {error && <p role="alert" className={`${detail.danger} text-sm`}>{error}</p>}
          </form>
        </section>
        {children}
      </div>

      <TirePressureModal
        currentFrontPillion={frontTirePressurePillion}
        currentFrontSolo={frontTirePressureSolo}
        currentRearPillion={rearTirePressurePillion}
        currentRearSolo={rearTirePressureSolo}
        isOpen={isOpen}
        vehicleId={vehicleId}
        onClose={() => setIsOpen(false)}
      />
    </section>
  );
}
