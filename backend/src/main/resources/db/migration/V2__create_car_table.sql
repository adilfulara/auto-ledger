-- Create cars table
CREATE TABLE cars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL CHECK (year >= 1900 AND year <= 2100),
    vin VARCHAR(17) UNIQUE,
    name VARCHAR(100) NOT NULL,
    primary_fuel_unit VARCHAR(20) NOT NULL DEFAULT 'GALLONS' CHECK (primary_fuel_unit IN ('GALLONS', 'LITERS')),
    primary_distance_unit VARCHAR(20) NOT NULL DEFAULT 'MILES' CHECK (primary_distance_unit IN ('MILES', 'KILOMETERS')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to users table
    CONSTRAINT fk_cars_user_id FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Indexes for common queries
CREATE INDEX idx_cars_user_id ON cars(user_id);
CREATE INDEX idx_cars_vin ON cars(vin) WHERE vin IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE cars IS 'User vehicles for mileage tracking';
COMMENT ON COLUMN cars.user_id IS 'References users.id (internal UUID)';
COMMENT ON COLUMN cars.vin IS 'Vehicle Identification Number (17 characters, optional)';
COMMENT ON COLUMN cars.name IS 'User-friendly name for the car (e.g., "My Tesla", "Work Truck")';
COMMENT ON COLUMN cars.primary_fuel_unit IS 'Default unit for fuel volume (GALLONS or LITERS)';
COMMENT ON COLUMN cars.primary_distance_unit IS 'Default unit for distance (MILES or KILOMETERS)';
