package com.example.grocerystore.service;

import com.example.grocerystore.model.Product;
import com.example.grocerystore.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAvailableProducts() {
        return productRepository.findByAvailableTrueOrderByNameAsc();
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void toggleAvailability(Long id) {
        Product product = findById(id);
        product.setAvailable(!product.isAvailable());
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
