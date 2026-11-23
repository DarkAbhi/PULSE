"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import LocalDate from "@/app/components/local-date";
import { formatMoney } from "@/app/lib/currency.mjs";
import MaintenanceRecordActions from "./maintenance-record-actions";
import type { MaintenanceRecord } from "./types";
import { workLabel } from "./maintenance-work-fields";
import { detail } from "./vehicle-detail-classes";

export default function MaintenanceHistory({ records, vehicleId, currency }: { records: MaintenanceRecord[]; vehicleId: string; currency: string }) {
  const [query, setQuery] = useState("");
  const term = query.trim().toLowerCase();
  const matches = records.filter((record) => !term || record.title.toLowerCase().includes(term) ||
    record.actions?.some((action) => `${workLabel(action.component)} ${workLabel(action.action)} ${action.description}`.toLowerCase().includes(term)));

  return (
    <>
      <label className="relative block max-w-[420px]">
        <span className="sr-only">Search maintenance records</span>
        <Search className={`${detail.muted} pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2`} aria-hidden="true" />
        <input type="search" className={`${detail.field} rounded-full bg-(--detail-surface) ps-11 pe-3.5`} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search component or work…" />
      </label>
      {matches.length === 0 ? (
        <p className={detail.empty}>{term ? "No matching maintenance records." : "No maintenance or expense records yet."}</p>
      ) : (
        <div className={`${detail.recordList} [--history-columns:minmax(0,1.5fr)_minmax(0,0.9fr)_minmax(0,1.3fr)_minmax(0,0.8fr)_minmax(0,0.8fr)_auto] [--history-actions:88px]`}>
          <div className={detail.rowHeader} aria-hidden="true">
            <span>Work done</span><span>Category</span><span>Date and time</span><span>Odometer</span><span>Amount</span><span className={detail.rowActions} />
          </div>
          {matches.map((record) => (
            <article key={record.id}>
              <div className={`${detail.row} ${detail.stackedRow}`}>
                <div className={detail.stackAcross}>
                  <h3 className="font-bold">{record.title}</h3>
                  {record.provider_name && <p className={`${detail.muted} mt-1 text-sm`}>{record.provider_name}</p>}
                </div>
                <div>
                  <span className={detail.cellLabel}>Category</span>
                  <span className="inline-flex items-center gap-2 rounded-full bg-(--detail-chip) px-2.5 py-1 text-[0.8rem] font-bold capitalize before:size-2 before:flex-none before:rounded-full before:bg-current data-[category=tyres]:before:bg-[#0f86c9] data-[category=washing]:before:bg-[#19d3f3] data-[category=insurance]:before:bg-[#7b2ce0] data-[category=repair]:before:bg-[#f0603a] data-[category=service]:before:bg-[#12a89a]" data-category={record.category}>{record.category}</span>
                </div>
                <div className="text-sm tabular-nums"><span className={detail.cellLabel}>Date and time</span><LocalDate dateString={record.occurred_at} /></div>
                <div className="tabular-nums"><span className={detail.cellLabel}>Odometer</span>{record.odometer_km == null ? "—" : `${record.odometer_km.toLocaleString("en-IN")} km`}</div>
                <div className="font-extrabold tabular-nums"><span className={detail.cellLabel}>Amount</span>{formatMoney(record.amount, currency)}</div>
                <div className={`${detail.rowActions} ${detail.stackAcross}`}><MaintenanceRecordActions vehicleId={vehicleId} recordId={record.id} /></div>
              </div>
              {(record.notes || !!record.actions?.length) && (
                <div className="mx-(--detail-pad) grid gap-2 pb-3.5 text-sm">
                  {record.notes && <p className={`${detail.muted} whitespace-pre-wrap wrap-anywhere`}>{record.notes}</p>}
                  {!!record.actions?.length && (
                    <ul className="grid gap-2" aria-label="Work performed">
                      {record.actions.map((action) => (
                        <li key={`${action.component}:${action.action}`} className="wrap-anywhere">
                          <p className="font-semibold">{workLabel(action.component)} · {workLabel(action.action)}</p>
                          {action.description && <p className={detail.muted}>{action.description}</p>}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </>
  );
}
