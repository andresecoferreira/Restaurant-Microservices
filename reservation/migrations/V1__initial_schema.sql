-- Reservation Service Database Schema
-- NOTE: This service only manages reservations. Restaurant, availability slots, and menu items
-- are managed by the Restaurant Service in a separate database.
-- We use application-level integration (Feign) to validate foreign references.

CREATE TABLE IF NOT EXISTS reservations (
	id UUID PRIMARY KEY,
	restaurant_id BIGINT NOT NULL,
	slot_id BIGINT NOT NULL,
	user_email VARCHAR(255) NOT NULL,
	party_size INTEGER NOT NULL,
	status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
	created_at TIMESTAMP NOT NULL,
	CONSTRAINT check_party_size CHECK (party_size > 0),
	CONSTRAINT check_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_restaurant_id ON reservations(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_user_email ON reservations(user_email);
CREATE INDEX IF NOT EXISTS idx_created_at ON reservations(created_at);
