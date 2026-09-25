"use server";

import { revalidatePath } from "next/cache";
import { cookies } from "next/headers";

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

export async function addVehicleAction(name: string) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(`${apiBaseURL}/api/vehicles`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Cookie: cookieHeader,
      },
      body: JSON.stringify({ name }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "We couldn't add that vehicle." };
    }

    revalidatePath("/garage");
    return { ok: true, vehicle: body };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

export async function deleteVehicleAction(vehicleID: number | string) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleID}`, {
      method: "DELETE",
      headers: {
        Cookie: cookieHeader,
      },
    });

    if (!response.ok) {
      return { ok: false, error: "We couldn't delete this vehicle. Please try again." };
    }

    revalidatePath("/garage");
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

export async function markAirFillAction(vehicleID: number) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(
      `${apiBaseURL}/api/vehicles/${vehicleID}/air-fills`,
      {
        method: "POST",
        headers: {
          Cookie: cookieHeader,
        },
      }
    );

    const body = await response.json().catch(() => ({}));
    if (!response.ok || !body.filled_at) {
      return {
        ok: false,
        error: body.error ?? "We couldn't record the air fill. Please try again.",
      };
    }

    revalidatePath("/garage");
    return { ok: true, filled_at: body.filled_at };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
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
  }
) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const response = await fetch(
      `${apiBaseURL}/api/vehicles/${vehicleID}/fuel-fillups`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Cookie: cookieHeader,
        },
        body: JSON.stringify(data),
      }
    );

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "We couldn't save this fuel entry." };
    }

    revalidatePath("/garage");
    revalidatePath(`/garage/${vehicleID}`);
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}
