-- Sample data for local development
-- This file is executed after all schema migrations (V1, V2, V3)
-- It provides realistic test data with:
-- - 2 users (Alice, Bob)
-- - 3 cars (Tesla Model 3, Honda Civic, Ford F-150)
-- - 36 fillups total (12 per car) with realistic MPG patterns

-- ============================================================================
-- USERS
-- ============================================================================
INSERT INTO users (id, auth_provider_id, email, created_at) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'user_clerk_alice123', 'alice@example.com', '2024-01-15 10:00:00'),
    ('b1ffcd88-8b1a-5df7-aa5c-5aa8ac271b22', 'user_clerk_bob456', 'bob@example.com', '2024-02-01 14:30:00');

-- ============================================================================
-- CARS
-- ============================================================================
-- Alice's Tesla Model 3 (Electric, but we track as MPG equivalent)
INSERT INTO cars (id, user_id, make, model, year, vin, name, fuel_unit, distance_unit, created_at) VALUES
    ('c2ddef77-7a2b-6ef8-99fc-499b9d482c33', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Tesla', 'Model 3', 2022, '5YJ3E1EA1KF123456', 'My Tesla', 'GALLONS', 'MILES', '2024-01-20 09:00:00');

-- Alice's Honda Civic (Metric units)
INSERT INTO cars (id, user_id, make, model, year, vin, name, fuel_unit, distance_unit, created_at) VALUES
    ('d3eeff66-6b3c-7df9-88eb-388a8c593d44', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Honda', 'Civic', 2020, '2HGFC2F59LH123456', 'Family Car', 'LITERS', 'KILOMETERS', '2024-01-22 11:00:00');

-- Bob's Ford F-150 (Imperial units)
INSERT INTO cars (id, user_id, make, model, year, vin, name, fuel_unit, distance_unit, created_at) VALUES
    ('e4fff055-5c4d-8ef0-77da-277b7d604e55', 'b1ffcd88-8b1a-5df7-aa5c-5aa8ac271b22', 'Ford', 'F-150', 2021, '1FTFW1E59MFC12345', 'The Truck', 'GALLONS', 'MILES', '2024-02-05 16:00:00');

-- ============================================================================
-- FILLUPS - Tesla Model 3 (Alice)
-- Target: ~35 MPG equivalent, includes 1 partial and 1 missed fill
-- ============================================================================
-- Week 1: Initial fill (no MPG - first fillup)
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000001', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-03-01 08:30:00', 5000, 11.5, 3.89, 44.74, false, false, '2024-03-01 08:30:00');

-- Week 2: 420 miles / 12.2 gallons = 34.4 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000002', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-03-08 09:15:00', 5420, 12.2, 3.92, 47.82, false, false, '2024-03-08 09:15:00');

-- Week 3: 430 miles / 11.8 gallons = 36.4 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000003', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-03-15 10:00:00', 5850, 11.8, 3.95, 46.61, false, false, '2024-03-15 10:00:00');

-- Week 4: PARTIAL FILL - 200 miles / 6.0 gallons = 33.3 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000004', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-03-22 14:30:00', 6050, 6.0, 3.99, 23.94, true, false, '2024-03-22 14:30:00');

-- Week 5: Continue from partial - 250 miles / 7.5 gallons = 33.3 MPG (accumulated 450 miles / 13.5 gallons = 33.3 MPG)
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000005', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-03-29 11:00:00', 6300, 7.5, 4.05, 30.38, false, false, '2024-03-29 11:00:00');

-- Week 6: 410 miles / 11.5 gallons = 35.7 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000006', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-04-05 09:45:00', 6710, 11.5, 4.10, 47.15, false, false, '2024-04-05 09:45:00');

-- Week 7: Missed previous fill - 880 miles / 24.0 gallons = 36.7 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000007', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-04-19 16:20:00', 7590, 24.0, 4.15, 99.60, false, true, '2024-04-19 16:20:00');

-- Week 8: 390 miles / 11.0 gallons = 35.5 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000008', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-04-26 10:30:00', 7980, 11.0, 4.20, 46.20, false, false, '2024-04-26 10:30:00');

-- Week 9: 440 miles / 12.5 gallons = 35.2 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000009', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-05-03 08:00:00', 8420, 12.5, 4.25, 53.13, false, false, '2024-05-03 08:00:00');

-- Week 10: 400 miles / 11.2 gallons = 35.7 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000010', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-05-10 12:15:00', 8820, 11.2, 4.18, 46.82, false, false, '2024-05-10 12:15:00');

-- Week 11: 380 miles / 10.8 gallons = 35.2 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000011', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-05-17 09:30:00', 9200, 10.8, 4.22, 45.58, false, false, '2024-05-17 09:30:00');

-- Week 12: 460 miles / 13.0 gallons = 35.4 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('10000000-0000-0000-0000-000000000012', 'c2ddef77-7a2b-6ef8-99fc-499b9d482c33', '2024-05-24 14:00:00', 9660, 13.0, 4.30, 55.90, false, false, '2024-05-24 14:00:00');

-- ============================================================================
-- FILLUPS - Honda Civic (Alice)
-- Target: ~14.6 km/L (34.4 MPG), includes 1 partial and 1 missed fill
-- ============================================================================
-- Week 1: Initial fill (no MPG - first fillup)
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000001', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-03-02 10:00:00', 15000, 38.0, 1.65, 62.70, false, false, '2024-03-02 10:00:00');

-- Week 2: 550 km / 38.5 L = 14.3 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000002', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-03-09 11:30:00', 15550, 38.5, 1.68, 64.68, false, false, '2024-03-09 11:30:00');

-- Week 3: 570 km / 39.0 L = 14.6 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000003', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-03-16 09:15:00', 16120, 39.0, 1.70, 66.30, false, false, '2024-03-16 09:15:00');

-- Week 4: PARTIAL FILL - 250 km / 18.0 L = 13.9 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000004', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-03-23 15:45:00', 16370, 18.0, 1.72, 30.96, true, false, '2024-03-23 15:45:00');

-- Week 5: Continue from partial - 320 km / 21.5 L = 14.9 km/L (accumulated 570 km / 39.5 L = 14.4 km/L)
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000005', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-03-30 10:20:00', 16690, 21.5, 1.75, 37.63, false, false, '2024-03-30 10:20:00');

-- Week 6: 580 km / 39.5 L = 14.7 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000006', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-04-06 12:00:00', 17270, 39.5, 1.78, 70.31, false, false, '2024-04-06 12:00:00');

-- Week 7: Missed previous fill - 1150 km / 78.0 L = 14.7 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000007', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-04-20 14:30:00', 18420, 78.0, 1.80, 140.40, false, true, '2024-04-20 14:30:00');

-- Week 8: 540 km / 37.0 L = 14.6 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000008', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-04-27 11:15:00', 18960, 37.0, 1.82, 67.34, false, false, '2024-04-27 11:15:00');

-- Week 9: 590 km / 40.0 L = 14.8 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000009', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-05-04 09:00:00', 19550, 40.0, 1.85, 74.00, false, false, '2024-05-04 09:00:00');

-- Week 10: 560 km / 38.0 L = 14.7 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000010', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-05-11 13:30:00', 20110, 38.0, 1.88, 71.44, false, false, '2024-05-11 13:30:00');

-- Week 11: 550 km / 37.5 L = 14.7 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000011', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-05-18 10:45:00', 20660, 37.5, 1.90, 71.25, false, false, '2024-05-18 10:45:00');

-- Week 12: 600 km / 41.0 L = 14.6 km/L
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('20000000-0000-0000-0000-000000000012', 'd3eeff66-6b3c-7df9-88eb-388a8c593d44', '2024-05-25 15:00:00', 21260, 41.0, 1.92, 78.72, false, false, '2024-05-25 15:00:00');

-- ============================================================================
-- FILLUPS - Ford F-150 (Bob)
-- Target: ~18 MPG, includes 1 partial and 1 missed fill
-- ============================================================================
-- Week 1: Initial fill (no MPG - first fillup)
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000001', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-03-03 08:00:00', 25000, 22.0, 3.75, 82.50, false, false, '2024-03-03 08:00:00');

-- Week 2: 400 miles / 22.5 gallons = 17.8 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000002', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-03-10 09:30:00', 25400, 22.5, 3.78, 85.05, false, false, '2024-03-10 09:30:00');

-- Week 3: 420 miles / 23.0 gallons = 18.3 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000003', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-03-17 11:00:00', 25820, 23.0, 3.80, 87.40, false, false, '2024-03-17 11:00:00');

-- Week 4: PARTIAL FILL - 180 miles / 10.0 gallons = 18.0 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000004', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-03-24 14:00:00', 26000, 10.0, 3.82, 38.20, true, false, '2024-03-24 14:00:00');

-- Week 5: Continue from partial - 230 miles / 13.0 gallons = 17.7 MPG (accumulated 410 miles / 23.0 gallons = 17.8 MPG)
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000005', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-03-31 10:15:00', 26230, 13.0, 3.85, 50.05, false, false, '2024-03-31 10:15:00');

-- Week 6: 390 miles / 21.5 gallons = 18.1 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000006', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-04-07 13:45:00', 26620, 21.5, 3.88, 83.42, false, false, '2024-04-07 13:45:00');

-- Week 7: Missed previous fill - 820 miles / 46.0 gallons = 17.8 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000007', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-04-21 16:00:00', 27440, 46.0, 3.90, 179.40, false, true, '2024-04-21 16:00:00');

-- Week 8: 380 miles / 21.0 gallons = 18.1 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000008', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-04-28 11:30:00', 27820, 21.0, 3.92, 82.32, false, false, '2024-04-28 11:30:00');

-- Week 9: 410 miles / 22.8 gallons = 18.0 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000009', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-05-05 09:00:00', 28230, 22.8, 3.95, 90.06, false, false, '2024-05-05 09:00:00');

-- Week 10: 370 miles / 20.5 gallons = 18.0 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000010', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-05-12 14:15:00', 28600, 20.5, 3.98, 81.59, false, false, '2024-05-12 14:15:00');

-- Week 11: 395 miles / 22.0 gallons = 18.0 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000011', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-05-19 10:30:00', 28995, 22.0, 4.00, 88.00, false, false, '2024-05-19 10:30:00');

-- Week 12: 425 miles / 23.5 gallons = 18.1 MPG
INSERT INTO fillups (id, car_id, date, odometer, fuel_volume, price_per_unit, total_cost, is_partial, is_missed, created_at) VALUES
    ('30000000-0000-0000-0000-000000000012', 'e4fff055-5c4d-8ef0-77da-277b7d604e55', '2024-05-26 15:45:00', 29420, 23.5, 4.05, 95.18, false, false, '2024-05-26 15:45:00');
