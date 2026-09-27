"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

async function getAuthHeader() {
  const cookieStore = await cookies();
  return {
    Cookie: cookieStore.toString(),
  };
}

export async function dismissNotification(notificationID: number) {
  try {
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/notifications/${notificationID}`, {
      method: "DELETE",
      headers,
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

export async function markGymVisited(notificationID: number) {
  try {
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/notifications/${notificationID}/gym-visit`, {
      method: "POST",
      headers,
    });
    if (!response.ok) {
      return { ok: false, error: "We couldn't save that gym visit. Please try again." };
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
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/notifications`, {
      method: "DELETE",
      headers,
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
