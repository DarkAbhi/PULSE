"use client";

import Button from "@/app/components/design-system/button";

import { useEffect, useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { addVehicleAction } from "./actions";
import Dialog, { DialogAction, DialogActions } from "@/app/components/design-system/dialog";

const fuelOptions = [
  { value: "petrol", label: "Petrol" },
  { value: "diesel", label: "Diesel" },
  { value: "lpg", label: "LPG" },
  { value: "cng", label: "CNG" },
  { value: "electric", label: "Electric" },
];

export default function AddVehicleButton() {
  const [isReady, setIsReady] = useState(false);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [vehicleType, setVehicleType] = useState("");
  const [fuelType, setFuelType] = useState("");
  const [dualTank, setDualTank] = useState(false);
  const [secondFuelType, setSecondFuelType] = useState("");
  const [vehicleName, setVehicleName] = useState("");
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  // Server-rendered controls become interactive once their handlers are attached.
  useEffect(() => setIsReady(true), []);

  function handleAdd(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    startTransition(async () => {
      const res = await addVehicleAction({
        name: vehicleName.trim(),
        vehicle_type: vehicleType,
        fuel_types: dualTank ? [fuelType, secondFuelType] : [fuelType],
      });
      if (!res.ok) {
        setError(res.error ?? "We couldn't add that vehicle.");
      } else {
        setVehicleName("");
        setVehicleType("");
        setFuelType("");
        setDualTank(false);
        setSecondFuelType("");
        setIsAddOpen(false);
      }
    });
  }

  return (
    <>
      <Button variant="primary" size="lg"
        id="add-vehicle-button"
        disabled={!isReady}
        aria-haspopup="dialog"
        aria-expanded={isAddOpen}
        className="shrink-0"
        icon={<Plus className="h-4 w-4" />}
        onClick={() => {
          setError("");
          setIsAddOpen(true);
        }}
        type="button"
      >
        Add vehicle
      </Button>

      {isAddOpen && (
        <Dialog labelledBy="add-vehicle-title">
            <h2
              className="text-2xl font-bold tracking-tight text-foreground"
              id="add-vehicle-title"
            >
              Add a vehicle
            </h2>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Add its name, vehicle type, and fuel details.
            </p>
            <form className="mt-6 space-y-4" onSubmit={handleAdd}>
              <label
                className="block text-sm font-medium text-muted-foreground"
                htmlFor="vehicle-name"
              >
                Vehicle name
              </label>
              <input
                autoFocus
                className="w-full rounded-lg border border-border bg-background text-foreground px-4 py-3 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/15"
                id="vehicle-name"
                maxLength={48}
                onChange={(event) => setVehicleName(event.target.value)}
                placeholder="e.g. Honda City"
                required
                value={vehicleName}
              />
              <label className="block text-sm font-medium text-muted-foreground" htmlFor="vehicle-type">
                Vehicle type
              </label>
              <select id="vehicle-type" required value={vehicleType} onChange={(event) => setVehicleType(event.target.value)}
                className="w-full rounded-lg border border-border bg-background px-4 py-3 text-foreground focus:ring-4 focus:ring-primary/15">
                <option value="" disabled>Select vehicle type</option>
                <option value="two_wheeler">Two-wheeler</option>
                <option value="four_wheeler">Four-wheeler</option>
              </select>
              <label className="block text-sm font-medium text-muted-foreground" htmlFor="vehicle-fuel-type">
                Fuel type
              </label>
              <select id="vehicle-fuel-type" required value={fuelType} onChange={(event) => {
                setFuelType(event.target.value);
                if (event.target.value === secondFuelType) setSecondFuelType("");
              }} className="w-full rounded-lg border border-border bg-background px-4 py-3 text-foreground focus:ring-4 focus:ring-primary/15">
                <option value="" disabled>Select fuel type</option>
                {fuelOptions.map((fuel) => (
                  <option key={fuel.value} value={fuel.value}>{fuel.label}</option>
                ))}
              </select>
              <label className="flex items-center gap-3 text-sm font-medium text-foreground">
                <input type="checkbox" checked={dualTank} onChange={(event) => setDualTank(event.target.checked)} />
                Dual tank (two fuel types)
              </label>
              {dualTank && (
                <>
                  <label className="block text-sm font-medium text-muted-foreground" htmlFor="vehicle-second-fuel-type">
                    Second fuel type
                  </label>
                  <select id="vehicle-second-fuel-type" required value={secondFuelType} onChange={(event) => setSecondFuelType(event.target.value)}
                    className="w-full rounded-lg border border-border bg-background px-4 py-3 text-foreground focus:ring-4 focus:ring-primary/15">
                    <option value="" disabled>Select second fuel type</option>
                    {fuelOptions.filter((fuel) => fuel.value !== fuelType).map((fuel) => (
                      <option key={fuel.value} value={fuel.value}>{fuel.label}</option>
                    ))}
                  </select>
                </>
              )}
              {error && (
                <p className="text-sm text-destructive" role="alert">
                  {error}
                </p>
              )}
              <DialogActions>
                <DialogAction
                  variant="secondary"
                  disabled={isPending}
                  onClick={() => setIsAddOpen(false)}
                  type="button"
                >
                  Cancel
                </DialogAction>
                <DialogAction
                  id="add-vehicle-submit"
                  disabled={isPending}
                  type="submit"
                >
                  {isPending ? "Adding…" : "Add vehicle"}
                </DialogAction>
              </DialogActions>
            </form>
        </Dialog>
      )}
    </>
  );
}
