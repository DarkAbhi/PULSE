import { VehicleDetailScreen, type VehicleDetailQuery } from "@/features/garage/screens/vehicle-detail-screen";

export const metadata = { title: "Vehicle details | PULSE" };

interface PageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<VehicleDetailQuery>;
}

export default async function VehiclePage({ params, searchParams }: PageProps) {
  const [{ id }, query] = await Promise.all([params, searchParams]);
  return <VehicleDetailScreen vehicleId={id} query={query} />;
}
