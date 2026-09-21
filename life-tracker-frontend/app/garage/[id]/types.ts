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

export type VehicleHistoryData = {
  vehicle_name: string;
  front_tire_pressure_solo: number | null;
  rear_tire_pressure_solo: number | null;
  front_tire_pressure_pillion: number | null;
  rear_tire_pressure_pillion: number | null;
  front_tire_pressure?: number | null;
  rear_tire_pressure?: number | null;
  air_fills: AirFill[];
  fuel_fillups: FuelFill[];
  maintenance_records: MaintenanceRecord[];
  average_mileage_km_per_litre: Record<string, number>;
};

export type SaveTirePressurePayload = {
  front_tire_pressure_solo: number | null;
  rear_tire_pressure_solo: number | null;
  front_tire_pressure_pillion: number | null;
  rear_tire_pressure_pillion: number | null;
};

