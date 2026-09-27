import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import AddVehicleButton from "./add-vehicle-button";
import VehiclesList from "./vehicles-list";

export const metadata = {
  title: "Garage | Life Tracker",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

type Vehicle = {
  id: number;
  name: string;
};

type AirFill = {
  vehicle_id: number;
  filled_at: string;
};

export default async function GaragePage() {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on the server
  let sessionResponse: Response | null = null;
  let error = "";
  try {
    sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
      headers: { Cookie: cookieHeader }, signal: AbortSignal.timeout(10_000),
    });
  } catch {
    error = "We couldn't reach the server. Please try again.";
  }
  if (sessionResponse && !sessionResponse.ok) {
    redirect("/");
  }

  let vehicles: Vehicle[] = [];
  let latestAirFills: Record<number, string> = {};
  if (!error) {
    try {
      const vehiclesResponse = await fetch(`${apiBaseURL}/api/vehicles`, {
        headers: {
          Cookie: cookieHeader,
        },
        signal: AbortSignal.timeout(10_000),
      });
      if (!vehiclesResponse.ok) {
        error = "We couldn't load your vehicles. Please try again.";
      } else {
        vehicles = (await vehiclesResponse.json()) as Vehicle[];

        const airFillsResponse = await fetch(
          `${apiBaseURL}/api/vehicle-air-fills/latest`,
          {
            headers: {
              Cookie: cookieHeader,
            },
            signal: AbortSignal.timeout(10_000),
          }
        );
        if (airFillsResponse.ok) {
          const airFills = (await airFillsResponse.json()) as AirFill[];
          latestAirFills = Object.fromEntries(
            airFills.map((fill) => [fill.vehicle_id, fill.filled_at])
          );
        }
      }
    } catch {
      error = "We couldn't reach the server. Please try again.";
    }
  }

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        <header className="mb-10 flex items-start justify-between gap-4">
          <div>
            <Link
              className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit"
              href="/dashboard"
            >
              <ArrowLeft className="h-4 w-4" /> Dashboard
            </Link>
            <p className="mt-5 text-sm font-semibold tracking-[0.18em] text-primary uppercase">
              Life Tracker
            </p>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              Garage
            </h1>
            <p className="mt-3 text-base text-muted-foreground">
              A simple home for every vehicle in your life.
            </p>
          </div>
          <AddVehicleButton />
        </header>

        {error ? (
          <p
            className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive"
            role="alert"
          >
            {error}
          </p>
        ) : vehicles.length === 0 ? (
          <section className="rounded-2xl border border-dashed border-border bg-card/70 p-10 text-center">
            <p className="text-lg font-semibold text-foreground">
              Your garage is ready for its first vehicle.
            </p>
            <p className="mt-2 text-sm text-muted-foreground">
              Add one whenever you&apos;re ready.
            </p>
          </section>
        ) : (
          <VehiclesList vehicles={vehicles} latestAirFills={latestAirFills} />
        )}
      </div>
    </main>
  );
}
