import "server-only";
import { cache } from "react";
import { cookies } from "next/headers";
import { DEFAULT_TIME_ZONE, validTimeZone } from "./time-zone.mjs";

// React cache is scoped to the server render, never shared between accounts.
export const getUserTimeZone = cache(async (): Promise<string> => {
  const cookieStore = await cookies();
  if (!cookieStore.has("life_session")) return DEFAULT_TIME_ZONE;
  const response = await fetch(`${process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080"}/api/profile`, {
    headers: { Cookie: cookieStore.toString() }, cache: "no-store", signal: AbortSignal.timeout(10_000),
  });
  if (response.status === 401) return DEFAULT_TIME_ZONE;
  if (!response.ok) throw new Error("Unable to load your time zone. Please refresh and try again.");
  const profile = await response.json();
  const zone = profile.timezone ?? DEFAULT_TIME_ZONE;
  if (!validTimeZone(zone)) throw new Error("Your saved time zone is invalid.");
  return zone;
});
