import type { WorkoutGroup } from "@/features/fitness/components/gym-visits-list";
import type { WorkoutClassification } from "@/features/fitness/types/workout-types";

export type FitnessStat = { id: string; label: string; value: string; subtitle: string; tooltip?: string };

export type FitnessOverview = {
  today: string;
  cards: FitnessStat[];
  calendar_workouts: Record<string, WorkoutGroup>;
  recent_workouts: WorkoutGroup[];
};

export type GymVisit = WorkoutClassification & {
  id: number;
  created_at: string;
  start_time: string | null;
  end_time: string | null;
  duration_seconds: number | null;
  calories_burned: number | null;
  synced_from_apple_watch: boolean;
};

