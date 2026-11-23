"use client";

import { useTimeZone } from "@/app/components/profile-preferences-provider";
import { toDateTimeInput, fromDateTimeInput } from "@/app/lib/time-zone.mjs";

import PulseButton from "@/app/components/design-system/pulse-button";

import { createContext, useCallback, useContext, useMemo, useState, useTransition, type ReactNode } from "react";
import { CircleDot, Droplet } from "lucide-react";
import { FuelForm, FuelFormItem } from "@/app/components/fuel-form";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import PulseDialog, { PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import { markAirFillAction, saveFuelAction } from "@/app/garage/actions";
import type { Vehicle } from "@/app/garage/types";

type FuelItem = FuelFormItem;
type ActionVehicle = Pick<Vehicle, "id" | "name">;

const newFuelItem = (): FuelItem => ({
  fuelType: "petrol",
  fillType: "full",
  quantity: "",
  unitPrice: "",
  totalCost: "",
});

export function VehicleActionProvider({ children }: { children: ReactNode }) {
  const { timeZone } = useTimeZone();
  const [entryTimeZone, setEntryTimeZone] = useState(timeZone);
  const [pendingAirFillVehicle, setPendingAirFillVehicle] =
    useState<ActionVehicle | null>(null);
  const [isMarkingAirFill, startMarkingAirFill] = useTransition();
  const [airFillError, setAirFillError] = useState("");

  const [fuelVehicle, setFuelVehicle] = useState<ActionVehicle | null>(null);
  const [fuelOdometer, setFuelOdometer] = useState("");
  const [fuelDateTime, setFuelDateTime] = useState("");
  const [stationName, setStationName] = useState("");
  const [fuelNotes, setFuelNotes] = useState("");
  const [fuelItems, setFuelItems] = useState<FuelItem[]>(() => [newFuelItem()]);
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
        setStationName("");
        setFuelNotes("");
        setFuelItems([newFuelItem()]);
      }
    });
  }

  const openFuel = useCallback((vehicle: ActionVehicle) => {
    setFuelError("");
    setEntryTimeZone(timeZone);
    setFuelDateTime(toDateTimeInput(new Date(), timeZone));
    setFuelVehicle(vehicle);
  }, [timeZone]);

  const openAirFill = useCallback((vehicle: ActionVehicle) => {
    setAirFillError("");
    setPendingAirFillVehicle(vehicle);
  }, []);

  const actions = useMemo(() => ({ openFuel, openAirFill }), [openFuel, openAirFill]);

  return (
    <VehicleActionContext.Provider value={actions}>
      {children}

      <PulseConfirmationDialog
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
        variant="positive"
      />

      {fuelVehicle && (
        <PulseDialog labelledBy="fuel-title" size="lg">
            <PulseDialogHeader id="fuel-title" icon={<Droplet className="size-5" />} description={fuelVehicle.name}>
              Add fuel
            </PulseDialogHeader>
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
        </PulseDialog>
      )}
    </VehicleActionContext.Provider>
  );
}


const VehicleActionContext = createContext<{
  openFuel: (vehicle: ActionVehicle) => void;
  openAirFill: (vehicle: ActionVehicle) => void;
} | null>(null);

export function VehicleQuickActions({ vehicle }: { vehicle: ActionVehicle }) {
  const actions = useContext(VehicleActionContext);
  if (!actions) throw new Error("VehicleQuickActions requires VehicleActionProvider.");
  const { openFuel, openAirFill } = actions;

  return (
    <div className="mt-auto grid grid-cols-2 gap-2 xl:gap-3">
      <PulseButton
        variant="gradient"
        size="lg"
        shape="pill"
        className="garage-fuel-action min-w-0 gap-1.5! px-1.5! leading-tight sm:min-h-12 lg:min-h-14"
        icon={<Droplet className="size-4 shrink-0" aria-hidden="true" />}
        aria-label={`Add fuel for ${vehicle.name}`}
        aria-haspopup="dialog"
        onClick={() => openFuel(vehicle)}
        type="button"
      >
        Add fuel
      </PulseButton>
      <PulseButton
        variant="outline"
        size="lg"
        shape="pill"
        className="min-w-0 gap-1.5! px-1.5! leading-tight sm:min-h-12 lg:min-h-14"
        icon={<CircleDot className="size-4 shrink-0" aria-hidden="true" />}
        aria-label={`Mark airfill now for ${vehicle.name}`}
        aria-haspopup="dialog"
        onClick={() => openAirFill(vehicle)}
        type="button"
      >
        Mark airfill
      </PulseButton>
    </div>
  );
}
