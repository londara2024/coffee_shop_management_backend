package com.aura.coffeeshop.customer.controller;

import com.aura.coffeeshop.customer.dto.CustomerDtos.CustomerResponse;
import com.aura.coffeeshop.customer.dto.CustomerDtos.SignInRequest;
import com.aura.coffeeshop.customer.dto.CustomerDtos.SignUpRequest;
import com.aura.coffeeshop.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final CustomerService customerService;

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a customer account", description = "Also opens a loyalty account at the `Member` tier.")
    @ApiResponse(responseCode = "409", description = "Email or phone already registered")
    public CustomerResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return customerService.signUp(request);
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in with email and password", description = "Demo account: elena.rostova@botanica.com / Password123")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    public CustomerResponse signIn(@Valid @RequestBody SignInRequest request) {
        return customerService.signIn(request);
    }
}
