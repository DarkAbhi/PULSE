import { QuestsScreen } from "@/features/quests/screens/quests-screen";

export const metadata = { title: "Pulse Quest | PULSE" };

export default async function QuestPage({ searchParams }: { searchParams: Promise<{ week?: string | string[] }> }) {
  const { week } = await searchParams;
  return <QuestsScreen week={week} />;
}
