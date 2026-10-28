"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Button from "../components/design-system/button";
import { useCurrency } from "../components/currency-provider";
import { CURRENCIES, currencyLabel } from "../lib/currency.mjs";
import { saveCurrencyAction } from "./currency-actions";

export default function CurrencySettings() {
  const { currency, setCurrency } = useCurrency();
  const [draft, setDraft] = useState(currency);
  const [search, setSearch] = useState("");
  const [zones, setZones] = useState([currency]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();
  useEffect(() => {
    setDraft(currency);
    setZones([...CURRENCIES]);
  }, [currency]);
  const normalize = (value: string) => value.replaceAll("_", " ").toLowerCase();
  const matches = zones.filter(zone => normalize(currencyLabel(zone)).includes(normalize(search.trim())));

  return <section aria-labelledby="currency-heading" className="mt-6 w-full rounded-2xl border border-border bg-card p-6 text-left shadow-sm sm:p-8">
    <h2 id="currency-heading" className="text-lg font-semibold">Currency</h2>
    <p className="mt-2 text-sm text-muted-foreground">{currencyLabel(currency)}</p>
    <p className="mt-2 text-sm text-muted-foreground">One currency applies to all amounts. Changing it relabels existing amounts; it does not convert them.</p>
    <form className="mt-5 space-y-4" onSubmit={event => {
      event.preventDefault(); setError(""); setMessage("");
      startTransition(async () => {
        const result = await saveCurrencyAction(draft);
        if (!result.ok) { setError(result.error); return; }
        setCurrency(result.currency); setSearch(""); setMessage("Currency saved."); router.refresh();
      });
    }}>
      <div><label htmlFor="currency-search" className="text-sm font-medium">Search currencies</label>
        <input id="currency-search" type="search" value={search} disabled={pending} onChange={event => setSearch(event.target.value)} placeholder="Search by code or name" className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2" />
      </div>
      <div><label htmlFor="currency-choice" className="text-sm font-medium">Choose a currency</label>
        <select id="currency-choice" size={6} value={matches.includes(draft) ? draft : ""} disabled={pending || !matches.length} onChange={event => { setDraft(event.target.value); setError(""); setMessage(""); }} className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2">
          {!matches.length ? <option value="">No matching currencies</option> : <><option value="" disabled>Select a currency</option>{matches.map(zone => <option key={zone} value={zone}>{currencyLabel(zone)}</option>)}</>}
        </select>
      </div>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      {message && <p role="status" className="text-sm text-success">{message}</p>}
      <Button type="submit" disabled={pending || draft === currency || !matches.includes(draft)}>{pending ? "Saving…" : "Save currency"}</Button>
    </form>
  </section>;
}
