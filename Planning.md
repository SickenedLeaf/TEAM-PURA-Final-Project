# Planning.md — GameCheck Backend

> This document covers the product vision, system architecture, technology stack,
> and all required tools and services needed to build the GameCheck backend.

---

## Product Vision

GameCheck solves a real gap for Filipino gamers: there is currently no single platform
that compares physical retail prices (DataBlitz, iTech, GameOne) against digital storefront
prices (Nintendo eShop, Steam, Shopee, Lazada) in one place, in Philippine Peso.

The goal is a web application where a Filipino gamer can search any Nintendo Switch title
and instantly see every available price from every tracked source, sorted cheapest to most
expensive — without having to open five browser tabs.

**Version 1.0 scope:** Nintendo Switch titles only, Philippine market only, no native mobile app.
**Future scope:** Expand to PS5/Xbox/PC titles, gaming gear (controllers, headphones), potentially monetize.

---

## System Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────┐
│                   EXTERNAL SOURCES                  │
│  Nintendo eShop API │ DataBlitz (scrape) │ iTech     │
│  Steam API │ Shopee API │ Lazada API │ Forex API     │
└────────────────────────┬────────────────────────────┘
                         │ daily cron job
                         ▼
┌─────────────────────────────────────────────────────┐
│             DATA AGGREGATION ENGINE                 │
│  One adapter class per source (modular pattern)     │
│  Fetches → Converts to PHP → Upserts to DB          │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│              POSTGRESQL DATABASE                    │
│  (Hosted on Supabase free tier)                     │
│  Tables: games, prices, sources,                    │
│          users, wishlists, comments                 │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│           SPRING BOOT REST API (Backend)            │
│  Controllers → Services → Repositories              │
│  Auth via Spring Security + JWT                     │
│  Exposes JSON endpoints at /api/...                 │
└────────────────────────┬────────────────────────────┘
                         │ HTTP (JSON)
                         ▼
┌─────────────────────────────────────────────────────┐
│         FRONTEND — HTML / CSS / JavaScript          │
│  (Separate teammate responsibility)                 │
│  Calls backend via fetch() to REST endpoints        │
└─────────────────────────────────────────────────────┘
```

### Backend Layer Breakdown

```
Controller Layer     — Receives HTTP requests, validates input, returns HTTP responses
      ↕
Service Layer        — All business logic lives here (price comparison, auth, aggregation)
      ↕
Repository Layer     — Talks to the database via Spring Data JPA interfaces
      ↕
Database (PostgreSQL) — Stores all persistent data
```

### Data Aggregation Flow (Daily Cron)

```
Scheduler triggers (daily)
      ↓
For each source adapter:
  → Fetch raw data (API call or web scrape)
  → Parse response into standard format
  → Convert price to PHP (via Forex API)
  → Upsert into prices table (update if exists, insert if new)
  → Update last_updated timestamp
      ↓
Done — DB now has fresh prices
```

### Adapter Pattern for Scrapers

Every data source implements one common interface:

```java
public interface SourceAdapter {
    String getSourceName();
    List<PriceRecord> fetchPrices();  // returns standardized price data
}

// One class per source:
// DataBlitzAdapter.java
// iTechAdapter.java
// NintendoEshopAdapter.java
// SteamAdapter.java
// etc.
```

This means adding a new retailer later = create one new class, zero changes to core logic.

---

## Technology Stack

### Backend

| Technology | Purpose | Why this choice |
|---|---|---|
| **Java 17+** | Backend language | Team has OOP Java background from last semester |
| **Spring Boot 3.x** | Web framework | Industry standard for Java REST APIs, extensive docs |
| **Maven** | Build tool | Simpler configuration than Gradle for first-timers |
| **Spring Data JPA** | ORM / DB layer | Eliminates most boilerplate SQL, works seamlessly with Spring Boot |
| **Hibernate** | JPA implementation | Comes bundled with Spring Data JPA |
| **Spring Security** | Auth framework | Handles security config, password encoding, request filtering |
| **JWT (JSON Web Token)** | Session management | Stateless auth — no server-side session storage needed |
| **BCrypt** | Password hashing | Industry standard, built into Spring Security |
| **Flyway** | DB migrations | Tracks schema changes as versioned SQL scripts |
| **Spring Scheduler** | Cron jobs | Built into Spring Boot — triggers daily price updates |
| **JSoup** | Web scraping | Java library for parsing HTML from retailer pages |
| **OkHttp / RestTemplate** | HTTP client | For calling external APIs (eShop, Steam, Forex) |

### Database

| Technology | Purpose | Why this choice |
|---|---|---|
| **PostgreSQL** | Primary database | Relational, handles the schema well, free on Supabase |
| **Supabase** | DB hosting | Free tier, visual dashboard, instant setup, provides connection string |

### Deployment

| Technology | Purpose | Why this choice |
|---|---|---|
| **Railway** or **Render** | Backend hosting | Free tier supports Java/Spring Boot, simple Git-based deploy |
| **Supabase** | DB hosting | Already used for PostgreSQL — no extra service needed |

### Frontend (Teammate — not your concern)

| Technology | Purpose |
|---|---|
| HTML / CSS / JavaScript | UI layer |
| `fetch()` API | Calls your REST endpoints |

---

## Required Tools & Services

### Development Tools (install on your machine)

| Tool | Purpose | Where to get |
|---|---|---|
| **JDK 17+** | Java runtime & compiler | https://adoptium.net |
| **IntelliJ IDEA** (Community) | IDE — best for Spring Boot | https://www.jetbrains.com/idea |
| **Maven** | Build tool (bundled with IntelliJ) | Comes with IntelliJ |
| **Git** | Version control | https://git-scm.com |
| **Postman** | Test your REST API endpoints | https://www.postman.com |
| **DBeaver** (optional) | Visual DB browser for PostgreSQL | https://dbeaver.io |

### External Services (create free accounts)

| Service | Purpose | URL |
|---|---|---|
| **Supabase** | PostgreSQL database hosting | https://supabase.com |
| **Railway** or **Render** | Spring Boot app deployment | https://railway.app / https://render.com |
| **GitHub** | Code repository, also used for deployment triggers | https://github.com |

### External APIs (requires registration/keys)

| API | Purpose | Notes |
|---|---|---|
| **Nintendo eShop API** | Game titles and prices | Unofficial/community endpoints exist (no official key needed for some) |
| **Steam Web API** | Steam game prices | Free key at https://steamcommunity.com/dev/apikey |
| **Forex / ExchangeRate API** | USD → PHP conversion | Free tier at https://www.exchangerate-api.com |
| **IGDB API** | Game metadata, cover images | Free, requires Twitch developer account at https://api-docs.igdb.com |
| **YouTube Data API v3** | Game trailer links | Free quota, requires Google Cloud account |

### API Keys Management

- Never hardcode API keys in source code
- Store them in `application.properties` or environment variables on the deployment server
- Add `application.properties` to `.gitignore` so keys never get pushed to GitHub

---

## CORS Configuration

Since the frontend (your teammate) and backend run on different origins during development,
Spring Boot needs CORS configured to allow cross-origin requests from the frontend's domain.

Add a global CORS config bean in the `config` package — allow all origins during development,
then lock it down to the actual frontend domain before final deployment.

---

## Environment Separation

| Environment | Purpose |
|---|---|
| **Local (dev)** | Your machine — connect to Supabase DB directly |
| **Staging** (optional) | Test deployment before final demo |
| **Production** | Final deployed version on Railway/Render |

Use Spring Boot profiles (`application-dev.properties`, `application-prod.properties`) to
switch configs between environments without changing code.

---

## Key Design Decisions

1. **Adapter pattern for scrapers** — every new data source is a new class, never modifies existing code. Required by the SRS maintainability spec.

2. **Price conversion at ingestion** — all prices converted to PHP when scraped/fetched, not when queried. This keeps the comparison logic simple and fast.

3. **JWT over sessions** — stateless auth means the backend doesn't need to track active sessions in memory or DB. Scales better and simpler to deploy.

4. **Flyway for migrations** — every DB schema change is a numbered SQL file (V1__, V2__, etc.). Flyway runs them automatically on startup. Keeps DB in sync across dev and production.

5. **Upsert strategy for prices** — daily scraper doesn't insert new rows every run. It updates the existing price record for a (game, source) pair and refreshes `last_updated`. Keeps the prices table clean.

6. **Graceful degradation** — if a scraper fails for one source, log the error and continue with other sources. Never crash the whole aggregation job over one bad source.
