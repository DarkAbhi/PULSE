import Link from "next/link";
import { ChevronRight, Droplet, Gauge, Wrench } from "lucide-react";
import PulseCard from "@/app/components/design-system/pulse-card";
import { UpcomingMaintenance } from "./types";

export default function ComingUp({ items, vehicleId }: { items: UpcomingMaintenance[]; vehicleId: string }) {
  if (!items.length) return null;
  return <PulseCard as="section" className="p-5 sm:p-6" aria-labelledby="coming-up-heading">
    <h2 id="coming-up-heading" className="text-2xl font-bold">Coming up</h2>
    <div className="mt-4 space-y-5">{items.map((item) => {
      const pressure = item.kind === "tire_pressure";
      const Icon = pressure ? Gauge : item.kind === "engine_oil" ? Droplet : Wrench;
      const title = pressure ? "Check tire pressure" : item.kind === "engine_oil" ? "Oil change" : "Service check";
      const days = item.remaining_days;
      const km = item.remaining_km;
      const detail = item.status === "no_history" ? (pressure ? "No air fill recorded" : "No completed work recorded · Add a record")
        : item.status === "needs_odometer" ? "Record an odometer with this task to use its distance interval"
        : days != null && days < 0 ? `Overdue by ${-days} ${days === -1 ? "day" : "days"}` : days === 0 ? "Due today"
        : km != null ? `${km < 0 ? "Overdue by" : "Due in"} ${Math.ceil(Math.abs(km)).toLocaleString("en-IN")} km${item.due_date ? ` · by ${item.due_date}` : ""}`
        : days != null ? `Due in ${days} ${days === 1 ? "day" : "days"}` : "Mileage needed";
      return <Link key={item.kind} href={pressure ? "#tires-air" : `/garage/${vehicleId}?maintenance=new`} className="flex min-h-14 items-center gap-4 rounded-lg py-2 focus-visible:outline-2 focus-visible:outline-action">
        <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full border border-border bg-muted"><Icon className="h-6 w-6 text-action" aria-hidden="true" /></span>
        <span className="min-w-0 flex-1"><span className="block text-lg font-semibold">{title}</span><span className="block text-xs text-muted-foreground">{item.source === "manual" ? "Vehicle manual" : "My intervals"}</span><span className={`block text-sm ${item.status === "overdue" ? "text-destructive" : "text-muted-foreground"}`}>{detail}</span>
          {item.distance_unavailable && item.status !== "needs_odometer" && <span className="block text-xs text-muted-foreground">Record an odometer with this task to use its distance interval.</span>}
          {pressure && item.last_at && <span className="block text-xs text-muted-foreground">Last air fill: {new Date(item.last_at).toLocaleDateString("en-IN", { timeZone: "Asia/Kolkata", day: "numeric", month: "short", year: "numeric" })}</span>}
          {!pressure && item.progress != null && <span role="progressbar" aria-label={`${title} interval elapsed`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(item.progress * 100)} className="mt-3 block h-2 overflow-hidden rounded-full bg-muted"><span className="block h-full rounded-full bg-action" style={{ width: `${Math.max(0, Math.min(100, item.progress * 100))}%` }} /></span>}
        </span><ChevronRight className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden="true" />
      </Link>;
    })}</div>
    <p className="mt-4 text-xs text-muted-foreground">Uses the active schedule shown for each task. Mileage uses the last recorded reading.</p>
  </PulseCard>;
}
