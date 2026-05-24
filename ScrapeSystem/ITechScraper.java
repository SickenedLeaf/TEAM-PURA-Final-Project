package ScrapeSystem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;

/**
 * Concrete scraper implementation for iTech Philippines.
 * Stabilized variables and safe token tracking parameters.
 */
public class ITechScraper extends GenericScraper {

    // ==========================================
    // CONSTRUCTOR
    // ==========================================
    public ITechScraper() {
        super("iTech", "https://www.itech.ph/sitemap.xml");
    }

    // ==========================================
    // POLYMORPHIC CORE PROCESSING HOOK
    // ==========================================
    @Override
    protected Product scrapePage(String url) {
        // Safe check: If the incoming URL string is null or blank, exit immediately
        if (url == null || url.trim().isEmpty()) {
            System.err.println("[iTech Error] Received an invalid or null URL reference.");
            return null;
        }

        try {
            // Execute connection using the inherited base configurations
            Document doc = Jsoup.connect(url)
                                .userAgent(this.userAgent)
                                .timeout(12000) // Slightly bumped timeout limit for stability
                                .get();

            // 1. Extract Title Node
            Element titleNode = doc.selectFirst("h1.product_title");
            if (titleNode == null) {
                System.err.println("[iTech Skip] Unable to resolve h1.product-title at link: " + url);
                return null; 
            }
            String rawTitle = titleNode.text().trim();

            // 2. Compute Unique Code ID
            String productCode = this.generateUniqueCode(rawTitle);
            
            Element boxArtNode = doc.selectFirst(".xts-col-inner a[href*='/uploads/']");
            if(boxArtNode == null) {
            	System.err.println("[iTech Skip] Unable to resolve box art at link: " + url);
            	return null;
            }
            String boxArtUrl = boxArtNode.attr("href").trim();
            
            // 3. Extract Pricing Block Element safely
            Element priceNode = doc.selectFirst("p.price");
            String price = "₱0.00";
            
            if (priceNode != null) {
                String combinedPrice = priceNode.text().trim();
                
                // If it contains a space, split it to isolate the sale price value
                if (combinedPrice.contains(" ")) {
                    String[] priceParts = combinedPrice.split("\\s+");
                    if (priceParts.length > 1) {
                        price = priceParts[1].trim(); 
                    } else {
                        price = combinedPrice;
                    }
                } else {
                    price = combinedPrice;
                }
            }

            // 4. Evaluate Inventory Status safely
            Element labelNode = doc.selectFirst("div.xts-product-labels");
            boolean isAvailable = true;
            if (labelNode != null) {
                String labelText = labelNode.text().toLowerCase();
                if (labelText.contains("sold out") || labelText.contains("out of stock")) {
                    isAvailable = false;
                }
            }
            
            // 5. Categorize Platform explicitly using the local URL input argument
            String platform;
            if (url.toLowerCase().contains("nintendo-switch-2")) {
                platform = "Switch 2 only";
            } else {
                platform = "Nintendo Switch and Switch 2";
            }

            // Return the finalized instance payload
            return new Product(productCode, rawTitle, platform, price, isAvailable, this.storeName, url, boxArtUrl);

        } catch (IOException e) {
            System.err.println("❌ Network / Parsing Exception at iTech URL [" + url + "]: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("❌ Critical runtime breakdown at iTech URL [" + url + "]: " + e.getMessage());
            e.printStackTrace(); // This prints out the exact line number causing the issue
            return null;
        }
    }
}