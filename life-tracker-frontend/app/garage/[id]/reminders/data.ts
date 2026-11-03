import { apiFetch } from "@/app/lib/server-api";
import { Droplet, Wind, Wrench } from "lucide-react";
import { notFound, redirect } from "next/navigation";
import { VehicleHistoryData } from "../types";

export const reminderDetails = {
  engine_oil: { title: "Engine oil", icon: Droplet },
  tire_pressure: { title: "Tire pressure", icon: Wind },
  general_service: { title: "General service", icon: Wrench },
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
