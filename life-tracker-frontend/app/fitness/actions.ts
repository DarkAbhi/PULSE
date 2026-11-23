"use server";

import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";


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
