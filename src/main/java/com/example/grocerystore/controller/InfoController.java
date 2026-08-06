package com.example.grocerystore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InfoController {

    @GetMapping("/user-guide")
    public String userGuide(Model model) {
        // Add any shared attributes here if needed
        return "user-guide";
    }
}
