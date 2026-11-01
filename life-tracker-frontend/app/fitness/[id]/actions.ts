"use server";

import { apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export type CatalogExercise = {
  id: string;
  name: string;
  equipment: string | null;
  data: {
    primaryMuscles: string[];
    secondaryMuscles: string[];
    instructions: string[];
    level: string;
    category: string;
    images: string[];
  };
};

export type CatalogSearch = {
  query: string;
  match_type: string;
  exercise_catalog_id: string | null;
  candidates: (CatalogExercise & { match_type: string })[];
};

export type SavedExercise = {
  id: number;
  name: string;
  exercise_catalog_id: string | null;
  catalog_exercise: CatalogExercise | null;
  sets: { id: number; set_number: number; reps: number; weight: number | null }[];
};

export async function updateExercise(
  visitID: string, exerciseID: number, name: string,
  sets: { reps: number; weight: number | null }[],
  exerciseCatalogID: string | null, rememberAlias: boolean
) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises/${exerciseID}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json",  },
      body: JSON.stringify({ name, sets, exercise_catalog_id: exerciseCatalogID, remember_alias: rememberAlias }),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      return { ok: false, error: body.error ?? "We couldn't update this exercise. Please try again." };
    }
    revalidatePath(`/fitness/${visitID}`);
    revalidatePath("/fitness");
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

export async function searchExercises(name: string): Promise<{
  result?: CatalogSearch;
  error?: string;
}> {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/exercise-catalog?${new URLSearchParams({ q: name })}`, {
      cache: "no-store",
    });
    if (!response.ok) return { error: "We couldn't search exercises. You can still save your own name." };
    return { result: (await response.json()) as CatalogSearch };
  } catch {
    return { error: "We couldn't search exercises. You can still save your own name." };
  }
}

export async function addExercise(
  visitID: string,
  name: string,
  sets: { reps: number; weight: number | null }[],
  exerciseCatalogID: string | null = null,
  rememberAlias: boolean = false
) {
  try {
    const response = await apiFetch(
      `${apiBaseURL}/api/gym-visits/${visitID}/exercises`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ name, sets, exercise_catalog_id: exerciseCatalogID, remember_alias: rememberAlias }),
      }
    );

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      return { ok: false, error: body.error ?? "We couldn't save that exercise." };
    }

    revalidatePath(`/fitness/${visitID}`);
    revalidatePath("/fitness");
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

export async function deleteExercise(visitID: string, exerciseID: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises/${exerciseID}`, {
      method: "DELETE",
    });
    if (!response.ok) {
      return { ok: false, error: "We couldn't delete this exercise. Please try again." };
    }
    revalidatePath(`/fitness/${visitID}`);
    revalidatePath("/fitness");
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

export async function deleteVisit(visitID: string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/gym-visits/${visitID}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return { ok: false, error: "We couldn't delete this gym visit. Please try again." };
    }

    revalidatePath("/fitness");
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}
