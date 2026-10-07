package com.aura.coffeeshop.customer.controller;

import com.aura.coffeeshop.customer.dto.CustomerDtos.CustomerResponse;
import com.aura.coffeeshop.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{customerId}")
    @Operation(summary = "Get a customer profile with loyalty status")
    public CustomerResponse get(@PathVariable String customerId) {
        return customerService.getProfile(customerId);
    }
}
