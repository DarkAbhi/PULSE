"use server";

import { apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

export async function saveVehicleAction(
  id: number | string | undefined,
  data: {
    name: string;
    kind: string;
    catalog_id: string;
    make_name: string;
    model_name: string;
    fuel_types: string[];
  },
) {
  try {
    const response = await apiFetch(
      `${apiBaseURL}/api/vehicles${id ? `/${id}` : ""}`,
      {
        method: id ? "PATCH" : "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      },
    );

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return {
        ok: false,
        error: body.error ?? "We couldn't save that vehicle.",
      };
    }

    revalidatePath("/garage");
    if (id) revalidatePath(`/garage/${id}`);
    return { ok: true, vehicle: body };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}

export async function catalogOptionsAction(
  resource: "makes" | "models",
  kind: string,
  make: string,
  q: string,
): Promise<
  | { ok: true; options: { id: string; name: string }[] }
  | { ok: false; error: string }
> {
  if (resource !== "makes" && resource !== "models")
    return { ok: false, error: "Invalid catalog request." };
  const params = new URLSearchParams({ kind, make_slug: make, q });
  try {
    const response = await apiFetch(
      `${apiBaseURL}/api/vehicle-catalog/${resource}?${params}`,
      {
       signal: AbortSignal.timeout(10_000),
        cache: "no-store",
      },
    );
    if (!response.ok)
      return {
        ok: false,
        error: "We couldn't load the vehicle catalog. Please try again.",
      };
    return { ok: true, options: await response.json() };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}

export async function deleteVehicleAction(vehicleID: number | string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/vehicles/${vehicleID}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return {
        ok: false,
        error: "We couldn't delete this vehicle. Please try again.",
      };
    }

    revalidatePath("/garage");
    return { ok: true };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}

export async function markAirFillAction(vehicleID: number, odometerKM: number | null = null) {
  try {
    const response = await apiFetch(
      `${apiBaseURL}/api/vehicles/${vehicleID}/air-fills`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ odometer_km: odometerKM }),
      },
    );

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return {
        ok: false,
        error:
          body.error ?? "We couldn't record the air fill. Please try again.",
      };
    }

    revalidatePath("/garage");
    revalidatePath(`/garage/${vehicleID}`);
    return { ok: true };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}

export async function saveFuelAction(
  vehicleID: number,
  data: {
    odometer_km: number;
    filled_at: string;
    station_name: string | null;
    notes: string | null;
    items: Array<{
      fuel_type: string;
      fill_type: string;
      quantity: number | null;
      unit_price: number | null;
      total_cost: number | null;
    }>;
  },
) {
  try {
    const response = await apiFetch(
      `${apiBaseURL}/api/vehicles/${vehicleID}/fuel-fillups`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      },
    );

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return {
        ok: false,
        error: body.error ?? "We couldn't save this fuel entry.",
      };
    }

    revalidatePath("/garage");
    revalidatePath(`/garage/${vehicleID}`);
    return { ok: true };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}
