import type { WorkoutGroup } from "@/app/fitness/gym-visits-list";

export type FitnessStat = { id: string; label: string; value: string; subtitle: string; tooltip?: string };

export type FitnessOverview = {
  today: string;
  cards: FitnessStat[];
  calendar_workouts: Record<string, WorkoutGroup>;
  recent_workouts: WorkoutGroup[];
};
