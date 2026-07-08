package com.example.grocerystore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String paymentUploadDir;

    public WebConfig(@Value("${app.uploads.payment-dir}") String paymentUploadDir) {
        this.paymentUploadDir = paymentUploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(paymentUploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/payments/**").addResourceLocations(location);
    }
}
