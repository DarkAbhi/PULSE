"use server";

import { revalidatePath } from "next/cache";
import { cookies } from "next/headers";

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
  const cookieStore = await cookies();
  try {
    const response = await fetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises/${exerciseID}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Cookie: cookieStore.toString() },
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
  const cookieStore = await cookies();
  try {
    const response = await fetch(`${apiBaseURL}/api/exercise-catalog?${new URLSearchParams({ q: name })}`, {
      headers: { Cookie: cookieStore.toString() },
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
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(
      `${apiBaseURL}/api/gym-visits/${visitID}/exercises`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Cookie: cookieHeader,
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
  const cookieStore = await cookies();
  try {
    const response = await fetch(`${apiBaseURL}/api/gym-visits/${visitID}/exercises/${exerciseID}`, {
      method: "DELETE",
      headers: { Cookie: cookieStore.toString() },
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
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(`${apiBaseURL}/api/gym-visits/${visitID}`, {
      method: "DELETE",
      headers: {
        Cookie: cookieHeader,
      },
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
