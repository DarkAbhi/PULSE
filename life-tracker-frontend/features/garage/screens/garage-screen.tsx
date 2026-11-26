import { apiBaseURL, apiFetch } from "@/lib/api/server-api";
import PulseCard from "@/components/ui/pulse-card";
import { redirect } from "next/navigation";
import { TopAppBar } from "@/features/notifications/server";
import { frame } from "@/components/layout/page-frame";
import VehicleEditor from "@/features/garage/components/vehicle-editor";
import VehiclesList from "@/features/garage/components/vehicle-list";
import type { Vehicle } from "@/features/garage/types/garage-types";

export async function GarageScreen() {
  const response = await apiFetch(`${apiBaseURL}/api/vehicles`, {
    signal: AbortSignal.timeout(10_000),
  }).catch(() => null);
  if (response?.status === 401) redirect("/");

  const vehicles: Vehicle[] | null = response?.ok ? await response.json().catch(() => null) : null;
  const error = response
    ? "We couldn't load your vehicles. Please try again."
    : "We couldn't reach the server. Please try again.";

  return (
    <main id="main-content" tabIndex={-1} className={frame.page}>
      <div className={frame.content}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" />

        <section aria-labelledby="garage-title" className="garage-banner relative mt-3 rounded-garage-banner bg-garage-banner p-garage-banner text-white shadow-garage-banner">
          <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden rounded-[inherit]">
            <svg className="absolute inset-bs-1/2 end-[var(--garage-gauge-offset)] hidden h-auto w-[min(38%,30rem)] -translate-y-1/2 overflow-visible sm:block" viewBox="0 0 400 400" fill="none">
              {["motion-safe:[animation-delay:-4s]", "motion-safe:[animation-delay:-2s]", ""].map((delay) => (
                <circle
                  key={delay}
                  className={`hidden motion-safe:inline motion-safe:origin-center motion-safe:transform-fill motion-safe:animate-gauge-ripple ${delay}`}
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
            <h1 id="garage-title" className="text-garage-title font-black tracking-garage-title">Garage</h1>
            <p className="mt-3 text-base font-medium sm:text-xl">Vehicles, fuel &amp; maintenance</p>
            <VehicleEditor triggerClassName="mt-5 sm:mt-6" />
          </div>
        </section>

        <section aria-labelledby="vehicles-title" className="mt-5 sm:mt-7">
          <h2 id="vehicles-title" className="sr-only">Your vehicles</h2>
          {!vehicles ? (
            <PulseCard as="p" padding="md" className="[--card-fill:var(--destructive-soft)] text-sm text-destructive" role="alert">
              {error}
            </PulseCard>
          ) : vehicles.length === 0 ? (
            <PulseCard padding="lg">
              <p className="font-semibold">Your garage is empty</p>
              <p className="mt-1 text-sm text-muted-foreground">
                Add your first vehicle to start logging fuel and tire air.
              </p>
            </PulseCard>
          ) : (
            <VehiclesList vehicles={vehicles} />
          )}
        </section>

      </div>
    </main>
  );
}
