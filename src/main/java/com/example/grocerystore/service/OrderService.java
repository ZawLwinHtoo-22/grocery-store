package com.example.grocerystore.service;

import com.example.grocerystore.dto.Cart;
import com.example.grocerystore.dto.CartItem;
import com.example.grocerystore.dto.CheckoutForm;
import com.example.grocerystore.model.CustomerOrder;
import com.example.grocerystore.model.OrderItem;
import com.example.grocerystore.model.OrderStatus;
import com.example.grocerystore.model.Product;
import com.example.grocerystore.repository.OrderRepository;
import com.example.grocerystore.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CloudinaryService cloudinaryService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public CustomerOrder createOrder(CheckoutForm form, Cart cart) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomerName(form.getCustomerName());
        order.setPhoneNumber(form.getPhoneNumber());
        order.setDeliveryAddress(form.getDeliveryAddress());
        order.setStatus(OrderStatus.PENDING_APPROVAL);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            if (!product.isAvailable()) {
                throw new IllegalArgumentException(product.getName() + " is currently out of stock");
            }

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            order.addItem(item);
            total = total.add(item.getLineTotal());
        }

        order.setTotalAmount(total);
        order.setUpdatedAt(LocalDateTime.now());

        // persist preferred delivery slot from the checkout form if present
        if (form != null) {
            order.setPreferredDeliverySlot(form.getPreferredDeliverySlot());
            order.setDeliveryNote(form.getDeliveryNote());
        }

        // Generate professional tracking code: ORD-YYYYMMDD-XXXXXX where XXXXXX is a 6-digit sequential number for the day
        String trackingCode = generateTrackingCode();
        order.setTrackingCode(trackingCode);

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public CustomerOrder findWithItems(Long id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional
    public CustomerOrder ensureTrackingCode(Long id) {
        CustomerOrder order = findWithItems(id);
        if (order.getTrackingCode() == null || order.getTrackingCode().isBlank()) {
            order.setTrackingCode(generateTrackingCode());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public CustomerOrder findWithItemsByTrackingCode(String trackingCode) {
        return orderRepository.findWithItemsByTrackingCode(trackingCode)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findOrders(OrderStatus status) {
        if (status == null) {
            return orderRepository.findAllByOrderByCreatedAtDesc();
        }
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findByPhoneNumber(String phoneNumber) {
        return orderRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
    }

    /**
     * Unified multi-criteria search for admin dashboard.
     * Searches by query string (customer name, phone, or tracking code) and optionally filters by status and date range.
     * Returns a de-duplicated list ordered by creation date descending.
     */
    @Transactional(readOnly = true)
    public List<CustomerOrder> searchOrders(String query, OrderStatus status, LocalDate from, LocalDate to) {
        // If no query and no date range — fall back to simple status filter
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasDateRange = from != null || to != null;

        if (!hasQuery && !hasDateRange) {
            return findOrders(status);
        }

        LocalDateTime start = from != null ? from.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay().minusNanos(1) : LocalDateTime.now().plusYears(1);

        if (!hasQuery) {
            // Date range only
            if (status != null) {
                return orderRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(status, start, end);
            }
            return orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        }

        // Search by name, phone, and tracking code — merge and de-duplicate
        String q = query.trim();
        Set<Long> seen = new LinkedHashSet<>();
        List<CustomerOrder> results = new ArrayList<>();

        for (CustomerOrder o : orderRepository.findByCustomerNameContainingIgnoreCase(q)) {
            if (seen.add(o.getId())) results.add(o);
        }
        for (CustomerOrder o : orderRepository.findByPhoneNumberContaining(q)) {
            if (seen.add(o.getId())) results.add(o);
        }
        for (CustomerOrder o : orderRepository.findByTrackingCodeContainingIgnoreCase(q)) {
            if (seen.add(o.getId())) results.add(o);
        }

        // Filter by status and/or date range
        List<CustomerOrder> filtered = new ArrayList<>();
        for (CustomerOrder o : results) {
            if (status != null && o.getStatus() != status) continue;
            if (hasDateRange) {
                if (o.getCreatedAt().isBefore(start) || o.getCreatedAt().isAfter(end)) continue;
            }
            filtered.add(o);
        }
        return filtered;
    }

    @Transactional
    public void approve(Long orderId) {
        CustomerOrder order = findWithItems(orderId);
        requireStatus(order, OrderStatus.PENDING_APPROVAL);
        order.setStatus(OrderStatus.WAITING_FOR_PAYMENT);
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void markProcessing(Long orderId) {
        CustomerOrder order = findWithItems(orderId);
        requireStatus(order, OrderStatus.PAYMENT_SUBMITTED);
        order.setStatus(OrderStatus.PROCESSING);
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void markOutForDelivery(Long orderId) {
        CustomerOrder order = findWithItems(orderId);
        requireStatus(order, OrderStatus.PROCESSING);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void confirm(Long orderId) {
        CustomerOrder order = findWithItems(orderId);
        // Allow confirming from PAYMENT_SUBMITTED (fast path) or OUT_FOR_DELIVERY (standard path)
        if (order.getStatus() != OrderStatus.PAYMENT_SUBMITTED && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("Order must be in Payment Submitted or Out For Delivery state to confirm.");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void cancel(Long orderId) {
        CustomerOrder order = findWithItems(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel an order that is already completed or cancelled.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void submitPayment(Long orderId, MultipartFile screenshot) throws IOException {
        CustomerOrder order = findWithItems(orderId);
        submitPayment(order, screenshot);
    }

    @Transactional
    public void submitPayment(String trackingCode, MultipartFile screenshot) throws IOException {
        CustomerOrder order = findWithItemsByTrackingCode(trackingCode);
        submitPayment(order, screenshot);
    }

    private void submitPayment(CustomerOrder order, MultipartFile screenshot) throws IOException {
        requireStatus(order, OrderStatus.WAITING_FOR_PAYMENT);
        if (screenshot == null || screenshot.isEmpty()) {
            throw new IllegalArgumentException("Payment screenshot is required");
        }

        // Upload screenshot to Cloudinary and store the returned secure URL on the order
        String uploadedUrl = cloudinaryService.uploadImage(screenshot);
        order.setPaymentScreenshotPath(uploadedUrl);
        order.setStatus(OrderStatus.PAYMENT_SUBMITTED);
        order.setUpdatedAt(LocalDateTime.now());
    }

    // Generate tracking code: ORD-YYYYMMDD-XXXXXX where XXXXXX is sequential per day
    private String generateTrackingCode() {
        java.time.LocalDate today = java.time.LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);
        long countToday = orderRepository.countByCreatedAtBetween(start, end);
        long sequence = countToday + 1;
        String seqStr = String.format("%06d", sequence);
        String dateStr = today.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        return "ORD-" + dateStr + "-" + seqStr;
    }

    private void requireStatus(CustomerOrder order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new IllegalStateException("Order must be in status: " + expected.getLabel());
        }
    }
}
