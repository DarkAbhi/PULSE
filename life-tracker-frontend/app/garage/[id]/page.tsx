import { apiFetch } from "@/app/lib/server-api";
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
import LocalDate from "@/app/components/local-date";
import VehicleEditor from "@/app/garage/vehicle-editor";
import { Bell, Bus, CarFront, Fuel, Motorbike, Pencil, Plus, Scooter, Truck, Van, Wrench } from "lucide-react";
import { frame } from "@/app/components/design-system/page-frame";
import FuelTypeDot from "@/app/garage/fuel-type-dot";
import { detail, vehicleDetailPalette } from "@/app/garage/[id]/vehicle-detail-classes";

const vehicleTag = "inline-flex items-center gap-2 rounded-full border-[1.5px] border-white/40 bg-white/20 px-3.5 py-[5px] text-[0.9rem] font-bold";
const fuelItems = "grid grid-cols-[minmax(0,1.35fr)_minmax(0,0.85fr)_minmax(0,1fr)] items-center gap-2.5 max-[761px]:grid-cols-2 max-[761px]:[&>:first-child]:col-span-full";

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
    <main className={`${frame.page} ${vehicleDetailPalette}`}>
      <div className={`${frame.content} flex flex-col gap-[clamp(20px,3vw,30px)]`}>
        <TopAppBar backHref="/garage" backLabel="Back to Garage" />

        <section aria-labelledby="vehicle-title" className="relative grid gap-[clamp(20px,3vw,32px)] rounded-[clamp(28px,4vw,40px)] bg-[linear-gradient(135deg,#2440e0,#0f86c9)] p-[clamp(20px,3vw,36px)] text-white shadow-[0_22px_44px_-26px_rgb(36_64_224/90%)]">
          <div className="pointer-events-none absolute inset-0 overflow-hidden rounded-[inherit]" aria-hidden="true">
            <VehicleIcon className="absolute -top-5 -right-[30px] size-[300px] opacity-10" strokeWidth={1} />
          </div>
          <div className="relative flex items-center gap-[clamp(14px,2vw,22px)] max-[641px]:flex-wrap">
            <span className="grid aspect-square w-[clamp(56px,8vw,88px)] flex-none place-items-center rounded-[clamp(20px,3vw,28px)] border-[1.5px] border-white/40 bg-white/20" aria-hidden="true"><VehicleIcon className="size-[52%]" strokeWidth={1.8} /></span>
            <div className="min-w-0 flex-1 max-[641px]:basis-[calc(100%-90px)]">
              <h1 id="vehicle-title" className="text-[clamp(2.1rem,6vw,4.6rem)] leading-[0.98] font-extrabold tracking-[-0.045em] wrap-anywhere">{data.vehicle_name}</h1>
              <div className="mt-2.5 flex flex-wrap gap-2 capitalize">
                {data.kind !== "unknown" && <span className={vehicleTag}>{data.kind}</span>}
                {data.fuel_types.map((fuel) => (
                  <span key={fuel} className={vehicleTag}>
                    <FuelTypeDot fuel={fuel} />
                    {formatFuel(fuel)}
                  </span>
                ))}
              </div>
            </div>
            <VehicleEditor
              key={JSON.stringify([data.catalog_id, data.vehicle_name, data.kind, data.make_name, data.model_name, data.fuel_types])}
              triggerClassName={detail.heroButton}
              vehicle={{ id, name: data.vehicle_name, kind: data.kind, catalog_id: data.catalog_id, make_name: data.make_name, model_name: data.model_name, fuel_types: data.fuel_types }}
            />
          </div>
          <dl className="relative grid grid-cols-4 rounded-[22px] border border-white/22 bg-white/14 max-[761px]:grid-cols-2">
            {specs.map(({ label, value, capitalize }) => (
              <div key={label} className="border-white/22 px-[clamp(10px,2vw,18px)] py-3.5 [&:not(:first-child)]:border-s max-[761px]:[&:nth-child(3)]:border-s-0 max-[761px]:[&:nth-child(n+3)]:border-t">
                <dt className={detail.specLabel}>{label}</dt>
                <dd className={`mt-0.5 text-[clamp(0.95rem,2vw,1.15rem)] font-extrabold wrap-anywhere ${capitalize && value ? "capitalize" : ""}`}>{value || "Not selected"}</dd>
              </div>
            ))}
          </dl>
          {data.kind === "unknown" && (
            <p className="relative text-sm">Use Edit vehicle to choose a vehicle type or link a catalog model.</p>
          )}
        </section>

        <nav aria-label="Vehicle sections" className="flex max-w-full self-start gap-1 overflow-x-auto rounded-full bg-(--detail-surface) p-1.5 shadow-(--detail-shadow)">
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

        {activeTab === "overview" && <ComingUp items={data.coming_up ?? []} vehicleId={id} />}

        {activeTab === "fuel" && (
          <section id="fuel" aria-labelledby="fuel-heading" className={detail.section}>
            <div className={detail.sectionHeader}>
              <h2 id="fuel-heading" className={detail.sectionTitle}><Fuel className="size-6" aria-hidden="true" /> Fuel</h2>
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
          <section id="maintenance" aria-labelledby="maintenance-heading" className={detail.section}>
            <div className={detail.sectionHeader}>
              <h2 id="maintenance-heading" className={detail.sectionTitle}><Wrench className="size-6" aria-hidden="true" /> Maintenance &amp; expenses</h2>
              <Link className={detail.primaryButton} href={`${detailHref}?maintenance=new`}>
                <Plus className="size-4" aria-hidden="true" /> Add record
              </Link>
            </div>
            <MaintenanceHistory records={data.maintenance_records} vehicleId={id} currency={currency} />
          </section>
        )}

        {(activeTab === "overview" || activeTab === "maintenance") && (
          <section aria-labelledby="reminders-heading" className={`${detail.card} flex flex-wrap items-center justify-between gap-4`}>
            <div>
              <h2 id="reminders-heading" className={`${detail.subheading} flex items-center gap-2`}><Bell className="size-5" aria-hidden="true" /> Reminders</h2>
              <p className={`${detail.muted} mt-1 text-sm`}>Set intervals for engine oil, tire pressure and general service.</p>
            </div>
            <Link className={detail.secondaryButton} href={`${detailHref}/reminders`}>Reminder settings</Link>
          </section>
        )}

        <section aria-labelledby="delete-vehicle-heading" className={`${detail.card} border-[1.5px] border-(--detail-danger)/40 shadow-none! flex flex-wrap items-center justify-between gap-4`}>
          <div>
            <h2 id="delete-vehicle-heading" className={`${detail.subheading} ${detail.danger}`}>Delete vehicle</h2>
            <p className={`${detail.muted} mt-1 text-sm`}>Permanently removes this vehicle and its fuel, air fill and maintenance history.</p>
          </div>
          <DeleteVehicleButton vehicleId={id} vehicleName={data.vehicle_name} />
        </section>

        <footer className={`${detail.muted} flex flex-wrap gap-x-4 gap-y-2 text-xs`}>
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
      <h3 id="fuel-history-heading" className={`${detail.subheading} mb-3`}>Fuel fill-ups</h3>
      {fills.length === 0 ? <p className={detail.empty}>No fuel entries yet.</p> : (
        <div className={`${detail.recordList} [--history-columns:minmax(0,1.15fr)_minmax(0,1fr)_minmax(0,2.8fr)_auto] [--history-actions:88px]`}>
          <div className={detail.rowHeader} aria-hidden="true">
            <span>Fill-up</span><span>Odometer</span>
            <div className={fuelItems}><span>Fuel</span><span>Quantity</span><span>Amount</span></div>
            <span className={detail.rowActions} />
          </div>
          <ul aria-labelledby="fuel-history-heading">
            {fills.map((fill) => (
              <li key={fill.id} className={`${detail.row} ${detail.stackedRow}`}>
                <div className={detail.stackAcross}>
                  <p className="font-bold">{fill.station_name || <LocalDate dateString={fill.filled_at} />}</p>
                  {fill.station_name && <p className={`${detail.muted} text-sm`}><LocalDate dateString={fill.filled_at} /></p>}
                </div>
                <div className="tabular-nums"><CellLabel>Odometer</CellLabel>{fill.odometer_km.toLocaleString("en-IN")} km</div>
                <div className={`${detail.stackAcross} flex flex-col gap-3`} role="group" aria-label="Fuel items">
                  {fill.items.map((item, index) => (
                    <div className={fuelItems} key={index}>
                      <div><p className="font-semibold capitalize">{item.fuel_type}</p><p className={`${detail.muted} text-xs capitalize`}>{item.fill_type}</p></div>
                      <div className="tabular-nums"><CellLabel>Quantity</CellLabel>{item.quantity} L</div>
                      <div className="font-extrabold tabular-nums"><CellLabel>Amount</CellLabel>{formatMoney(item.total_cost, currency)}</div>
                    </div>
                  ))}
                </div>
                <div className={`${detail.rowActions} ${detail.stackAcross}`}>
                  <Link className={detail.iconButton} aria-label="Edit fuel fill-up" href={`/garage/${vehicleId}?edit=${fill.id}`}>
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
    <section aria-labelledby="air-fill-history-heading" className={`${detail.recordList} [--history-columns:minmax(0,1.25fr)_minmax(0,1fr)_auto] [--history-actions:44px]`}>
      <h3 id="air-fill-history-heading" className={`${detail.subheading} px-(--detail-pad) pt-(--detail-pad) pb-3`}>Air fill history</h3>
      {fills.length === 0 ? (
        <p className={`${detail.muted} px-(--detail-pad) pb-(--detail-pad) text-sm`}>Record an air fill to start your history.</p>
      ) : (
        <>
          <div className={detail.rowHeader} aria-hidden="true"><span>Date</span><span>Odometer</span><span className={detail.rowActions} /></div>
          <ul aria-labelledby="air-fill-history-heading">
            {fills.map((fill) => (
              <li key={fill.id} className={detail.row}>
                <div className="font-bold"><LocalDate dateString={fill.filled_at} /></div>
                <div className="tabular-nums"><CellLabel>Odometer</CellLabel>{fill.odometer_km == null ? "—" : `${fill.odometer_km.toLocaleString("en-IN")} km`}</div>
                <div className={detail.rowActions}><DeleteButton vehicleId={vehicleId} recordId={fill.id} kind="air-fills" /></div>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

function CellLabel({ children }: { children: string }) {
  return <span className={detail.cellLabel}>{children}</span>;
}
