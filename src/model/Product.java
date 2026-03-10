package model;

public record Product(
        int id,
        String name,
        double price,
        int maxStock,
        String description
) {

    /* ===============================
       FORMATTED TUNISIAN PRICE
       =============================== */
    public String formattedPrice() {
        return String.format("%.3f DT", price);
    }

    /* ===============================
       DISPLAY FORMAT
       =============================== */
    @Override
    public String toString() {
        return "ID: " + id +
                " | Name: " + name +
                " | Price: " + formattedPrice() +
                " | Max: " + maxStock;
    }
}