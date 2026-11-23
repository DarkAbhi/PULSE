import Link from "next/link";
import PulseCard from "@/app/components/design-system/pulse-card";
import { VehicleActionProvider, VehicleQuickActions } from "@/app/garage/vehicle-actions";
import { formatVehicleLabel, getVehicleIcon, vehicleCardClassName } from "@/app/garage/vehicle-presentation";
import FuelTypeDot from "@/app/garage/fuel-type-dot";
import type { Vehicle } from "@/app/garage/types";

const vehicleBadge = "inline-flex items-center gap-1.5 rounded-full bg-chip px-3 py-1.5 text-sm leading-tight font-semibold";

export default function VehiclesList({ vehicles }: { vehicles: Vehicle[] }) {
  return (
    <VehicleActionProvider>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:gap-6">
        {vehicles.map((vehicle) => {
          const VehicleIcon = getVehicleIcon(vehicle.kind);
          const model = [vehicle.make_name, vehicle.model_name].filter(Boolean).join(" ");

          return (
            <PulseCard
              as="article"
              key={vehicle.id}
              className={`${vehicleCardClassName} flex flex-col gap-4 p-4 sm:gap-5 sm:p-5 xl:p-7`}
            >
              <Link
                href={`/garage/${vehicle.id}`}
                className="flex min-w-0 items-center gap-3 rounded-2xl sm:gap-4"
                aria-label={`View ${vehicle.name} details`}
              >
                <span aria-hidden="true" data-kind={vehicle.kind} className="grid size-14 shrink-0 place-items-center rounded-garage-vehicle-icon bg-garage-car text-white data-[kind=motorcycle]:bg-garage-motorcycle data-[kind=moped]:bg-garage-scooter data-[kind=scooter]:bg-garage-scooter sm:size-16 xl:size-20">
                  <VehicleIcon className="size-7 xl:size-9" strokeWidth={2} />
                </span>
                <div className="min-w-0 flex-1">
                  <h3 className="text-xl leading-tight font-extrabold tracking-tight wrap-anywhere sm:text-2xl xl:text-3xl">{vehicle.name}</h3>
                  {model && (
                    <p className="mt-0.5 text-sm wrap-anywhere text-muted-foreground sm:text-base">{model}</p>
                  )}
                </div>
              </Link>

              <div className="flex flex-wrap gap-2">
                <span className={vehicleBadge}>
                  <VehicleIcon className="size-4 shrink-0" aria-hidden="true" />
                  {vehicle.kind === "unknown" ? "Kind not selected" : formatVehicleLabel(vehicle.kind)}
                </span>
                {vehicle.fuel_types.map((fuel) => (
                  <span key={fuel} className={vehicleBadge}>
                    <FuelTypeDot fuel={fuel} />
                    {formatVehicleLabel(fuel)}
                  </span>
                ))}
              </div>

              <VehicleQuickActions vehicle={{ id: vehicle.id, name: vehicle.name }} />
            </PulseCard>
          );
        })}
      </div>
    </VehicleActionProvider>
  );
}
