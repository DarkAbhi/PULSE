"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import LocalDate from "@/app/components/local-date";
import PulseCard from "@/app/components/design-system/pulse-card";
import { formatMoney } from "@/app/lib/currency.mjs";
import MaintenanceRecordActions from "@/app/garage/[id]/maintenance-record-actions";
import type { MaintenanceRecord } from "@/app/garage/[id]/types";
import { workLabel } from "@/app/garage/[id]/maintenance-work-fields";
import { detail } from "@/app/garage/[id]/vehicle-detail-classes";

export default function MaintenanceHistory({ records, vehicleId, currency }: { records: MaintenanceRecord[]; vehicleId: string; currency: string }) {
  const [query, setQuery] = useState("");
  const term = query.trim().toLowerCase();
  const matches = records.filter((record) => !term || record.title.toLowerCase().includes(term) ||
    record.actions?.some((action) => `${workLabel(action.component)} ${workLabel(action.action)} ${action.description}`.toLowerCase().includes(term)));

  return (
    <>
      <label className="relative block max-w-104">
        <span className="sr-only">Search maintenance records</span>
        <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2" aria-hidden="true" />
        <input type="search" name="maintenance-search" aria-label="Search maintenance history" autoComplete="off" className={`${detail.field} rounded-full bg-card ps-11 pe-3.5`} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search component or work…" />
      </label>
      {matches.length === 0 ? (
        <p className={detail.empty}>{term ? "No matching maintenance records." : "No maintenance or expense records yet."}</p>
      ) : (
        <PulseCard className={`${detail.recordList} [--history-columns:minmax(0,1.5fr)_minmax(0,0.9fr)_minmax(0,1.3fr)_minmax(0,0.8fr)_minmax(0,0.8fr)_auto] [--history-actions:88px]`}>
          <div className={detail.rowHeader} aria-hidden="true">
            <span>Work done</span><span>Category</span><span>Date and time</span><span>Odometer</span><span>Amount</span><span className={detail.rowActions} />
          </div>
          {matches.map((record) => (
            <article key={record.id}>
              <div className={`${detail.row} ${detail.stackedRow}`}>
                <div className={detail.stackAcross}>
                  <h3 className="font-bold">{record.title}</h3>
                  {record.provider_name && <p className="text-muted-foreground mt-1 text-sm">{record.provider_name}</p>}
                </div>
                <div>
                  <span className={detail.cellLabel}>Category</span>
                  <span className="inline-flex items-center gap-2 rounded-full bg-chip px-2.5 py-1 text-xs font-bold capitalize before:size-2 before:flex-none before:rounded-full before:bg-current data-[category=tyres]:before:bg-garage-service-blue data-[category=washing]:before:bg-brand-cyan data-[category=insurance]:before:bg-garage-service-violet data-[category=repair]:before:bg-garage-service-orange data-[category=service]:before:bg-garage-service-teal" data-category={record.category}>{record.category}</span>
                </div>
                <div className="text-sm tabular-nums"><span className={detail.cellLabel}>Date and time</span><LocalDate dateString={record.occurred_at} /></div>
                <div className="tabular-nums"><span className={detail.cellLabel}>Odometer</span>{record.odometer_km == null ? "—" : `${record.odometer_km.toLocaleString("en-IN")} km`}</div>
                <div className="font-extrabold tabular-nums"><span className={detail.cellLabel}>Amount</span>{formatMoney(record.amount, currency)}</div>
                <div className={`${detail.rowActions} ${detail.stackAcross}`}><MaintenanceRecordActions vehicleId={vehicleId} recordId={record.id} /></div>
              </div>
              {(record.notes || !!record.actions?.length) && (
                <div className="mx-garage-detail grid gap-2 pb-3.5 text-sm">
                  {record.notes && <p className="text-muted-foreground whitespace-pre-wrap wrap-anywhere">{record.notes}</p>}
                  {!!record.actions?.length && (
                    <ul className="grid gap-2" aria-label="Work performed">
                      {record.actions.map((action) => (
                        <li key={`${action.component}:${action.action}`} className="wrap-anywhere">
                          <p className="font-semibold">{workLabel(action.component)} · {workLabel(action.action)}</p>
                          {action.description && <p className="text-muted-foreground">{action.description}</p>}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </article>
          ))}
        </PulseCard>
      )}
    </>
  );
}
