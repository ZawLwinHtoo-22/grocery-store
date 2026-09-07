package com.example.grocerystore.service;

import com.example.grocerystore.model.CustomerOrder;
import com.example.grocerystore.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends Telegram Bot notifications when a new order is placed.
 * Uses the Telegram sendMessage API with HTML parse_mode for rich formatting.
 * All errors are caught and logged — this service never disrupts the checkout flow.
 */
@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Value("${telegram.bot.enabled:false}")
    private boolean enabled;

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.chat-id:}")
    private String chatId;

    private final RestTemplate restTemplate;

    public TelegramNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends a new-order notification asynchronously so the HTTP response to the customer
     * is never delayed by Telegram network latency or failures.
     */
    @Async
    public void sendNewOrderNotification(CustomerOrder order) {
        if (!enabled) {
            log.debug("Telegram notifications are disabled — skipping.");
            return;
        }
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.warn("Telegram bot token or chat-id is not configured. Skipping notification.");
            return;
        }

        try {
            String message = buildOrderMessage(order);
            sendMessage(message);
            log.info("Telegram order notification sent for order: {}", order.getTrackingCode());
        } catch (Exception ex) {
            log.error("Failed to send Telegram order notification for order {}: {}",
                    order.getTrackingCode(), ex.getMessage(), ex);
        }
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private String buildOrderMessage(CustomerOrder order) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("🛒 <b>New Order Received!</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Order meta
        sb.append("📦 <b>Order:</b> <code>").append(escape(order.getTrackingCode())).append("</code>\n");
        sb.append("🕐 <b>Date:</b> ")
          .append(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "N/A")
          .append("\n\n");

        // Customer info
        sb.append("👤 <b>Customer Details</b>\n");
        sb.append("├ <b>Name:</b> ").append(escape(order.getCustomerName())).append("\n");
        sb.append("├ <b>Phone:</b> ").append(escape(order.getPhoneNumber())).append("\n");
        sb.append("└ <b>Address:</b> ").append(escape(order.getDeliveryAddress())).append("\n\n");

        // Delivery preferences (optional fields)
        boolean hasSlot = order.getPreferredDeliverySlot() != null && !order.getPreferredDeliverySlot().isBlank();
        boolean hasNote = order.getDeliveryNote() != null && !order.getDeliveryNote().isBlank();
        if (hasSlot || hasNote) {
            sb.append("🚚 <b>Delivery</b>\n");
            if (hasSlot) {
                sb.append("├ <b>Slot:</b> ").append(escape(order.getPreferredDeliverySlot())).append("\n");
            }
            if (hasNote) {
                sb.append("└ <b>Note:</b> ").append(escape(order.getDeliveryNote())).append("\n");
            }
            sb.append("\n");
        }

        // Order items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            sb.append("🛍️ <b>Items Ordered</b>\n");
            int i = 1;
            for (OrderItem item : order.getItems()) {
                String productName = (item.getProduct() != null) ? item.getProduct().getName() : "Unknown";
                String prefix = (i == order.getItems().size()) ? "└" : "├";
                sb.append(prefix)
                  .append(" ").append(escape(productName))
                  .append(" × ").append(item.getQuantity())
                  .append(" — <b>").append(formatAmount(item.getLineTotal())).append(" MMK</b>\n");
                i++;
            }
            sb.append("\n");
        }

        // Total
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("💰 <b>Total Amount: ").append(formatAmount(order.getTotalAmount())).append(" MMK</b>\n");
        sb.append("💳 <b>Status:</b> ⏳ Pending Payment\n\n");

        // Footer
        sb.append("👉 Check admin dashboard to manage this order.");

        return sb.toString();
    }

    private void sendMessage(String text) {
        String url = TELEGRAM_API_BASE + botToken + "/sendMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        body.put("disable_web_page_preview", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("Telegram API returned non-2xx status: {} — body: {}",
                    response.getStatusCode(), response.getBody());
        }
    }

    /** Escape HTML special characters for Telegram HTML parse_mode. */
    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /** Format a BigDecimal amount as a grouped integer string (e.g. 65,000). */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
