import BackLink from "@/app/components/design-system/back-link";
import { ChevronRight } from "lucide-react";
import PulseCard, { PulseCardLink } from "@/app/components/design-system/pulse-card";
import { loadReminderSettings, reminderDetails } from "./data";

export const metadata = { title: "Reminder settings | PULSE" };

export default async function ReminderSettingsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { vehicle, settings } = await loadReminderSettings(id);

  return (
    <>
      <BackLink href={`/garage/${id}?tab=maintenance`}>Back to Maintenance</BackLink>
      <header className="mt-4 border-b border-divider pb-5 sm:pb-6">
        <p className="break-words text-sm font-medium text-muted-foreground">{vehicle.vehicle_name}</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight sm:text-4xl">Reminder settings</h1>
        <p className="mt-2 max-w-xl text-sm text-muted-foreground sm:text-base">
          Review the active intervals for this vehicle and choose when each reminder comes due.
        </p>
      </header>

      {settings.length === 0 ? (
        <PulseCard className="mt-5 p-5 text-sm text-muted-foreground">No reminder settings are available for this vehicle.</PulseCard>
      ) : (
        <ul className="mt-5 grid gap-3">
          {settings.map((setting) => {
            const { title, icon: Icon } = reminderDetails[setting.kind];
            const fromManual = setting.active_source === "manual";
            const { kilometers, duration } = fromManual ? setting.manual : setting.personal;
            const limits = [
              kilometers > 0 ? `${kilometers.toLocaleString("en-IN")} km` : "",
              duration > 0 ? `${duration} ${duration === 1 ? "month" : "months"}` : "",
            ].filter(Boolean).join(" or ");

            return (
              <li key={setting.kind}>
                <PulseCardLink href={`/garage/${id}/reminders/${setting.kind}`} className="group grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-x-3 gap-y-2 p-4 hover:bg-secondary/50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring sm:grid-cols-[auto_minmax(0,1fr)_minmax(0,1fr)_auto] sm:gap-x-5 sm:px-5">
                  <span className="col-start-1 row-span-2 row-start-1 grid size-11 place-items-center rounded-xl bg-accent text-accent-foreground sm:row-span-1">
                    <Icon className="size-5" aria-hidden="true" />
                  </span>
                  <h2 className="col-start-2 row-start-1 font-semibold">{title}</h2>
                  <div className="col-start-2 row-start-2 sm:col-start-3 sm:row-start-1">
                    <p className="text-sm font-semibold tabular-nums">{limits ? `Every ${limits}` : "No interval set"}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">{fromManual ? "Vehicle manual" : "My intervals"}</p>
                  </div>
                  <ChevronRight className="col-start-3 row-span-2 row-start-1 size-5 text-muted-foreground group-hover:text-action sm:col-start-4 sm:row-span-1" aria-hidden="true" />
                </PulseCardLink>
              </li>
            );
          })}
        </ul>
      )}
    </>
  );
}
