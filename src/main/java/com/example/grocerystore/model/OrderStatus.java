package com.example.grocerystore.model;

public enum OrderStatus {
    PENDING_APPROVAL("Pending Approval"),
    WAITING_FOR_PAYMENT("Waiting For Payment"),
    PAYMENT_SUBMITTED("Payment Submitted"),
    COMPLETED("Completed");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
