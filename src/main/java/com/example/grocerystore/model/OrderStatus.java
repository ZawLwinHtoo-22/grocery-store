package com.example.grocerystore.model;

public enum OrderStatus {
    PENDING_APPROVAL("Pending Approval"),
    WAITING_FOR_PAYMENT("Waiting For Payment"),
    PAYMENT_SUBMITTED("Payment Submitted"),
    PROCESSING("Processing"),
    OUT_FOR_DELIVERY("Out For Delivery"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
