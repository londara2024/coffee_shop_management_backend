package com.aura.coffeeshop.order.controller;

import com.aura.coffeeshop.common.web.PageResponse;
import com.aura.coffeeshop.order.OrderEnums.OrderStatus;
import com.aura.coffeeshop.order.dto.OrderDtos.OrderResponse;
import com.aura.coffeeshop.order.dto.OrderDtos.OrderSummaryResponse;
import com.aura.coffeeshop.order.dto.OrderDtos.PlaceOrderRequest;
import com.aura.coffeeshop.order.dto.OrderDtos.QuoteRequest;
import com.aura.coffeeshop.order.dto.OrderDtos.QuoteResponse;
import com.aura.coffeeshop.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Checkout & Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout/quote")
    @Operation(summary = "Price a cart before checkout", description = "Applies tip, promo code and discount without placing the order.")
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
        return orderService.quote(request);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order from a cart",
            description = "Snapshots prices, empties the cart, records a pending payment and opens a kitchen (KDS) ticket.")
    public OrderResponse place(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @GetMapping("/orders")
    @Operation(summary = "List orders", description = "Newest first. Filter by customer for a customer's order history.")
    public PageResponse<OrderSummaryResponse> list(
            @RequestParam(required = false) String customerId,
            @Parameter(description = "new | brewing | ready | completed | cancelled") @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.search(customerId, status, page, size);
    }

    @GetMapping("/orders/{orderCode}")
    @Operation(summary = "Track an order", description = "Order status page (e.g. /order/ACR-8942 on the frontend).")
    public OrderResponse get(@Parameter(example = "ACR-8942") @PathVariable String orderCode) {
        return orderService.getByCode(orderCode);
    }

    @PostMapping("/orders/{orderCode}/cancel")
    @Operation(summary = "Cancel an order", description = "Only allowed while the order is still `new`.")
    public OrderResponse cancel(@PathVariable String orderCode) {
        return orderService.cancel(orderCode);
    }

    @PostMapping("/orders/{orderCode}/payment/confirm")
    @Operation(summary = "Confirm payment received", description = "Marks the order's payment as paid (cash at pickup or ABA/KHQR confirmation).")
    public OrderResponse confirmPayment(@PathVariable String orderCode) {
        return orderService.confirmPayment(orderCode);
    }
}
