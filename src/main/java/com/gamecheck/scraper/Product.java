package com.gamecheck.scraper;

/**
 * Represents the finalized parameter data scraped from a retailer page.
 * Follows strict encapsulation guidelines.
 */
public class Product {
    
    // ==========================================
    // INSTANCE VARIABLES (Encapsulated & Immutable)
    // ==========================================
    private final String productCode; // The dynamic 8-character unique hash ID
    private final String title;       // Raw title from the storefront
    private final String platform;    // Detected console generation (e.g., Nintendo Switch)
    private final String price;       // Normalized price string
    private final boolean isAvailable; // Stock availability flag
    private final String storeName;   // "DataBlitz" or "iTech"
    private final String url;         // Source product link
    private final String boxArtUrl;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================
    public Product(String productCode, String title, String platform, String price, 
                       boolean isAvailable, String storeName, String url, String boxArtUrl) {
        this.productCode = productCode;
        this.title = title;
        this.platform = platform;
        this.price = price;
        this.isAvailable = isAvailable;
        this.storeName = storeName;
        this.url = url;
        this.boxArtUrl = boxArtUrl;
    }

    // ==========================================
    // GETTER METHODS (Public Accessors)
    // ==========================================
    public String getProductCode() {
        return productCode;
    }

    public String getTitle() {
        return title;
    }

    public String getPlatform() {
        return platform;
    }

    public String getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getUrl() {
        return url;
    }
    public String getBoxArtUrl() {
        return boxArtUrl;
    }

    // ==========================================
    // TOSTRING OVERRIDE (For Clean Debug Display)
    // ==========================================
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ID: [%s] | %s (%s)\n", productCode, title, platform));
        sb.append(String.format("  Retailer: %-10s | Price: %-12s\n", storeName, price));
        sb.append(String.format("  Status:   %-10s | Link: %s\n", 
                (isAvailable ? "In Stock" : "Sold Out"), url));
        return sb.toString();
    }
    
    public String toJson() {
        String cleanTitle = (this.title != null) ? this.title : "Unknown Title";
        cleanTitle = cleanTitle.replace("\\", "\\\\").replace("\"", "\\\"");

         return "    {\n" +
             "      \"productCode\": \"" + this.productCode + "\",\n" +
             "      \"title\": \"" + cleanTitle + "\",\n" +
             "      \"platform\": \"" + this.platform + "\",\n" +
             "      \"price\": \"" + this.price + "\",\n" +
             "      \"isAvailable\": " + this.isAvailable + ",\n" +
             "      \"storeName\": \"" + this.storeName + "\",\n" +
             "      \"url\": \"" + this.url + "\",\n" +
             "      \"imageUrl\": \"" + (this.boxArtUrl == null ? "" : this.boxArtUrl) + "\"\n" +
             "    }";
    }
}
