import { ReminderScreen } from "@/features/garage/screens/reminder-screen";

export const metadata = { title: "Edit reminder | PULSE" };

export default async function ReminderPage({ params }: { params: Promise<{ id: string; kind: string }> }) {
  const { id, kind } = await params;
  return <ReminderScreen vehicleId={id} kind={kind} />;
}
