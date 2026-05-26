package com.gamecheck.scraper;

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
            Document doc = openBrowserLikeConnection(url).get();

            // 1. Extract Heading Title Node (Shopify theme product selector)
            Element titleNode = doc.selectFirst("h1.product-meta__title.heading.h1");
            if (titleNode == null) {
                return null; 
            }
            String rawTitle = titleNode.text().trim();

            // 2. Compute Unique Code via the inherited transformation gauntlet method
            String productCode = this.generateUniqueCode(rawTitle);
            
            Element boxArtNode = doc.selectFirst(".aspect-ratio img.product-gallery__image, img.product-gallery__image");
            if (boxArtNode == null) {
                System.err.println("[DataBlitz Skip] Unable to resolve box art at link: " + url);
                return null;
            }

            String boxArtUrl = resolveHighestQualityImage(boxArtNode);
            if (boxArtUrl.isEmpty() && boxArtNode.hasAttr("src")) {
                boxArtUrl = boxArtNode.attr("src").trim();
            }

            if (boxArtUrl.startsWith("//")) {
                boxArtUrl = "https:" + boxArtUrl;
            }

            if (boxArtUrl.isEmpty()) {
                System.err.println("[DataBlitz Skip] Unable to resolve high-res box art at link: " + url);
                return null;
            }

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
                isAvailable = false; 
            }
            
            // 5. Dynamic Platform Categorization Gauntlet Rule
            String platform;
            if (url.contains("nintendo-switch-2")) {
                platform = "Nintendo Switch 2";
            } else {
                platform = "Nintendo Switch";
            }

            // Return the finalized instance payload cleanly back up into the pipeline
            return new Product(productCode, rawTitle, platform, price, isAvailable, this.storeName, url, boxArtUrl);

        } catch (IOException e) {
            System.err.println("Failed parsing layout elements at DataBlitz: " + url + " | " + e.getMessage());
            return null;
        }
    }
}
