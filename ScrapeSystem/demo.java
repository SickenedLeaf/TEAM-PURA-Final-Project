package ScrapeSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Integrated orchestration runner to test both iTech and DataBlitz scraping pipelines,
 * aggregate their extracted listings, and cleanly export the results to a JSON cache.
 */
public class demo {


    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   MULTI-STORE BACKGROUND PIPELINE INTEGRATION TEST ");
        System.out.println("==================================================");

        // A unified accumulator list to capture all valid scraped product payloads
        List<Product> compiledMasterList = new ArrayList<>();

        // 1. Initialize an array of our polymorphic concrete background workers
        GenericScraper[] scrapers = {
            new ITechScraper(),
            new DatablitzScraper()
        };

        // 2. Execute sequential scraping operations across both platforms
        for (GenericScraper scraper : scrapers) {
            System.out.println("\n--------------------------------------------------");
            System.out.println("▶ STARTING PIPELINE RUN FOR STORE: " + scraper.storeName.toUpperCase());
            System.out.println("--------------------------------------------------");
            
            System.out.println("[Step 1] Resolving XML sitemap index elements...");
            List<String> activeSubSitemaps = scraper.discoverSubSitemaps();
            
            System.out.println("[Step 2] Scraping sub-sitemaps and running RAM platform filters...");
            List<String> filteredUrls = scraper.extractAndFilterUrls(activeSubSitemaps);

            if (filteredUrls.isEmpty()) {
                System.err.println("❌ Alert: No Nintendo Switch URLs captured for " + scraper.storeName);
                continue;
            }

            // Cap the integrated test run to the first 10 listings for performance tracking
            int totalDiscovered = filteredUrls.size();
            int testLimit = Math.min(10, totalDiscovered);
            
            System.out.printf("🔍 Total inventory links in RAM buffer: %d%n", totalDiscovered);
            System.out.printf("🎯 Isolating the first %d items for HTML node testing...%n", testLimit);

            // [Step 3] Execute crawling loops and store the instances
            for (int i = 0; i < testLimit; i++) {
                String targetUrl = filteredUrls.get(i);
                System.out.printf("%n  [%s - Item %d/%d] Requesting link: %s%n", scraper.storeName, (i + 1), testLimit, targetUrl);
                
                Product payload = scraper.scrapePage(targetUrl);

                System.out.println("  ------------------------------------------------");
                if (payload != null) {
                    // Accumulate the product directly into our cache array buffer
                    compiledMasterList.add(payload);
                    
                    System.out.println("    • Generated Code ID : " + payload.getProductCode());
                    System.out.println("    • Raw Scraped Title : " + payload.getTitle());
                    System.out.println("    • Extracted Price   : " + payload.getPrice());
                    System.out.println("    • Inventory Asset   : " + (payload.isAvailable() ? "IN STOCK" : "OUT OF STOCK"));
                    System.out.println("    • Art Url   : " + payload.getBoxArtUrl());
                } else {
                    System.err.println("    ❌ Parsing Error: Concrete hook returned a null data payload.");
                }
                System.out.println("  ------------------------------------------------");
                
                // Polite throttling delay to shield local network IP profiles
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            System.out.printf("%n✔ PIPELINE CONCLUDED FOR %s CLEANLY%n", scraper.storeName.toUpperCase());
            System.out.println("--------------------------------------------------");
        }
    }
}