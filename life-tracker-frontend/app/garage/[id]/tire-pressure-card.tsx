"use client";

import Button from "@/app/components/design-system/button";
import { markAirFillAction } from "../actions";
import { useRouter } from "next/navigation";
import { useId, useState, useTransition, type ReactNode } from "react";
import { CircleDot, Pencil } from "lucide-react";
import TirePressureModal from "./tire-pressure-modal";
import styles from "./vehicle-detail.module.css";

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
    <section id="tires-air" aria-labelledby="tires-heading" className={styles.section}>
      <div className={styles.sectionHeader}>
        <h2 id="tires-heading"><CircleDot className="size-6" aria-hidden="true" /> Tires &amp; air</h2>
        <Button className={`${styles.button} ${styles.secondary}`} variant="secondary" type="button" aria-haspopup="dialog" onClick={() => setIsOpen(true)}>
          <Pencil className="size-4" aria-hidden="true" /> {hasAnyPressure ? "Edit pressure" : "Set pressure"}
        </Button>
      </div>

      <section aria-labelledby="pressure-heading" className={styles.card}>
        <h3 id="pressure-heading" className={`${styles.subheading} mb-4`}>Target tire pressure</h3>
        <div className={styles.pressures}>
          {pressureRows.map(({ label, readings }) => (
            <div key={label}>
              <h4 className="mb-3 text-base font-extrabold">{label}</h4>
              <dl className="grid grid-cols-2 gap-3">
                {readings.map(({ position, pressure }) => (
                  <div key={position} className={styles.psi}>
                    <dt className={styles.label}>{position}</dt>
                    <dd className={`${styles.metric} mt-1 tabular-nums`}>
                      {pressure == null ? <span className={`${styles.muted} text-sm font-semibold`}>Not set</span> : <>{pressure}<small>PSI</small></>}
                    </dd>
                  </div>
                ))}
              </dl>
            </div>
          ))}
        </div>
      </section>

      <div className={styles.twoColumns}>
        <section aria-labelledby="air-fill-heading" className={styles.card}>
          <h3 id="air-fill-heading" className={styles.subheading}>Record an air fill</h3>
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
              <label htmlFor={odometerId} className={`${styles.muted} mb-1.5 block text-sm font-bold`}>Odometer (km, optional)</label>
              <input id={odometerId} type="number" min="0" max="99999999999.9" step="0.1" value={odometer} onChange={(event) => setOdometer(event.target.value)} className={styles.field} />
            </div>
            <Button className={`${styles.button} ${styles.primary} justify-self-start`} type="submit" disabled={pending}>
              <CircleDot className="size-4" aria-hidden="true" /> {pending ? "Saving…" : "Record air fill"}
            </Button>
            {error && <p role="alert" className={`${styles.dangerText} text-sm`}>{error}</p>}
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
