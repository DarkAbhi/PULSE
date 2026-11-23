"use server";

import { apiFetch, apiBaseURL } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

export async function saveNameAction(name: string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/profile`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Unable to save your name." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/profile");
    return { ok: true, name: body.name };
  } catch {
    return {
      ok: false,
      error: "Unable to reach the server. Please try again.",
    };
  }
}
