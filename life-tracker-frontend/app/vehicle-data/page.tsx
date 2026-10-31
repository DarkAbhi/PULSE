import { readFile } from "node:fs/promises";
import path from "node:path";
import Link from "next/link";
import PulseCard from "@/app/components/design-system/pulse-card";

export const metadata = { title: "Vehicle data credits | PULSE" };

export default async function VehicleDataPage() {
  const attribution = await readFile(
    path.join(process.cwd(), "app/vehicle-data/ATTRIBUTION.txt"),
    "utf8",
  );
  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10">
      <div className="mx-auto max-w-4xl">
        <Link href="/garage" className="text-sm font-semibold text-action">
          ← Garage
        </Link>
        <h1 className="mt-5 text-3xl font-bold">Vehicle data and credits</h1>
        <p className="mt-4 text-muted-foreground">
          Vehicle data by{" "}
          <a className="text-action underline" href="https://vehiclesdb.com">
            VehiclesDB
          </a>
          , release{" "}
          <a
            className="text-action underline"
            href="https://github.com/vehiclesdb/vehiclesdb/releases/tag/v2026.10.4"
          >
            2026.10.4
          </a>
          , licensed under{" "}
          <a
            className="text-action underline"
            href="https://creativecommons.org/licenses/by/4.0/"
          >
            CC BY 4.0
          </a>
          . Pulse imports the catalog for vehicle selection and adapts its
          storage format. Catalog coverage varies by market; you can enter
          missing vehicles manually.
        </p>
        <PulseCard as="section" className="mt-8 p-6">
          <h2 className="text-lg font-semibold">Upstream source notices</h2>
          <pre className="mt-4 whitespace-pre-wrap break-words font-sans text-sm leading-6 text-muted-foreground">
            {attribution.replace(/<!--[\s\S]*?-->/g, "").trim()}
          </pre>
        </PulseCard>
      </div>
    </main>
  );
}
