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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final Path paymentUploadDir;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        @Value("${app.uploads.payment-dir}") String paymentUploadDir) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentUploadDir = Path.of(paymentUploadDir);
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
            order.setTrackingCode(UUID.randomUUID().toString());
            order.setUpdatedAt(LocalDateTime.now());
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

        Files.createDirectories(paymentUploadDir);
        String originalName = screenshot.getOriginalFilename() == null ? "payment" : screenshot.getOriginalFilename();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        String fileName = "order-" + order.getId() + "-" + UUID.randomUUID() + extension;
        Path destination = paymentUploadDir.resolve(fileName).normalize();
        Files.copy(screenshot.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        order.setPaymentScreenshotPath("/payments/" + fileName);
        order.setStatus(OrderStatus.PAYMENT_SUBMITTED);
        order.setUpdatedAt(LocalDateTime.now());
    }

    private void requireStatus(CustomerOrder order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new IllegalStateException("Order must be " + expected.getLabel());
        }
    }
}
