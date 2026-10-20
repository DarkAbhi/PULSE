"use client";

import Button from "../../components/design-system/button";

import { useState, useEffect, useTransition } from "react";
import { useRouter } from "next/navigation";
import { FuelForm, FuelFormItem } from "../../components/fuel-form";
import { FuelFill, FuelItem } from "./types";
import { saveFuelFill } from "./actions";
import Dialog from "../../components/design-system/dialog";

interface EditFuelModalProps {
  vehicleId: string;
  fill: FuelFill;
}

const toLocal = (value: string) =>
  new Date(new Date(value).getTime() - new Date().getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);

const toFormItem = (item: FuelItem): FuelFormItem => ({
  fuelType: item.fuel_type,
  fillType: item.fill_type,
  quantity: String(item.quantity),
  unitPrice: String(item.unit_price),
  totalCost: String(item.total_cost),
});

export default function EditFuelModal({ vehicleId, fill }: EditFuelModalProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [toastError, setToastError] = useState("");

  const [odometer, setOdometer] = useState(String(fill.odometer_km));
  const [filledAt, setFilledAt] = useState(toLocal(fill.filled_at));
  const [station, setStation] = useState(fill.station_name ?? "");
  const [notes, setNotes] = useState(fill.notes ?? "");
  const [items, setItems] = useState<FuelFormItem[]>(fill.items.map(toFormItem));

  // Auto-dismiss toast after 4 seconds
  useEffect(() => {
    if (toastError) {
      const timer = setTimeout(() => setToastError(""), 4000);
      return () => clearTimeout(timer);
    }
  }, [toastError]);

  const handleCancel = () => {
    router.replace(`/garage/${vehicleId}`);
  };

  const handleSave = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setToastError("");

    const optional = (value: string) => (value === "" ? null : Number(value));
    const payload = {
      odometer_km: Number(odometer),
      filled_at: new Date(filledAt).toISOString(),
      station_name: station || null,
      notes: notes || null,
      items: items.map((item) => ({
        fuel_type: item.fuelType,
        fill_type: item.fillType,
        quantity: optional(item.quantity),
        unit_price: optional(item.unitPrice),
        total_cost: optional(item.totalCost),
      })),
    };

    startTransition(async () => {
      const res = await saveFuelFill(vehicleId, fill.id, payload);
      if (!res.ok) {
        setToastError(res.error ?? "We couldn't save this fuel entry.");
        return;
      }
      router.replace(`/garage/${vehicleId}`);
    });
  };

  return (
    <>
      <Dialog labelledBy="fuel-title" size="lg">
          <h2
            className="text-2xl font-bold tracking-tight text-foreground"
            id="fuel-title"
          >
            Edit fuel fill-up
          </h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Enter any two fuel-cost values; the third is calculated
            automatically when saved.
          </p>
          <FuelForm
            error=""
            filledAt={filledAt}
            isSaving={isPending}
            items={items}
            notes={notes}
            odometer={odometer}
            onCancel={handleCancel}
            onFilledAtChange={setFilledAt}
            onItemsChange={setItems}
            onNotesChange={setNotes}
            onOdometerChange={setOdometer}
            onStationNameChange={setStation}
            onSubmit={handleSave}
            savingLabel="Saving…"
            stationName={station}
            submitLabel="Save changes"
          />
      </Dialog>

      {/* Error Toast Notification */}
      {toastError && (
        <div
          className="fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-xl border border-destructive/20 bg-destructive/10 p-4 text-destructive shadow-xl backdrop-blur-md transition-all duration-300"
          role="alert"
        >
          <span className="text-sm font-medium">{toastError}</span>
          <Button variant="iconDanger" size="sm"
            onClick={() => setToastError("")}
            type="button"
            aria-label="Close"
          >
            &times;
          </Button>
        </div>
      )}
    </>
  );
}
