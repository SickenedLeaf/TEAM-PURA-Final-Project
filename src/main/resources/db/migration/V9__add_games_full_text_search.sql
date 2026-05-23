-- Full-text search on games (title + platform)

ALTER TABLE games ADD COLUMN search_vector TSVECTOR;

UPDATE games
SET search_vector =
    to_tsvector('english', coalesce(game_title, '') || ' ' || coalesce(platform, ''));

ALTER TABLE games ALTER COLUMN search_vector SET NOT NULL;

CREATE INDEX idx_games_search_vector ON games USING GIN (search_vector);

CREATE OR REPLACE FUNCTION games_search_vector_update()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.search_vector :=
        to_tsvector('english', coalesce(NEW.game_title, '') || ' ' || coalesce(NEW.platform, ''));
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_games_search_vector
    BEFORE INSERT OR UPDATE OF game_title, platform ON games
    FOR EACH ROW
    EXECUTE FUNCTION games_search_vector_update();
