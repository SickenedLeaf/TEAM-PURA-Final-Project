package com.gamecheck.scraper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Exporter {//idk how this works ngl I read something about needing an external library called Jackson, try it ig idk
	/**
	 * Handles saving and exporting collected product payloads into a local JSON cache file.
	 */
	    private static final String DEFAULT_CACHE_FILE = "store_cache.json";

	    /**
	     * Exports a compiled list of products out to a standard formatted JSON cache file.
	     * @param products The source active data array collected from the scraper pipeline loops.
	     */
	    public static void exportCache(List<Product> products) {
	        exportCache(products, DEFAULT_CACHE_FILE);
	    }

	    /**
	     * Overloaded method to export data out to a specified custom filepath destination.
	     */
	    public static void exportCache(List<Product> products, String filename) {
	        System.out.println("\n==================================================");
	        System.out.println("💾 INITIALIZING DATA CACHE LOADER EXPORT ENGINE");
	        System.out.println("==================================================");
	        System.out.printf("Preparing data stream write to disk target: '%s'%n", filename);

	        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
	            // Open master JSON array block
	            writer.write("[\n");

	            int totalSize = products.size();
	            for (int i = 0; i < totalSize; i++) {
	                Product item = products.get(i);
	                
	                // Invoke individual item string injection mapping block
	                writer.write(item.toJson());

	                // Avoid appending a trailing comma to the last element to keep syntax valid
	                if (i < totalSize - 1) {
	                    writer.write(",\n");
	                } else {
	                    writer.write("\n");
	                }
	            }

	            // Close master array block
	            writer.write("]");
	            
	            System.out.printf("🎉 Cache export successful! Compiled total parameters: %,d listings.%n", totalSize);
	            System.out.println("==================================================");

	        } catch (IOException e) {
	            System.err.println("❌ Critical IO Exception writing cache mapping structure to disk: " + e.getMessage());
	        }
	    }
	}
