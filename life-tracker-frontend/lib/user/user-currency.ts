import { getUserProfile } from "@/lib/user/user-profile";
import { DEFAULT_CURRENCY, validCurrency } from "@/lib/currency/currency.mjs";

export async function getUserCurrency(): Promise<string> {
  const value = (await getUserProfile().catch(() => null))?.currency;
  return validCurrency(value) && value ? value : DEFAULT_CURRENCY;
}
