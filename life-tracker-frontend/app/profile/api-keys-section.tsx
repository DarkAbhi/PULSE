import "server-only";
import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import APIKeys from "@/app/profile/api-keys";
import type { APIKey } from "@/app/profile/types";

async function loadKeys(): Promise<APIKey[] | null> {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/profile/api-keys`, { signal: AbortSignal.timeout(10_000) });
    if (!response.ok) return null;

    const keys: APIKey[] = await response.json();
    return keys.map(({ id, name, token_prefix, created_at }) => ({ id, name, token_prefix, created_at }));
  } catch {
    return null;
  }
}

export default async function APIKeysSection() {
  return <APIKeys initialKeys={await loadKeys()} />;
}
