"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Car } from "lucide-react";
import { FuelForm, FuelFormItem } from "../components/fuel-form";
import ConfirmationDialog from "../components/design-system/confirmation-dialog";
import LocalDate from "../components/local-date";
import { markAirFillAction, saveFuelAction } from "./actions";

type Vehicle = {
  id: number;
  name: string;
};

type FuelItem = FuelFormItem;

const newFuelItem = (): FuelItem => ({
  fuelType: "petrol",
  fillType: "full",
  quantity: "",
  unitPrice: "",
  totalCost: "",
});

const localDateTime = () =>
  new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);

const airFillDateOptions: Intl.DateTimeFormatOptions = {
  day: "numeric",
  month: "short",
  hour: "numeric",
  minute: "2-digit",
};

interface VehiclesListProps {
  vehicles: Vehicle[];
  latestAirFills: Record<number, string>;
}

export default function VehiclesList({
  vehicles,
  latestAirFills: initialAirFills,
}: VehiclesListProps) {
  const router = useRouter();
  const [latestAirFills, setLatestAirFills] =
    useState<Record<number, string>>(initialAirFills);
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
        setLatestAirFills((current) => ({
          ...current,
          [pendingAirFillVehicle.id]: res.filled_at!,
        }));
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

    const payload = {
      odometer_km: Number(fuelOdometer),
      filled_at: new Date(fuelDateTime).toISOString(),
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
          <article
            className="cursor-pointer rounded-2xl border border-border bg-card p-6 shadow-sm"
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
            <p className="mt-4 text-sm text-muted-foreground">
              {latestAirFills[vehicle.id] ? (
                <>
                  Air last filled{" "}
                  <LocalDate
                    dateString={latestAirFills[vehicle.id]}
                    options={airFillDateOptions}
                  />
                </>
              ) : (
                "No air fill recorded yet."
              )}
            </p>
            <button
              className="mt-4 w-full rounded-lg border border-border px-4 py-3 text-sm font-semibold text-primary"
              onClick={(event) => {
                event.stopPropagation();
                setAirFillError("");
                setPendingAirFillVehicle(vehicle);
              }}
              type="button"
            >
              Mark air filled now
            </button>
            <button
              className="mt-3 w-full rounded-lg bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground"
              onClick={(event) => {
                event.stopPropagation();
                setFuelError("");
                setFuelVehicle(vehicle);
              }}
              type="button"
            >
              Add fuel
            </button>
          </article>
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
        <div
          className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/60 p-4 backdrop-blur-xs sm:p-6"
          role="dialog"
          aria-labelledby="fuel-title"
          aria-modal="true"
        >
          <section className="w-full max-w-2xl rounded-2xl border border-border bg-card p-6 shadow-2xl sm:p-8">
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
          </section>
        </div>
      )}
    </>
  );
}
