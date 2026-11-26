import "server-only";
import { apiBaseURL, apiFetch } from "@/lib/api/server-api";
import APIKeys from "@/features/profile/components/api-keys";
import type { APIKey } from "@/features/profile/types/profile-types";

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
