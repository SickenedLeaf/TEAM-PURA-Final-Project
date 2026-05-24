package ScrapeSystem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;

/**
 * Concrete scraper implementation for DataBlitz (Shopify E-commerce).
 * Natively leverages parent parsing and identity transformation pipelines.
 */
public class DatablitzScraper extends GenericScraper {

    // ==========================================
    // CONSTRUCTOR
    // ==========================================
    public DatablitzScraper() {
        // Pointing natively to their shopify master index endpoint from your basis code
        super("DataBlitz", "https://ecommerce.datablitz.com.ph/sitemap.xml");
    }

    // ==========================================
    // POLYMORPHIC CORE PROCESSING HOOK
    // ==========================================
    @Override
    protected Product scrapePage(String url) {
        try {
            Document doc = Jsoup.connect(url)
                                .userAgent(this.userAgent)
                                .timeout(10000)
                                .get();

            // 1. Extract Heading Title Node (Shopify theme product selector)
            Element titleNode = doc.selectFirst("h1.product-meta__title.heading.h1");
            if (titleNode == null) {
                return null; 
            }
            String rawTitle = titleNode.text().trim();

            // 2. Compute Unique Code via the inherited transformation gauntlet method
            String productCode = this.generateUniqueCode(rawTitle);

            // 3. Extract Pricing Block Element
            Element priceNode = doc.selectFirst("span.price");
            String price = (priceNode != null) ? priceNode.text().trim() : "₱0.00";

            // 4. Evaluate Inventory Label Configurations safely
            Element stockNotif = doc.selectFirst("span.product-form__inventory");
            boolean isAvailable = true;

            if (stockNotif != null) {
                if (!stockNotif.text().contains("In Stock")) {
                    isAvailable = false;
                }
            } else {
                // Fallback: If the "In Stock" badge is completely missing from the HTML structure, flag it out of stock
                isAvailable = false; 
            }
            
            // 5. Dynamic Platform Categorization Gauntlet Rule
            String platform;
            if (url.contains("nintendo-switch-2")) {
                platform = "Switch 2 only";
            } else {
                platform = "Nintendo Switch and Switch 2";
            }

            // Return the finalized instance payload cleanly back up into the pipeline
            return new Product(productCode, rawTitle, platform, price, isAvailable, this.storeName, url);

        } catch (IOException e) {
            System.err.println("Failed parsing layout elements at DataBlitz: " + url + " | " + e.getMessage());
            return null;
        }
    }
}
