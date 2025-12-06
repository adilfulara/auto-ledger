-- Create fillups table for tracking fuel purchases
CREATE TABLE fillups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    car_id UUID NOT NULL,
    date TIMESTAMP NOT NULL,
    odometer BIGINT NOT NULL CHECK (odometer >= 0),
    fuel_volume NUMERIC(10, 3) NOT NULL CHECK (fuel_volume > 0),
    price_per_unit NUMERIC(10, 3) NOT NULL CHECK (price_per_unit >= 0),
    total_cost NUMERIC(10, 2) NOT NULL CHECK (total_cost >= 0),
    is_partial BOOLEAN NOT NULL DEFAULT FALSE,
    is_missed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to cars table
    CONSTRAINT fk_fillups_car_id FOREIGN KEY (car_id)
        REFERENCES cars(id)
        ON DELETE CASCADE
);

-- Indexes for common queries
CREATE INDEX idx_fillups_car_id ON fillups(car_id);
CREATE INDEX idx_fillups_car_date ON fillups(car_id, date DESC);
CREATE INDEX idx_fillups_date ON fillups(date);

-- Constraint to ensure odometer values increase for each car
-- This will be enforced at the application layer for better error messages
-- CREATE UNIQUE INDEX idx_fillups_car_odometer ON fillups(car_id, odometer);

-- Add comments for documentation
COMMENT ON TABLE fillups IS 'Fuel fill-up records for MPG calculation';
COMMENT ON COLUMN fillups.car_id IS 'References cars.id';
COMMENT ON COLUMN fillups.odometer IS 'Total vehicle mileage/kilometers at fill-up';
COMMENT ON COLUMN fillups.fuel_volume IS 'Amount of fuel added (units from parent car)';
COMMENT ON COLUMN fillups.price_per_unit IS 'Price per unit of fuel';
COMMENT ON COLUMN fillups.total_cost IS 'Total cost of fill-up (currency)';
COMMENT ON COLUMN fillups.is_partial IS 'TRUE if tank was not filled to top (skip MPG calc)';
COMMENT ON COLUMN fillups.is_missed IS 'TRUE if user missed logging previous fill-up (break MPG chain)';
