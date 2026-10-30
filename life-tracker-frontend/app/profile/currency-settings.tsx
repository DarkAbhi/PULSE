"use client";

import { useCurrency } from "../components/currency-provider";
import { CURRENCIES, currencyLabel } from "../lib/currency.mjs";
import { saveCurrencyAction } from "./currency-actions";
import PreferenceSettings from "./preference-settings";

export default function CurrencySettings() {
  const { currency, setCurrency } = useCurrency();

  return <PreferenceSettings id="currency" title="Currency" value={currency} choices={CURRENCIES}
    label={currencyLabel} placeholder="Search by code or name"
    description="One currency applies to all amounts. Changing it relabels existing amounts; it does not convert them."
    save={async value => { const result = await saveCurrencyAction(value); return result.ok ? { ok: true, value: result.currency } : result; }}
    onSaved={setCurrency} />;
}
