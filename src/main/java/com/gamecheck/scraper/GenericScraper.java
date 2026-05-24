package com.gamecheck.scraper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Abstract scraper engine built for automated background operations.
 * Natively handles sitemap discovery, network throttling, and identity hashing.
 */
public abstract class GenericScraper {
    
    // ==========================================
    // INSTANCE VARIABLES
    // ==========================================
    protected final String storeName;
    protected final String sitemapIndexURL;
    protected final String userAgent;
    
    private static final int BASE_DELAY_MS = 1500;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================
    public GenericScraper(String storeName, String sitemapIndexURL) {
        this.storeName = storeName;
        this.sitemapIndexURL = sitemapIndexURL;
        this.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    }

    protected Connection openBrowserLikeConnection(String url) throws IOException {
        return Jsoup.connect(url)
            .userAgent(this.userAgent)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .referrer("https://www.google.com")
            .timeout(10000);
    }

    // ==========================================
    // POLYMORPHIC HOOK METHOD
    // ==========================================
    protected abstract Product scrapePage(String url);

    // ==========================================
    // PUBLIC SYNC ENGINE EXECUTION
    // ==========================================
    public List<Product> syncStore() {
        return syncStore(Integer.MAX_VALUE);
    }

    public List<Product> syncStore(int urlLimit) {
        List<Product> syncResults = new ArrayList<>();

        List<String> activeSubSitemaps = discoverSubSitemaps();
        List<String> targetedUrls = extractAndFilterUrls(activeSubSitemaps);

        int processed = 0;
        for (String url : targetedUrls) {
            if (processed >= urlLimit) {
                break;
            }

            Product extractedProduct = scrapePage(url);
            if (extractedProduct != null) {
                syncResults.add(extractedProduct);
            }

            processed++;
            applyPoliteThrottle();
        }

        return syncResults;
    }

    // ==========================================
    // INTEGRATED DATA TRANSFORMATION ENGINE
    // ==========================================
    /**
     * Cleanses messy storefront titles by dropping platform and region clutter,
     * then transforms the result into a uniform 8-character unique hash string.
     */
    protected String generateUniqueCode(String rawTitle) {
        String cleanText = rawTitle.toLowerCase();

        // 1. Strip platform and hardware branding
        cleanText = cleanText.replace("nintendo switch 2", "")
                             .replace("nintendo switch", "")
                             .replace("nintendo", "")
                             .replace("switch 2", "")
                             .replace("switch", "")
                             .replace("ns2", "")
                             .replace("ns", "");

        // 2. Strip storefront packaging and market regional tags
        cleanText = cleanText.replace("mde", "")
                             .replace("asi", "")
                             .replace("us/eng/fr", "")
                             .replace("us/eng", "")
                             .replace("eng", "")
                             .replace("standard edition", "")
                             .replace("edition", "");

        // 3. Flatten remaining characters using alpha-numeric regex matching
        cleanText = cleanText.replaceAll("[^a-zA-Z0-9]", "").trim();

        // 4. Pass down to the mathematical hashing layer
        return hashString(cleanText);
    }

    /**
     * Converts a baseline normalized text string into a deterministic 
     * 8-character SHA-256 hex signature.
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            // Extract the first 4 bytes to establish a short, compact 8-character ID
            for (int i = 0; i < 4; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
            
        } catch (NoSuchAlgorithmException e) {
            // Unconditional fallback to basic absolute hash code if system crypto fails
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }

    protected String resolveHighestQualityImage(Element element) {
        if (element == null) {
            return "";
        }

        String srcset = firstNonBlank(
            element.attr("data-srcset"),
            element.attr("srcset"),
            element.attr("data-lazy-srcset"),
            element.attr("data-set")
        );

        if (!srcset.isBlank()) {
            String best = selectBestUrlFromSrcSet(srcset);
            if (!best.isBlank()) {
                return normalizeImageUrl(best);
            }
        }

        for (String attr : new String[]{"data-src", "data-zoom-image", "data-image-src", "data-src-full", "src"}) {
            String candidate = element.attr(attr).trim();
            if (!candidate.isBlank()) {
                return normalizeImageUrl(candidate);
            }
        }

        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String selectBestUrlFromSrcSet(String srcset) {
        String bestUrl = "";
        int bestScore = -1;

        for (String candidate : srcset.split(",")) {
            String trimmed = candidate.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split("\\s+");
            String url = parts[0].trim();
            int score = -1;

            if (parts.length > 1) {
                String descriptor = parts[parts.length - 1].trim();
                if (descriptor.endsWith("w")) {
                    try {
                        score = Integer.parseInt(descriptor.substring(0, descriptor.length() - 1));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (descriptor.endsWith("x")) {
                    try {
                        score = Integer.parseInt(descriptor.substring(0, descriptor.length() - 1)) * 1000;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestUrl = url;
            } else if (bestScore == -1 && !url.isBlank()) {
                bestUrl = url;
            }
        }

        if (bestUrl.isBlank()) {
            String[] fallback = srcset.split(",");
            if (fallback.length > 0) {
                String last = fallback[fallback.length - 1].trim();
                if (!last.isBlank()) {
                    bestUrl = last.split("\\s+")[0].trim();
                }
            }
        }

        return bestUrl;
    }

    private String normalizeImageUrl(String url) {
        if (url == null) {
            return "";
        }
        String cleaned = url.trim();
        if (cleaned.startsWith("//")) {
            cleaned = "https:" + cleaned;
        }
        return cleaned;
    }

    // ==========================================
    // PRIVATE INTERNAL PIPELINE FILTERS
    // ==========================================
    protected List<String> discoverSubSitemaps() {
        List<String> discoveredMaps = new ArrayList<>();
        try {
            Document masterDoc = openBrowserLikeConnection(sitemapIndexURL).get();
            Elements sitemapElements = masterDoc.select("sitemap loc");
            
            for (Element element : sitemapElements) {
                String mapUrl = element.text();
                if (mapUrl.toLowerCase().contains("product-sitemap")||mapUrl.toLowerCase().contains("sitemap_products")) {
                    discoveredMaps.add(mapUrl);
                }
            }
            
            if (discoveredMaps.isEmpty()) {
                discoveredMaps.add(sitemapIndexURL);
            }
        } catch (IOException e) {
            System.err.println("[" + storeName + " Index Error] Mapping failed: " + e.getMessage());
            discoveredMaps.add(sitemapIndexURL);
        }
        return discoveredMaps;
    }

    protected List<String> extractAndFilterUrls(List<String> subSitemaps) {
        List<String> filteredList = new ArrayList<>();
        for (String sitemapUrl : subSitemaps) {
            try {
                Document sitemapDoc = openBrowserLikeConnection(sitemapUrl).get();
                Elements urlElements = sitemapDoc.select("loc");

                for (Element element : urlElements) {
                    String urlText = element.text();
                    String lowUrl = urlText.toLowerCase();

                    // 1. Must be a Switch item
                    boolean isSwitchPlatform = lowUrl.contains("nintendo-switch") || 
                                               lowUrl.contains("nsw") || 
                                               lowUrl.contains("switch-2") || 
                                               lowUrl.contains("switch-");

                    // 2. Must NOT match accessory/hardware signatures (Fixed with && operators)
                    boolean isNotHardwareOrAccessory = !lowUrl.contains("oled") && 
                                                       !lowUrl.contains("bundle") && 
                                                       !lowUrl.contains("con-") &&
                                                       !lowUrl.contains("controller") &&
                                                       !lowUrl.contains("case") && 
                                                       !lowUrl.contains("model") && 
                                                       !lowUrl.contains("hori-") &&
                                                       !lowUrl.contains("dobe-") &&
                                                       !lowUrl.contains("pouch");

                    // Combine both states cleanly
                    if (isSwitchPlatform && isNotHardwareOrAccessory) {
                        if (!filteredList.contains(urlText)) {
                            filteredList.add(urlText);
                        }                 
                    }
                }
            } catch (IOException e) {
                System.err.println("[" + storeName + " Sub-Map Error] Failed parsing: " + sitemapUrl);
            }
        }
        return filteredList;
    }

    private void applyPoliteThrottle() {
        try {
            long jitter = (long) (Math.random() * 500); 
            Thread.sleep(BASE_DELAY_MS + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}