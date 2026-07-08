package com.example.grocerystore.controller;

import com.example.grocerystore.dto.CheckoutForm;
import com.example.grocerystore.model.CustomerOrder;
import com.example.grocerystore.service.CartService;
import com.example.grocerystore.service.OrderService;
import com.example.grocerystore.service.ProductService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
public class StoreController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final String kpayPhone;
    private final String wavePhone;

    public StoreController(ProductService productService,
                           CartService cartService,
                           OrderService orderService,
                           @Value("${app.payment.kpay-phone}") String kpayPhone,
                           @Value("${app.payment.wave-phone}") String wavePhone) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.kpayPhone = kpayPhone;
        this.wavePhone = wavePhone;
    }

    @ModelAttribute
    void addSharedAttributes(Model model, HttpSession session) {
        model.addAttribute("cart", cartService.getCart(session));
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("products", productService.findAvailableProducts());
        return "index";
    }

    @GetMapping("/products/{productId}")
    public String productDetail(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.findById(productId));
        return "product-detail";
    }

    @PostMapping("/cart/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            cartService.addToCart(productId, quantity, session);
            redirectAttributes.addFlashAttribute("success", "Product added to cart.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", new CheckoutForm());
        }
        return "cart";
    }

    @GetMapping("/track")
    public String trackSearch(@RequestParam(required = false) Long orderId,
                              @RequestParam(required = false) String trackingCode) {
        if (trackingCode != null && !trackingCode.isBlank()) {
            return "redirect:/orders/track/" + trackingCode.trim();
        }
        if (orderId == null) {
            return "track-search";
        }
        return "redirect:/orders/" + orderId;
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long productId,
                             @RequestParam int quantity,
                             HttpSession session) {
        cartService.updateQuantity(productId, quantity, session);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeCartItem(@RequestParam Long productId, HttpSession session) {
        cartService.remove(productId, session);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute CheckoutForm checkoutForm,
                           BindingResult bindingResult,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.checkoutForm", bindingResult);
            redirectAttributes.addFlashAttribute("checkoutForm", checkoutForm);
            return "redirect:/cart";
        }

        try {
            CustomerOrder order = orderService.createOrder(checkoutForm, cartService.getCart(session));
            cartService.clear(session);
            return "redirect:/orders/track/" + order.getTrackingCode();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/orders/{orderId}")
    public String legacyTrack(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            CustomerOrder order = orderService.ensureTrackingCode(orderId);
            return "redirect:/orders/track/" + order.getTrackingCode();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Order မတွေ့ပါ။ Order number ကို ပြန်စစ်ပေးပါ။");
            return "redirect:/track";
        }
    }

    @GetMapping("/orders/track/{trackingCode}")
    public String track(@PathVariable String trackingCode,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("order", orderService.findWithItemsByTrackingCode(trackingCode));
            model.addAttribute("kpayPhone", kpayPhone);
            model.addAttribute("wavePhone", wavePhone);
            return "track";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Order မတွေ့ပါ။ Tracking code ကို ပြန်စစ်ပေးပါ။");
            return "redirect:/track";
        }
    }

    @PostMapping("/orders/track/{trackingCode}/payment")
    public String submitPayment(@PathVariable String trackingCode,
                                @RequestParam("screenshot") MultipartFile screenshot,
                                RedirectAttributes redirectAttributes) {
        try {
            orderService.submitPayment(trackingCode, screenshot);
            redirectAttributes.addFlashAttribute("success", "Payment screenshot submitted. We will verify it soon.");
        } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/track/" + trackingCode;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
