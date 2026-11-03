import PulseCard from "@/app/components/design-system/pulse-card";
import { Gauge } from "lucide-react";
import { formatMoney } from "@/app/lib/currency.mjs";
import { getUserCurrency } from "@/app/lib/user-currency";
import { FuelEfficiencyStats } from "./types";

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

export default async function FuelEfficiencySection({
  stats,
}: {
  stats: Record<string, FuelEfficiencyStats>;
}) {
  const currency = await getUserCurrency();
  const entries = Object.entries(stats).sort(([a], [b]) => a.localeCompare(b));
  const groups = entries.length > 0 ? entries : [["", emptyStats] as const];

  return (
    <PulseCard as="section" className="mt-4 px-4 py-5 sm:px-5" aria-labelledby="fuel-efficiency-heading">
      <h3 id="fuel-efficiency-heading" className="flex items-center gap-2 font-semibold">
        <Gauge className="size-5 text-action" aria-hidden="true" /> Fuel economy
      </h3>
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
          <div className="mt-3" key={fuelType}>
            {fuelType && (
              <h4 className="mb-2 text-sm font-semibold capitalize">{fuelType}</h4>
            )}
            <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-divider bg-divider sm:grid-cols-3">
              {readings.map(({ label, value, unit }) => (
                <div className={`min-w-0 px-3 py-2.5 ${label === "Average" ? "bg-accent text-accent-foreground" : "bg-card"}`} key={label}>
                  <dt className={`text-xs ${label === "Average" ? "font-semibold" : "text-muted-foreground"}`}>{label}</dt>
                  <dd className="mt-0.5 break-words text-lg font-semibold tabular-nums tracking-tight">
                    {value}
                    {unit && value !== "—" && <span className="ml-1 text-xs font-normal tracking-normal">{unit}</span>}
                  </dd>
                </div>
              ))}
            </dl>
            {summary.average_km_per_litre === null && (
              <p className="mt-3 text-sm text-muted-foreground">
                Add two full-tank entries{fuelType ? ` for ${fuelType}` : ""} with increasing odometer readings to calculate efficiency.
              </p>
            )}
          </div>
        );
      })}
      <details className="mt-2 text-xs text-muted-foreground">
        <summary className="w-fit cursor-pointer rounded py-2 font-medium text-action focus-visible:outline-2 focus-visible:outline-ring">
          How fuel economy is calculated
        </summary>
        <p>Efficiency uses complete full-tank intervals, including partial fills. Missed fills reset the calculation. Average is weighted by fuel used, and latest is the most recent complete interval.</p>
      </details>
    </PulseCard>
  );
}
