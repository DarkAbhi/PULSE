"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Button from "../components/design-system/button";
import { useTimeZone } from "../components/time-zone-provider";
import { timeZoneChoices } from "../lib/time-zone.mjs";
import { saveTimeZoneAction } from "./time-zone-actions";

export default function TimeZoneSettings() {
  const { timeZone, setTimeZone } = useTimeZone();
  const [draft, setDraft] = useState(timeZone);
  const [search, setSearch] = useState("");
  const [zones, setZones] = useState([timeZone]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();
  useEffect(() => {
    setDraft(timeZone);
    setZones(timeZoneChoices(timeZone));
  }, [timeZone]);
  const normalize = (value: string) => value.replaceAll("_", " ").toLowerCase();
  const matches = zones.filter(zone => normalize(zone).includes(normalize(search.trim())));

  return <section aria-labelledby="time-zone-heading" className="mt-6 w-full rounded-2xl border border-border bg-card p-6 text-left shadow-sm sm:p-8">
    <h2 id="time-zone-heading" className="text-lg font-semibold">Time zone</h2>
    <p className="mt-2 text-sm text-muted-foreground">{timeZone.replaceAll("_", " ")}</p>
    <form className="mt-5 space-y-4" onSubmit={event => {
      event.preventDefault(); setError(""); setMessage("");
      startTransition(async () => {
        const result = await saveTimeZoneAction(draft);
        if (!result.ok) { setError(result.error); return; }
        setTimeZone(result.timezone); setSearch(""); setMessage("Time zone saved."); router.refresh();
      });
    }}>
      <div><label htmlFor="time-zone-search" className="text-sm font-medium">Search time zones</label>
        <input id="time-zone-search" type="search" value={search} disabled={pending} onChange={event => setSearch(event.target.value)} placeholder="Search by city or region" className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2" />
      </div>
      <div><label htmlFor="time-zone-choice" className="text-sm font-medium">Choose a time zone</label>
        <select id="time-zone-choice" size={6} value={matches.includes(draft) ? draft : ""} disabled={pending || !matches.length} onChange={event => { setDraft(event.target.value); setError(""); setMessage(""); }} className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2">
          {!matches.length ? <option value="">No matching time zones</option> : <><option value="" disabled>Select a time zone</option>{matches.map(zone => <option key={zone} value={zone}>{zone.replaceAll("_", " ")}</option>)}</>}
        </select>
      </div>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      {message && <p role="status" className="text-sm text-success">{message}</p>}
      <Button type="submit" disabled={pending || draft === timeZone || !matches.includes(draft)}>{pending ? "Saving…" : "Save time zone"}</Button>
    </form>
  </section>;
}
