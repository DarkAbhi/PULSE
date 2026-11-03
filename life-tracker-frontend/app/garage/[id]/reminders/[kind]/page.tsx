import BackLink from "@/app/components/design-system/back-link";
import { notFound } from "next/navigation";
import MaintenanceScheduleForm from "../../maintenance-schedules";
import { loadReminderSettings, reminderDetails } from "../data";

export const metadata = { title: "Edit reminder | PULSE" };

export default async function ReminderPage({ params }: { params: Promise<{ id: string; kind: string }> }) {
  const { id, kind } = await params;
  if (!Object.hasOwn(reminderDetails, kind)) notFound();
  const { vehicle, settings } = await loadReminderSettings(id);
  const setting = settings.find((item) => item.kind === kind);
  if (!setting) notFound();
  const { title, icon: Icon } = reminderDetails[setting.kind];

  return (
    <>
      <BackLink href={`/garage/${id}/reminders`}>Back to reminder settings</BackLink>
      <header className="mt-4 flex items-center gap-4 border-b border-divider pb-5 sm:pb-6">
        <span className="grid size-12 shrink-0 place-items-center rounded-xl bg-accent text-accent-foreground sm:size-16">
          <Icon className="size-6 sm:size-8" aria-hidden="true" />
        </span>
        <div className="min-w-0">
          <p className="break-words text-sm font-medium text-muted-foreground">{vehicle.vehicle_name}</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight sm:text-4xl">{title} reminder</h1>
        </div>
      </header>
      <MaintenanceScheduleForm key={`${id}:${JSON.stringify(setting)}`} initial={setting} vehicleId={id} />
    </>
  );
}
