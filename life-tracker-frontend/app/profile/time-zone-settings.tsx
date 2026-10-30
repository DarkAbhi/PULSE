"use client";

import { useEffect, useState } from "react";
import { useTimeZone } from "../components/time-zone-provider";
import { timeZoneChoices } from "../lib/time-zone.mjs";
import { saveTimeZoneAction } from "./time-zone-actions";
import PreferenceSettings from "./preference-settings";

export default function TimeZoneSettings() {
  const { timeZone, setTimeZone } = useTimeZone();
  const [zones, setZones] = useState([timeZone]);
  useEffect(() => { setZones(timeZoneChoices(timeZone)); }, [timeZone]);

  return <PreferenceSettings id="time-zone" title="Time zone" value={timeZone} choices={zones}
    label={zone => zone.replaceAll("_", " ")} placeholder="Search by city or region"
    save={async value => { const result = await saveTimeZoneAction(value); return result.ok ? { ok: true, value: result.timezone } : result; }}
    onSaved={setTimeZone} />;
}
