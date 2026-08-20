package com.example.grocerystore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects shared model attributes into every controller's model,
 * making them available to all Thymeleaf templates without per-controller setup.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    /**
     * Viber support phone number (configurable via app.viber.phone in application.properties).
     * Default falls back to the store's main contact number.
     */
    @Value("${app.viber.phone:+959962800540}")
    private String viberPhone;

    /**
     * Exposes the Viber phone number to all Thymeleaf templates as ${viberPhone}.
     * Used by the floating Viber FAB in fragments/layout.html.
     */
    @ModelAttribute("viberPhone")
    public String viberPhone() {
        return viberPhone;
    }
}
