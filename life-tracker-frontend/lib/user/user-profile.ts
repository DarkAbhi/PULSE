import "server-only";
import { cache } from "react";
import { cookies } from "next/headers";
import { apiBaseURL, apiFetch } from "@/lib/api/server-api";

interface UserProfile {
  name?: string;
  has_profile?: boolean;
  currency?: string;
  timezone?: string;
}

// Request-scoped: layouts and feature loaders can share the same authenticated read.
export const getUserProfile = cache(async (): Promise<UserProfile | null> => {
  if (!(await cookies()).get("life_session")?.value) return null;

  const response = await apiFetch(`${apiBaseURL}/api/profile`, {
    signal: AbortSignal.timeout(10_000),
  });
  if (response.status === 401) return null;
  if (!response.ok) throw new Error("We couldn't load your profile.");
  return response.json();
});
