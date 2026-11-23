import { getUserProfile } from "@/app/lib/user-profile";
import { DEFAULT_CURRENCY, validCurrency } from "@/app/lib/currency.mjs";

export async function getUserCurrency(): Promise<string> {
  const value = (await getUserProfile().catch(() => null))?.currency;
  return validCurrency(value) && value ? value : DEFAULT_CURRENCY;
}
