"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { validTimeZone } from "../lib/time-zone.mjs";

export async function saveTimeZoneAction(timezone: string): Promise<{ ok: true; timezone: string } | { ok: false; error: string }> {
  if (!validTimeZone(timezone)) return { ok: false, error: "Choose a valid time zone." };
  try {
    const response = await fetch(`${process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080"}/api/profile/timezone`, {
      method: "PUT", headers: { "Content-Type": "application/json", Cookie: (await cookies()).toString() },
      body: JSON.stringify({ timezone }), cache: "no-store",
    });
    if (!response.ok) return { ok: false, error: response.status === 401 ? "Your session expired. Please sign in again." : "We couldn't save your time zone. Please try again." };
    const body = await response.json();
    if (body.timezone !== timezone) throw new Error("Unexpected time zone response");
    revalidatePath("/", "layout");
    return { ok: true, timezone: body.timezone };
  } catch {
    return { ok: false, error: "We couldn't confirm the change. Refresh your profile before trying again." };
  }
}
