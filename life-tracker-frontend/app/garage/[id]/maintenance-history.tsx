"use client";

import { useState } from "react";
import PulseCard from "@/app/components/design-system/pulse-card";
import LocalDate from "@/app/components/local-date";
import { formatMoney } from "@/app/lib/currency.mjs";
import MaintenanceRecordActions from "./maintenance-record-actions";
import { MaintenanceRecord } from "./types";
import { workLabel } from "./maintenance-work-fields";

export default function MaintenanceHistory({ records, vehicleId, currency }: { records: MaintenanceRecord[]; vehicleId: string; currency: string }) {
  const [query, setQuery] = useState("");
  const matches = records.filter((r) => !query.trim() || r.actions?.some((a) => `${workLabel(a.component)} ${workLabel(a.action)} ${a.description}`.toLowerCase().includes(query.trim().toLowerCase())));
  return <>
    <label className="block text-sm font-medium">Search component or work<input className="mt-2 w-full rounded-lg bg-secondary px-3 py-2" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="e.g. clutch cable" /></label>
    {matches.length === 0 && <PulseCard as="p" className="p-5 text-sm text-muted-foreground">{query ? "No matching maintenance records." : "No maintenance or expense records yet."}</PulseCard>}
    {matches.map((record) => <PulseCard as="article" className="p-5" key={record.id}>
      <div className="flex flex-wrap justify-between gap-3"><div><p className="font-semibold">{record.title}</p><p className="mt-1 text-sm text-muted-foreground"><span className="capitalize">{record.category}</span> · <LocalDate dateString={record.occurred_at} />{record.odometer_km != null ? ` · ${record.odometer_km} km` : ""}</p></div><div className="flex items-start gap-4"><p className="font-semibold">{formatMoney(record.amount, currency)}</p><MaintenanceRecordActions vehicleId={vehicleId} recordId={record.id} /></div></div>
      {record.provider_name && <p className="mt-2 text-sm text-muted-foreground">{record.provider_name}</p>}
      {record.notes && <p className="mt-2 text-sm text-muted-foreground">{record.notes}</p>}
      {!!record.actions?.length && <ul className="mt-3 space-y-2 border-t border-divider pt-3" aria-label="Work performed">{record.actions.map((a) => <li key={`${a.component}:${a.action}`}><p className="text-sm font-medium">{workLabel(a.component)} · {workLabel(a.action)}</p>{a.description && <p className="text-sm text-muted-foreground">{a.description}</p>}</li>)}</ul>}
    </PulseCard>)}
  </>;
}
