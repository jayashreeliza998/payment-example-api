package com.paypal.example.controller;




import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.example.entities.Product;
import com.paypal.example.repository.ProductRepository;
import com.paypal.example.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductRepository productRepository;

    private static final String SUCCESS_URL = "http://localhost:8080/api/payment/success";
    private static final String CANCEL_URL = "http://localhost:8080/api/payment/cancel";

    // API to list all products
    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
//http://localhost:8080/api/payment/pay/1
    // API to pay for a specific product
    @PostMapping("/pay/{productId}")
    public String payForProduct(@PathVariable("productId") Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return "Product not found!";
        }

        try {
            Payment payment = paymentService.createPayment(
                    product.getPrice(),
                    "USD",
                    "paypal",
                    "sale",
                    product.getDescription(),
                    CANCEL_URL,
                    SUCCESS_URL
            );
            for (Links link : payment.getLinks()) {
                if (link.getRel().equals("approval_url")) {
                    return "redirect:" + link.getHref();
                }
            }
        } catch (PayPalRESTException e) {
            e.printStackTrace();
        }
        return "redirect:/";
    }

    // Payment success handler
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId) {
        try {
            Payment payment = paymentService.executePayment(paymentId, payerId);
            if (payment.getState().equals("approved")) {
                return "Payment successful!";
            }
        } catch (PayPalRESTException e) {
            e.printStackTrace();
        }
        return "Payment failed!";
    }

    // Payment cancel handler
    @GetMapping("/cancel")
    public String paymentCancel() {
        return "Payment canceled!";
    }
}
