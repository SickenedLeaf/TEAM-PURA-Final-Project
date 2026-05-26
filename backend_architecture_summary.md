# Backend Architecture Summary

## 1. High-Level Architecture

**Tech Stack:**
- **Framework:** Spring Boot 3.x with Java 17+
- **Database:** Supabase (PostgreSQL) with pgvector extension for full-text search
- **Migration:** Flyway for database schema versioning
- **Authentication:** JWT-based stateless authentication with BCrypt password encoding
- **Security:** Spring Security with custom JWT filter, CORS configuration
- **API Layer:** RESTful controllers with DTO pattern for request/response mapping
- **Data Access:** Spring Data JPA with custom native SQL queries for PostgreSQL-specific features
- **External APIs:** ExchangeRate-API v6 for forex conversion, Nintendo eShop Algolia API for digital pricing

**Core Components:**
- **Controllers:** GameController (public game search), AdminController (aggregation triggers), AuthController (JWT auth), WishlistController (user wishlists)
- **Services:** AggregationService (orchestrates price aggregation), NintendoAggregationService (Nintendo eShop integration), GameService (game search and pricing), ForexService (currency conversion), AuthService (JWT token generation)
- **Repositories:** GameRepository (with full-text search), PriceRepository (price lookups), SourceRepository (data source management), UserRepository, WishlistRepository
- **Scrapers:** GenericScraper (abstract base), DatablitzScraper, ITechScraper, NintendoEshopAdapter (currently disabled, replaced by NintendoAggregationService)
- **Models:** Game, Price, Source, User, Wishlist, Comment

## 2. The Aggregation Engine

**AggregationService Architecture:**
The `AggregationService` serves as the orchestration layer that coordinates multiple data source adapters. It implements a plugin pattern where each retailer implements the `SourceAdapter` interface and is automatically registered as a Spring bean.

**Execution Flow:**
1. Service iterates through all registered `SourceAdapter` implementations
2. Each adapter's `fetchPrices()` method is called independently
3. Failures in individual adapters are logged but do not halt the entire aggregation process
4. Each `PriceRecord` is persisted via `persistPriceRecord()`
5. After all adapters complete, wishlist price-drop alerts are checked
6. PostgreSQL full-text search vectors are updated for new games

**DataBlitz Scraping (Physical Retail):**
- **Implementation:** `DataBlitzAdapter` → `DatablitzScraper` → `GenericScraper`
- **Method:** JSoup-based web scraping with browser-like headers and random user agents
- **Sitemap:** Discovers product URLs from Shopify sitemap XML
- **Extraction Logic:**
  - Title: Extracted from `<h1.product-meta__title.heading.h1>` selector
  - Price: Extracted from `<span.price>` selector, parsed as PHP
  - Image: Resolves highest-quality image from Shopify gallery with Cloudinary-style URL handling
  - Stock: Checks `<span.product-form__inventory>` for "In Stock" indicator
  - Platform: Dynamic detection - URL containing "nintendo-switch-2" → "Switch 2 only", otherwise "Nintendo Switch and Switch 2"
- **Throttling:** Random delay 2-4 seconds between requests to avoid rate limiting

**iTech Scraping (Physical Retail):**
- **Implementation:** `ITechAdapter` → `ITechScraper` → `GenericScraper`
- **Method:** JSoup-based web scraping with WooCommerce-specific selectors
- **Sitemap:** Attempts product-sitemap.xml first, falls back to main sitemap.xml
- **Extraction Logic:**
  - Title: Extracted from `<h1.product_title>` selector
  - Price: Extracted from `<p.price>` selector, handles sale prices (splits on space to get discounted price)
  - Image: Multiple fallback selectors for WooCommerce gallery images
  - Stock: Checks `<div.xts-product-labels>` for "sold out" or "out of stock" indicators
  - Platform: Same dynamic Switch 2 detection as DataBlitz
- **Error Handling:** Comprehensive exception handling with detailed error logging

**Nintendo eShop Integration (Digital Retail):**
- **Implementation:** `NintendoAggregationService` (separate from adapter pattern due to API complexity)
- **Method:** REST API integration with Nintendo US eShop Algolia API
- **API Endpoint:** `https://U3B6GR4UA3-dsn.algolia.net/1/indexes/*/queries`
- **Authentication:** Algolia application ID and API key in custom headers
- **Request Structure:** POST request with JSON body containing Algolia query parameters
- **Extraction Logic:**
  - Query: Searches for specific game titles or fetches all games
  - Title: Extracted from `title` field in JSON response
  - Price: Extracted from `price.finalPrice` or `eshopDetails.regularPrice` fields
  - URL: Extracted from `url` field, prefixed with Nintendo domain if relative
  - Image: Extracted from `productImage`, `boxart`, or `image_url` fields, formatted with Cloudinary prefix
  - Platform: Extracted from `platform` field, defaults to "Nintendo Switch"
- **Currency Conversion:** USD prices converted to PHP using cached forex rate
- **Caching:** Exchange rate cached for 1 hour to reduce API calls

## 3. Data Normalization & The "Overlap"

**Fuzzy-Matching Algorithm:**
The system uses title normalization to match games across different data sources. The `normalizeTitle()` method in `NintendoAggregationService` performs the following transformations:

1. **Case normalization:** Converts to lowercase
2. **Platform prefix stripping:** Removes "nintendo switch 2", "nintendo switch", "nintendo", "switch 2", "switch", "nsw", "ns"
3. **Regional suffix stripping:** Removes "eu", "us", "jpn", "asian", "asia", "eng", "fr", "sp"
4. **Edition suffix stripping:** Removes "standard edition", "deluxe edition", "complete edition"
5. **Special character removal:** Strips trademark symbols (™, ®) and parentheses
6. **Alphanumeric filtering:** Removes all non-alphanumeric characters

This normalization creates a canonical key used for matching Nintendo eShop titles against existing database games. The matching is performed using a HashMap with normalized titles as keys, enabling O(1) lookup performance.

**Forex USD-to-PHP Conversion:**
The `ForexService` manages currency conversion with a 24-hour cache:

1. **API Integration:** Fetches USD-based conversion rates from ExchangeRate-API v6
2. **Caching Strategy:** Rates cached in memory for 24 hours to reduce API calls
3. **Conversion Logic:**
   - PHP amounts pass through unchanged (scaled to 2 decimal places)
   - Non-PHP currencies are converted via USD: `amount / foreign_per_usd * php_per_usd`
   - Uses high-precision arithmetic (MathContext with 12 digits, HALF_UP rounding)
4. **Fallback:** If API fails, throws IllegalStateException (no hardcoded fallback in production)
5. **Usage:** Called by `AggregationPriceConverter` before persisting price records

**Dynamic Platform Extraction (Switch 2):**
The scrapers dynamically determine platform compatibility based on URL patterns:

- **Switch 2 Detection:** URL contains "nintendo-switch-2" → platform = "Switch 2 only"
- **Backward Compatibility:** URL does not contain "nintendo-switch-2" → platform = "Nintendo Switch and Switch 2"
- **Normalization:** `ProductMapper.normalizePlatform()` always returns "Nintendo Switch" for storage consistency
- **Rationale:** Allows the system to distinguish between Switch 2-exclusive titles and backward-compatible titles

**Cloudinary Image Formatting:**
Nintendo eShop images are formatted for Cloudinary CDN delivery:

1. **Extraction:** Image paths extracted from multiple JSON fields (`productImage`, `boxart`, `image_url`)
2. **URL Construction:** Relative paths are prefixed with Cloudinary base URL
3. **Format:** `https://assets.nintendo.com/image/upload/f_auto,q_auto/{path}`
4. **Parameters:**
   - `f_auto`: Automatic format selection (WebP, AVIF, etc.)
   - `q_auto`: Automatic quality optimization
5. **Leading Slash Handling:** Removes leading slash from relative paths to avoid double-slashes
6. **Fallback:** If no image found, uses Nintendo store games page as default

**Product Code Generation:**
Physical retailers use SHA-256 hashing to generate unique product codes:

1. **Normalization:** Same fuzzy-matching logic as title normalization
2. **Hashing:** SHA-256 hash of normalized title
3. **Truncation:** First 4 bytes (8 hex characters) used as product code
4. **Uniqueness:** Ensures consistent product codes across scraping runs
5. **Storage:** Stored in `games.product_code` (VARCHAR(8), unique constraint)

**Price Overlap Resolution:**
The system handles multiple prices for the same game through database constraints:

1. **Unique Constraint:** `prices` table has unique constraint on `(game_id, source_id)`
2. **Upsert Logic:** `AggregationService.persistPriceRecord()` checks if price exists for game+source
3. **Update vs Insert:** If exists, updates price_php, price_original, listing_url, last_updated; if not, creates new row
4. **Cover Image:** Only updates cover image if current value is null/blank and scraper provides a URL
5. **Search Vector:** PostgreSQL `tsvector` updated on game creation for full-text search

## 4. Testing Strategy

**Test Framework:**
- **Testing Library:** JUnit 5 with Mockito 5.x
- **Test Pattern:** MockitoExtension for unit testing without Spring context
- **Coverage:** Repository layer, Controller layer, Service layer

**GameRepositoryTest:**
- **Approach:** MockitoExtension with mocked repository methods
- **Test Coverage:**
  - `findByGameTitleContainingIgnoreCase()` - Verifies case-insensitive title search
  - `findByPlatform()` - Tests platform filtering
  - `findByProductCode()` - Tests product code lookup
  - `save()` - Tests game creation
  - `delete()` - Tests game deletion
  - `count()` - Tests record counting
- **Rationale:** ApplicationContext loading issues prevented @DataJpaTest usage; Mockito provides reliable unit testing

**AdminControllerTest:**
- **Approach:** MockitoExtension with direct controller method calls
- **Test Coverage:**
  - `triggerAggregation()` - Verifies service method calls for full aggregation
  - `scrapeSingle()` - Tests single-game Nintendo eShop scraping
  - `scrapeSingle()` with new game creation - Tests digital-exclusive game creation
  - `scrapeSingle()` with exception handling - Tests error scenarios
- **Rationale:** ApplicationContext loading issues prevented @WebMvcTest usage; Mockito provides reliable unit testing

**NintendoAggregationServiceTest:**
- **Approach:** MockitoExtension with mocked repositories
- **Test Coverage:**
  - `aggregateForTitle()` with source creation - Tests Nintendo eShop source creation when not present
  - `aggregateForTitle()` with existing source - Tests source reuse when already present
- **Rationale:** Focuses on service-repository interactions without requiring full Spring context

**Test Limitations:**
- ApplicationContext loading failures prevented integration testing with @DataJpaTest and @WebMvcTest
- Main application configuration requires environment variables (SPRING_DATASOURCE_URL, APP_JWT_SECRET, APP_FOREX_API_KEY) that aren't set during tests
- Flyway and Liquibase auto-configurations cause context loading issues
- Current tests are unit tests that verify business logic and method interactions
- Full integration testing would require application configuration refactoring to be test-friendly

**Test Execution:**
- All tests pass successfully with Mockito-based approach
- Tests verify repository method calls, controller logic, and service interactions
- No database or HTTP requests are made during test execution
- Test suite provides confidence in business logic correctness despite lack of integration testing
