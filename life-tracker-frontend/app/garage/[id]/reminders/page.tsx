import Link from "next/link";
import { ArrowLeft, ChevronRight } from "lucide-react";
import PulseCard from "@/app/components/design-system/pulse-card";
import { loadReminderSettings, reminderTitles } from "./data";

export const metadata = { title: "Reminder settings | PULSE" };
export default async function ReminderSettingsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { vehicle, settings } = await loadReminderSettings(id);
  return <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10">
    <div className="mx-auto max-w-3xl">
      <Link href={`/garage/${id}#maintenance`} className="inline-flex min-h-11 items-center gap-2 text-sm font-medium text-action"><ArrowLeft className="h-4 w-4" /> Back to Maintenance</Link>
      <p className="mt-6 text-sm text-muted-foreground">{vehicle.vehicle_name}</p>
      <h1 className="mt-2 text-3xl font-bold">Reminder settings</h1>
      <p className="mt-3 text-muted-foreground">Choose a reminder to set your intervals or enter the recommendations from your vehicle manual.</p>
      <div className="mt-6 space-y-4">{settings.map((setting) => <Link key={setting.kind} href={`/garage/${id}/reminders/${setting.kind}`} className="block rounded-xl focus-visible:outline-2 focus-visible:outline-action">
        <PulseCard className="flex items-center justify-between gap-4 p-5 hover:bg-accent"><div><h2 className="text-lg font-semibold">{reminderTitles[setting.kind]}</h2><p className="mt-1 text-sm text-muted-foreground">{setting.active_source === "manual" ? "Vehicle manual" : "My intervals"}</p></div><ChevronRight className="h-5 w-5 text-muted-foreground" aria-hidden="true" /></PulseCard>
      </Link>)}</div>
    </div>
  </main>;
}
