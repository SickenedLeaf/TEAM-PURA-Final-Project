package Test;
import java.time.LocalDateTime;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ScrapeITech {
	
	private String webURL = "https://www.itech.ph/shop/nintendo-switch/";
	private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
	
	public void scrape() {
		try {
			Document doc = parseDocument(webURL);
			Elements products = doc.select("#main_loop div.xts-col");;
			Element saleElement;
			Element originalElement;
			Element titleElement;
			Element regularElement;
			Element product;
			String title;
			String price;
			String salePrice;
			String oldPrice;
			String gameUrl;
			String platform;
			String sourceName = "iTech";
			String sourceID = "0";
			Boolean available;
			
			System.out.println("Results for: " + "");
			
			int lim = products.size();
			
			for (int i = 0; i < lim ; i++) {
				product = products.get(i);
			    titleElement = product.selectFirst("h2 > a");
			    
			    if (titleElement != null) {
			        title = titleElement.text().trim();
			        gameUrl = product.selectFirst("a.xts-product-link").attr("href");
			        
			        if(gameUrl.toLowerCase().contains("nintendo-switch-2")) {
			        	platform = "Nintendo Switch 2 Only";
			        } else if (gameUrl.toLowerCase().contains("nintendo-switch")||gameUrl.toLowerCase().contains("nsw")) {
			        	platform = "Nintendo Switch and Switch 2";
			        } else {
			        	platform = "Unknown";
			        }

			        saleElement = product.selectFirst("ins span.woocommerce-Price-amount bdi");
			        originalElement = product.selectFirst("del span.woocommerce-Price-amount bdi");
			        
			        if (saleElement != null) {
			            
			            salePrice = saleElement.ownText().trim().replace(",", "");
			            oldPrice = (originalElement != null) ? originalElement.ownText().trim().replace(",", "") : "";
			            
			            price = salePrice + " (ON SALE!) \nOriginal: " + oldPrice;
			        } else {
			            regularElement = product.selectFirst("span.woocommerce-Price-amount bdi");
			            if (regularElement != null) {
			                price = regularElement.ownText().trim().replace(",", "");
			            } else {
			                price = "Price Not Found";
			            }
			        }
			        if (product.selectFirst("div.xts-product-thumb").text().contains("Sold Out")==false){
			        	available = true;
			        } else { available = false; }
			        
			        LocalDateTime lastUpdate = LocalDateTime.now();
			        
			        System.out.println("Title: " + title);
			        System.out.println("Price: " + price);
			        System.out.println("Platform: " + platform);
			        System.out.println("Available: " + available);
			        System.out.println("Retailer: " + sourceName);
			        System.out.println("Game URL: " + gameUrl);
			        System.out.println("Last Updated: " + lastUpdate);
			        System.out.println("==========================");
			    }
			}
		} catch (IOException e){
			System.err.println("Error fetching results: " + e.getMessage());
			}
		}
	
	private Document parseDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(5000)
                .get();
    }
	
	public static void main(String[] args) {
		ScrapeITech scrappy = new ScrapeITech();
		scrappy.scrape();
	}
}
