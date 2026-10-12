package com.healthkart.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Medicine domain entity extending BaseEntity.
 * Implements Comparable<Medicine> for generic collections sorting.
 */
public class Medicine extends BaseEntity implements Comparable<Medicine> {
    private String name;
    private String category;
    private String description;
    private double price;
    private double rating;
    private String icon;
    private String image;
    private int stockQuantity;
    private String manufacturer;
    private List<Map<String, Object>> providers = new ArrayList<>();

    public Medicine() {
        super();
        this.stockQuantity = 100;
        this.manufacturer = "HealthKart Labs";
    }

    public Medicine(String id, String name, String category, String description,
                    double price, double rating, String icon) {
        super(id);
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.rating = rating;
        this.icon = icon;
        this.stockQuantity = 100;
        this.manufacturer = "HealthKart Labs";
    }

    public Medicine(String id, String name, String category, String description,
                    double price, double rating, String icon, String image) {
        this(id, name, category, description, price, rating, icon);
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<Map<String, Object>> getProviders() {
        return providers;
    }

    public void setProviders(List<Map<String, Object>> providers) {
        this.providers = providers != null ? providers : new ArrayList<>();
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Override
    public String getEntitySummary() {
        return name + " [" + category + "] - ₹" + price + " (" + rating + "★)";
    }

    /**
     * Natural ordering by medicine name ascending.
     */
    @Override
    public int compareTo(Medicine other) {
        if (other == null) return 1;
        if (this.name == null) return -1;
        if (other.name == null) return 1;
        return this.name.compareToIgnoreCase(other.name);
    }
}
