import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import PulseCard from "@/app/components/design-system/pulse-card";
import MaintenanceScheduleForm from "../../maintenance-schedules";
import { loadReminderSettings, reminderTitles } from "../data";

export const metadata = { title: "Edit reminder | PULSE" };
export default async function ReminderPage({ params }: { params: Promise<{ id: string; kind: string }> }) {
  const { id, kind } = await params;
  if (!Object.hasOwn(reminderTitles, kind)) notFound();
  const { vehicle, settings } = await loadReminderSettings(id);
  const setting = settings.find((s) => s.kind === kind);
  if (!setting) notFound();
  return <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10">
    <div className="mx-auto max-w-3xl">
      <Link href={`/garage/${id}/reminders`} className="inline-flex min-h-11 items-center gap-2 text-sm font-medium text-action"><ArrowLeft className="h-4 w-4" /> Back to reminder settings</Link>
      <p className="mt-6 text-sm text-muted-foreground">{vehicle.vehicle_name}</p>
      <h1 className="mt-2 text-3xl font-bold">{reminderTitles[kind]} reminder</h1>
      <PulseCard className="mt-6 p-5 sm:p-6"><MaintenanceScheduleForm key={`${id}:${JSON.stringify(setting)}`} initial={setting} vehicleId={id} /></PulseCard>
    </div>
  </main>;
}
