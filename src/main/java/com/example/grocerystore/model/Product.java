package com.example.grocerystore.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 120, columnDefinition = "NVARCHAR(120)")
    private String name;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Optional discount price. If set and lower than price, it is used as the effective selling price. */
    @Column(nullable = true, precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = true, length = 500, columnDefinition = "NVARCHAR(500)")
    private String imageUrl;

    @Column(nullable = false)
    private boolean available = true;

    @Column(length = 40, columnDefinition = "NVARCHAR(40)")
    private String unitLabel = "unit";

    // ── Discount helpers ──────────────────────────────────────────────────────

    /**
     * Returns true when a valid discount price exists (non-null, positive, and strictly less than the regular price).
     */
    public boolean hasDiscount() {
        return discountPrice != null
                && discountPrice.compareTo(BigDecimal.ZERO) > 0
                && discountPrice.compareTo(price) < 0;
    }

    /**
     * Returns the effective selling price: discountPrice if a valid discount exists, otherwise the regular price.
     */
    public BigDecimal getEffectivePrice() {
        return hasDiscount() ? discountPrice : price;
    }

    /**
     * Returns the discount percentage saved (0-100) as an integer, or 0 if no discount applies.
     * Formula: round((price - discountPrice) / price * 100)
     */
    public int getDiscountPercent() {
        if (!hasDiscount()) return 0;
        return price.subtract(discountPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * Returns the absolute savings amount (price - discountPrice) if a discount applies, or 0.
     */
    public BigDecimal getSavingsAmount() {
        if (!hasDiscount()) return BigDecimal.ZERO;
        return price.subtract(discountPrice);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String unitLabel) {
        this.unitLabel = unitLabel;
    }
}
