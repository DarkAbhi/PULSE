import { apiFetch } from "@/app/lib/server-api";
import PulseCard from "@/app/components/design-system/pulse-card";
import { getUserCurrency } from "@/app/lib/user-currency";
import { formatMoney } from "@/app/lib/currency.mjs";
import { redirect } from "next/navigation";
import Link from "next/link";
import TopAppBar from "@/app/components/top-app-bar";
import type { AirFill, FuelFill, VehicleHistoryData } from "./types";
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
import { Bell, Bus, CarFront, Fuel, Motorbike, Pencil, Plus, Scooter, Truck, Van, Wrench } from "lucide-react";
import frame from "@/app/components/design-system/page-frame.module.css";
import garage from "../garage.module.css";
import styles from "./vehicle-detail.module.css";

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
      <main className={`${frame.page} bg-background text-foreground`}>
        <div className={frame.content}>
          <TopAppBar backHref="/garage" backLabel="Back to Garage" />
          <PulseCard className="mt-5 p-8 text-center">
            <h2 className="text-xl font-semibold text-foreground">
              Vehicle not found
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              This vehicle may have been deleted or does not exist.
            </p>
          </PulseCard>
        </div>
      </main>
    );
  }

  if (!response.ok) {
    return (
      <main className={`${frame.page} bg-background text-foreground`}>
        <div className={frame.content}>
          <TopAppBar backHref="/garage" backLabel="Back to Garage" />
          <PulseCard className="mt-5 p-6">
            <p role="alert" className="text-destructive">We couldn&apos;t load this vehicle&apos;s history.</p>
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

  const VehicleIcon = data.kind === "motorcycle" ? Motorbike
    : data.kind === "moped" || data.kind === "scooter" ? Scooter
    : data.kind === "bus" ? Bus
    : data.kind === "van" ? Van
    : data.kind === "truck" ? Truck
    : CarFront;
  const formatFuel = (fuel: string) => fuel === "lpg" || fuel === "cng" ? fuel.toUpperCase() : fuel;
  const specs = [
    { label: "Type", value: data.kind === "unknown" ? "" : data.kind, capitalize: true },
    { label: "Make", value: data.make_name },
    { label: "Model", value: data.model_name },
    { label: "Fuel type", value: data.fuel_types.map(formatFuel).join(" + "), capitalize: true },
  ];
  const detailHref = `/garage/${id}`;

  return (
    <main className={`${frame.page} ${styles.page}`}>
      <div className={`${frame.content} ${styles.content}`}>
        <TopAppBar backHref="/garage" backLabel="Back to Garage" />

        <section aria-labelledby="vehicle-title" className={styles.hero}>
          <div className={styles.heroDecoration} aria-hidden="true">
            <VehicleIcon className={styles.watermark} strokeWidth={1} />
          </div>
          <div className={styles.identity}>
            <span className={styles.glyph} aria-hidden="true"><VehicleIcon strokeWidth={1.8} /></span>
            <div className="min-w-0 flex-1">
              <h1 id="vehicle-title" className={styles.title}>{data.vehicle_name}</h1>
              <div className="mt-2.5 flex flex-wrap gap-2 capitalize">
                {data.kind !== "unknown" && <span className={styles.tag}>{data.kind}</span>}
                {data.fuel_types.map((fuel) => (
                  <span key={fuel} className={styles.tag}>
                    <span aria-hidden="true" data-fuel={fuel} className={garage.fuelDot} />
                    {formatFuel(fuel)}
                  </span>
                ))}
              </div>
            </div>
            <VehicleEditor
              key={JSON.stringify([data.catalog_id, data.vehicle_name, data.kind, data.make_name, data.model_name, data.fuel_types])}
              triggerClassName={`${styles.button} ${styles.heroButton}`}
              vehicle={{ id, name: data.vehicle_name, kind: data.kind, catalog_id: data.catalog_id, make_name: data.make_name, model_name: data.model_name, fuel_types: data.fuel_types }}
            />
          </div>
          <dl className={styles.specs}>
            {specs.map(({ label, value, capitalize }) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd className={capitalize && value ? "capitalize" : undefined}>{value || "Not selected"}</dd>
              </div>
            ))}
          </dl>
          {data.kind === "unknown" && (
            <p className="relative text-sm">Use Edit vehicle to choose a vehicle type or link a catalog model.</p>
          )}
        </section>

        <nav aria-label="Vehicle sections" className={styles.tabs}>
          {vehicleTabs.map(({ id: tabId, label }) => (
            <Link
              key={tabId}
              href={tabId === "overview" ? detailHref : `${detailHref}?tab=${tabId}`}
              scroll={false}
              aria-current={activeTab === tabId ? "page" : undefined}
            >
              {label}
            </Link>
          ))}
        </nav>

        {activeTab === "overview" && <ComingUp items={data.coming_up ?? []} vehicleId={id} />}

        {activeTab === "fuel" && (
          <section id="fuel" aria-labelledby="fuel-heading" className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 id="fuel-heading"><Fuel className="size-6" aria-hidden="true" /> Fuel</h2>
            </div>
            <FuelEfficiencySection stats={data.fuel_efficiency} currency={currency} />
            <FuelHistory fills={data.fuel_fillups} vehicleId={id} currency={currency} />
          </section>
        )}

        {activeTab === "tires" && (
          <TirePressureCard
            frontTirePressurePillion={data.front_tire_pressure_pillion}
            frontTirePressureSolo={data.front_tire_pressure_solo}
            rearTirePressurePillion={data.rear_tire_pressure_pillion}
            rearTirePressureSolo={data.rear_tire_pressure_solo}
            vehicleId={id}
          >
            <AirFillHistory fills={data.air_fills} vehicleId={id} />
          </TirePressureCard>
        )}

        {activeTab === "maintenance" && (
          <section id="maintenance" aria-labelledby="maintenance-heading" className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 id="maintenance-heading"><Wrench className="size-6" aria-hidden="true" /> Maintenance &amp; expenses</h2>
              <Link className={`${styles.button} ${styles.primary}`} href={`${detailHref}?maintenance=new`}>
                <Plus className="size-4" aria-hidden="true" /> Add record
              </Link>
            </div>
            <MaintenanceHistory records={data.maintenance_records} vehicleId={id} currency={currency} />
          </section>
        )}

        {(activeTab === "overview" || activeTab === "maintenance") && (
          <section aria-labelledby="reminders-heading" className={`${styles.card} flex flex-wrap items-center justify-between gap-4`}>
            <div>
              <h2 id="reminders-heading" className={`${styles.subheading} flex items-center gap-2`}><Bell className="size-5" aria-hidden="true" /> Reminders</h2>
              <p className={`${styles.muted} mt-1 text-sm`}>Set intervals for engine oil, tire pressure and general service.</p>
            </div>
            <Link className={`${styles.button} ${styles.secondary}`} href={`${detailHref}/reminders`}>Reminder settings</Link>
          </section>
        )}

        <section aria-labelledby="delete-vehicle-heading" className={`${styles.card} ${styles.dangerCard} flex flex-wrap items-center justify-between gap-4`}>
          <div>
            <h2 id="delete-vehicle-heading" className={`${styles.subheading} ${styles.dangerText}`}>Delete vehicle</h2>
            <p className={`${styles.muted} mt-1 text-sm`}>Permanently removes this vehicle and its fuel, air fill and maintenance history.</p>
          </div>
          <DeleteVehicleButton vehicleId={id} vehicleName={data.vehicle_name} />
        </section>

        <footer className={`${styles.muted} flex flex-wrap gap-x-4 gap-y-2 text-xs`}>
          <span>Vehicle data by <a className="underline" href="https://vehiclesdb.com">VehiclesDB</a></span>
          <Link className="underline" href="/vehicle-data">Sources and credits</Link>
        </footer>
      </div>
      {editingFill && <EditFuelModal vehicleId={id} fill={editingFill} />}
      {maintenance === "new" && <MaintenanceRecordModal vehicleId={id} />}
      {editingMaintenanceRecord && <MaintenanceRecordModal vehicleId={id} record={editingMaintenanceRecord} />}
    </main>
  );
}

function FuelHistory({ fills, vehicleId, currency }: { fills: FuelFill[]; vehicleId: string; currency: string }) {
  return (
    <section aria-labelledby="fuel-history-heading">
      <h3 id="fuel-history-heading" className={`${styles.subheading} mb-3`}>Fuel fill-ups</h3>
      {fills.length === 0 ? <p className={styles.empty}>No fuel entries yet.</p> : (
        <div className={`${styles.recordList} ${styles.fuelHistory}`}>
          <div className={styles.rowHeader} aria-hidden="true">
            <span>Fill-up</span><span>Odometer</span>
            <div className={styles.fuelItems}><span>Fuel</span><span>Quantity</span><span>Amount</span></div>
            <span className={styles.rowActions} />
          </div>
          <ul aria-labelledby="fuel-history-heading">
            {fills.map((fill) => (
              <li key={fill.id} className={styles.row}>
                <div className={styles.rowMain}>
                  <p className="font-bold">{fill.station_name || <LocalDate dateString={fill.filled_at} />}</p>
                  {fill.station_name && <p className={`${styles.muted} text-sm`}><LocalDate dateString={fill.filled_at} /></p>}
                </div>
                <div className="tabular-nums"><CellLabel>Odometer</CellLabel>{fill.odometer_km.toLocaleString("en-IN")} km</div>
                <div className={`${styles.fuelGroup} flex flex-col gap-3`} role="group" aria-label="Fuel items">
                  {fill.items.map((item, index) => (
                    <div className={styles.fuelItems} key={index}>
                      <div><p className="font-semibold capitalize">{item.fuel_type}</p><p className={`${styles.muted} text-xs capitalize`}>{item.fill_type}</p></div>
                      <div className="tabular-nums"><CellLabel>Quantity</CellLabel>{item.quantity} L</div>
                      <div className="font-extrabold tabular-nums"><CellLabel>Amount</CellLabel>{formatMoney(item.total_cost, currency)}</div>
                    </div>
                  ))}
                </div>
                <div className={styles.rowActions}>
                  <Link className={styles.iconButton} aria-label="Edit fuel fill-up" href={`/garage/${vehicleId}?edit=${fill.id}`}>
                    <Pencil className="size-5" aria-hidden="true" />
                  </Link>
                  <DeleteButton vehicleId={vehicleId} recordId={fill.id} kind="fuel-fillups" />
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function AirFillHistory({ fills, vehicleId }: { fills: AirFill[]; vehicleId: string }) {
  return (
    <section aria-labelledby="air-fill-history-heading" className={`${styles.recordList} ${styles.airHistory}`}>
      <h3 id="air-fill-history-heading" className={`${styles.subheading} px-[var(--detail-pad)] pt-[var(--detail-pad)] pb-3`}>Air fill history</h3>
      {fills.length === 0 ? (
        <p className={`${styles.muted} px-[var(--detail-pad)] pb-[var(--detail-pad)] text-sm`}>Record an air fill to start your history.</p>
      ) : (
        <>
          <div className={styles.rowHeader} aria-hidden="true"><span>Date</span><span>Odometer</span><span className={styles.rowActions} /></div>
          <ul aria-labelledby="air-fill-history-heading">
            {fills.map((fill) => (
              <li key={fill.id} className={styles.row}>
                <div className="font-bold"><LocalDate dateString={fill.filled_at} /></div>
                <div className="tabular-nums"><CellLabel>Odometer</CellLabel>{fill.odometer_km == null ? "—" : `${fill.odometer_km.toLocaleString("en-IN")} km`}</div>
                <div className={styles.rowActions}><DeleteButton vehicleId={vehicleId} recordId={fill.id} kind="air-fills" /></div>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

function CellLabel({ children }: { children: string }) {
  return <span className={`${styles.label} ${styles.cellLabel} sr-only block`}>{children}</span>;
}
