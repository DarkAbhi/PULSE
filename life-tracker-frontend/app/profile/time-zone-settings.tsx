"use client";

import { useEffect, useState } from "react";
import { Globe } from "lucide-react";
import { useTimeZone } from "@/app/components/profile-preferences-provider";
import { formatInstant, timeZoneChoices } from "@/app/lib/time-zone.mjs";
import { saveTimeZoneAction } from "@/app/profile/time-zone-actions";
import PreferenceSettings from "@/app/profile/preference-settings";

export default function TimeZoneSettings() {
  const { timeZone, setTimeZone } = useTimeZone();
  const [zones, setZones] = useState([timeZone]);
  const [now, setNow] = useState<number>();
  useEffect(() => { setZones(timeZoneChoices(timeZone)); }, [timeZone]);
  useEffect(() => {
    const update = () => setNow(Date.now());
    update();
    const timer = window.setInterval(update, 15000);
    return () => window.clearInterval(timer);
  }, []);

  const offset = now === undefined ? undefined : new Intl.DateTimeFormat("en", {
    timeZone, timeZoneName: "shortOffset",
  }).formatToParts(now).find(part => part.type === "timeZoneName")?.value.replace("GMT", "UTC");
  const detail = now === undefined ? undefined : `${offset} · local time ${formatInstant(now, timeZone, { timeStyle: "short" })}`;

  return <PreferenceSettings id="time-zone" title="Time zone" value={timeZone} icon={Globe}
    displayValue={timeZone} detail={detail} choices={zones}
    label={zone => zone.replaceAll("_", " ")} placeholder="Search by city or region"
    save={async value => { const result = await saveTimeZoneAction(value); return result.ok ? { ok: true, value: result.timezone } : result; }}
    onSaved={setTimeZone} />;
}
