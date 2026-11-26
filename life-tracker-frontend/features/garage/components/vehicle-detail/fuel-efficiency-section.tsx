import { Info } from "lucide-react";
import PulseCard from "@/components/ui/pulse-card";
import { formatMoney } from "@/lib/currency/currency.mjs";
import type { FuelEfficiencyStats } from "@/features/garage/types/vehicle-detail-types";
import { detail } from "@/features/garage/styles/vehicle-detail-classes";

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
    <section aria-labelledby="fuel-efficiency-heading" className={detail.section}>
      <h3 id="fuel-efficiency-heading" className={detail.subheading}>Fuel economy</h3>
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
              <h4 className={detail.label}>{fuelType}</h4>
            )}
            <dl className={detail.tiles}>
              {readings.map(({ label, value, unit }) => (
                <PulseCard className={`${detail.card} grid min-h-29 content-start gap-2`} key={label}>
                  <dt className={detail.label}>{label}</dt>
                  <dd className={`${detail.metric} tabular-nums`}>
                    {value}
                    {unit && value !== "—" && <small className={detail.metricUnit}>{unit}</small>}
                  </dd>
                </PulseCard>
              ))}
            </dl>
          </div>
        );
      })}
      <div className="flex items-start gap-3 rounded-2xl bg-muted px-4 py-3.5 text-sm leading-normal text-muted-foreground">
        <Info className="mt-px size-5 flex-none text-action" aria-hidden="true" />
        <p>Add two full tank entries with increasing odometer readings to calculate efficiency.</p>
      </div>
      <details className="text-muted-foreground text-sm">
        <summary className="w-fit cursor-pointer rounded py-2 font-bold underline focus-visible:outline-2 focus-visible:outline-ring">
          How fuel economy is calculated
        </summary>
        <p>Efficiency uses complete full-tank intervals, including partial fills. Missed fills reset the calculation. Average is weighted by fuel used, and latest is the most recent complete interval.</p>
      </details>
    </section>
  );
}
