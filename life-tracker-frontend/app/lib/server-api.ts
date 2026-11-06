import "server-only";
import { cookies } from "next/headers";
import { authenticatedFetch } from "./api-transport";

export async function apiFetch(url: string, init: RequestInit = {}) {
  const token = (await cookies()).get("life_session")?.value;
  return authenticatedFetch(url, init, token);
}
