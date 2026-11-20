import { apiFetch } from "@/app/lib/server-api";
import PulseCard, { PulseCardLink } from "@/app/components/design-system/pulse-card";
import { getUserCurrency } from "@/app/lib/user-currency";
import { formatMoney } from "@/app/lib/currency.mjs";
import { redirect } from "next/navigation";
import Link from "next/link";
import BackLink from "@/app/components/design-system/back-link";
import { VehicleHistoryData } from "./types";
import DeleteButton from "./delete-button";
import DeleteVehicleButton from "./delete-vehicle-button";
import EditFuelModal from "./edit-fuel-modal";
import ComingUp from "./coming-up";
import MaintenanceHistory from "./maintenance-history";
import MaintenanceRecordModal from "./maintenance-record-modal";
import FuelEfficiencySection from "./fuel-efficiency-section";
import TirePressureCard from "./tire-pressure-card";
import LocalDate from "@/app/components/local-date";
import VehicleEditor from "../vehicle-editor";
import { Bike, Bus, CarFront, ChevronRight, Fuel, Plus, ReceiptText, Truck, Wind } from "lucide-react";

export const metadata = { title: "Vehicle details | PULSE" };

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

const vehicleTabs = [
  { id: "overview", label: "Overview" },
  { id: "fuel", label: "Fuel" },
  { id: "tires", label: "Tires & air" },
  { id: "maintenance", label: "Maintenance" },
] as const;

interface PageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ tab?: string; edit?: string; maintenance?: string; "edit-maintenance"?: string }>;
}

export default async function VehiclePage({ params, searchParams }: PageProps) {
  const currency = await getUserCurrency();
  const { id } = await params;
  const { tab, edit, maintenance, "edit-maintenance": editMaintenance } = await searchParams;

  // Validate session on the server
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) {
    redirect("/");
  }

  // Load history on the server
  const response = await apiFetch(`${apiBaseURL}/api/vehicles/${id}/history`);

  if (response.status === 404) {
    return (
      <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-8 sm:py-10">
        <div className="mx-auto max-w-5xl">
          <PulseCard className="p-8 text-center">
            <h2 className="text-xl font-semibold text-foreground">
              Vehicle not found
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              This vehicle may have been deleted or does not exist.
            </p>
            <BackLink href="/garage" className="mt-6">Back to Garage</BackLink>
          </PulseCard>
        </div>
      </main>
    );
  }

  if (!response.ok) {
    return (
      <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-8 sm:py-10">
        <div className="mx-auto max-w-5xl">
          <PulseCard className="p-6">
            <p role="alert" className="text-destructive">We couldn&apos;t load this vehicle&apos;s history.</p>
            <BackLink href="/garage" className="mt-4">Back to Garage</BackLink>
          </PulseCard>
        </div>
      </main>
    );
  }

  const data = (await response.json()) as VehicleHistoryData;

  const editingFill = edit
    ? data.fuel_fillups.find((fill) => fill.id === Number(edit))
    : null;
  const editingMaintenanceRecord = editMaintenance
    ? data.maintenance_records.find((record) => record.id === Number(editMaintenance))
    : null;

  const activeTab = editingFill ? "fuel"
    : maintenance === "new" || editingMaintenanceRecord ? "maintenance"
    : vehicleTabs.find((item) => item.id === tab)?.id ?? "overview";

  const VehicleIcon = data.kind === "motorcycle" || data.kind === "moped" ? Bike
    : data.kind === "bus" ? Bus
    : data.kind === "truck" || data.kind === "van" ? Truck
    : CarFront;
  const specs = [
    { label: "Type", value: data.kind === "unknown" ? "" : data.kind },
    { label: "Make", value: data.make_name },
    { label: "Model", value: data.model_name },
    { label: "Fuel", value: data.fuel_types.map((fuel) => fuel === "lpg" || fuel === "cng" ? fuel.toUpperCase() : fuel).join(" + ") },
  ];

  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-8 sm:py-10">
      <div className="mx-auto max-w-5xl">
        <BackLink href="/garage">Garage</BackLink>

        <PulseCard as="section" aria-labelledby="vehicle-title" className="mt-3 bg-secondary px-5 py-6 sm:px-8 sm:py-8">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex min-w-0 items-center gap-4">
              <span className="grid size-14 shrink-0 place-items-center rounded-xl bg-accent text-accent-foreground sm:size-20">
                <VehicleIcon className="size-8 sm:size-11" strokeWidth={1.4} aria-hidden="true" />
              </span>
              <h1 id="vehicle-title" className="min-w-0 break-words text-3xl font-bold tracking-tight sm:text-5xl">{data.vehicle_name}</h1>
            </div>
            <VehicleEditor key={`${data.catalog_id}/${data.vehicle_name}/${data.kind}/${data.make_name}/${data.model_name}/${data.fuel_types.join()}`} vehicle={{ id, name: data.vehicle_name, kind: data.kind, catalog_id: data.catalog_id, make_name: data.make_name, model_name: data.model_name, fuel_types: data.fuel_types }} />
          </div>
          <dl className="mt-6 grid grid-cols-2 gap-4 border-t border-divider pt-5 sm:grid-cols-4">
            {specs.map(({ label, value }) => (
              <div key={label} className="min-w-0">
                <dt className="text-xs text-muted-foreground">{label}</dt>
                <dd className={`mt-1 break-words text-sm font-semibold ${label === "Type" || label === "Fuel" ? "capitalize" : ""}`}>{value || "Not selected"}</dd>
              </div>
            ))}
          </dl>
          {data.kind === "unknown" && (
            <p className="mt-4 text-sm text-muted-foreground">Use Edit vehicle to choose a vehicle type or link a catalog model.</p>
          )}
        </PulseCard>

        <nav aria-label="Vehicle sections" className="mt-4 grid grid-cols-2 gap-1 rounded-xl bg-secondary p-1 sm:grid-cols-4">
          {vehicleTabs.map(({ id: tabId, label }) => (
            <Link
              key={tabId}
              href={tabId === "overview" ? `/garage/${id}` : `/garage/${id}?tab=${tabId}`}
              scroll={false}
              aria-current={activeTab === tabId ? "page" : undefined}
              className="flex min-h-11 items-center justify-center rounded-lg px-2 py-2 text-center text-sm font-semibold text-muted-foreground hover:bg-card/60 focus-visible:outline-2 focus-visible:outline-ring aria-[current=page]:bg-card aria-[current=page]:text-action aria-[current=page]:shadow-xs"
            >
              {label}
            </Link>
          ))}
        </nav>

        {activeTab === "overview" && (
          <div className="mt-6">
            {data.coming_up?.length ? (
              <ComingUp items={data.coming_up} vehicleId={id} />
            ) : (
              <PulseCard as="section" className="p-5 sm:p-6" aria-labelledby="coming-up-heading">
                <h2 id="coming-up-heading" className="text-xl font-semibold">Coming up</h2>
                <p className="mt-2 text-sm text-muted-foreground">No upcoming reminders for this vehicle.</p>
                <Link href={`/garage/${id}/reminders`} className="mt-2 inline-flex min-h-11 items-center text-sm font-semibold text-action underline">
                  Reminder settings
                </Link>
              </PulseCard>
            )}
          </div>
        )}

        {activeTab === "fuel" && (
          <section id="fuel" aria-labelledby="fuel-heading" className="mt-6 min-w-0">
            <h2 id="fuel-heading" className="flex items-center gap-2 text-xl font-semibold">
              <Fuel className="size-5 text-action" aria-hidden="true" /> Fuel
            </h2>
            <FuelEfficiencySection stats={data.fuel_efficiency} />
            <h3 className="mt-5 font-semibold">Fuel fill-ups</h3>
            <PulseCard className="mt-2 divide-y divide-divider px-4 sm:px-5">
              {data.fuel_fillups.length === 0 ? (
                <p className="py-4 text-sm text-muted-foreground">No fuel entries yet.</p>
              ) : (
                data.fuel_fillups.map((fill) => (
                  <article className="py-3" key={fill.id}>
                    <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
                      <div className="flex min-w-0 items-center gap-2.5">
                        <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-accent text-accent-foreground">
                          <Fuel className="size-4" aria-hidden="true" />
                        </span>
                        <div className="min-w-0">
                          <p className="break-words text-sm font-semibold">
                            {fill.station_name || <LocalDate dateString={fill.filled_at} />}
                          </p>
                          <p className="text-xs tabular-nums text-muted-foreground">
                            {fill.odometer_km.toLocaleString("en-IN")} km
                            {fill.station_name && <> · <LocalDate dateString={fill.filled_at} /></>}
                          </p>
                        </div>
                      </div>
                      <span className="flex items-center gap-3">
                        <Link
                          className="rounded px-1 py-2 text-sm font-semibold text-action hover:underline focus-visible:outline-2 focus-visible:outline-ring"
                          href={`/garage/${id}?edit=${fill.id}`}
                        >
                          Edit
                        </Link>
                        <DeleteButton
                          vehicleId={id}
                          recordId={fill.id}
                          kind="fuel-fillups"
                        />
                      </span>
                    </div>
                    <ul aria-label="Fuel items" className="mt-2 space-y-1 pl-[2.625rem]">
                      {fill.items.map((item, index) => (
                        <li className="flex flex-wrap items-baseline gap-x-2.5 gap-y-0.5 text-sm" key={index}>
                          <span className="font-medium capitalize">{item.fuel_type}</span>
                          <span className="tabular-nums text-muted-foreground">{item.quantity} L</span>
                          <span className="border-l border-divider pl-2.5 font-medium tabular-nums text-action">
                            {formatMoney(item.total_cost, currency)}
                          </span>
                          <span className="text-xs capitalize text-muted-foreground">{item.fill_type}</span>
                        </li>
                      ))}
                    </ul>
                  </article>
                ))
              )}
            </PulseCard>
          </section>
        )}

        {activeTab === "tires" && (
          <section id="tires-air" aria-labelledby="tires-heading" className="mt-6 min-w-0">
            <h2 id="tires-heading" className="flex items-center gap-2 text-xl font-semibold">
              <Wind className="size-5 text-action" aria-hidden="true" /> Tires & air
            </h2>
            <div className="mt-4 grid items-start gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)] lg:grid-rows-[auto_1fr]">
              <TirePressureCard
                frontTirePressurePillion={data.front_tire_pressure_pillion}
                frontTirePressureSolo={data.front_tire_pressure_solo}
                rearTirePressurePillion={data.rear_tire_pressure_pillion}
                rearTirePressureSolo={data.rear_tire_pressure_solo}
                vehicleId={id}
              />
              <PulseCard as="section" aria-labelledby="air-fill-history-heading" className="min-w-0 p-4 sm:p-5 lg:col-start-2 lg:row-start-2">
                <h3 id="air-fill-history-heading" className="font-semibold">Air fill history</h3>
                {data.air_fills.length === 0 ? (
                  <p className="mt-2 text-sm text-muted-foreground">Record an air fill to start your history.</p>
                ) : (
                  <ul className="mt-3 divide-y divide-divider border-t border-divider">
                    {data.air_fills.map((fill) => (
                      <li className="flex items-center justify-between gap-3 py-2.5 text-sm" key={fill.id}>
                        <div className="min-w-0">
                          <div className="font-medium"><LocalDate dateString={fill.filled_at} /></div>
                          {fill.odometer_km != null && (
                            <p className="mt-0.5 text-xs tabular-nums text-muted-foreground">
                              {fill.odometer_km.toLocaleString("en-IN")} km
                            </p>
                          )}
                        </div>
                        <DeleteButton vehicleId={id} recordId={fill.id} kind="air-fills" />
                      </li>
                    ))}
                  </ul>
                )}
              </PulseCard>
            </div>
          </section>
        )}

        {activeTab === "maintenance" && (
          <section id="maintenance" aria-labelledby="maintenance-heading" className="mt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 id="maintenance-heading" className="flex items-center gap-2 text-xl font-semibold">
                <ReceiptText className="size-5 text-action" aria-hidden="true" /> Maintenance & expenses
              </h2>
              <Link className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary-pressed focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring" href={`/garage/${id}?maintenance=new`}>
                <Plus className="size-4" aria-hidden="true" /> Add record
              </Link>
            </div>
            <PulseCardLink href={`/garage/${id}/reminders`} className="mt-4 flex items-center justify-between gap-4 p-4 sm:px-5">
              <span>
                <span className="block font-semibold">Reminder settings</span>
                <span className="mt-1 block text-sm text-muted-foreground">Set intervals for engine oil, tire pressure and general service.</span>
              </span>
              <ChevronRight className="size-4 shrink-0 text-action" aria-hidden="true" />
            </PulseCardLink>
            <div className="mt-4 space-y-3">
              <MaintenanceHistory records={data.maintenance_records} vehicleId={id} currency={currency} />
            </div>
          </section>
        )}

        <footer className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-divider py-5">
          <div className="flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted-foreground">
            <span>Vehicle data by <a className="text-action underline" href="https://vehiclesdb.com">VehiclesDB</a></span>
            <Link className="text-action underline" href="/vehicle-data">Sources and credits</Link>
          </div>
          <DeleteVehicleButton vehicleId={id} vehicleName={data.vehicle_name} />
        </footer>
      </div>
      {editingFill && <EditFuelModal vehicleId={id} fill={editingFill} />}
      {maintenance === "new" && <MaintenanceRecordModal vehicleId={id} />}
      {editingMaintenanceRecord && <MaintenanceRecordModal vehicleId={id} record={editingMaintenanceRecord} />}
    </main>
  );
}
