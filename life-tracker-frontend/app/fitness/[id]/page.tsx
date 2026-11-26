import { WorkoutDetailScreen } from "@/features/fitness/screens/workout-detail-screen";

export const metadata = {
  title: "Fitness Workout | PULSE",
};

export default async function GymVisitPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <WorkoutDetailScreen visitId={id} />;
}
