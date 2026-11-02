import { apiFetch } from "@/app/lib/server-api";
import PulseCard from "@/app/components/design-system/pulse-card";
import { getUserCurrency } from "@/app/lib/user-currency";
import { formatMoney } from "@/app/lib/currency.mjs";
import { redirect } from "next/navigation";
import Link from "next/link";
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
import { ArrowLeft, Fuel, Plus, ReceiptText, Wind } from "lucide-react";

export const metadata = { title: "Vehicle details | PULSE" };

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

interface PageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ edit?: string; maintenance?: string; "edit-maintenance"?: string }>;
}

export default async function VehiclePage({ params, searchParams }: PageProps) {
  const currency = await getUserCurrency();
  const { id } = await params;
  const { edit, maintenance, "edit-maintenance": editMaintenance } = await searchParams;

  // Validate session on the server
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) {
    redirect("/");
  }

  // Load history on the server
  const response = await apiFetch(`${apiBaseURL}/api/vehicles/${id}/history`);

  if (response.status === 404) {
    return (
      <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
        <div className="mx-auto max-w-6xl">
          <PulseCard className="p-8 text-center">
            <h2 className="text-xl font-semibold text-foreground">
              Vehicle not found
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              This vehicle may have been deleted or does not exist.
            </p>
            <Link
              className="mt-6 inline-flex items-center gap-1.5 text-sm font-semibold text-action transition hover:opacity-80"
              href="/garage"
            >
              <ArrowLeft className="h-4 w-4" /> Back to Garage
            </Link>
          </PulseCard>
        </div>
      </main>
    );
  }

  if (!response.ok) {
    return (
      <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
        <div className="mx-auto max-w-6xl">
          <p className="rounded-xl bg-destructive/10 p-4 text-destructive">
            We couldn't load this vehicle's history.
          </p>
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

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        <div className="flex items-start justify-between gap-4">
          <Link
            className="flex items-center gap-1 text-sm font-semibold text-action transition hover:opacity-80 w-fit"
            href="/garage"
          >
            <ArrowLeft className="h-4 w-4" /> Garage
          </Link>
          <DeleteVehicleButton vehicleId={id} vehicleName={data.vehicle_name} />
        </div>
        <p className="mt-5 text-sm font-semibold tracking-[0.18em] text-action uppercase">
          VEHICLE HISTORY
        </p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
          {data.vehicle_name}
        </h1>
        <PulseCard as="section" className="mt-6 p-6" aria-label="Vehicle details">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold">Vehicle details</h2>
              <dl className="mt-3 grid grid-cols-2 gap-x-8 gap-y-2 text-sm">
                <dt className="text-muted-foreground">Kind</dt><dd className="capitalize">{data.kind === "unknown" ? "Not selected" : data.kind}</dd>
                <dt className="text-muted-foreground">Make</dt><dd>{data.make_name || "Not selected"}</dd>
                <dt className="text-muted-foreground">Model</dt><dd>{data.model_name || "Not selected"}</dd>
                <dt className="text-muted-foreground">Fuel</dt><dd className="capitalize">{data.fuel_types.join(" + ")}</dd>
              </dl>
            </div>
            <VehicleEditor key={`${data.catalog_id}/${data.vehicle_name}/${data.kind}/${data.make_name}/${data.model_name}/${data.fuel_types.join()}`} vehicle={{ id, name: data.vehicle_name, kind: data.kind, catalog_id: data.catalog_id, make_name: data.make_name, model_name: data.model_name, fuel_types: data.fuel_types }} />
          </div>
          {data.kind === "unknown" && <p className="mt-4 text-sm text-muted-foreground">Select a kind or link this vehicle to a catalog model using Edit vehicle.</p>}
          <p className="mt-4 text-xs text-muted-foreground">Vehicle data by <a className="text-action underline" href="https://vehiclesdb.com">VehiclesDB</a> · <Link className="text-action underline" href="/vehicle-data">Sources and credits</Link></p>
        </PulseCard>
        <div className="mt-8 grid gap-8 lg:grid-cols-2">
          <section>
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <Fuel className="h-5 w-5 text-action" /> Fuel fill-ups
            </h2>
            <div className="mt-4 space-y-3">
              {data.fuel_fillups.length === 0 ? (
                <p className="text-sm text-muted-foreground">No fuel entries yet.</p>
              ) : (
                data.fuel_fillups.map((fill) => (
                  <PulseCard as="article"
                    className="p-5"
                    key={fill.id}
                  >
                    <div className="flex justify-between gap-3">
                      <p className="font-semibold">
                        {fill.odometer_km} km ·{" "}
                        <LocalDate dateString={fill.filled_at} />
                      </p>
                      <span className="flex items-center gap-3">
                        <Link
                          className="text-sm font-semibold text-action hover:opacity-85"
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
                    {fill.station_name && (
                      <p className="mt-1 text-sm text-muted-foreground">
                        {fill.station_name}
                      </p>
                    )}
                    {fill.items.map((item, index) => (
                      <p className="mt-2 text-sm text-muted-foreground" key={index}>
                        {item.fuel_type} · {item.fill_type} · {item.quantity} L
                        · {formatMoney(item.total_cost, currency)}
                      </p>
                    ))}
                  </PulseCard>
                ))
              )}
            </div>
          </section>
          <section>
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <Wind className="h-5 w-5 text-action" /> Air fills
            </h2>
            <div id="tires-air" />
            <TirePressureCard
              frontTirePressurePillion={data.front_tire_pressure_pillion}
              frontTirePressureSolo={data.front_tire_pressure_solo}
              rearTirePressurePillion={data.rear_tire_pressure_pillion}
              rearTirePressureSolo={data.rear_tire_pressure_solo}
              vehicleId={id}
            />
            <div className="mt-4 space-y-3">
              {data.air_fills.length === 0 ? (
                <p className="text-sm text-muted-foreground">No air fill records yet.</p>
              ) : (
                data.air_fills.map((fill) => (
                  <PulseCard as="article"
                    className="flex items-center justify-between gap-3 p-5"
                    key={fill.id}
                  >
                    <span>
                      Air filled · <LocalDate dateString={fill.filled_at} />
                    </span>
                    <DeleteButton
                      vehicleId={id}
                      recordId={fill.id}
                      kind="air-fills"
                    />
                  </PulseCard>
                ))
              )}
            </div>
          </section>
        </div>
        <FuelEfficiencySection stats={data.fuel_efficiency} />
        <ComingUp items={data.coming_up ?? []} vehicleId={id} settings={(data.maintenance_schedules ?? []).filter((s) => s.kind !== "engine_oil" || data.fuel_types.some((f) => f !== "electric"))} />
        <section className="mt-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <ReceiptText className="h-5 w-5 text-action" /> Maintenance & expenses
            </h2>
            <Link className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90" href={`/garage/${id}?maintenance=new`}>
              <Plus className="h-4 w-4" /> Add record
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            <MaintenanceHistory records={data.maintenance_records} vehicleId={id} currency={currency} />
          </div>
        </section>
      </div>
      {editingFill && <EditFuelModal vehicleId={id} fill={editingFill} />}
      {maintenance === "new" && <MaintenanceRecordModal vehicleId={id} />}
      {editingMaintenanceRecord && <MaintenanceRecordModal vehicleId={id} record={editingMaintenanceRecord} />}
    </main>
  );
}
