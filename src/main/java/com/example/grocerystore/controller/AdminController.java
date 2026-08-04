package com.example.grocerystore.controller;

import com.example.grocerystore.model.OrderStatus;
import com.example.grocerystore.model.Product;
import com.example.grocerystore.service.CloudinaryService;
import com.example.grocerystore.service.OrderService;
import com.example.grocerystore.service.ProductService;
import com.example.grocerystore.service.ReportService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final CloudinaryService cloudinaryService;
    private final ReportService reportService;

    public AdminController(ProductService productService,
                           OrderService orderService,
                           CloudinaryService cloudinaryService,
                           ReportService reportService) {
        this.productService = productService;
        this.orderService = orderService;
        this.cloudinaryService = cloudinaryService;
        this.reportService = reportService;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String query,
                            @RequestParam(required = false) String from,
                            @RequestParam(required = false) String to,
                            Model model) {
        // Parse dates
        LocalDate fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : null;
        LocalDate toDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : null;

        // Robustly parse status: avoid binding errors when status param is "null" or empty
        OrderStatus selectedStatus = null;
        if (status != null && !status.isBlank() && !"null".equalsIgnoreCase(status)) {
            try {
                selectedStatus = OrderStatus.valueOf(status);
            } catch (IllegalArgumentException ex) {
                // ignore invalid status and treat as null (All)
                selectedStatus = null;
            }
        }

        addDashboardModel(model, selectedStatus, query, fromDate, toDate, new Product());

        // Analytics range: default last 30 days
        java.time.LocalDateTime end = java.time.LocalDateTime.now();
        java.time.LocalDateTime start = end.minusDays(30);
        if (fromDate != null) start = fromDate.atStartOfDay();
        if (toDate != null) end = toDate.plusDays(1).atStartOfDay().minusNanos(1);

        model.addAttribute("analyticsRangeStart", start);
        model.addAttribute("analyticsRangeEnd", end);
        model.addAttribute("analytics", reportService.salesOverview(start, end));
        return "admin/admin-dashboard";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        // Provide an empty Product form for creating a new product
        model.addAttribute("productForm", new Product());
        return "admin/product-form";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id,
                              @RequestParam(required = false) OrderStatus status,
                              Model model) {
        // Load product into the product form template for editing
        Product p = productService.findById(id);
        model.addAttribute("productForm", p);
        model.addAttribute("editing", true);
        return "admin/product-form";
    }

    @PostMapping("/products/save")
    public String saveProductSave(@Valid @ModelAttribute("productForm") Product product,
                                  BindingResult bindingResult,
                                  @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        // Delegate to the existing save logic (shared)
        return saveOrUpdateProduct(product, bindingResult, imageFile, redirectAttributes, model);
    }

    // Backwards-compatible endpoint used by admin dashboard form
    @PostMapping("/products")
    public String saveProduct(@Valid @ModelAttribute("productForm") Product product,
                              BindingResult bindingResult,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        return saveOrUpdateProduct(product, bindingResult, imageFile, redirectAttributes, model);
    }

    // Shared save method for create/update
    private String saveOrUpdateProduct(Product product,
                                       BindingResult bindingResult,
                                       MultipartFile imageFile,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {

        if (product.getId() == null && (imageFile == null || imageFile.isEmpty())) {
            bindingResult.rejectValue("imageUrl", "error.product", "Please upload an image.");
        }

        if (bindingResult.hasErrors()) {
            // on error, return to product-form if editing/creating
            if (product.getId() != null) model.addAttribute("editing", true);
            model.addAttribute("productForm", product);
            return "admin/product-form";
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedImageUrl = cloudinaryService.uploadImage(imageFile);
                product.setImageUrl(uploadedImageUrl);
            }

            productService.save(product);
            redirectAttributes.addFlashAttribute("success", "Product saved successfully.");
        } catch (IOException e) {
            bindingResult.rejectValue("imageUrl", "error.product", "Failed to upload image.");
            model.addAttribute("productForm", product);
            return "admin/product-form";
        }

        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/toggle")
    public String toggleProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.toggleAvailability(id);
        redirectAttributes.addFlashAttribute("success", "Product stock availability updated.");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "This product is already used by an order. Toggle it out of stock instead.");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/orders/{id}/approve")
    public String approveOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.approve(id);
            redirectAttributes.addFlashAttribute("success", "Payment verified. Order is now Processing.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/orders/{id}/processing")
    public String markProcessing(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.markProcessing(id);
            redirectAttributes.addFlashAttribute("success", "Order marked as Processing.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/orders/{id}/out-for-delivery")
    public String markOutForDelivery(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.markOutForDelivery(id);
            redirectAttributes.addFlashAttribute("success", "Order marked as Out for Delivery.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/orders/{id}/confirm")
    public String confirmOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.confirm(id);
            redirectAttributes.addFlashAttribute("success", "Order marked as Completed.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(id);
            redirectAttributes.addFlashAttribute("success", "Order cancelled.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/orders/{id}/reject")
    public String rejectOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(id);
            redirectAttributes.addFlashAttribute("success", "Order rejected and cancelled.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        try {
            switch (status) {
                case "APPROVE":
                    orderService.approve(id);
                    redirectAttributes.addFlashAttribute("success", "Order approved.");
                    break;
                case "REJECT":
                case "CANCELLED":
                    orderService.cancel(id);
                    redirectAttributes.addFlashAttribute("success", "Order cancelled.");
                    break;
                case "PROCESSING":
                    orderService.markProcessing(id);
                    redirectAttributes.addFlashAttribute("success", "Order marked as Processing.");
                    break;
                case "OUT_FOR_DELIVERY":
                    orderService.markOutForDelivery(id);
                    redirectAttributes.addFlashAttribute("success", "Order marked as Out for Delivery.");
                    break;
                case "COMPLETED":
                    orderService.confirm(id);
                    redirectAttributes.addFlashAttribute("success", "Order marked as Completed.");
                    break;
                default:
                    redirectAttributes.addFlashAttribute("error", "Unknown status action.");
            }
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    private void addDashboardModel(Model model, OrderStatus status, String query,
                                   LocalDate from, LocalDate to, Product productForm) {
        model.addAttribute("orders", orderService.searchOrders(query, status, from, to));
        model.addAttribute("products", productService.findAll());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("searchQuery", query);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("productForm", productForm);
    }
}
