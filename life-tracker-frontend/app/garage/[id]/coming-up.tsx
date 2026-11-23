import Link from "next/link";
import PulseCard from "@/app/components/design-system/pulse-card";
import { CircleDot, Droplet, Gauge, Plus, Wrench } from "lucide-react";
import type { UpcomingMaintenance } from "@/app/garage/[id]/types";
import { detail } from "@/app/garage/[id]/vehicle-detail-classes";

export default function ComingUp({ items, vehicleId }: { items: UpcomingMaintenance[]; vehicleId: string }) {
  return (
    <section aria-labelledby="coming-up-heading" className={detail.section}>
      <div className={detail.sectionHeader}>
        <div>
          <h2 id="coming-up-heading" className={detail.sectionTitle}><Gauge className="size-6" aria-hidden="true" /> Overview</h2>
          <p className="text-muted-foreground mt-1 text-sm">Coming up for this vehicle</p>
        </div>
      </div>
      {items.length === 0 ? (
        <p className={detail.empty}>No upcoming reminders for this vehicle.</p>
      ) : (
        <>
          <div className={detail.tiles}>
            {items.map((item) => <ReminderCard key={item.kind} item={item} vehicleId={vehicleId} />)}
          </div>
          <p className="text-sm text-muted-foreground">Uses the active schedule shown for each task. Mileage uses the last recorded reading.</p>
        </>
      )}
    </section>
  );
}

function ReminderCard({ item, vehicleId }: { item: UpcomingMaintenance; vehicleId: string }) {
  const pressure = item.kind === "tire_pressure";
  const oil = item.kind === "engine_oil";
  const Icon = pressure ? CircleDot : oil ? Droplet : Wrench;
  const ActionIcon = pressure ? CircleDot : Plus;
  const title = pressure ? "Check tire pressure" : oil ? "Oil change" : "Service check";
  const days = item.remaining_days;
  const km = item.remaining_km;
  const dueText = item.status === "no_history" ? (pressure ? "No air fill recorded" : "No completed work recorded")
    : item.status === "needs_odometer" ? "Odometer reading needed"
    : days != null && days < 0 ? `Overdue by ${-days} ${days === -1 ? "day" : "days"}`
    : days === 0 ? "Due today"
    : km != null ? `${km < 0 ? "Overdue by" : "Due in"} ${Math.ceil(Math.abs(km)).toLocaleString("en-IN")} km`
    : days != null ? `Due in ${days} ${days === 1 ? "day" : "days"}` : "Mileage needed";
  const progress = item.progress == null ? null : Math.max(0, Math.min(100, item.progress * 100));
  const status = item.status === "overdue" ? "overdue"
    : days != null && days >= 0 && days <= 7 ? "due_soon" : undefined;

  return (
    <PulseCard as="article" className={`${detail.card} grid grid-rows-[auto_1fr_auto] gap-4`}>
      <div className="flex items-center gap-3">
        <span className="grid size-11 flex-none place-items-center rounded-control bg-garage-service-violet text-white data-[kind=engine_oil]:bg-garage-oil data-[kind=engine_oil]:text-garage-oil-foreground data-[kind=tire_pressure]:bg-brand-cyan data-[kind=tire_pressure]:text-garage-air-foreground" data-kind={item.kind}><Icon className="size-5" aria-hidden="true" /></span>
        <div>
          <h3 className={detail.label}>{title}</h3>
          <p className="text-sm text-muted-foreground">{item.source === "manual" ? "Vehicle manual" : "My intervals"}</p>
        </div>
      </div>
      <div className="flex flex-col justify-center gap-2">
        <p className={`${detail.metric} ${status === "overdue" ? "text-destructive" : status === "due_soon" ? "text-warning" : ""}`}>{dueText}</p>
        {km != null && item.due_date && days !== 0 && (days == null || days > 0) && (
          <p className="text-sm text-muted-foreground">By {item.due_date}</p>
        )}
        {(item.distance_unavailable || item.status === "needs_odometer") && (
          <p className="text-sm text-muted-foreground">Record an odometer with this task to use its distance interval.</p>
        )}
        {pressure && item.last_at && (
          <p className="text-sm text-muted-foreground">Last air fill: {new Date(item.last_at).toLocaleDateString("en-IN", { timeZone: "Asia/Kolkata", day: "numeric", month: "short", year: "numeric" })}</p>
        )}
        {!pressure && progress != null && (
          <div className="group h-2 overflow-hidden rounded-full bg-chip" data-status={status} role="progressbar" aria-label={`${title} interval elapsed`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress)}>
            <span className="block h-full rounded-[inherit] bg-garage-progress group-data-[status=due_soon]:bg-garage-progress-warning group-data-[status=overdue]:bg-none group-data-[status=overdue]:bg-destructive" style={{ width: `${progress}%` }} />
          </div>
        )}
      </div>
      <Link
        className={`${item.status === "no_history" ? detail.primaryLink : detail.secondaryLink} justify-self-start`}
        href={`/garage/${vehicleId}?${pressure ? "tab=tires" : "maintenance=new"}`}
      >
        <ActionIcon className="size-4 shrink-0" aria-hidden="true" /> {pressure ? "Record air fill" : "Add record"}
        {!pressure && <span className="sr-only"> for {title.toLowerCase()}</span>}
      </Link>
    </PulseCard>
  );
}
