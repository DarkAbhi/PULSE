-- Backfill existing vehicles with the agreed temporary classification.
ALTER TABLE vehicles
    ADD COLUMN vehicle_type TEXT NOT NULL DEFAULT 'two_wheeler',
    ADD COLUMN fuel_types TEXT[] NOT NULL DEFAULT ARRAY['petrol']::TEXT[],
    ADD CONSTRAINT vehicles_vehicle_type_check CHECK (vehicle_type IN ('two_wheeler', 'four_wheeler')),
    ADD CONSTRAINT vehicles_fuel_types_check CHECK (
        array_ndims(fuel_types) = 1
        AND array_lower(fuel_types, 1) = 1
        AND cardinality(fuel_types) BETWEEN 1 AND 2
        AND array_position(fuel_types, NULL) IS NULL
        AND fuel_types <@ ARRAY['petrol', 'diesel', 'lpg', 'cng', 'electric']::TEXT[]
        AND (cardinality(fuel_types) = 1 OR fuel_types[1] <> fuel_types[2])
    );

-- New vehicles must explicitly supply their own details.
ALTER TABLE vehicles
    ALTER COLUMN vehicle_type DROP DEFAULT,
    ALTER COLUMN fuel_types DROP DEFAULT;
