package com.example.grocerystore.controller;

import com.example.grocerystore.service.CartService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
public class CartApiController {

    private final CartService cartService;

    public CartApiController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/count")
    public Map<String, Integer> count(HttpSession session) {
        return Map.of("count", cartService.getCart(session).getItemCount());
    }
}