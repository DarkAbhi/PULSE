import { Info } from "lucide-react";
import { formatMoney } from "@/app/lib/currency.mjs";
import type { FuelEfficiencyStats } from "./types";
import styles from "./vehicle-detail.module.css";

const emptyStats: FuelEfficiencyStats = {
  total_cost: 0,
  total_volume: 0,
  average_km_per_litre: null,
  max_km_per_litre: null,
  min_km_per_litre: null,
  last_km_per_litre: null,
};

const formatNumber = (value: number | null, decimals: number) =>
  value === null
    ? "—"
    : value.toLocaleString("en-IN", {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
      });

export default function FuelEfficiencySection({
  stats, currency,
}: {
  stats: Record<string, FuelEfficiencyStats>;
  currency: string;
}) {
  const entries = Object.entries(stats).sort(([a], [b]) => a.localeCompare(b));
  const groups = entries.length > 0 ? entries : [["", emptyStats] as const];

  return (
    <section aria-labelledby="fuel-efficiency-heading" className={styles.section}>
      <h3 id="fuel-efficiency-heading" className={styles.subheading}>Fuel economy</h3>
      {groups.map(([fuelType, summary]) => {
        const readings = [
          { label: "Average", value: formatNumber(summary.average_km_per_litre, 3), unit: "km/L" },
          { label: "Latest", value: formatNumber(summary.last_km_per_litre, 3), unit: "km/L" },
          { label: "Best", value: formatNumber(summary.max_km_per_litre, 3), unit: "km/L" },
          { label: "Lowest", value: formatNumber(summary.min_km_per_litre, 3), unit: "km/L" },
          { label: "Fuel used", value: formatNumber(summary.total_volume, 3), unit: "L" },
          { label: "Total spent", value: formatMoney(summary.total_cost, currency) },
        ];
        return (
          <div className="grid gap-3" key={fuelType}>
            {fuelType && (
              <h4 className={styles.label}>{fuelType}</h4>
            )}
            <dl className={styles.tiles}>
              {readings.map(({ label, value, unit }) => (
                <div className={`${styles.card} ${styles.stat} grid content-start gap-2`} key={label}>
                  <dt className={styles.label}>{label}</dt>
                  <dd className={`${styles.metric} tabular-nums`}>
                    {value}
                    {unit && value !== "—" && <small>{unit}</small>}
                  </dd>
                </div>
              ))}
            </dl>
          </div>
        );
      })}
      <div className={styles.note}>
        <Info aria-hidden="true" />
        <p>Add two full tank entries with increasing odometer readings to calculate efficiency.</p>
      </div>
      <details className={`${styles.muted} text-sm`}>
        <summary className="w-fit cursor-pointer rounded py-2 font-bold underline focus-visible:outline-2 focus-visible:outline-ring">
          How fuel economy is calculated
        </summary>
        <p>Efficiency uses complete full-tank intervals, including partial fills. Missed fills reset the calculation. Average is weighted by fuel used, and latest is the most recent complete interval.</p>
      </details>
    </section>
  );
}
