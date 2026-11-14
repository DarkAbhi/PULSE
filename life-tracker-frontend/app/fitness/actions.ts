"use server";

import { apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

export async function markGymVisitAction(date?: string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/${date ? "gym-visits" : "workout/today"}`, {
      method: "POST",
      headers: {
        ...(date ? { "Content-Type": "application/json" } : {}),
      },
      ...(date ? { body: JSON.stringify({ date }) } : {}),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return {
        ok: false,
        error: body.error ?? "We couldn't save your workout.",
      };
    }

    revalidatePath("/dashboard");
    revalidatePath("/fitness");
    return { ok: true, id: body.id };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}
