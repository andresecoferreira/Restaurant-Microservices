CREATE TABLE IF NOT EXISTS analytics_reservations (
    id UUID PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    party_size INTEGER NOT NULL,
    reservation_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance indexes for analytical queries
CREATE INDEX idx_restaurant_id ON analytics_reservations(restaurant_id);
CREATE INDEX idx_user_email ON analytics_reservations(user_email);
CREATE INDEX idx_status ON analytics_reservations(status);
CREATE INDEX idx_reservation_date ON analytics_reservations(reservation_date);
CREATE INDEX idx_event_timestamp ON analytics_reservations(event_timestamp);

-- Composite indexes for complex analytics
CREATE INDEX idx_restaurant_status ON analytics_reservations(restaurant_id, status);
CREATE INDEX idx_user_status ON analytics_reservations(user_email, status);
