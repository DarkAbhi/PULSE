import { apiFetch } from "@/app/lib/server-api";
import PulseCard from "@/app/components/design-system/pulse-card";
import { redirect } from "next/navigation";
import Link from "next/link";
import BackLink from "@/app/components/design-system/back-link";
import { CarFront } from "lucide-react";
import AddVehicleButton from "./add-vehicle-button";
import VehiclesList from "./vehicles-list";

export const metadata = {
  title: "Garage | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

type Vehicle = {
  id: number;
  name: string;
  kind: string;
  make_name: string;
  model_name: string;
  fuel_types: string[];
};

export default async function GaragePage() {
  // Validate session on the server
  let sessionResponse: Response | null = null;
  let error = "";
  try {
    sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`, {
      signal: AbortSignal.timeout(10_000),
    });
  } catch {
    error = "We couldn't reach the server. Please try again.";
  }
  if (sessionResponse && !sessionResponse.ok) {
    redirect("/");
  }

  let vehicles: Vehicle[] = [];
  if (!error) {
    try {
      const vehiclesResponse = await apiFetch(`${apiBaseURL}/api/vehicles`, {
        signal: AbortSignal.timeout(10_000),
      });
      if (!vehiclesResponse.ok) {
        error = "We couldn't load your vehicles. Please try again.";
      } else {
        vehicles = (await vehiclesResponse.json()) as Vehicle[];
      }
    } catch {
      error = "We couldn't reach the server. Please try again.";
    }
  }

  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-8 sm:py-10">
      <div className="mx-auto max-w-5xl">
        <BackLink href="/dashboard">Dashboard</BackLink>

        <PulseCard as="section" aria-labelledby="garage-title"
          className="relative isolate mt-3 overflow-hidden bg-secondary px-6 py-8 sm:px-10 sm:py-12"
        >
          <div aria-hidden="true" className="pointer-events-none absolute inset-y-0 right-12 hidden w-44 -skew-x-12 bg-action/5 items-center justify-center md:flex lg:right-24 lg:w-56">
            <CarFront className="size-24 skew-x-12 text-action/40" strokeWidth={1} />
          </div>
          <div className="relative max-w-xs">
            <h1 id="garage-title" className="text-5xl font-bold tracking-tight sm:text-6xl">
              Garage
            </h1>
            <p className="mt-3 text-sm leading-relaxed text-muted-foreground sm:text-base">
              Manage your vehicles and log fuel and tire air.
            </p>
          </div>
        </PulseCard>

        <section aria-labelledby="vehicles-title" className="mt-8 sm:mt-10">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <h2 id="vehicles-title" className="text-xl font-semibold tracking-tight">Your vehicles</h2>
              {!error && (
                <span className="rounded-full bg-secondary px-2.5 py-0.5 text-sm font-medium tabular-nums text-secondary-foreground">
                  <span className="sr-only">Total vehicles: </span>{vehicles.length}
                </span>
              )}
            </div>
            <div className="shrink-0"><AddVehicleButton /></div>
          </div>

          {error ? (
            <PulseCard as="p" className="bg-destructive-soft px-6 py-5 text-sm text-destructive" role="alert">
              {error}
            </PulseCard>
          ) : vehicles.length === 0 ? (
            <PulseCard className="px-6 py-8 sm:px-8">
              <p className="font-semibold">Your garage is empty</p>
              <p className="mt-1 text-sm text-muted-foreground">
                Add your first vehicle to start logging fuel and tire air.
              </p>
            </PulseCard>
          ) : (
            <VehiclesList vehicles={vehicles} />
          )}
        </section>

        <footer className="mt-6 flex flex-wrap items-center gap-x-4 gap-y-2 px-1 text-xs text-muted-foreground">
          <span>Vehicle data by <a className="text-action underline" href="https://vehiclesdb.com">VehiclesDB</a></span>
          <Link className="text-action underline" href="/vehicle-data">Sources and credits</Link>
        </footer>
      </div>
    </main>
  );
}
