import { apiFetch } from "@/app/lib/server-api";
import PulseCard from "@/app/components/design-system/pulse-card";
import { redirect } from "next/navigation";
import Link from "next/link";
import TopAppBar from "@/app/components/top-app-bar";
import { frame } from "@/app/components/design-system/page-frame";
import AddVehicleButton from "@/app/garage/add-vehicle-button";
import VehiclesList, { type Vehicle } from "@/app/garage/vehicles-list";
import { garageStyles as styles } from "@/app/garage/garage-classes";

export const metadata = {
  title: "Garage | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

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
    <main className={`${styles.page} ${frame.page}`}>
      <div className={frame.content}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" />

        <section aria-labelledby="garage-title" className={`${styles.banner} relative mt-3`}>
          <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden rounded-[inherit]">
            <svg className={styles.gauge} viewBox="0 0 400 400" fill="none">
              {["motion-safe:[animation-delay:-4s]", "motion-safe:[animation-delay:-2s]", ""].map((delay) => (
                <circle
                  key={delay}
                  className={`${styles.gaugeRipple} ${delay}`}
                  cx="200"
                  cy="200"
                  r="153"
                  stroke="white"
                  strokeWidth="2"
                />
              ))}
              <path d="M60 275a140 140 0 0 1 280 0" pathLength="140" stroke="white" strokeOpacity=".56" strokeWidth="17" strokeLinecap="round" strokeDasharray="1.8 8.2" />
              <path d="m200 275 8-111" stroke="white" strokeWidth="11" strokeLinecap="round" />
              <circle cx="200" cy="275" r="17" fill="white" />
            </svg>
          </div>
          <div className="relative">
            <h1 id="garage-title" className={`${styles.title} text-white`}>Garage</h1>
            <p className="mt-3 text-base font-medium text-white sm:text-xl">Vehicles, fuel &amp; maintenance</p>
            <AddVehicleButton triggerClassName={`${styles.addVehicle} mt-5 sm:mt-6`} />
          </div>
        </section>

        <section aria-labelledby="vehicles-title" className="mt-5 sm:mt-7">
          <h2 id="vehicles-title" className="sr-only">Your vehicles</h2>
          {error ? (
            <PulseCard as="p" className="bg-destructive-soft px-6 py-5 text-sm text-destructive" role="alert">
              {error}
            </PulseCard>
          ) : vehicles.length === 0 ? (
            <PulseCard className={`${styles.vehicleCard} px-6 py-8 sm:px-8`}>
              <p className="font-semibold">Your garage is empty</p>
              <p className={`${styles.muted} mt-1 text-sm`}>
                Add your first vehicle to start logging fuel and tire air.
              </p>
            </PulseCard>
          ) : (
            <VehiclesList vehicles={vehicles} />
          )}
        </section>

        <footer className={`${styles.muted} mt-6 flex flex-wrap items-center gap-x-4 gap-y-2 px-1 text-xs`}>
          <span>Vehicle data by <a className="underline" href="https://vehiclesdb.com">VehiclesDB</a></span>
          <Link className="underline" href="/vehicle-data">Sources and credits</Link>
        </footer>
      </div>
    </main>
  );
}
