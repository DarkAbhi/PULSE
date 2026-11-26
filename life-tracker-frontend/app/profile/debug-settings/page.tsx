import { DebugSettingsScreen } from "@/features/profile/screens/debug-settings-screen";

export const metadata = { title: "Debug settings | PULSE" };

export default async function DebugSettingsPage({ searchParams }: {
  searchParams: Promise<{ tab?: string | string[] }>;
}) {
  const { tab } = await searchParams;
  return <DebugSettingsScreen tab={tab} />;
}
