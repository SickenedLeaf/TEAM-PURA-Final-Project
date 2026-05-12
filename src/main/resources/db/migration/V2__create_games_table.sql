CREATE TABLE games (
    game_id SERIAL PRIMARY KEY,
    game_title VARCHAR(255) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    metacritic_score INTEGER,
    trailer_url TEXT,
    cover_image_url TEXT
);
