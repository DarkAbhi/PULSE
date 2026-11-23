"use client";

import { useCurrency } from "@/app/components/currency-provider";
import { CircleDollarSign } from "lucide-react";
import { CURRENCIES, currencyLabel, currencySymbol, formatMoney } from "@/app/lib/currency.mjs";
import { saveCurrencyAction } from "@/app/profile/currency-actions";
import PreferenceSettings from "@/app/profile/preference-settings";

export default function CurrencySettings() {
  const { currency, setCurrency } = useCurrency();
  const currencyName = new Intl.DisplayNames(["en"], { type: "currency" }).of(currency) ?? currency;

  return <PreferenceSettings id="currency" title="Currency" value={currency} icon={CircleDollarSign}
    displayValue={currencyName} detail={`${currency} · ${currencySymbol(currency)} · e.g. ${formatMoney(1234.5, currency, "en-IN")}`} choices={CURRENCIES}
    label={currencyLabel} placeholder="Search by code or name"
    description="One currency applies to all amounts. Changing it relabels existing amounts; it does not convert them."
    save={async value => { const result = await saveCurrencyAction(value); return result.ok ? { ok: true, value: result.currency } : result; }}
    onSaved={setCurrency} />;
}
