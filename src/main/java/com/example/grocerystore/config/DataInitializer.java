package com.example.grocerystore.config;

import com.example.grocerystore.model.Product;
import com.example.grocerystore.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }
            productRepository.save(product("Paw San Hmwe Rice", "25 kg bag", "65000.00", "/images/rice-bag.svg"));
            productRepository.save(product("Thai Jasmine Rice", "10 kg bag", "42000.00", "/images/rice-sack.svg"));
            productRepository.save(product("Peanut Oil", "1 viss bottle", "14500.00", "/images/oil-bottle.svg"));
            productRepository.save(product("Sunflower Oil", "1 liter bottle", "7800.00", "/images/oil-jug.svg"));
        };
    }

    private Product product(String name, String unitLabel, String price, String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setUnitLabel(unitLabel);
        product.setPrice(new BigDecimal(price));
        product.setImageUrl(imageUrl);
        product.setAvailable(true);
        return product;
    }
}
