package com.example.grocerystore.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class CheckoutForm {

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String customerName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{6,30}$", message = "Enter a valid phone number")
    private String phoneNumber;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 1000)
    private String deliveryAddress;

    // Preferred delivery slot - optional
    @Size(max = 120)
    private String preferredDeliverySlot;

    @Size(max = 500)
    private String deliveryNote;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPreferredDeliverySlot() {
        return preferredDeliverySlot;
    }

    public void setPreferredDeliverySlot(String preferredDeliverySlot) {
        this.preferredDeliverySlot = preferredDeliverySlot;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }
}
