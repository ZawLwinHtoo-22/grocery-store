package com.example.grocerystore.controller;

import com.example.grocerystore.model.OrderStatus;
import com.example.grocerystore.model.Product;
import com.example.grocerystore.service.OrderService;
import com.example.grocerystore.service.ProductService;
import javax.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;

    public AdminController(ProductService productService, OrderService orderService) {
        this.productService = productService;
        this.orderService = orderService;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(@RequestParam(required = false) OrderStatus status, Model model) {
        addDashboardModel(model, status, new Product());
        return "admin/admin-dashboard";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id,
                              @RequestParam(required = false) OrderStatus status,
                              Model model) {
        addDashboardModel(model, status, productService.findById(id));
        model.addAttribute("editing", true);
        return "admin/admin-dashboard";
    }

    @PostMapping("/products")
    public String saveProduct(@Valid @ModelAttribute("productForm") Product product,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            addDashboardModel(model, null, product);
            return "admin/admin-dashboard";
        }
        productService.save(product);
        redirectAttributes.addFlashAttribute("success", "Product saved.");
        return "redirect:/admin/dashboard#products";
    }

    @PostMapping("/products/{id}/toggle")
    public String toggleProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.toggleAvailability(id);
        redirectAttributes.addFlashAttribute("success", "Product stock availability updated.");
        return "redirect:/admin/dashboard#products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "This product is already used by an order. Toggle it out of stock instead.");
        }
        return "redirect:/admin/dashboard#products";
    }

    @PostMapping("/orders/{id}/approve")
    public String approveOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.approve(id);
            redirectAttributes.addFlashAttribute("success", "Order approved. Customer can now submit payment.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/orders/{id}/confirm")
    public String confirmOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.confirm(id);
            redirectAttributes.addFlashAttribute("success", "Order completed.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard?status=PAYMENT_SUBMITTED";
    }

    private void addDashboardModel(Model model, OrderStatus status, Product productForm) {
        model.addAttribute("orders", orderService.findOrders(status));
        model.addAttribute("products", productService.findAll());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("productForm", productForm);
    }
}
