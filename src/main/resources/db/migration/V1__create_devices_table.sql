
CREATE TABLE devices (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT chk_devices_state
        CHECK (state IN ('AVAILABLE', 'IN_USE', 'INACTIVE'))
);

CREATE INDEX idx_devices_brand
    ON devices (brand);

CREATE INDEX idx_devices_state
    ON devices (state);
