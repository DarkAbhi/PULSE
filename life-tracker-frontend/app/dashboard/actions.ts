"use server";

import { revalidatePath } from "next/cache";
import { cookies } from "next/headers";

const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

export async function saveNameAction(name: string) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(`${apiBaseURL}/api/profile`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Cookie: cookieHeader,
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

export async function markGymVisitAction(date?: string) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(`${apiBaseURL}/api/${date ? "gym-visits" : "workout/today"}`, {
      method: "POST",
      headers: {
        Cookie: cookieHeader,
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

export async function dismissNotificationAction(notificationID: number) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(
      `${apiBaseURL}/api/notifications/${notificationID}`,
      {
        method: "DELETE",
        headers: {
          Cookie: cookieHeader,
        },
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
