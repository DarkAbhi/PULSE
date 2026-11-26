import "server-only";
import { cookies } from "next/headers";
import { authenticatedFetch } from "@/lib/api/api-transport";

export const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

export async function apiFetch(url: string, init: RequestInit = {}) {
  const token = (await cookies()).get("life_session")?.value;
  return authenticatedFetch(url, init, token);
}
