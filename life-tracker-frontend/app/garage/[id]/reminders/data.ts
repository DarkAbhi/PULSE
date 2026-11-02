import { apiFetch } from "@/app/lib/server-api";
import { notFound, redirect } from "next/navigation";
import { VehicleHistoryData } from "../types";

export const reminderTitles: Record<string, string> = {
  engine_oil: "Engine oil", tire_pressure: "Tire pressure", general_service: "General service",
};
export async function loadReminderSettings(id: string) {
  const base = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";
  const response = await apiFetch(`${base}/api/vehicles/${id}/history`);
  if (response.status === 401) redirect("/");
  if (response.status === 404) notFound();
  if (!response.ok) throw new Error("Could not load reminder settings.");
  const vehicle = await response.json() as VehicleHistoryData;
  const settings = (vehicle.maintenance_schedules ?? []).filter((s) => s.kind !== "engine_oil" || vehicle.fuel_types.some((f) => f !== "electric"));
  return { vehicle, settings };
}
