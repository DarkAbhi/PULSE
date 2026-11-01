"use server";

import { apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export async function dismissNotification(notificationID: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/notifications/${notificationID}`, {
      method: "DELETE",
    });
    if (!response.ok) {
      return { ok: false, error: "We couldn't dismiss that notification. Please try again." };
    }
    revalidatePath("/notifications");
    revalidatePath("/dashboard");
    return { ok: true };
  } catch (err) {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

export async function clearAllNotifications() {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/notifications`, {
      method: "DELETE",
    });
    if (!response.ok) {
      return { ok: false, error: "We couldn't clear your notifications. Please try again." };
    }
    revalidatePath("/notifications");
    revalidatePath("/dashboard");
    return { ok: true };
  } catch (err) {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}
