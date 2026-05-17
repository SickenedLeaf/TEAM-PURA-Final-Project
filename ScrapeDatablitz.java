package Test;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ScrapeDatablitz {

	private String webURL = "https://ecommerce.datablitz.com.ph/collections/ugreen-power-up-for-less-promo-until-june-15-2026";
	private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

	public void scrape() {
		try {
			Document doc = parseDocument(webURL);
			Elements products = doc.select("div.product-item.product-item--vertical");
			Element saleElement;
			Element titleElement;
			Element regularElement;
			Element product;
			String title;
			String price;
			String salePrice;
			String oldPrice;

			System.out.println("Results for: " + "");

			int lim = products.size();

			for (int i = 0; i < lim; i++) {
				product = products.get(i);

				titleElement = product.selectFirst("a.product-item__title");

				if (titleElement != null) {
					title = titleElement.text().trim();
					price = "";
					
					
					saleElement = product.selectFirst("div.product-item__price-list.price-list");
					String temp = saleElement.text().trim();
					
					String[] tempSplit = temp.split("₱");
					
					if (tempSplit.length >= 2) {

						salePrice = tempSplit[1];
						oldPrice = tempSplit[2];

						price = salePrice + " (ON SALE!) \nOriginal: " + oldPrice;
					} else {
						regularElement = product.selectFirst("span.price");
						if (regularElement != null) {
							price = regularElement.ownText().trim().replace(",", "");
						} else {
							price = "Price Not Found";
						}
					}

					System.out.println("Title: " + title);
					System.out.println("Price: " + price);
					System.out.println("==========================");
				}
			}
		} catch (IOException e) {
			System.err.println("Error fetching results: " + e.getMessage());
		}
	}

	private Document parseDocument(String url) throws IOException {
		return Jsoup.connect(url).userAgent(userAgent).timeout(5000).get();
	}

	public static void main(String[] args) {
		ScrapeDatablitz scrappy = new ScrapeDatablitz();
		scrappy.scrape();
	}
}
