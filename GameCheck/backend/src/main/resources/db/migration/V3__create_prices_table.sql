CREATE TABLE prices (
    price_id SERIAL PRIMARY KEY,
    game_id INTEGER NOT NULL REFERENCES games (game_id) ON DELETE CASCADE,
    source_id INTEGER NOT NULL REFERENCES sources (source_id) ON DELETE CASCADE,
    price_php DECIMAL(10, 2) NOT NULL,
    price_original DECIMAL(10, 2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    listing_url TEXT NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_prices_game_source UNIQUE (game_id, source_id)
);

CREATE INDEX idx_prices_game_id ON prices (game_id);
