package com.aura.coffeeshop.order.controller;

import com.aura.coffeeshop.order.dto.OrderDtos.DiscountResponse;
import com.aura.coffeeshop.order.dto.OrderDtos.PromoCodeResponse;
import com.aura.coffeeshop.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Promotions")
public class PromotionController {

    private final OrderService orderService;

    @GetMapping("/promo-codes")
    @Operation(summary = "List active promo codes")
    public List<PromoCodeResponse> promoCodes() {
        return orderService.activePromos();
    }

    @GetMapping("/promo-codes/{code}")
    @Operation(summary = "Validate a promo code", description = "404 when the code does not exist or is inactive.")
    public PromoCodeResponse validate(@PathVariable String code) {
        return orderService.validatePromo(code);
    }

    @GetMapping("/discounts")
    @Operation(summary = "List preset staff discounts")
    public List<DiscountResponse> discounts() {
        return orderService.discounts();
    }
}
