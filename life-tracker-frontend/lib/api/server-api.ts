import "server-only";
import { cookies } from "next/headers";
import { authenticatedFetch } from "@/lib/api/api-transport";

export const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

export async function apiFetch(url: string, init: RequestInit = {}) {
  const token = (await cookies()).get("life_session")?.value;
  return authenticatedFetch(url, init, token);
}

export interface BackendResource<T> {
  /** HTTP status, or 0 when the backend could not be reached. */
  status: number;
  /** Parsed body of an OK response; null otherwise or when the body is not JSON. */
  data: T | null;
}

/** GETs a JSON resource from the backend with the session cookie. Never throws. */
export async function readJSON<T>(path: string, timeoutMs = 10_000): Promise<BackendResource<T>> {
  const response = await apiFetch(`${apiBaseURL}${path}`, { signal: AbortSignal.timeout(timeoutMs) }).catch(() => null);
  if (!response) return { status: 0, data: null };
  const data = response.ok ? ((await response.json().catch(() => null)) as T | null) : null;
  return { status: response.status, data };
}
