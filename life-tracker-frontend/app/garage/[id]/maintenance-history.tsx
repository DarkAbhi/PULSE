"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import LocalDate from "@/app/components/local-date";
import { formatMoney } from "@/app/lib/currency.mjs";
import MaintenanceRecordActions from "./maintenance-record-actions";
import type { MaintenanceRecord } from "./types";
import { workLabel } from "./maintenance-work-fields";
import styles from "./vehicle-detail.module.css";

export default function MaintenanceHistory({ records, vehicleId, currency }: { records: MaintenanceRecord[]; vehicleId: string; currency: string }) {
  const [query, setQuery] = useState("");
  const term = query.trim().toLowerCase();
  const matches = records.filter((record) => !term || record.title.toLowerCase().includes(term) ||
    record.actions?.some((action) => `${workLabel(action.component)} ${workLabel(action.action)} ${action.description}`.toLowerCase().includes(term)));
  const cellLabel = `${styles.label} ${styles.cellLabel} sr-only block`;

  return (
    <>
      <label className={`${styles.search} relative block`}>
        <span className="sr-only">Search maintenance records</span>
        <Search className={`${styles.muted} pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2`} aria-hidden="true" />
        <input type="search" className={styles.field} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search component or work…" />
      </label>
      {matches.length === 0 ? (
        <p className={styles.empty}>{term ? "No matching maintenance records." : "No maintenance or expense records yet."}</p>
      ) : (
        <div className={`${styles.recordList} ${styles.maintenanceHistory}`}>
          <div className={styles.rowHeader} aria-hidden="true">
            <span>Work done</span><span>Category</span><span>Date and time</span><span>Odometer</span><span>Amount</span><span className={styles.rowActions} />
          </div>
          {matches.map((record) => (
            <article key={record.id}>
              <div className={styles.row}>
                <div className={styles.rowMain}>
                  <h3 className="font-bold">{record.title}</h3>
                  {record.provider_name && <p className={`${styles.muted} mt-1 text-sm`}>{record.provider_name}</p>}
                </div>
                <div>
                  <span className={cellLabel}>Category</span>
                  <span className={`${styles.chip} capitalize`} data-category={record.category}>{record.category}</span>
                </div>
                <div className="text-sm tabular-nums"><span className={cellLabel}>Date and time</span><LocalDate dateString={record.occurred_at} /></div>
                <div className="tabular-nums"><span className={cellLabel}>Odometer</span>{record.odometer_km == null ? "—" : `${record.odometer_km.toLocaleString("en-IN")} km`}</div>
                <div className="font-extrabold tabular-nums"><span className={cellLabel}>Amount</span>{formatMoney(record.amount, currency)}</div>
                <div className={styles.rowActions}><MaintenanceRecordActions vehicleId={vehicleId} recordId={record.id} /></div>
              </div>
              {(record.notes || !!record.actions?.length) && (
                <div className={`${styles.recordDetails} grid gap-2 text-sm`}>
                  {record.notes && <p className={`${styles.muted} whitespace-pre-wrap wrap-anywhere`}>{record.notes}</p>}
                  {!!record.actions?.length && (
                    <ul className="grid gap-2" aria-label="Work performed">
                      {record.actions.map((action) => (
                        <li key={`${action.component}:${action.action}`} className="wrap-anywhere">
                          <p className="font-semibold">{workLabel(action.component)} · {workLabel(action.action)}</p>
                          {action.description && <p className={styles.muted}>{action.description}</p>}
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
