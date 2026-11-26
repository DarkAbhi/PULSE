import { getUserProfile } from "@/lib/user/user-profile";
import { DEFAULT_TIME_ZONE, validTimeZone } from "@/lib/time-zone/time-zone.mjs";

export async function getUserTimeZone(): Promise<string> {
  const value = (await getUserProfile().catch(() => null))?.timezone;
  return validTimeZone(value) && value ? value : DEFAULT_TIME_ZONE;
}
