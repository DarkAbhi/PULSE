import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { AirFill, FuelFill, MaintenanceRecord, VehicleHistoryData } from "./types";
import DeleteButton from "./delete-button";
import DeleteVehicleButton from "./delete-vehicle-button";
import EditFuelModal from "./edit-fuel-modal";
import MaintenanceRecordModal from "./maintenance-record-modal";
import MaintenanceRecordActions from "./maintenance-record-actions";
import TirePressureCard from "./tire-pressure-card";
import LocalDate from "../../components/local-date";
import { ArrowLeft, Fuel, Gauge, Plus, ReceiptText, Wind } from "lucide-react";

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

interface PageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ edit?: string; maintenance?: string; "edit-maintenance"?: string }>;
}

export default async function VehiclePage({ params, searchParams }: PageProps) {
  const { id } = await params;
  const { edit, maintenance, "edit-maintenance": editMaintenance } = await searchParams;
  
  // Forward cookies from incoming request to backend for auth/session validation
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on the server
  const sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!sessionResponse.ok) {
    redirect("/");
  }

  // Load history on the server
  const response = await fetch(`${apiBaseURL}/api/vehicles/${id}/history`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  
  if (response.status === 404) {
    redirect("/garage");
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

  const mileageEntries = Object.entries(data.average_mileage_km_per_litre);

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
            className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit"
            href="/garage"
          >
            <ArrowLeft className="h-4 w-4" /> Garage
          </Link>
          <DeleteVehicleButton vehicleId={id} vehicleName={data.vehicle_name} />
        </div>
        <p className="mt-5 text-sm font-semibold tracking-[0.18em] text-primary uppercase">
          VEHICLE HISTORY
        </p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
          {data.vehicle_name}
        </h1>
        <div className="mt-8 grid gap-8 lg:grid-cols-2">
          <section>
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <Fuel className="h-5 w-5 text-primary" /> Fuel fill-ups
            </h2>
            <aside className="mt-4 rounded-2xl border border-primary/20 bg-primary/5 p-5">
              <h3 className="flex items-center gap-2 font-semibold text-foreground">
                <Gauge className="h-5 w-5 text-primary" /> Average mileage
              </h3>
              {mileageEntries.length === 0 ? (
                <p className="mt-2 text-sm text-muted-foreground">
                  Add two full-tank fuel entries to calculate mileage.
                </p>
              ) : (
                <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2">
                  {mileageEntries.map(([fuelType, mileage]) => (
                    <p className="text-sm text-muted-foreground" key={fuelType}>
                      <span className="capitalize">{fuelType}</span>{" "}
                      <span className="font-semibold text-foreground">
                        {mileage.toFixed(1)} km/L
                      </span>
                    </p>
                  ))}
                </div>
              )}
            </aside>
            <div className="mt-4 space-y-3">
              {data.fuel_fillups.length === 0 ? (
                <p className="text-sm text-muted-foreground">No fuel entries yet.</p>
              ) : (
                data.fuel_fillups.map((fill) => (
                  <article
                    className="rounded-2xl bg-card border border-border p-5 shadow-sm"
                    key={fill.id}
                  >
                    <div className="flex justify-between gap-3">
                      <p className="font-semibold">
                        {fill.odometer_km} km ·{" "}
                        <LocalDate dateString={fill.filled_at} />
                      </p>
                      <span className="flex items-center gap-3">
                        <Link
                          className="text-sm font-semibold text-primary hover:opacity-85"
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
                        · ₹{item.total_cost}
                      </p>
                    ))}
                  </article>
                ))
              )}
            </div>
          </section>
          <section>
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <Wind className="h-5 w-5 text-primary" /> Air fills
            </h2>
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
                  <article
                    className="flex items-center justify-between gap-3 rounded-2xl bg-card border border-border p-5 shadow-sm"
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
                  </article>
                ))
              )}
            </div>
          </section>
        </div>
        <section className="mt-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <ReceiptText className="h-5 w-5 text-primary" /> Maintenance & expenses
            </h2>
            <Link className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90" href={`/garage/${id}?maintenance=new`}>
              <Plus className="h-4 w-4" /> Add record
            </Link>
          </div>
          <div className="mt-4 space-y-3">
            {data.maintenance_records.length === 0 ? (
              <p className="rounded-2xl border border-dashed border-border p-5 text-sm text-muted-foreground">No maintenance or expense records yet.</p>
            ) : data.maintenance_records.map((record) => (
              <article className="rounded-2xl border border-border bg-card p-5 shadow-sm" key={record.id}>
                <div className="flex flex-wrap justify-between gap-3">
                  <div>
                    <p className="font-semibold">{record.title}</p>
                    <p className="mt-1 text-sm text-muted-foreground"><span className="capitalize">{record.category}</span> · <LocalDate dateString={record.occurred_at} />{record.odometer_km != null ? ` · ${record.odometer_km} km` : ""}</p>
                  </div>
                  <div className="flex items-start gap-4"><p className="font-semibold">₹{record.amount.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p><MaintenanceRecordActions vehicleId={id} recordId={record.id} /></div>
                </div>
                {record.provider_name && <p className="mt-2 text-sm text-muted-foreground">{record.provider_name}</p>}
                {record.notes && <p className="mt-2 text-sm text-muted-foreground">{record.notes}</p>}
              </article>
            ))}
          </div>
        </section>
      </div>
      {editingFill && <EditFuelModal vehicleId={id} fill={editingFill} />}
      {maintenance === "new" && <MaintenanceRecordModal vehicleId={id} />}
      {editingMaintenanceRecord && <MaintenanceRecordModal vehicleId={id} record={editingMaintenanceRecord} />}
    </main>
  );
}
