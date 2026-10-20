import { Fuel } from "lucide-react";
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

export default function FuelEfficiencySection({
  stats,
}: {
  stats: Record<string, FuelEfficiencyStats>;
}) {
  const entries = Object.entries(stats).sort(([a], [b]) => a.localeCompare(b));
  const groups = entries.length > 0 ? entries : [["", emptyStats] as const];

  return (
    <section className="mt-8" aria-labelledby="fuel-efficiency-heading">
      <h2 id="fuel-efficiency-heading" className="flex items-center gap-2 text-xl font-semibold">
        <Fuel className="h-5 w-5 text-primary" /> Fuel Efficiency
      </h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Monitoring your fuel economy as you go.
      </p>
      {groups.map(([fuelType, summary]) => {
        const cards = [
          { label: "Total refuelling", value: summary.total_cost, decimals: 2, unit: "INR so far" },
          { label: "Total volume", value: summary.total_volume, decimals: 3, unit: fuelType ? `L · ${fuelType}` : "L" },
          { label: "Avg fuel efficiency", value: summary.average_km_per_litre, decimals: 3, unit: "km/L" },
          { label: "Max fuel efficiency", value: summary.max_km_per_litre, decimals: 3, unit: "km/L" },
          { label: "Min fuel efficiency", value: summary.min_km_per_litre, decimals: 3, unit: "km/L" },
          { label: "Last fuel efficiency", value: summary.last_km_per_litre, decimals: 3, unit: "km/L" },
        ];
        return (
          <div className="mt-4" key={fuelType}>
            {entries.length > 1 && (
              <h3 className="mb-3 font-semibold capitalize">{fuelType}</h3>
            )}
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {cards.map((card) => (
                <div className="rounded-2xl border border-border bg-card px-4 py-6 text-center shadow-sm" key={card.label}>
                  <dt className="text-sm text-foreground">{card.label}</dt>
                  <dd className="mt-3 text-3xl font-bold tabular-nums tracking-tight">
                    {formatNumber(card.value, card.decimals)}
                    <span className="mt-2 block text-sm font-normal tracking-normal text-muted-foreground capitalize">
                      {card.unit}
                    </span>
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
      <p className="mt-3 text-xs text-muted-foreground">
        Efficiency uses complete full-tank intervals, including partial fills. Missed fills reset the calculation. Average is weighted by fuel used; last is the latest complete interval.
      </p>
    </section>
  );
}
