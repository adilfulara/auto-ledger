-- Create cars table in app schema
CREATE TABLE app.cars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL CHECK (year >= 1900 AND year <= 2100),
    vin VARCHAR(17) UNIQUE,
    name VARCHAR(100) NOT NULL,
    fuel_unit VARCHAR(20) NOT NULL DEFAULT 'GALLONS' CHECK (fuel_unit IN ('GALLONS', 'LITERS')),
    distance_unit VARCHAR(20) NOT NULL DEFAULT 'MILES' CHECK (distance_unit IN ('MILES', 'KILOMETERS')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to users table
    CONSTRAINT fk_cars_user_id FOREIGN KEY (user_id)
        REFERENCES app.users(id)
        ON DELETE CASCADE
);

-- Indexes for common queries
CREATE INDEX idx_cars_user_id ON app.cars(user_id);
CREATE INDEX idx_cars_vin ON app.cars(vin) WHERE vin IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE app.cars IS 'User vehicles for mileage tracking';
COMMENT ON COLUMN app.cars.user_id IS 'References app.users.id (internal UUID)';
COMMENT ON COLUMN app.cars.vin IS 'Vehicle Identification Number (17 characters, optional)';
COMMENT ON COLUMN app.cars.name IS 'User-friendly name for the car (e.g., "My Tesla", "Work Truck")';
COMMENT ON COLUMN app.cars.fuel_unit IS 'Unit for fuel volume (GALLONS or LITERS), immutable after creation';
COMMENT ON COLUMN app.cars.distance_unit IS 'Unit for distance (MILES or KILOMETERS), immutable after creation';
