"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";

const apiBaseURL = process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export interface SaveFuelPayload {
  odometer_km: number;
  filled_at: string;
  station_name: string | null;
  notes: string | null;
  items: {
    fuel_type: string;
    fill_type: string;
    quantity: number | null;
    unit_price: number | null;
    total_cost: number | null;
  }[];
}

export interface SaveMaintenancePayload {
  category: "service" | "repair" | "insurance" | "washing" | "tyres";
  title: string;
  amount: number;
  occurred_at: string;
  odometer_km: number | null;
  provider_name: string | null;
  notes: string | null;
}

import { SaveTirePressurePayload } from "./types";

async function getAuthHeader() {
  const cookieStore = await cookies();
  return {
    Cookie: cookieStore.toString(),
  };
}

export async function deleteFuelFill(vehicleId: string, recordId: number) {
  try {
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/fuel-fillups/${recordId}`, {
      method: "DELETE",
      headers,
    });
    if (!response.ok) {
      return { ok: false, error: "We couldn't delete that record." };
    }
    revalidatePath(`/garage/${vehicleId}`);
    return { ok: true };
  } catch (err) {
    return { ok: false, error: "We couldn't reach the server." };
  }
}

export async function deleteAirFill(vehicleId: string, recordId: number) {
  try {
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/air-fills/${recordId}`, {
      method: "DELETE",
      headers,
    });
    if (!response.ok) {
      return { ok: false, error: "We couldn't delete that record." };
    }
    revalidatePath(`/garage/${vehicleId}`);
    return { ok: true };
  } catch (err) {
    return { ok: false, error: "We couldn't reach the server." };
  }
}

export async function saveFuelFill(vehicleId: string, fuelFillId: number, payload: SaveFuelPayload) {
  try {
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/fuel-fillups/${fuelFillId}`, {
      method: "PUT",
      headers: {
        ...headers,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    
    if (!response.ok) {
      const body = await response.json() as { error?: string };
      return { ok: false, error: body.error ?? "We couldn't save this fuel entry." };
    }
    revalidatePath(`/garage/${vehicleId}`);
    return { ok: true };
  } catch (err) {
    return { ok: false, error: "We couldn't reach the server." };
  }
}

export async function createMaintenanceRecord(vehicleId: string, payload: SaveMaintenancePayload) {
  return saveMaintenanceRecord(vehicleId, payload, "POST");
}

export async function updateMaintenanceRecord(vehicleId: string, recordId: number, payload: SaveMaintenancePayload) {
  return saveMaintenanceRecord(vehicleId, payload, "PUT", recordId);
}

async function saveMaintenanceRecord(vehicleId: string, payload: SaveMaintenancePayload, method: "POST" | "PUT", recordId?: number) {
  try {
    const headers = await getAuthHeader();
    const suffix = recordId ? `/${recordId}` : "";
    const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/maintenance-records${suffix}`, {
      method,
      headers: { ...headers, "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) return { ok: false, error: body.error ?? "We couldn't save that record." };
    revalidatePath(`/garage/${vehicleId}`);
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server." };
  }
}

export async function deleteMaintenanceRecord(vehicleId: string, recordId: number) {
  try {
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/maintenance-records/${recordId}`, { method: "DELETE", headers });
    if (!response.ok) return { ok: false, error: "We couldn't delete that record." };
    revalidatePath(`/garage/${vehicleId}`);
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server." };
  }
}

export async function saveTirePressure(
  vehicleId: string,
  payload: SaveTirePressurePayload
) {
  try {
    const headers = await getAuthHeader();
    const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/tire-pressure`, {
      method: "PUT",
      headers: {
        ...headers,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const body = (await response.json().catch(() => ({}))) as { error?: string };
      return { ok: false, error: body.error ?? "We couldn't save tire pressure values." };
    }

    revalidatePath(`/garage/${vehicleId}`);
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server." };
  }
}

