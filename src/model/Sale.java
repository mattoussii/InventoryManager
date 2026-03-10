package model;

public record Sale(
        int productId,
        String productName,
        String clientName,
        String clientPhone,
        int quantity,
        double price,
        long timestamp
) {}