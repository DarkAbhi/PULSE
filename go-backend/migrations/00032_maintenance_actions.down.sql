DROP TABLE vehicle_maintenance_actions;
-- Rollback requires removing or assigning a positive amount to free maintenance records first.
ALTER TABLE vehicle_maintenance_records DROP CONSTRAINT vehicle_maintenance_records_amount_check;
ALTER TABLE vehicle_maintenance_records ADD CONSTRAINT vehicle_maintenance_records_amount_check CHECK (amount > 0);
