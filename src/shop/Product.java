package shop;

import java.io.Serializable;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int LOW_STOCK = 5;

    final int id;
    final int sellerId;
    String name;
    String description;
    String category;
    double price;
    int stock;

    Product(int id, int sellerId, String name, String description, String category, double price, int stock) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    boolean lowOnStock() {
        return stock <= LOW_STOCK;
    }
}
