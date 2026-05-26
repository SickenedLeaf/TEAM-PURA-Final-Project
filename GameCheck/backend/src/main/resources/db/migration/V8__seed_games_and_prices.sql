-- Seed sample Nintendo Switch games and prices for local / Postman testing (Milestone 4).

INSERT INTO games (game_title, platform, metacritic_score, trailer_url, cover_image_url)
VALUES
    ('The Legend of Zelda: Tears of the Kingdom', 'Nintendo Switch', 96, NULL, NULL),
    ('Super Mario Odyssey', 'Nintendo Switch', 97, NULL, NULL),
    ('Animal Crossing: New Horizons', 'Nintendo Switch', 90, NULL, NULL);

INSERT INTO prices (game_id, source_id, price_php, price_original, currency_code, listing_url, last_updated)
SELECT g.game_id,
       s.source_id,
       v.price_php,
       v.price_original,
       v.currency_code,
       v.listing_url,
       v.last_updated
FROM (
    VALUES
        ('The Legend of Zelda: Tears of the Kingdom', 'DataBlitz', 2699.00::numeric(10, 2), 2699.00::numeric(10, 2), 'PHP', 'https://www.datablitz.com.ph/games/nintendo-switch/zelda-totk', TIMESTAMP '2026-05-01 10:00:00'),
        ('The Legend of Zelda: Tears of the Kingdom', 'iTech', 2595.00::numeric(10, 2), 2595.00::numeric(10, 2), 'PHP', 'https://www.itech.ph/products/zelda-totk-switch', TIMESTAMP '2026-05-02 14:30:00'),
        ('The Legend of Zelda: Tears of the Kingdom', 'Nintendo eShop', 2499.00::numeric(10, 2), 49.99::numeric(10, 2), 'USD', 'https://www.nintendo.com/store/products/the-legend-of-zelda-tears-of-the-kingdom-switch/', TIMESTAMP '2026-05-03 09:15:00'),
        ('Super Mario Odyssey', 'DataBlitz', 2299.00::numeric(10, 2), 2299.00::numeric(10, 2), 'PHP', 'https://www.datablitz.com.ph/games/nintendo-switch/super-mario-odyssey', TIMESTAMP '2026-05-01 11:00:00'),
        ('Super Mario Odyssey', 'iTech', 2199.00::numeric(10, 2), 2199.00::numeric(10, 2), 'PHP', 'https://www.itech.ph/products/super-mario-odyssey-switch', TIMESTAMP '2026-05-02 16:00:00'),
        ('Super Mario Odyssey', 'Nintendo eShop', 2999.00::numeric(10, 2), 59.99::numeric(10, 2), 'USD', 'https://www.nintendo.com/store/products/super-mario-odyssey-switch/', TIMESTAMP '2026-05-04 08:00:00'),
        ('Animal Crossing: New Horizons', 'DataBlitz', 2495.00::numeric(10, 2), 2495.00::numeric(10, 2), 'PHP', 'https://www.datablitz.com.ph/games/nintendo-switch/animal-crossing-nh', TIMESTAMP '2026-05-01 12:00:00'),
        ('Animal Crossing: New Horizons', 'iTech', 2399.00::numeric(10, 2), 2399.00::numeric(10, 2), 'PHP', 'https://www.itech.ph/products/animal-crossing-nh-switch', TIMESTAMP '2026-05-02 10:00:00'),
        ('Animal Crossing: New Horizons', 'Nintendo eShop', 2349.00::numeric(10, 2), 59.99::numeric(10, 2), 'USD', 'https://www.nintendo.com/store/products/animal-crossing-new-horizons-switch/', TIMESTAMP '2026-05-03 12:45:00')
) AS v(game_title, source_name, price_php, price_original, currency_code, listing_url, last_updated)
JOIN games g ON g.game_title = v.game_title
JOIN sources s ON s.source_name = v.source_name;
