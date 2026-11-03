"use client";

import PulseCard from "@/app/components/design-system/pulse-card";
import { useTimeZone } from "@/app/components/time-zone-provider";
import { toDateTimeInput, fromDateTimeInput } from "@/app/lib/time-zone.mjs";

import Button from "@/app/components/design-system/button";

import { useState, useTransition } from "react";
import Link from "next/link";
import { Bike, Bus, Car, ChevronRight, Fuel, Gauge, Truck } from "lucide-react";
import { FuelForm, FuelFormItem } from "@/app/components/fuel-form";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import Dialog from "@/app/components/design-system/dialog";
import { markAirFillAction, saveFuelAction } from "./actions";

type Vehicle = {
  id: number;
  name: string;
  kind: string;
  make_name: string;
  model_name: string;
  fuel_types: string[];
};

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
      <PulseCard className="divide-y divide-divider px-4 sm:px-6">
        {vehicles.map((vehicle) => {
          const VehicleIcon = vehicle.kind === "motorcycle" || vehicle.kind === "moped" ? Bike
            : vehicle.kind === "bus" ? Bus
            : vehicle.kind === "truck" || vehicle.kind === "van" ? Truck
            : Car;

          return (
            <article key={vehicle.id} className="grid gap-3 py-4 sm:py-5 md:grid-cols-[minmax(0,1fr)_auto] md:items-center md:gap-6">
              <Link
                href={`/garage/${vehicle.id}`}
                className="group flex min-w-0 items-center gap-3 rounded-xl p-2 hover:bg-secondary/60 focus-visible:outline-2 focus-visible:outline-ring sm:gap-4"
              >
                <span aria-hidden="true" className="grid size-12 shrink-0 place-items-center rounded-xl bg-accent text-accent-foreground sm:size-14">
                  <VehicleIcon className="size-6" strokeWidth={1.6} />
                </span>
                <div className="min-w-0 flex-1">
                  <h3 className="text-lg leading-tight font-semibold break-words text-foreground">{vehicle.name}</h3>
                  {(vehicle.make_name || vehicle.model_name) && (
                    <p className="mt-1 text-sm break-words text-muted-foreground">{vehicle.make_name} {vehicle.model_name}</p>
                  )}
                  <p className="mt-1 text-xs text-muted-foreground">
                    <span>{vehicle.kind === "unknown" ? "Kind not selected" : vehicle.kind.charAt(0).toUpperCase() + vehicle.kind.slice(1)}</span>
                    {vehicle.fuel_types.length > 0 && (
                      <span className="ml-2 border-l border-border pl-2">{vehicle.fuel_types.join(" + ").toUpperCase()}</span>
                    )}
                  </p>
                </div>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground group-hover:text-action" aria-hidden="true" />
              </Link>

              <div className="flex flex-wrap gap-2 px-2">
                <Button
                  variant="secondary"
                  size="lg"
                  icon={<Gauge className="size-4 shrink-0" aria-hidden="true" />}
                  aria-label={`Mark air filled now for ${vehicle.name}`}
                  onClick={() => {
                    setAirFillError("");
                    setPendingAirFillVehicle(vehicle);
                  }}
                  type="button"
                >
                  Mark air filled now
                </Button>
                <Button
                  variant="soft"
                  size="lg"
                  icon={<Fuel className="size-4 shrink-0" aria-hidden="true" />}
                  aria-label={`Add fuel for ${vehicle.name}`}
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
              </div>
            </article>
          );
        })}
      </PulseCard>

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
