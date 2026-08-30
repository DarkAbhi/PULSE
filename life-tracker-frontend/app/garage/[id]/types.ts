export type AirFill = { id: number; filled_at: string };
export type FuelItem = {
  fuel_type: string;
  fill_type: string;
  quantity: number;
  unit_price: number;
  total_cost: number;
};
export type FuelFill = {
  id: number;
  odometer_km: number;
  filled_at: string;
  station_name: string | null;
  notes: string | null;
  items: FuelItem[];
};

export type MaintenanceRecord = {
  id: number;
  category: "service" | "repair" | "insurance" | "washing" | "tyres";
  title: string;
  amount: number;
  occurred_at: string;
  odometer_km: number | null;
  provider_name: string | null;
  notes: string | null;
  attachments: MaintenanceAttachment[];
};

export type MaintenanceAttachment = {
  id: number;
  file_name: string;
  content_type: string;
  size_bytes: number;
  created_at: string;
};
