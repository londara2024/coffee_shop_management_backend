package com.aura.coffeeshop.order.dto;

import com.aura.coffeeshop.common.Money;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentMethod;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentTiming;
import com.aura.coffeeshop.order.OrderEnums.OrderStatus;
import com.aura.coffeeshop.order.OrderEnums.PaymentMethod;
import com.aura.coffeeshop.order.OrderEnums.Station;
import com.aura.coffeeshop.order.entity.Discount;
import com.aura.coffeeshop.order.entity.Order;
import com.aura.coffeeshop.order.entity.OrderItem;
import com.aura.coffeeshop.order.entity.OrderItemNote;
import com.aura.coffeeshop.order.entity.Payment;
import com.aura.coffeeshop.order.entity.PromoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    /** Price adjustments shared by quote and checkout. */
    public record QuoteRequest(
            @NotNull String cartId,
            @Schema(example = "2.00") @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal tip,
            @Schema(example = "WELCOME5") @Size(max = 40) String promoCode,
            @Schema(description = "Preset staff discount (discounts.id)") String discountId,
            @Schema(description = "Ad-hoc staff discount; cannot be combined with discountId", example = "1.50")
            @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal customDiscountAmount) {
    }

    public record PlaceOrderRequest(
            @NotNull String cartId,
            @Schema(description = "Defaults to the cart's customer; omit both for a guest order") String customerId,
            @Schema(description = "Defaults to the flagship store") String storeId,
            @Schema(example = "store") @NotNull FulfillmentMethod fulfillmentMethod,
            @Schema(example = "asap", defaultValue = "asap") FulfillmentTiming fulfillmentTiming,
            @Schema(description = "Required when fulfillmentTiming = later; must be in the future") Instant scheduledFor,
            @Schema(example = "aba") @NotNull PaymentMethod paymentMethod,
            @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal tip,
            @Size(max = 40) String promoCode,
            String discountId,
            @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal customDiscountAmount) {

        public QuoteRequest toQuote() {
            return new QuoteRequest(cartId, tip, promoCode, discountId, customDiscountAmount);
        }
    }

    public record QuoteResponse(int itemCount, BigDecimal subtotal, BigDecimal tax, BigDecimal tip,
                                String promoCode, BigDecimal promoDiscountAmount, String discountId,
                                BigDecimal discountAmount, BigDecimal total) {
    }

    public record PaymentResponse(PaymentMethod method, String methodLabel, BigDecimal amount, boolean paid,
                                  Instant paidAt) {
        public static PaymentResponse from(Payment p) {
            return p == null ? null
                    : new PaymentResponse(p.getMethod(), p.getMethodLabel(), p.getAmount(), p.getPaidAt() != null, p.getPaidAt());
        }
    }

    public record OrderItemResponse(String id, String productId, String productSlug, String name, String nameKm,
                                    String imageUrl, int qty, BigDecimal unitPrice, BigDecimal lineTotal,
                                    String detailsLabel, Station station, List<String> notes) {
        public static OrderItemResponse from(OrderItem i) {
            var p = i.getProduct();
            return new OrderItemResponse(i.getId(), p.getId(), p.getSlug(), p.getName(), p.getNameKm(),
                    p.getImageUrl(), i.getQty(), i.getUnitPrice(), Money.scale(i.lineTotal()), i.getDetailsLabel(),
                    i.getStation(), i.getNotes().stream().map(OrderItemNote::getNote).toList());
        }
    }

    public record OrderResponse(String id, String orderCode, OrderStatus status, String customerId, String customerName,
                                String storeId, String storeName, FulfillmentMethod fulfillmentMethod,
                                FulfillmentTiming fulfillmentTiming, Instant scheduledFor, String pickupLabel,
                                List<OrderItemResponse> items, BigDecimal subtotal, BigDecimal tax, BigDecimal tip,
                                String promoCode, BigDecimal promoDiscountAmount, BigDecimal discountAmount,
                                BigDecimal total, PaymentResponse payment, Instant placedAt) {
        public static OrderResponse from(Order o) {
            var customer = o.getCustomer();
            return new OrderResponse(o.getId(), o.getOrderCode(), o.getStatus(),
                    customer == null ? null : customer.getId(), customer == null ? null : customer.getName(),
                    o.getStore().getId(), o.getStore().getName(), o.getFulfillmentMethod(), o.getFulfillmentTiming(),
                    o.getScheduledFor(), o.pickupLabel(),
                    o.getItems().stream().map(OrderItemResponse::from).toList(),
                    o.getSubtotal(), o.getTax(), o.getTip(),
                    o.getPromoCode() == null ? null : o.getPromoCode().getCode(), o.getPromoDiscountAmount(),
                    o.getDiscountAmount(), o.getTotal(), PaymentResponse.from(o.getPayment()), o.getPlacedAt());
        }
    }

    /** Lightweight row for order history lists. */
    public record OrderSummaryResponse(String id, String orderCode, OrderStatus status, String customerName,
                                       String storeName, FulfillmentMethod fulfillmentMethod, BigDecimal total,
                                       boolean paid, Instant placedAt) {
        public static OrderSummaryResponse from(Order o) {
            return new OrderSummaryResponse(o.getId(), o.getOrderCode(), o.getStatus(),
                    o.getCustomer() == null ? null : o.getCustomer().getName(), o.getStore().getName(),
                    o.getFulfillmentMethod(), o.getTotal(),
                    o.getPayment() != null && o.getPayment().getPaidAt() != null, o.getPlacedAt());
        }
    }

    public record PromoCodeResponse(String code, String label, BigDecimal amount) {
        public static PromoCodeResponse from(PromoCode p) {
            return new PromoCodeResponse(p.getCode(), p.getLabel(), p.getAmount());
        }
    }

    public record DiscountResponse(String id, String label, BigDecimal amount) {
        public static DiscountResponse from(Discount d) {
            return new DiscountResponse(d.getId(), d.getLabel(), d.getAmount());
        }
    }
}
