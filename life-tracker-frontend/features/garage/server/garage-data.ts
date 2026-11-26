import "server-only";
import { apiBaseURL, apiFetch, readJSON } from "@/lib/api/server-api";
import type { Vehicle } from "@/features/garage/types/garage-types";
import type { VehicleHistoryData } from "@/features/garage/types/vehicle-detail-types";

export function loadVehicles() {
  return readJSON<Vehicle[]>("/api/vehicles");
}

/** status is 0 when the backend is unreachable. An OK response with a malformed body throws. */
export async function loadVehicleHistory(id: string): Promise<{ status: number; data: VehicleHistoryData | null }> {
  const response = await apiFetch(`${apiBaseURL}/api/vehicles/${encodeURIComponent(id)}/history`, {
    signal: AbortSignal.timeout(10_000),
  }).catch(() => null);
  if (!response) return { status: 0, data: null };
  return { status: response.status, data: response.ok ? (await response.json()) as VehicleHistoryData : null };
}
