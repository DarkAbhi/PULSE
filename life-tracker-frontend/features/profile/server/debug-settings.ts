import "server-only";
import { apiBaseURL, apiFetch } from "@/lib/api/server-api";

export async function getDebugSettingsStatus() {
  const response = await apiFetch(`${apiBaseURL}/api/debug/settings`, {
    signal: AbortSignal.timeout(3_000),
  }).catch(() => null);
  return response?.status;
}
