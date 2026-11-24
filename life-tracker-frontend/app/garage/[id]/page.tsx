import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import PulseCard from "@/app/components/design-system/pulse-card";
import { getUserCurrency } from "@/app/lib/user-currency";
import { formatMoney } from "@/app/lib/currency.mjs";
import { redirect } from "next/navigation";
import Link from "next/link";
import TopAppBar from "@/app/components/top-app-bar";
import type { AirFill, FuelFill, VehicleHistoryData } from "@/app/garage/[id]/types";
import DeleteButton from "@/app/garage/[id]/delete-button";
import DeleteVehicleButton from "@/app/garage/[id]/delete-vehicle-button";
import EditFuelModal from "@/app/garage/[id]/edit-fuel-modal";
import ComingUp from "@/app/garage/[id]/coming-up";
import MaintenanceHistory from "@/app/garage/[id]/maintenance-history";
import MaintenanceRecordModal from "@/app/garage/[id]/maintenance-record-modal";
import FuelEfficiencySection from "@/app/garage/[id]/fuel-efficiency-section";
import TirePressureCard from "@/app/garage/[id]/tire-pressure-card";
import { getUserTimeZone } from "@/app/lib/user-time-zone";
import { formatInstant } from "@/app/lib/time-zone.mjs";
import VehicleEditor from "@/app/garage/vehicle-editor";
import { Bell, Fuel, Pencil, Plus, Wrench } from "lucide-react";
import { frame } from "@/app/components/design-system/page-frame";
import FuelTypeDot from "@/app/garage/fuel-type-dot";
import { formatVehicleLabel, getVehicleIcon } from "@/app/garage/vehicle-presentation";
import { detail } from "@/app/garage/[id]/vehicle-detail-classes";

const vehicleTag = "inline-flex items-center gap-2 rounded-full border border-white/40 bg-white/20 px-3.5 py-1.5 text-sm font-bold";
const fuelItems = "grid grid-cols-[minmax(0,1.35fr)_minmax(0,0.85fr)_minmax(0,1fr)] items-center gap-2.5 max-sheet:grid-cols-2 max-sheet:[&>:first-child]:col-span-full";

export const metadata = { title: "Vehicle details | PULSE" };

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
  const { id } = await params;
  const [response, currency, timeZone, query] = await Promise.all([
    apiFetch(`${apiBaseURL}/api/vehicles/${encodeURIComponent(id)}/history`, {
      signal: AbortSignal.timeout(10_000),
    }).catch(() => null),
    getUserCurrency(),
    getUserTimeZone(),
    searchParams,
  ]);
  const { tab, edit, maintenance, "edit-maintenance": editMaintenance } = query;
  if (response?.status === 401) redirect("/");

  if (response?.status === 404) {
    return (
      <main id="main-content" tabIndex={-1} className={`${frame.page} bg-background text-foreground`}>
        <div className={frame.content}>
          <TopAppBar backHref="/garage" backLabel="Back to Garage" />
          <PulseCard className="mt-5 p-8 text-center">
            <h1 className="text-xl font-semibold text-foreground">
              Vehicle not found
            </h1>
            <p className="mt-2 text-sm text-muted-foreground">
              This vehicle may have been deleted or does not exist.
            </p>
          </PulseCard>
        </div>
      </main>
    );
  }

  if (!response?.ok) {
    return (
      <main id="main-content" tabIndex={-1} className={`${frame.page} bg-background text-foreground`}>
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

  const VehicleIcon = getVehicleIcon(data.kind);
  const specs = [
    { label: "Type", value: data.kind === "unknown" ? "" : data.kind, capitalize: true },
    { label: "Make", value: data.make_name },
    { label: "Model", value: data.model_name },
    { label: "Fuel type", value: data.fuel_types.map(formatVehicleLabel).join(" + "), capitalize: true },
  ];
  const detailHref = `/garage/${id}`;

  return (
    <main id="main-content" tabIndex={-1} className={frame.page}>
      <div className={`${frame.content} flex flex-col gap-5 md:gap-7`}>
        <TopAppBar backHref="/garage" backLabel="Back to Garage" />

        <section aria-labelledby="vehicle-title" className="garage-hero relative grid gap-garage-hero-gap rounded-garage-hero bg-garage-hero p-garage-hero text-white shadow-garage-hero">
          <div className="pointer-events-none absolute inset-0 overflow-hidden rounded-[inherit]" aria-hidden="true">
            <VehicleIcon className="absolute -top-5 -end-7 size-75 opacity-10" strokeWidth={1} />
          </div>
          <div className="relative flex items-center gap-3.5 max-sm:flex-wrap sm:gap-5">
            <span className="grid size-garage-hero-icon flex-none place-items-center rounded-garage-hero-icon border border-white/40 bg-white/20" aria-hidden="true"><VehicleIcon className="size-1/2" strokeWidth={1.8} /></span>
            <div className="min-w-0 flex-1 max-sm:basis-[calc(100%-5.5rem)]">
              <h1 id="vehicle-title" className="text-garage-vehicle-title font-extrabold tracking-tight wrap-anywhere">{data.vehicle_name}</h1>
              <div className="mt-2.5 flex flex-wrap gap-2 capitalize">
                {data.kind !== "unknown" && <span className={vehicleTag}>{data.kind}</span>}
                {data.fuel_types.map((fuel) => (
                  <span key={fuel} className={vehicleTag}>
                    <FuelTypeDot fuel={fuel} />
                    {formatVehicleLabel(fuel)}
                  </span>
                ))}
              </div>
            </div>
            <VehicleEditor
              key={JSON.stringify([data.catalog_id, data.vehicle_name, data.kind, data.make_name, data.model_name, data.fuel_types])}
              vehicle={{ id, name: data.vehicle_name, kind: data.kind, catalog_id: data.catalog_id, make_name: data.make_name, model_name: data.model_name, fuel_types: data.fuel_types }}
            />
          </div>
          <dl className="relative grid grid-cols-4 rounded-3xl border border-white/20 bg-white/15 max-sheet:grid-cols-2">
            {specs.map(({ label, value, capitalize }) => (
              <div key={label} className="border-white/20 px-2.5 py-3.5 sm:px-4 [&:not(:first-child)]:border-s max-sheet:[&:nth-child(3)]:border-s-0 max-sheet:[&:nth-child(n+3)]:border-t">
                <dt className={detail.specLabel}>{label}</dt>
                <dd className={`mt-0.5 text-base sm:text-section font-extrabold wrap-anywhere ${capitalize && value ? "capitalize" : ""}`}>{value || "Not selected"}</dd>
              </div>
            ))}
          </dl>
          {data.kind === "unknown" && (
            <p className="relative text-sm">Use Edit vehicle to choose a vehicle type or link a catalog model.</p>
          )}
        </section>

        <nav aria-label="Vehicle sections" className="flex max-w-full self-start gap-1 overflow-x-auto rounded-full bg-card p-1.5 shadow-card">
          {vehicleTabs.map(({ id: tabId, label }) => (
            <Link
              key={tabId}
              href={tabId === "overview" ? detailHref : `${detailHref}?tab=${tabId}`}
              className={detail.tab}
              scroll={false}
              aria-current={activeTab === tabId ? "page" : undefined}
            >
              {label}
            </Link>
          ))}
        </nav>

        {activeTab === "overview" && <ComingUp items={data.coming_up ?? []} vehicleId={id} timeZone={timeZone} />}

        {activeTab === "fuel" && (
          <section id="fuel" aria-labelledby="fuel-heading" className={detail.section}>
            <div className={detail.sectionHeader}>
              <h2 id="fuel-heading" className={detail.sectionTitle}><Fuel className="size-6" aria-hidden="true" /> Fuel</h2>
            </div>
            <FuelEfficiencySection stats={data.fuel_efficiency} currency={currency} />
            <FuelHistory fills={data.fuel_fillups} vehicleId={id} currency={currency} timeZone={timeZone} />
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
            <AirFillHistory fills={data.air_fills} vehicleId={id} timeZone={timeZone} />
          </TirePressureCard>
        )}

        {activeTab === "maintenance" && (
          <section id="maintenance" aria-labelledby="maintenance-heading" className={detail.section}>
            <div className={detail.sectionHeader}>
              <h2 id="maintenance-heading" className={detail.sectionTitle}><Wrench className="size-6" aria-hidden="true" /> Maintenance &amp; expenses</h2>
              <Link className={detail.primaryLink} href={`${detailHref}?maintenance=new`}>
                <Plus className="size-4" aria-hidden="true" /> Add record
              </Link>
            </div>
            <MaintenanceHistory records={data.maintenance_records} vehicleId={id} currency={currency} />
          </section>
        )}

        {(activeTab === "overview" || activeTab === "maintenance") && (
          <PulseCard as="section" aria-labelledby="reminders-heading" className={`${detail.card} flex flex-wrap items-center justify-between gap-4`}>
            <div>
              <h2 id="reminders-heading" className={`${detail.subheading} flex items-center gap-2`}><Bell className="size-5" aria-hidden="true" /> Reminders</h2>
              <p className="text-muted-foreground mt-1 text-sm">Set intervals for engine oil, tire pressure and general service.</p>
            </div>
            <Link className={detail.secondaryLink} href={`${detailHref}/reminders`}>Reminder settings</Link>
          </PulseCard>
        )}

        <PulseCard as="section" aria-labelledby="delete-vehicle-heading" className={`${detail.card} flex flex-wrap items-center justify-between gap-4`}>
          <div>
            <h2 id="delete-vehicle-heading" className={`${detail.subheading} text-destructive`}>Delete vehicle</h2>
            <p className="text-muted-foreground mt-1 text-sm">Permanently removes this vehicle and its fuel, air fill and maintenance history.</p>
          </div>
          <DeleteVehicleButton vehicleId={id} vehicleName={data.vehicle_name} />
        </PulseCard>

      </div>
      {editingFill && <EditFuelModal vehicleId={id} fill={editingFill} />}
      {maintenance === "new" && <MaintenanceRecordModal vehicleId={id} />}
      {editingMaintenanceRecord && <MaintenanceRecordModal vehicleId={id} record={editingMaintenanceRecord} />}
    </main>
  );
}

function FuelHistory({ fills, vehicleId, currency, timeZone }: { fills: FuelFill[]; vehicleId: string; currency: string; timeZone: string }) {
  return (
    <section aria-labelledby="fuel-history-heading">
      <h3 id="fuel-history-heading" className={`${detail.subheading} mb-3`}>Fuel fill-ups</h3>
      {fills.length === 0 ? <p className={detail.empty}>No fuel entries yet.</p> : (
        <PulseCard className={`${detail.recordList} [--history-columns:minmax(0,1.15fr)_minmax(0,1fr)_minmax(0,2.8fr)_auto] [--history-actions:88px]`}>
          <div className={detail.rowHeader} aria-hidden="true">
            <span>Fill-up</span><span>Odometer</span>
            <div className={fuelItems}><span>Fuel</span><span>Quantity</span><span>Amount</span></div>
            <span className={detail.rowActions} />
          </div>
          <ul aria-labelledby="fuel-history-heading">
            {fills.map((fill) => (
              <li key={fill.id} className={`${detail.row} ${detail.stackedRow}`}>
                <div className={detail.stackAcross}>
                  <p className="font-bold">{fill.station_name || <time dateTime={fill.filled_at}>{formatInstant(fill.filled_at, timeZone)}</time>}</p>
                  {fill.station_name && <p className="text-sm text-muted-foreground"><time dateTime={fill.filled_at}>{formatInstant(fill.filled_at, timeZone)}</time></p>}
                </div>
                <div className="tabular-nums"><CellLabel>Odometer</CellLabel>{fill.odometer_km.toLocaleString("en-IN")} km</div>
                <div className={`${detail.stackAcross} flex flex-col gap-3`} role="group" aria-label="Fuel items">
                  {fill.items.map((item, index) => (
                    <div className={fuelItems} key={index}>
                      <div><p className="font-semibold capitalize">{item.fuel_type}</p><p className="text-muted-foreground text-xs capitalize">{item.fill_type}</p></div>
                      <div className="tabular-nums"><CellLabel>Quantity</CellLabel>{item.quantity} L</div>
                      <div className="font-extrabold tabular-nums"><CellLabel>Amount</CellLabel>{formatMoney(item.total_cost, currency)}</div>
                    </div>
                  ))}
                </div>
                <div className={`${detail.rowActions} ${detail.stackAcross}`}>
                  <Link className={detail.iconLink} aria-label="Edit fuel fill-up" href={`/garage/${vehicleId}?edit=${fill.id}`}>
                    <Pencil className="size-5" aria-hidden="true" />
                  </Link>
                  <DeleteButton vehicleId={vehicleId} recordId={fill.id} kind="fuel-fillups" />
                </div>
              </li>
            ))}
          </ul>
        </PulseCard>
      )}
    </section>
  );
}

function AirFillHistory({ fills, vehicleId, timeZone }: { fills: AirFill[]; vehicleId: string; timeZone: string }) {
  return (
    <PulseCard as="section" aria-labelledby="air-fill-history-heading" className={`${detail.recordList} [--history-columns:minmax(0,1.25fr)_minmax(0,1fr)_auto] [--history-actions:44px]`}>
      <h3 id="air-fill-history-heading" className={`${detail.subheading} px-garage-detail pt-garage-detail pb-3`}>Air fill history</h3>
      {fills.length === 0 ? (
        <p className="text-muted-foreground px-garage-detail pb-garage-detail text-sm">Record an air fill to start your history.</p>
      ) : (
        <>
          <div className={detail.rowHeader} aria-hidden="true"><span>Date</span><span>Odometer</span><span className={detail.rowActions} /></div>
          <ul aria-labelledby="air-fill-history-heading">
            {fills.map((fill) => (
              <li key={fill.id} className={detail.row}>
                <div className="font-bold"><time dateTime={fill.filled_at}>{formatInstant(fill.filled_at, timeZone)}</time></div>
                <div className="tabular-nums"><CellLabel>Odometer</CellLabel>{fill.odometer_km == null ? "—" : `${fill.odometer_km.toLocaleString("en-IN")} km`}</div>
                <div className={detail.rowActions}><DeleteButton vehicleId={vehicleId} recordId={fill.id} kind="air-fills" /></div>
              </li>
            ))}
          </ul>
        </>
      )}
    </PulseCard>
  );
}

function CellLabel({ children }: { children: string }) {
  return <span className={detail.cellLabel}>{children}</span>;
}
