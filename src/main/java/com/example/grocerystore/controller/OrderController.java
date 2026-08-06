package com.example.grocerystore.controller;

import com.example.grocerystore.model.CustomerOrder;
import com.example.grocerystore.model.OrderStatus;
import com.example.grocerystore.service.CartService;
import com.example.grocerystore.service.OrderService;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @ModelAttribute
    void addSharedAttributes(Model model, HttpSession session) {
        model.addAttribute("cart", cartService.getCart(session));
    }

    @GetMapping("/orders/history")
    public String orderHistory(HttpSession session, Model model) {
        @SuppressWarnings("unchecked")
        List<String> sessionOrderCodes = (List<String>) session.getAttribute("SESSION_ORDER_CODES");

        List<CustomerOrder> activeOrders = new ArrayList<>();
        List<CustomerOrder> pastOrders = new ArrayList<>();

        if (sessionOrderCodes != null && !sessionOrderCodes.isEmpty()) {
            for (String code : sessionOrderCodes) {
                try {
                    CustomerOrder order = orderService.findWithItemsByTrackingCode(code);
                    if (order != null) {
                        OrderStatus status = order.getStatus();
                        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
                            pastOrders.add(order);
                        } else {
                            activeOrders.add(order);
                        }
                    }
                } catch (Exception ignored) {
                    // Ignore if order no longer exists
                }
            }
        }

        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("pastOrders", pastOrders);
        return "order-history";
    }

    public static void recordOrderInSession(HttpSession session, String trackingCode) {
        if (session == null || trackingCode == null || trackingCode.isBlank()) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> sessionOrderCodes = (List<String>) session.getAttribute("SESSION_ORDER_CODES");
        if (sessionOrderCodes == null) {
            sessionOrderCodes = new ArrayList<>();
        }
        if (!sessionOrderCodes.contains(trackingCode.trim())) {
            sessionOrderCodes.add(0, trackingCode.trim());
        }
        session.setAttribute("SESSION_ORDER_CODES", sessionOrderCodes);
    }
}
