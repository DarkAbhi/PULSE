import "server-only";
import { cache } from "react";
import { cookies } from "next/headers";
import { apiFetch, apiBaseURL } from "@/lib/api/server-api";

interface Session {
  username: string;
}

export const getSession = cache(async (): Promise<Session | null> => {
  if (!(await cookies()).get("life_session")?.value) return null;

  try {
    const response = await apiFetch(`${apiBaseURL}/api/auth/session`, {
      signal: AbortSignal.timeout(10_000),
    });
    if (!response.ok) return null;

    const session = await response.json();
    return typeof session?.username === "string" ? { username: session.username } : null;
  } catch {
    return null;
  }
});
