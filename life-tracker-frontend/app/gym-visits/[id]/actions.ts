"use server";

import { apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

export async function addExercise(
  visitID: string,
  name: string,
  sets: { reps: number; weight: number | null }[],
) {
  try {
    const response = await apiFetch(
      `${apiBaseURL}/api/gym-visits/${visitID}/exercises`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ name, sets }),
      },
    );

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      return {
        ok: false,
        error: body.error ?? "We couldn't save that exercise.",
      };
    }

    revalidatePath(`/fitness/${visitID}`);
    return { ok: true };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}

export async function deleteVisit(visitID: string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/gym-visits/${visitID}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return {
        ok: false,
        error: "We couldn't delete this gym visit. Please try again.",
      };
    }

    revalidatePath("/fitness");
    return { ok: true };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}
