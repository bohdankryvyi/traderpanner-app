CREATE TABLE securities (
    ticker VARCHAR(32) PRIMARY KEY,
    company VARCHAR(255) NOT NULL,
    sector VARCHAR(255) NOT NULL
);

CREATE TABLE portfolio_positions (
    id           BIGSERIAL PRIMARY KEY,
    sector       VARCHAR(255) NOT NULL,
    company      VARCHAR(255) NOT NULL,
    ticker       VARCHAR(32)  NOT NULL REFERENCES securities (ticker),
    buy_price    NUMERIC(18, 6) NOT NULL,
    target_price NUMERIC(18, 6),
    quantity     NUMERIC(18, 6) NOT NULL,
    currency     VARCHAR(3)  NOT NULL,
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE trading_entries (
    id          BIGSERIAL PRIMARY KEY,
    ticker      VARCHAR(32) NOT NULL REFERENCES securities (ticker),
    note        TEXT,
    entry_price NUMERIC(18, 6) NOT NULL,
    currency    VARCHAR(3) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

