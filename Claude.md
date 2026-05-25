# Claude.md — GameCheck Backend AI Briefing File

> Load this file at the start of every AI coding session.
> This file gives full context on the GameCheck project so no re-explaining is needed.

---

## Project Identity

- **Project name:** GameCheck
- **Team name:** Team Pura
- **Course:** CPE 2201 – Software Design, University of San Carlos
- **Developer (backend):** Khyzian Shaun Dumoran
- **Current SRS version:** 1.2 (as of May 9, 2026)

---

## What GameCheck Is

GameCheck is a Philippine-focused, Nintendo-focused video game price comparison web application. It aggregates pricing data from both physical retail stores (e.g., DataBlitz, iTech, GameOne) and digital storefronts (e.g., Nintendo eShop, Steam, Shopee, Lazada), converts all prices to Philippine Peso (PHP), and presents them side-by-side so Filipino gamers can find the cheapest available price for a game.

**The problem it solves:** Filipino gamers currently have no single platform that compares physical store prices vs. digital storefront prices in one place, in PHP.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend language | Java |
| Backend framework | Spring Boot |
| Build tool | Maven |
| Database ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL (hosted on Supabase free tier) |
| Authentication | Spring Security + JWT |
| Password hashing | BCrypt |
| REST API format | JSON |
| Deployment | Railway or Render (free tier) |
| Frontend (teammate) | HTML, CSS, vanilla JavaScript |
| Frontend ↔ Backend | REST API (JSON responses via fetch()) |

---

## System Architecture Overview

```
[External Sources]
  DataBlitz (scrape), iTech (scrape), Nintendo eShop (API),
  Steam (API), Shopee (API), Lazada (API), Forex/BSP (API)
        |
        v
[Data Aggregation Engine]  ← scheduled cron job (daily)
  Scraper/adapter modules per source
  Upserts price records into PostgreSQL
        |
        v
[PostgreSQL Database]
  Tables: games, prices, sources, users, wishlists, comments
        |
        v
[Spring Boot REST API]
  Controllers → Services → Repositories → DB
        |
        v
[Frontend — HTML/CSS/JS]
  Calls backend via fetch() to REST endpoints
  Renders price comparison tables, search results, etc.
```

---

## Spring Boot Project Package Structure

```
com.gamecheck
├── controller        # REST endpoints (@RestController)
├── service           # Business logic (@Service)
├── repository        # DB queries (@Repository, JPA interfaces)
├── model             # Entity classes (@Entity) mapped to DB tables
├── dto               # Data Transfer Objects (what the API sends/receives)
├── scraper           # One class per retailer source (adapter pattern)
├── scheduler         # Cron job that triggers daily price updates
├── security          # Spring Security config, JWT filter, auth logic
└── config            # App-wide config beans (CORS, API keys, etc.)
```

---

## Database Schema

All prices stored and displayed in **Philippine Peso (PHP)**. Currency conversion happens at ingestion time (scraper/aggregation layer), not at query time.

### Table: `games`
| Column | Type | Notes |
|---|---|---|
| game_id | SERIAL PRIMARY KEY | Auto-incrementing |
| game_title | VARCHAR(255) | Official title |
| platform | VARCHAR(50) | e.g., "Nintendo Switch", "PC" |
| metacritic_score | INTEGER | 0–100, nullable (optional feature) |
| trailer_url | TEXT | YouTube URL, nullable (optional feature) |
| cover_image_url | TEXT | Nullable |

### Table: `sources`
| Column | Type | Notes |
|---|---|---|
| source_id | SERIAL PRIMARY KEY | |
| source_name | VARCHAR(100) | e.g., "DataBlitz", "Steam" |
| source_type | VARCHAR(20) | "physical" or "digital" |
| source_url | TEXT | Base URL of the retailer |

### Table: `prices`
| Column | Type | Notes |
|---|---|---|
| price_id | SERIAL PRIMARY KEY | |
| game_id | INTEGER FK → games | |
| source_id | INTEGER FK → sources | |
| price_php | DECIMAL(10,2) | Converted price in PHP |
| price_original | DECIMAL(10,2) | Original price before conversion |
| currency_code | VARCHAR(10) | ISO 4217, e.g., "USD", "PHP" |
| listing_url | TEXT | Direct link to the product listing |
| last_updated | TIMESTAMP | When this record was last refreshed |

### Table: `users`
| Column | Type | Notes |
|---|---|---|
| user_id | SERIAL PRIMARY KEY | |
| email | VARCHAR(255) UNIQUE | Login identifier |
| password_hash | VARCHAR(255) | BCrypt hashed, never plain text |
| created_at | TIMESTAMP | |
| is_active | BOOLEAN | For soft-disable without deletion |

### Table: `wishlists`
| Column | Type | Notes |
|---|---|---|
| wishlist_id | SERIAL PRIMARY KEY | |
| user_id | INTEGER FK → users | |
| game_id | INTEGER FK → games | |
| price_alert_threshold | DECIMAL(10,2) | Nullable — user-defined alert price in PHP |
| added_at | TIMESTAMP | |

### Table: `comments` *(optional feature)*
| Column | Type | Notes |
|---|---|---|
| comment_id | SERIAL PRIMARY KEY | |
| user_id | INTEGER FK → users | |
| game_id | INTEGER FK → games | |
| comment_body | TEXT | |
| upvote_count | INTEGER | Default 0 |
| created_at | TIMESTAMP | |

---

## Feature Priorities (from SRS)

### Core — must be built
| ID | Feature |
|---|---|
| F1 | Game search by title/keyword with platform filter |
| F2 | Daily automated price aggregation from all sources |
| F3 | PHP currency conversion (updated every 24 hours) |
| F4 | Price comparison display — side-by-side, cheapest highlighted |
| F5 | Sorting and filtering (price, platform, source type) |
| F9 | User registration, login, logout, password reset |

### Optional — implement after core is stable
| ID | Feature |
|---|---|
| F6 | Game metadata (Metacritic score, screenshots, trailer) |
| F7 | Wishlist with price drop notifications |
| F8 | Community comments with upvotes/downvotes |

---

## Supported Data Sources

### Mandatory (must be scraped/integrated)
| Source | Method |
|---|---|
| Nintendo eShop (US) | API |
| iTech PH | Web scrape |
| DataBlitz PH | Web scrape |

### Optional (time-permitting)
| Source | Method |
|---|---|
| Steam | Steam Web API |
| Shopee PH | API |
| Lazada PH | API |
| GameOne | Web scrape |
| Toy Kingdom PH | Web scrape |
| DekuDeals | Web scrape |
| Metacritic / IGDB | API |
| YouTube Data API | API |
| Forex / BSP API | API (for currency conversion) |

---

## API Conventions

- All endpoints are prefixed with `/api`
- Responses are JSON
- Auth-protected endpoints require a `Bearer <JWT>` token in the `Authorization` header
- Timestamps in ISO 8601 format
- Prices always returned as numbers (not strings), in PHP

### Key REST Endpoints (to be built)

```
GET    /api/games/search?query=&platform=       → search games
GET    /api/games/{id}                          → game detail + all prices
GET    /api/games/{id}/prices                   → price comparison table
POST   /api/auth/register                       → create account
POST   /api/auth/login                          → returns JWT
POST   /api/auth/logout                         → invalidate token
GET    /api/wishlist                            → get user's wishlist (auth required)
POST   /api/wishlist/{gameId}                   → add to wishlist (auth required)
DELETE /api/wishlist/{gameId}                   → remove from wishlist (auth required)
```

---

## Security Requirements

- Passwords hashed with **BCrypt** — never stored plain text
- All API communication over **HTTPS**
- Login rate limiting — lockout after 5 failed attempts in 10 minutes
- Input validation and sanitization on all endpoints (prevent SQL injection, XSS)
- JWT tokens for session management
- Compliance with Philippine Data Privacy Act (RA 10173)

---

## Non-Functional Requirements

- Search results returned within **3 seconds**
- Price comparison table loads within **2 seconds**
- Full price update cycle completes within **6 hours**
- Price data never older than **24 hours**
- Currency rates refreshed every **24 hours**
- 99% monthly uptime target
- If a data source goes offline, show last-known prices with a staleness timestamp — do not crash or show empty results
- New retailer sources must be addable by creating a new adapter class, without touching core comparison logic

---

## Constraints

- Philippine market only — all prices in PHP
- Nintendo Switch titles only for v1.0
- No native mobile app — responsive web only
- Web scraping of DataBlitz/GameOne may violate their ToS — monitor this risk
- Designed using COMET methodology, documented in UML (course requirement)
- This is a prototype/final project — not a commercial release yet

---

## Important Notes for AI Sessions

- **Do not generate frontend code** — that is a separate teammate's responsibility
- **Do not suggest NoSQL databases** — the relational schema is fixed per the SRS
- **Keep scraper classes modular** — each source gets its own adapter class implementing a common interface
- **Optional features (F6, F7, F8) should never block core features (F1–F5, F9)**
- **First-time Spring Boot project** — explanations of Spring Boot concepts are welcome and encouraged
- When generating code, use **Java 17+** conventions
- Use **Spring Boot 3.x** (latest stable)
- Database migrations should use **Flyway** (integrates cleanly with Spring Boot)
