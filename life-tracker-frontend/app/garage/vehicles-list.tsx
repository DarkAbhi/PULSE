"use client";

import PulseCard from "../components/design-system/pulse-card";
import { useTimeZone } from "../components/time-zone-provider";
import { toDateTimeInput, fromDateTimeInput } from "../lib/time-zone.mjs";

import Button from "../components/design-system/button";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Car } from "lucide-react";
import { FuelForm, FuelFormItem } from "../components/fuel-form";
import ConfirmationDialog from "../components/design-system/confirmation-dialog";
import Dialog from "../components/design-system/dialog";
import { markAirFillAction, saveFuelAction } from "./actions";

type Vehicle = {
  id: number;
  name: string;
  vehicle_type: "two_wheeler" | "four_wheeler";
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
  const router = useRouter();
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
      <section
        className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
        aria-label="Vehicles"
      >
        {vehicles.map((vehicle) => (
          <PulseCard as="article"
            className="cursor-pointer p-6"
            key={vehicle.id}
            onClick={() => router.push(`/garage/${vehicle.id}`)}
          >
            <div
              className="flex h-11 w-11 items-center justify-center rounded-xl bg-secondary text-secondary-foreground"
              aria-hidden="true"
            >
              <Car className="h-5 w-5" />
            </div>
            <h2 className="mt-4 text-lg font-semibold text-foreground">
              {vehicle.name}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {vehicle.vehicle_type === "two_wheeler" ? "Two-wheeler" : "Four-wheeler"}
              {" · "}{vehicle.fuel_types.join(" + ").toUpperCase()}
            </p>
            <Button variant="secondary" size="lg"
              onClick={(event) => {
                event.stopPropagation();
                setAirFillError("");
                setPendingAirFillVehicle(vehicle);
              }}
              type="button"
              className="mt-4 w-full"
            >
              Mark air filled now
            </Button>
            <Button variant="primary" size="lg"
              className="mt-3 w-full"
              onClick={(event) => {
                event.stopPropagation();
                setFuelError("");
                setEntryTimeZone(timeZone);
                setFuelDateTime(localDateTime());
                setFuelVehicle(vehicle);
              }}
              type="button"
            >
              Add fuel
            </Button>
          </PulseCard>
        ))}
      </section>

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
              Add fuel for {fuelVehicle.name}
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              Enter any two fuel-cost values; the third is calculated
              automatically when saved.
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
