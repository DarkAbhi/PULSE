"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { validCurrency } from "@/app/lib/currency.mjs";

export async function saveCurrencyAction(currency: string): Promise<{ ok: true; currency: string } | { ok: false; error: string }> {
  if (!validCurrency(currency)) return { ok: false, error: "Choose a valid currency." };
  try {
    const response = await fetch(`${process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080"}/api/profile/currency`, {
      method: "PUT", headers: { "Content-Type": "application/json", Cookie: (await cookies()).toString() },
      body: JSON.stringify({ currency }), cache: "no-store",
    });
    if (!response.ok) return { ok: false, error: response.status === 401 ? "Your session expired. Please sign in again." : "We couldn't save your currency. Please try again." };
    const body = await response.json();
    if (body.currency !== currency) throw new Error("Unexpected currency response");
    revalidatePath("/", "layout");
    return { ok: true, currency: body.currency };
  } catch {
    return { ok: false, error: "We couldn't confirm the change. Refresh your profile before trying again." };
  }
}
