CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_recipient ON notifications(recipient);
CREATE INDEX idx_reservation_id ON notifications(reservation_id);
CREATE INDEX idx_sent_at ON notifications(sent_at);
CREATE INDEX idx_event_type ON notifications(event_type);
