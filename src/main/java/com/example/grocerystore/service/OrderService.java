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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final com.example.grocerystore.service.CloudinaryService cloudinaryService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        com.example.grocerystore.service.CloudinaryService cloudinaryService) {
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

    @Transactional
    public void approve(Long orderId) {
        CustomerOrder order = findWithItems(orderId);
        requireStatus(order, OrderStatus.PENDING_APPROVAL);
        order.setStatus(OrderStatus.WAITING_FOR_PAYMENT);
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void confirm(Long orderId) {
        CustomerOrder order = findWithItems(orderId);
        requireStatus(order, OrderStatus.PAYMENT_SUBMITTED);
        order.setStatus(OrderStatus.COMPLETED);
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
        try {
            String uploadedUrl = cloudinaryService.uploadImage(screenshot);
            order.setPaymentScreenshotPath(uploadedUrl);
            order.setStatus(OrderStatus.PAYMENT_SUBMITTED);
            order.setUpdatedAt(LocalDateTime.now());
        } catch (IOException ex) {
            throw ex;
        }
    }

    // Generate tracking code: ORD-YYYYMMDD-XXXXXX where XXXXXX is sequential per day
    private String generateTrackingCode() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime start = today.atStartOfDay();
        java.time.LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);
        long countToday = orderRepository.countByCreatedAtBetween(start, end);
        long sequence = countToday + 1; // simple increment — note: race conditions may occur under heavy concurrent orders
        String seqStr = String.format("%06d", sequence);
        String dateStr = today.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        return "ORD-" + dateStr + "-" + seqStr;
    }

    private void requireStatus(CustomerOrder order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new IllegalStateException("Order must be " + expected.getLabel());
        }
    }
}
