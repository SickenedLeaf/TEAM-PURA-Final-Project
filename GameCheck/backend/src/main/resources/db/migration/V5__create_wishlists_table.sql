CREATE TABLE wishlists (
    wishlist_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    game_id INTEGER NOT NULL REFERENCES games (game_id) ON DELETE CASCADE,
    price_alert_threshold DECIMAL(10, 2),
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wishlists_user_game UNIQUE (user_id, game_id)
);
