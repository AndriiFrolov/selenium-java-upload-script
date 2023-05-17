package org.script.upload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Product {
    private final String filename;
    private int line;
    private String category;
    private String template;
    private String productName;
    private String link;
    private String productDescription;
    private boolean isAlreadyProcessed;

    private List<String> errors;
    private Map<String, String> data;

    public Product(Map<String, String> data) {
        this.data = data;
        this.errors = new ArrayList<>();

        this.line = Integer.valueOf(get("line"));
        this.category = get("Category");
        this.template = get("Template Type");
        this.productName = get("Product Name");
        this.productDescription = get("Product Description");
        this.link = get("Link");
        this.isAlreadyProcessed = this.data.containsKey("Processed");
        this.filename = this.data.containsKey("File name") ? get("File name") : "Download Template";
    }

    public String getFilename() {
        return filename;
    }

    public int getLine() {
        return line;
    }

    public String getCategory() {
        return category;
    }

    public String getTemplate() {
        return template;
    }

    public String getProductName() {
        return productName;
    }

    public String getLink() {
        return link;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public boolean isAlreadyProcessed() {
        return isAlreadyProcessed;
    }

    public List<String> getErrors() {
        return errors;
    }

    public Map<String, String> getData() {
        return data;
    }

    private String get(String key) {
        if (this.data.containsKey(key)) {
            return data.get(key);
        }
        errors.add("Could not get info about " + key);
        return null;
    }
}
