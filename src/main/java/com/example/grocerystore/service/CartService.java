package com.example.grocerystore.service;

import com.example.grocerystore.dto.Cart;
import com.example.grocerystore.dto.CartItem;
import com.example.grocerystore.model.Product;
import com.example.grocerystore.repository.ProductRepository;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    public static final String CART_SESSION_KEY = "shoppingCart";

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public void addToCart(Long productId, int quantity, HttpSession session) {
        if (quantity <= 0) {
            quantity = 1;
        }
        Product product = productRepository.findById(productId)
                .filter(Product::isAvailable)
                .orElseThrow(() -> new IllegalArgumentException("Product is not available"));

        CartItem item = new CartItem();
        item.setProductId(product.getId());
        item.setName(product.getName());
        item.setImageUrl(product.getImageUrl());
        item.setUnitLabel(product.getUnitLabel());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(quantity);
        getCart(session).addItem(item);
    }

    public void updateQuantity(Long productId, int quantity, HttpSession session) {
        getCart(session).updateQuantity(productId, quantity);
    }

    public void remove(Long productId, HttpSession session) {
        getCart(session).removeItem(productId);
    }

    public void clear(HttpSession session) {
        getCart(session).clear();
    }
}
