CREATE TABLE IF NOT EXISTS trecom (
    id BIGSERIAL PRIMARY KEY,
    created_at DATE NOT NULL DEFAULT NOW(),
    customer_name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    hours_spent NUMERIC(6,2) NOT NULL CHECK (hours_spent >= 0),
    salesperson_last_name VARCHAR(100) NOT NULL,
    salesperson_first_name VARCHAR(100) NOT NULL,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_tre_created_at ON trecom (created_at);
