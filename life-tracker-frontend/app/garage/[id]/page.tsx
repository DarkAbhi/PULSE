import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { AirFill, FuelFill } from "./types";
import DeleteButton from "./delete-button";
import EditFuelModal from "./edit-fuel-modal";
import LocalDate from "../../components/local-date";
import { ArrowLeft, Fuel, Gauge, Wind } from "lucide-react";

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

interface PageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ edit?: string }>;
}

export default async function VehiclePage({ params, searchParams }: PageProps) {
  const { id } = await params;
  const { edit } = await searchParams;
  
  // Forward cookies from incoming request to backend for auth/session validation
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on the server
  const sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!sessionResponse.ok) {
    redirect("/");
  }

  // Load history on the server
  const response = await fetch(`${apiBaseURL}/api/vehicles/${id}/history`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  
  if (response.status === 404) {
    redirect("/garage");
  }

  if (!response.ok) {
    return (
      <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10">
        <div className="mx-auto max-w-4xl">
          <p className="rounded-xl bg-destructive/10 p-4 text-destructive">
            We couldn't load this vehicle's history.
          </p>
        </div>
      </main>
    );
  }

  const data = await response.json() as {
    vehicle_name: string;
    air_fills: AirFill[];
    fuel_fillups: FuelFill[];
    average_mileage_km_per_litre: Record<string, number>;
  };

  const mileageEntries = Object.entries(data.average_mileage_km_per_litre);

  const editingFill = edit
    ? data.fuel_fillups.find((fill) => fill.id === Number(edit))
    : null;

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10">
      <div className="mx-auto max-w-4xl">
        <Link className="flex items-center gap-1 text-sm font-semibold text-primary hover:opacity-80" href="/garage">
          <ArrowLeft className="h-4 w-4" /> Garage
        </Link>
        <p className="mt-6 text-sm font-semibold tracking-[0.18em] text-primary">
          VEHICLE HISTORY
        </p>
        <h1 className="mt-2 text-3xl font-bold text-foreground">
          {data.vehicle_name}
        </h1>
        <div className="mt-8 grid gap-8 lg:grid-cols-2">
          <section>
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <Fuel className="h-5 w-5 text-primary" /> Fuel fill-ups
            </h2>
            <aside className="mt-4 rounded-2xl border border-primary/20 bg-primary/5 p-5">
              <h3 className="flex items-center gap-2 font-semibold text-foreground">
                <Gauge className="h-5 w-5 text-primary" /> Average mileage
              </h3>
              {mileageEntries.length === 0 ? (
                <p className="mt-2 text-sm text-muted-foreground">
                  Add two full-tank fuel entries to calculate mileage.
                </p>
              ) : (
                <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2">
                  {mileageEntries.map(([fuelType, mileage]) => (
                    <p className="text-sm text-muted-foreground" key={fuelType}>
                      <span className="capitalize">{fuelType}</span>{" "}
                      <span className="font-semibold text-foreground">
                        {mileage.toFixed(1)} km/L
                      </span>
                    </p>
                  ))}
                </div>
              )}
            </aside>
            <div className="mt-4 space-y-3">
              {data.fuel_fillups.length === 0 ? (
                <p className="text-sm text-muted-foreground">No fuel entries yet.</p>
              ) : (
                data.fuel_fillups.map((fill) => (
                  <article
                    className="rounded-2xl bg-card border border-border p-5 shadow-sm"
                    key={fill.id}
                  >
                    <div className="flex justify-between gap-3">
                      <p className="font-semibold">
                        {fill.odometer_km} km ·{" "}
                        <LocalDate dateString={fill.filled_at} />
                      </p>
                      <span className="flex items-center gap-3">
                        <Link
                          className="text-sm font-semibold text-primary hover:opacity-85"
                          href={`/garage/${id}?edit=${fill.id}`}
                        >
                          Edit
                        </Link>
                        <DeleteButton
                          vehicleId={id}
                          recordId={fill.id}
                          kind="fuel-fillups"
                        />
                      </span>
                    </div>
                    {fill.station_name && (
                      <p className="mt-1 text-sm text-muted-foreground">
                        {fill.station_name}
                      </p>
                    )}
                    {fill.items.map((item, index) => (
                      <p className="mt-2 text-sm text-muted-foreground" key={index}>
                        {item.fuel_type} · {item.fill_type} · {item.quantity} L
                        · ₹{item.total_cost}
                      </p>
                    ))}
                  </article>
                ))
              )}
            </div>
          </section>
          <section>
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <Wind className="h-5 w-5 text-primary" /> Air fills
            </h2>
            <div className="mt-4 space-y-3">
              {data.air_fills.map((fill) => (
                <article
                  className="flex items-center justify-between gap-3 rounded-2xl bg-card border border-border p-5 shadow-sm"
                  key={fill.id}
                >
                  <span>
                    Air filled · <LocalDate dateString={fill.filled_at} />
                  </span>
                  <DeleteButton
                    vehicleId={id}
                    recordId={fill.id}
                    kind="air-fills"
                  />
                </article>
              ))}
            </div>
          </section>
        </div>
      </div>
      {editingFill && <EditFuelModal vehicleId={id} fill={editingFill} />}
    </main>
  );
}
