package com.gamecheck.scraper;

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
        super("iTech", determineWorkingSitemapUrl());
    }

    private static String determineWorkingSitemapUrl() {
        // Try product-sitemap.xml first (WooCommerce stores often expose this directly)
        String productSitemapUrl = "https://www.itech.ph/product-sitemap.xml";
        String mainSitemapUrl = "https://www.itech.ph/sitemap.xml";

        try {
            // Attempt to fetch the product sitemap with browser spoofing headers
            org.jsoup.Jsoup.connect(productSitemapUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .timeout(10000)
                .get();
            System.out.println("[iTech] Using product-sitemap.xml directly");
            return productSitemapUrl;
        } catch (Exception e) {
            System.out.println("[iTech] product-sitemap.xml not accessible, falling back to main sitemap.xml");
            return mainSitemapUrl;
        }
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
            Document doc = openBrowserLikeConnection(url).get();

            // 1. Extract Title Node
            Element titleNode = doc.selectFirst("h1.product_title");
            if (titleNode == null) {
                System.err.println("[iTech Skip] Unable to resolve h1.product-title at link: " + url);
                return null; 
            }
            String rawTitle = titleNode.text().trim();

            // 2. Compute Unique Code ID
            String productCode = this.generateUniqueCode(rawTitle);

            // 3. Extract Box Art URL
            Element boxArtNode = doc.selectFirst(".woocommerce-product-gallery__image img, .xts-col-inner img[srcset], .xts-col-inner img[data-srcset], .xts-col-inner img[data-large_image]");
            String boxArtUrl = "";

            if (boxArtNode != null) {
                boxArtUrl = resolveHighestQualityImage(boxArtNode);
            }

            if (boxArtUrl.isBlank()) {
                Element fallbackAnchor = doc.selectFirst(".xts-col-inner a[href*='/uploads/']");
                if (fallbackAnchor != null && fallbackAnchor.hasAttr("href")) {
                    boxArtUrl = fallbackAnchor.attr("href").trim();
                }
            }

            if (boxArtUrl.startsWith("//")) {
                boxArtUrl = "https:" + boxArtUrl;
            }

            if (boxArtUrl.isBlank()) {
                System.err.println("[iTech Skip] Unable to resolve box art at link: " + url);
                return null;
            }
            
            // 4. Extract Pricing Block Element safely
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
                platform = "Nintendo Switch 2";
            } else {
                platform = "Nintendo Switch";
            }

            // Return the finalized instance payload
            return new Product(productCode, rawTitle, platform, price, isAvailable, this.storeName, url, boxArtUrl);

        } catch (IOException e) {
            System.err.println("Network / Parsing Exception at iTech URL [" + url + "]: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Critical runtime breakdown at iTech URL [" + url + "]: " + e.getMessage());
            e.printStackTrace(); // This prints out the exact line number causing the issue
            return null;
        }
    }
}
