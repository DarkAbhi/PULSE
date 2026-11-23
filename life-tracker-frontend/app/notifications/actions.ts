"use server";

import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { refresh, revalidatePath } from "next/cache";


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
    refresh();
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
