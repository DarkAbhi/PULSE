import "server-only";
import { apiBaseURL, apiFetch, readJSON } from "@/lib/api/server-api";
import type { SavedExercise } from "@/features/fitness/api/workout-detail-actions";
import type { FitnessOverview, GymVisit } from "@/features/fitness/types/fitness-types";
import type { WorkoutScheduleDay } from "@/features/fitness/types/workout-types";

export async function loadFitnessOverview() {
  const [overview, schedule] = await Promise.all([
    readJSON<FitnessOverview>("/api/fitness/overview"),
    readJSON<WorkoutScheduleDay[]>("/api/fitness/workout-schedule"),
  ]);
  return { overview, schedule: schedule.data };
}

/** Loads a workout and its exercises; each part reports its own error so the page can still render. */
export async function loadWorkoutDetail(visitID: string) {
  let exercises: SavedExercise[] = [];
  let visit: GymVisit | null = null;
  let error = "";
  let detailsError = "";
  let visitNotFound = false;
  let unauthorized = false;

  try {
    const [exercisesResponse, visitResponse] = await Promise.all([
      apiFetch(`${apiBaseURL}/api/gym-visits/${encodeURIComponent(visitID)}/exercises`, { signal: AbortSignal.timeout(10_000) }),
      apiFetch(`${apiBaseURL}/api/fitness-workouts/${encodeURIComponent(visitID)}`, { signal: AbortSignal.timeout(10_000) }),
    ]);
    unauthorized = exercisesResponse.status === 401 || visitResponse.status === 401;
    visitNotFound = exercisesResponse.status === 404 || visitResponse.status === 404;
    if (exercisesResponse.ok) {
      exercises = ((await exercisesResponse.json()) as SavedExercise[] | null) ?? [];
    } else {
      error = "We couldn't load this workout's exercises. Please try again.";
    }

    if (visitResponse.ok) {
      visit = (await visitResponse.json()) as GymVisit;
    } else {
      detailsError = "We couldn't load this workout's details. Please try again.";
    }
  } catch {
    error = "We couldn't reach the server. Please try again.";
  }

  return { exercises, visit, error, detailsError, visitNotFound, unauthorized };
}
