"use client";

import PulseCard from "@/app/components/design-system/pulse-card";
import { useTimeZone } from "@/app/components/time-zone-provider";
import { toDateTimeInput, fromDateTimeInput } from "@/app/lib/time-zone.mjs";

import Button from "@/app/components/design-system/button";

import { useState, useTransition } from "react";
import Link from "next/link";
import { Bus, CarFront, CircleDot, Droplet, Motorbike, Scooter, Truck, Van } from "lucide-react";
import { FuelForm, FuelFormItem } from "@/app/components/fuel-form";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import Dialog from "@/app/components/design-system/dialog";
import { markAirFillAction, saveFuelAction } from "./actions";
import styles from "./garage.module.css";

export type Vehicle = {
  id: number;
  name: string;
  kind: string;
  make_name: string;
  model_name: string;
  fuel_types: string[];
};

const vehicleIcons: Record<string, typeof CarFront> = {
  car: CarFront,
  motorcycle: Motorbike,
  moped: Scooter,
  scooter: Scooter,
  van: Van,
  truck: Truck,
  bus: Bus,
};

function formatLabel(value: string) {
  return value === "cng" || value === "lpg"
    ? value.toUpperCase()
    : value.charAt(0).toUpperCase() + value.slice(1);
}

type FuelItem = FuelFormItem;

const newFuelItem = (): FuelItem => ({
  fuelType: "petrol",
  fillType: "full",
  quantity: "",
  unitPrice: "",
  totalCost: "",
});

interface VehiclesListProps {
  vehicles: Vehicle[];
}

export default function VehiclesList({ vehicles }: VehiclesListProps) {
  const { timeZone } = useTimeZone();
  const [entryTimeZone, setEntryTimeZone] = useState(timeZone);
  const localDateTime = () => toDateTimeInput(new Date(), timeZone);
  const [pendingAirFillVehicle, setPendingAirFillVehicle] =
    useState<Vehicle | null>(null);
  const [isMarkingAirFill, startMarkingAirFill] = useTransition();
  const [airFillError, setAirFillError] = useState("");

  const [fuelVehicle, setFuelVehicle] = useState<Vehicle | null>(null);
  const [fuelOdometer, setFuelOdometer] = useState("");
  const [fuelDateTime, setFuelDateTime] = useState(localDateTime());
  const [stationName, setStationName] = useState("");
  const [fuelNotes, setFuelNotes] = useState("");
  const [fuelItems, setFuelItems] = useState<FuelItem[]>([newFuelItem()]);
  const [isSavingFuel, startSavingFuel] = useTransition();
  const [fuelError, setFuelError] = useState("");

  function handleMarkAirFill() {
    if (!pendingAirFillVehicle) return;
    setAirFillError("");

    startMarkingAirFill(async () => {
      const res = await markAirFillAction(pendingAirFillVehicle.id);
      if (!res.ok) {
        setAirFillError(
          res.error ?? "We couldn't record the air fill. Please try again.",
        );
      } else {
        setPendingAirFillVehicle(null);
      }
    });
  }

  function handleSaveFuel(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!fuelVehicle) return;
    setFuelError("");

    const numberOrNull = (value: string) =>
      value === "" ? null : Number(value);

    let filledAt: string;
    try { filledAt = fromDateTimeInput(fuelDateTime, entryTimeZone); }
    catch (error) { setFuelError(error instanceof Error ? error.message : "Choose a valid date and time."); return; }
    const payload = {
      odometer_km: Number(fuelOdometer),
      filled_at: filledAt,
      station_name: stationName || null,
      notes: fuelNotes || null,
      items: fuelItems.map((item) => ({
        fuel_type: item.fuelType,
        fill_type: item.fillType,
        quantity: numberOrNull(item.quantity),
        unit_price: numberOrNull(item.unitPrice),
        total_cost: numberOrNull(item.totalCost),
      })),
    };

    startSavingFuel(async () => {
      const res = await saveFuelAction(fuelVehicle.id, payload);
      if (!res.ok) {
        setFuelError(res.error ?? "We couldn't save this fuel entry.");
      } else {
        setFuelVehicle(null);
        setFuelOdometer("");
        setFuelDateTime(localDateTime());
        setStationName("");
        setFuelNotes("");
        setFuelItems([newFuelItem()]);
      }
    });
  }

  return (
    <>
      <div className="grid grid-cols-1 gap-4 min-[680px]:grid-cols-2 min-[1040px]:grid-cols-3 xl:gap-6">
        {vehicles.map((vehicle) => {
          const VehicleIcon = vehicleIcons[vehicle.kind] ?? CarFront;
          const model = [vehicle.make_name, vehicle.model_name].filter(Boolean).join(" ");

          return (
            <PulseCard
              as="article"
              key={vehicle.id}
              className={`${styles.vehicleCard} flex flex-col gap-4 p-4 sm:gap-5 sm:p-5 xl:p-7`}
            >
              <Link
                href={`/garage/${vehicle.id}`}
                className="flex min-w-0 items-center gap-3 rounded-2xl sm:gap-4"
                aria-label={`View ${vehicle.name} details`}
              >
                <span aria-hidden="true" data-kind={vehicle.kind} className={`${styles.vehicleIcon} grid size-14 shrink-0 place-items-center text-white sm:size-16 xl:size-20`}>
                  <VehicleIcon className="size-7 xl:size-9" strokeWidth={2} />
                </span>
                <div className="min-w-0 flex-1">
                  <h3 className="text-xl leading-tight font-extrabold tracking-tight wrap-anywhere sm:text-2xl xl:text-3xl">{vehicle.name}</h3>
                  {model && (
                    <p className={`${styles.muted} mt-0.5 text-sm wrap-anywhere sm:text-base`}>{model}</p>
                  )}
                </div>
              </Link>

              <div className="flex flex-wrap gap-2">
                <span className={styles.badge}>
                  <VehicleIcon className="size-4 shrink-0" aria-hidden="true" />
                  {vehicle.kind === "unknown" ? "Kind not selected" : formatLabel(vehicle.kind)}
                </span>
                {vehicle.fuel_types.map((fuel) => (
                  <span key={fuel} className={styles.badge}>
                    <span aria-hidden="true" data-fuel={fuel} className={styles.fuelDot} />
                    {formatLabel(fuel)}
                  </span>
                ))}
              </div>

              <div className="mt-auto grid grid-cols-2 gap-2 xl:gap-3">
                <Button
                  variant="soft"
                  size="lg"
                  className={`${styles.quickAction} ${styles.addFuel}`}
                  icon={<Droplet className="size-4 shrink-0" aria-hidden="true" />}
                  aria-label={`Add fuel for ${vehicle.name}`}
                  aria-haspopup="dialog"
                  onClick={() => {
                    setFuelError("");
                    setEntryTimeZone(timeZone);
                    setFuelDateTime(localDateTime());
                    setFuelVehicle(vehicle);
                  }}
                  type="button"
                >
                  Add fuel
                </Button>
                <Button
                  variant="secondary"
                  size="lg"
                  className={`${styles.quickAction} ${styles.markAirFill}`}
                  icon={<CircleDot className="size-4 shrink-0" aria-hidden="true" />}
                  aria-label={`Mark airfill now for ${vehicle.name}`}
                  aria-haspopup="dialog"
                  onClick={() => {
                    setAirFillError("");
                    setPendingAirFillVehicle(vehicle);
                  }}
                  type="button"
                >
                  Mark airfill
                </Button>
              </div>
            </PulseCard>
          );
        })}
      </div>

      <ConfirmationDialog
        isOpen={!!pendingAirFillVehicle}
        onClose={() => setPendingAirFillVehicle(null)}
        onConfirm={handleMarkAirFill}
        title="Mark air filled?"
        description={
          pendingAirFillVehicle
            ? `Record that you filled air in ${pendingAirFillVehicle.name} right now. We'll remind you again in 30 days.`
            : ""
        }
        confirmText="Yes, mark air filled"
        confirmLoadingText="Marking…"
        isLoading={isMarkingAirFill}
        error={airFillError}
        variant="amber"
      />

      {fuelVehicle && (
        <Dialog labelledBy="fuel-title" size="lg">
            <h2
              className="text-2xl font-bold tracking-tight text-foreground"
              id="fuel-title"
            >
              Add fuel
            </h2>
            <p className="mt-1 text-sm break-words text-muted-foreground">
              {fuelVehicle.name}
            </p>
            <FuelForm
              error={fuelError}
              filledAt={fuelDateTime}
              isSaving={isSavingFuel}
              items={fuelItems}
              notes={fuelNotes}
              odometer={fuelOdometer}
              onCancel={() => setFuelVehicle(null)}
              onFilledAtChange={setFuelDateTime}
              onItemsChange={setFuelItems}
              onNotesChange={setFuelNotes}
              onOdometerChange={setFuelOdometer}
              onStationNameChange={setStationName}
              onSubmit={handleSaveFuel}
              savingLabel="Saving…"
              stationName={stationName}
              submitLabel="Save fuel entry"
            />
        </Dialog>
      )}
    </>
  );
}
