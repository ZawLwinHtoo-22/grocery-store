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
            Product p1 = product("Paw San Hmwe Rice", "25 kg bag", "65000.00", "/images/rice-bag.svg");
            productRepository.save(p1);

            Product p2 = product("Thai Jasmine Rice", "10 kg bag", "45000.00", "/images/rice-sack.svg");
            p2.setDiscountPrice(new BigDecimal("40000.00"));
            productRepository.save(p2);

            Product p3 = product("Peanut Oil", "1 viss bottle", "15000.00", "/images/oil-bottle.svg");
            p3.setDiscountPrice(new BigDecimal("13500.00"));
            productRepository.save(p3);

            Product p4 = product("Sunflower Oil", "1 liter bottle", "7800.00", "/images/oil-jug.svg");
            productRepository.save(p4);
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
