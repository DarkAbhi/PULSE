"use server";

import { apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

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

export async function dismissNotificationAction(notificationID: number) {
  try {
    const response = await apiFetch(
      `${apiBaseURL}/api/notifications/${notificationID}`,
      {
        method: "DELETE",
      },
    );

    if (!response.ok) {
      return {
        ok: false,
        error: "We couldn't dismiss that notification. Please try again.",
      };
    }

    revalidatePath("/dashboard");
    revalidatePath("/notifications");
    return { ok: true };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}
