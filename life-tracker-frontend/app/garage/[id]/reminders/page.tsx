import { ReminderSettingsScreen } from "@/features/garage/screens/reminder-settings-screen";

export const metadata = { title: "Reminder settings | PULSE" };

export default async function ReminderSettingsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ReminderSettingsScreen vehicleId={id} />;
}
