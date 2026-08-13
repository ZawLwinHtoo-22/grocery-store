package com.example.grocerystore.model;

public enum OrderStatus {
    PENDING_PAYMENT("Pending Payment"),
    PAYMENT_SUBMITTED("Pending Payment Verification"),
    PROCESSING("Processing & Packing"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    COMPLETED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
