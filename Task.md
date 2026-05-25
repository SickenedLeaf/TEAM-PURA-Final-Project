# Task.md — GameCheck Backend Milestones

> Build order is intentional — each milestone unblocks the next.
> Do not skip ahead. Optional features (F6, F7, F8) come last.
> Check off tasks as you complete them.

---

## Milestone 0 — Environment Setup
*Goal: Have a working Spring Boot project connected to a real database before writing any feature code.*

- [x] Install JDK 17+ on your machine
- [ ] Install IntelliJ IDEA Community Edition
- [x] Create a GitHub repository for the project (`gamecheck-backend`)
- [x] Generate a new Spring Boot project at https://start.spring.io with these dependencies:
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - PostgreSQL Driver
  - Flyway Migration
  - Spring Boot DevTools
  - Lombok  
  *(Done in-repo: `pom.xml` + `com.gamecheck.GamecheckApplication`.)*
- [ ] Import the generated project into IntelliJ
- [x] Create a free Supabase account and create a new project
- [x] Copy the Supabase PostgreSQL connection string
- [x] Configure `application.properties` with DB credentials (Supabase connection string)  
  *(Base config uses `SPRING_DATASOURCE_*` env vars; optional `classpath:application-local.properties` — copy from `application-local.properties.example`.)*
- [x] Keep secrets out of Git — `.gitignore` includes `**/application-local.properties` (and `application-secrets.properties`)
- [x] Confirm Spring Boot starts without errors and connects to the database
- [ ] Install Postman for API testing
- [ ] Push initial project to GitHub

---

## Milestone 1 — Database Schema
*Goal: All tables exist in PostgreSQL, managed by Flyway migrations.*

- [x] Create Flyway migration file `V1__create_sources_table.sql`
  - source_id, source_name, source_type (physical/digital), source_url
- [x] Create `V2__create_games_table.sql`
  - game_id, game_title, platform, metacritic_score (nullable), trailer_url (nullable), cover_image_url (nullable)
- [x] Create `V3__create_prices_table.sql`
  - price_id, game_id (FK), source_id (FK), price_php, price_original, currency_code, listing_url, last_updated  
  *(Includes unique `(game_id, source_id)` for upserts.)*
- [x] Create `V4__create_users_table.sql`
  - user_id, email (unique), password_hash, created_at, is_active
- [x] Create `V5__create_wishlists_table.sql`
  - wishlist_id, user_id (FK), game_id (FK), price_alert_threshold (nullable), added_at
- [x] Create `V6__create_comments_table.sql` *(optional — create now, implement later)*
  - comment_id, user_id (FK), game_id (FK), comment_body, upvote_count, created_at
- [x] Run the app and confirm Flyway applies all migrations cleanly
- [x] Seed the `sources` table with initial source records (DataBlitz, iTech, Nintendo eShop)  
  *(Automated via `V7__seed_sources.sql`.)*
- [x] Verify tables exist in Supabase dashboard

---

## Milestone 2 — Core Data Models & Repositories
*Goal: Java entity classes and repository interfaces exist for all tables.*

- [x] Create `Game.java` entity class in `model` package (mapped to `games` table)
- [x] Create `Source.java` entity class (mapped to `sources` table)
- [x] Create `Price.java` entity class (mapped to `prices` table, with FK relationships)
- [x] Create `User.java` entity class (mapped to `users` table)
- [x] Create `Wishlist.java` entity class (mapped to `wishlists` table)
- [x] Create `Comment.java` *(maps `comments` table for future F8)*
- [x] Create `GameRepository.java` interface extending JpaRepository
  - Add method: `List<Game> findByGameTitleContainingIgnoreCase(String title)`
  - Add method: `List<Game> findByPlatform(String platform)`
- [x] Create `PriceRepository.java` interface extending JpaRepository
  - Add method: `List<Price> findByGame_GameId(Integer gameId)` *(Spring Data derived name for `game_id`)*
- [x] Create `SourceRepository.java` interface extending JpaRepository
- [x] Create `UserRepository.java` interface extending JpaRepository
  - Add method: `Optional<User> findByEmail(String email)`
- [x] Create `WishlistRepository.java` interface extending JpaRepository
  - Add method: `List<Wishlist> findByUser_UserId(Integer userId)` *(Spring Data derived name for `user_id`)*
- [x] Confirm no errors on startup — JPA should validate entity mappings against the DB

---

## Milestone 3 — User Authentication (F9)
*Goal: Register, login, and JWT-protected endpoints work end to end.*

- [x] Add JWT library dependency to `pom.xml` (e.g., `jjwt` by io.jsonwebtoken)
- [x] Create `JwtUtil.java` in `security` package — generates and validates JWT tokens
- [x] Create `JwtFilter.java` — Spring Security filter that reads JWT from request headers
- [x] Configure `SecurityConfig.java` — define which endpoints are public vs. protected
- [x] Create `AuthService.java` in `service` package:
  - `register(email, password)` — hash password with BCrypt, save user, return success
  - `login(email, password)` — verify credentials, return JWT token
- [x] Create `AuthController.java` in `controller` package:
  - `POST /api/auth/register` — takes email + password, returns success message
  - `POST /api/auth/login` — takes email + password, returns JWT token
- [x] Add login rate limiting (lockout after 5 failed attempts in 10 minutes)
- [x] Add input validation on all auth endpoints (email format, password not empty)
- [x] Test registration in Postman — confirm user appears in DB with hashed password
- [x] Test login in Postman — confirm JWT token is returned
- [x] Test a protected endpoint with and without the JWT token

---

## Milestone 4 — Game Search & Price Comparison API (F1, F4, F5)
*Goal: Core price comparison endpoints work with manually seeded data.*

- [x] Create `GameService.java` in `service` package:
  - `searchGames(String query, String platform)` — searches games table, returns matches
  - `getGameById(Integer id)` — returns game details
  - `getPricesForGame(Integer id)` — returns all prices for a game, sorted cheapest first
- [x] Create `GameController.java` in `controller` package:
  - `GET /api/games/search?query=&platform=` — returns list of matching games
  - `GET /api/games/{id}` — returns game detail
  - `GET /api/games/{id}/prices` — returns price comparison table (sorted cheapest first)
- [x] Create DTOs for API responses (do not expose raw entity objects):
  - `GameSummaryDto.java` — for search results (id, title, platform, best price)
  - `GameDetailDto.java` — for game detail page
  - `PriceDto.java` — for individual price entries (source name, type, price in PHP, listing URL, last updated)
- [x] Implement sorting on price comparison endpoint (price asc/desc, source type filter)
- [x] Add `last_updated` timestamp to price responses so frontend can show data freshness
- [x] Manually seed 2–3 games and prices into the DB for testing *(Flyway `V8__seed_games_and_prices.sql` — run app once against DB to apply.)*
- [x] Test all endpoints in Postman — confirm correct JSON responses
- [x] Confirm cheapest price is correctly identified in the response

---

## Milestone 5 — Currency Conversion (F3)
*Goal: Forex API integrated, all prices stored in PHP after conversion.*

- [x] Register for a free ExchangeRate API key (https://www.exchangerate-api.com)
- [x] Store API key in `application.properties` (not in code)
- [x] Create `ForexService.java` in `service` package:
  - `getUsdToPhpRate()` — calls Forex API, returns current exchange rate
  - Cache the rate in memory for 24 hours to avoid excessive API calls
- [x] Integrate `ForexService` into the scraper/aggregation layer
- [x] Every non-PHP price must be converted before storing in the `prices` table
- [x] Test with a USD price — confirm it is stored as PHP in the DB

---

## Milestone 6 — Data Aggregation Engine (F2)
*Goal: At least one real data source is scraped/fetched and stored in the DB automatically.*

- [ ] Create `SourceAdapter.java` interface in `scraper` package:
  - `String getSourceName()`
  - `List<PriceRecord> fetchPrices()`
- [ ] Create `PriceRecord.java` DTO — standardized format for scraped data (gameTitle, platform, priceOriginal, currencyCode, listingUrl, sourceName)
- [ ] Implement `NintendoEshopAdapter.java` — call Nintendo eShop API, parse results into PriceRecord list
- [ ] Implement `DataBlitzAdapter.java` — scrape DataBlitz product pages using JSoup
- [ ] Implement `iTechAdapter.java` — scrape iTech product pages using JSoup
- [ ] Create `AggregationService.java`:
  - Loops through all registered adapters
  - For each PriceRecord: find or create the game entry, convert price to PHP, upsert the price record
  - Logs errors per-source without crashing the whole job
- [ ] Create `PriceUpdateScheduler.java` in `scheduler` package:
  - Uses `@Scheduled(cron = "0 0 2 * * ?")` to run at 2 AM daily
  - Calls `AggregationService.runFullUpdate()`
- [ ] Test by triggering the aggregation manually (call the service directly from a test or temp endpoint)
- [ ] Confirm prices appear in DB with correct PHP values and timestamps
- [ ] Confirm if one adapter fails, others still complete

---

## Milestone 7 — Wishlist API (F7)
*Goal: Authenticated users can manage a wishlist.*
*Depends on: Milestone 3 (auth) and Milestone 4 (game data)*

- [x] Create `WishlistService.java`:
  - `getWishlist(Integer userId)` — returns user's wishlist
  - `addToWishlist(Integer userId, Integer gameId, BigDecimal alertThreshold)` — adds entry
  - `removeFromWishlist(Integer userId, Integer gameId)` — removes entry
- [x] Create `WishlistController.java`:
  - `GET /api/wishlist` — returns wishlist for authenticated user (JWT required)
  - `POST /api/wishlist/{gameId}` — add to wishlist, optional alert threshold in body (JWT required)
  - `DELETE /api/wishlist/{gameId}` — remove from wishlist (JWT required)
- [x] Price drop notification (basic): after each aggregation run, check if any wishlisted game dropped below a user's threshold — log it for now (email can be added later) *(via `WishlistPriceAlertService`, called from `AggregationService.runFullUpdate()`.)*
- [x] Test all wishlist endpoints in Postman with a valid JWT

---

## Milestone 8 — Deployment
*Goal: Backend is live and accessible from the internet.*

- [x] Create a Railway or Render account
- [x] Connect GitHub repository to Railway/Render
- [x] Set environment variables on the deployment platform:
  - `DB_URL` (Supabase connection string)
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `JWT_SECRET`
  - `FOREX_API_KEY`
  - Any other API keys
- [x] Configure `application-prod.properties` to read from environment variables
- [x] Confirm Flyway runs migrations on first deploy
- [x] Confirm all endpoints respond correctly on the live URL
- [x] Share the live API base URL with your frontend teammate
- [x] Test CORS — confirm frontend origin is allowed

---

## Milestone 9 — Optional Features (time-permitting)

### F6 — Game Metadata
- [ ] Register for IGDB API (via Twitch developer account)
- [ ] Create `MetadataService.java` — fetches Metacritic score, screenshots from IGDB
- [ ] Register for YouTube Data API v3 (via Google Cloud Console)
- [ ] Add trailer URL fetching to `MetadataService`
- [ ] Populate `metacritic_score`, `trailer_url`, `cover_image_url` on game records
- [ ] Expose metadata fields in `GameDetailDto`

### F8 — Community Comments
- [ ] Create `CommentService.java` and `CommentController.java`
  - `GET /api/games/{id}/comments` — public
  - `POST /api/games/{id}/comments` — JWT required
  - `POST /api/comments/{id}/upvote` — JWT required
- [ ] Add content moderation: reject comments with prohibited keywords
- [ ] Test all comment endpoints

---

## Ongoing / Throughout All Milestones

- [ ] Write Javadoc comments on all public service methods
- [ ] Add proper HTTP status codes to all responses (200, 201, 400, 401, 403, 404, 500)
- [ ] Add a global exception handler (`@ControllerAdvice`) so errors return clean JSON, not stack traces
- [ ] Keep `README.md` updated with how to run the project locally
- [ ] Commit to GitHub after every completed milestone
- [ ] Never push API keys or passwords to GitHub

---

## Build Order Summary

```
M0 Setup → M1 DB Schema → M2 Models → M3 Auth → M4 Search/Compare
→ M5 Currency → M6 Aggregation → M7 Wishlist → M8 Deploy → M9 Optional
```

Each milestone builds on the previous. Auth before wishlist. Models before services.
Schema before everything.
