import { getUserProfile } from "@/app/lib/user-profile";
import { DEFAULT_TIME_ZONE, validTimeZone } from "@/app/lib/time-zone.mjs";

export async function getUserTimeZone(): Promise<string> {
  const value = (await getUserProfile().catch(() => null))?.timezone;
  return validTimeZone(value) && value ? value : DEFAULT_TIME_ZONE;
}
