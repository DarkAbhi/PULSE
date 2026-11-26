import { Bus, CarFront, Motorbike, Scooter, Truck, Van } from "lucide-react";

const vehicleIcons: Record<string, typeof CarFront> = {
  car: CarFront,
  motorcycle: Motorbike,
  moped: Scooter,
  scooter: Scooter,
  van: Van,
  truck: Truck,
  bus: Bus,
};

export function getVehicleIcon(kind: string) {
  return vehicleIcons[kind] ?? CarFront;
}

export function formatVehicleLabel(value: string) {
  return value === "cng" || value === "lpg"
    ? value.toUpperCase()
    : value.charAt(0).toUpperCase() + value.slice(1);
}

