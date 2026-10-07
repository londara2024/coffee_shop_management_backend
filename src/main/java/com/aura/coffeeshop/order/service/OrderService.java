package com.aura.coffeeshop.order.service;

import com.aura.coffeeshop.cart.entity.Cart;
import com.aura.coffeeshop.cart.entity.CartItem;
import com.aura.coffeeshop.cart.service.CartService;
import com.aura.coffeeshop.common.Money;
import com.aura.coffeeshop.common.config.AppProperties;
import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.common.web.PageResponse;
import com.aura.coffeeshop.customer.entity.Customer;
import com.aura.coffeeshop.customer.service.CustomerService;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentTiming;
import com.aura.coffeeshop.order.OrderEnums.OrderStatus;
import com.aura.coffeeshop.order.OrderEnums.Station;
import com.aura.coffeeshop.order.OrderPlacedEvent;
import com.aura.coffeeshop.order.dto.OrderDtos.DiscountResponse;
import com.aura.coffeeshop.order.dto.OrderDtos.OrderResponse;
import com.aura.coffeeshop.order.dto.OrderDtos.OrderSummaryResponse;
import com.aura.coffeeshop.order.dto.OrderDtos.PlaceOrderRequest;
import com.aura.coffeeshop.order.dto.OrderDtos.PromoCodeResponse;
import com.aura.coffeeshop.order.dto.OrderDtos.QuoteRequest;
import com.aura.coffeeshop.order.dto.OrderDtos.QuoteResponse;
import com.aura.coffeeshop.order.entity.Discount;
import com.aura.coffeeshop.order.entity.Order;
import com.aura.coffeeshop.order.entity.OrderItem;
import com.aura.coffeeshop.order.entity.Payment;
import com.aura.coffeeshop.order.entity.PromoCode;
import com.aura.coffeeshop.order.repository.DiscountRepository;
import com.aura.coffeeshop.order.repository.OrderRepository;
import com.aura.coffeeshop.order.repository.PromoCodeRepository;
import com.aura.coffeeshop.store.entity.Store;
import com.aura.coffeeshop.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    /** details_label parts become KDS prep notes. */
    private static final String DETAILS_SEPARATOR = " · ";

    private final OrderRepository orderRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final DiscountRepository discountRepository;
    private final CartService cartService;
    private final CustomerService customerService;
    private final StoreService storeService;
    private final ApplicationEventPublisher events;
    private final AppProperties properties;

    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest request) {
        Cart cart = cartService.getCart(request.cartId());
        return computeTotals(cart, request).toResponse(cart.itemCount());
    }

    /**
     * Converts a cart into an order: snapshots prices, routes lines to kitchen stations, records the
     * pending payment, empties the cart and (via {@link OrderPlacedEvent}) opens a KDS ticket.
     */
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Cart cart = cartService.getCart(request.cartId());
        if (cart.getItems().isEmpty()) {
            throw BusinessRuleException.badRequest("Cannot place an order from an empty cart");
        }
        cart.getItems().stream().filter(i -> !i.getProduct().isLive()).findFirst().ifPresent(i -> {
            throw new BusinessRuleException("'" + i.getProduct().getName() + "' is no longer on the menu; remove it from the cart");
        });

        FulfillmentTiming timing = request.fulfillmentTiming() == null ? FulfillmentTiming.ASAP : request.fulfillmentTiming();
        Instant scheduledFor = null;
        if (timing == FulfillmentTiming.LATER) {
            if (request.scheduledFor() == null || !request.scheduledFor().isAfter(Instant.now())) {
                throw BusinessRuleException.badRequest("scheduledFor must be a future time when fulfillmentTiming is 'later'");
            }
            scheduledFor = request.scheduledFor();
        }

        Store store = storeService.resolveStore(request.storeId());
        if (!store.isOpen()) {
            throw new BusinessRuleException(store.getName() + " is currently closed and not accepting orders");
        }
        Customer customer = request.customerId() != null
                ? customerService.getCustomer(request.customerId())
                : cart.getCustomer();
        Totals totals = computeTotals(cart, request.toQuote());

        Order order = new Order();
        order.setOrderCode(properties.orderCodePrefix() + "-" + orderRepository.nextOrderNumber());
        order.setCustomer(customer);
        order.setStore(store);
        order.setFulfillmentMethod(request.fulfillmentMethod());
        order.setFulfillmentTiming(timing);
        order.setScheduledFor(scheduledFor);
        order.setSubtotal(totals.subtotal());
        order.setTax(totals.tax());
        order.setTip(totals.tip());
        order.setPromoCode(totals.promo());
        order.setPromoDiscountAmount(totals.promoAmount());
        order.setDiscount(totals.discount());
        order.setDiscountAmount(totals.discountAmount());
        order.setTotal(totals.total());
        order.setStatus(OrderStatus.NEW);

        for (CartItem line : cart.getItems()) {
            OrderItem item = new OrderItem();
            item.setProduct(line.getProduct());
            item.setQty(line.getQty());
            item.setUnitPrice(line.getUnitPrice());
            item.setDetailsLabel(line.getDetailsLabel());
            item.setStation(Station.forCategory(line.getProduct().getCategory().getId()));
            if (line.getDetailsLabel() != null) {
                for (String part : line.getDetailsLabel().split(DETAILS_SEPARATOR)) {
                    if (!part.isBlank()) {
                        item.addNote(part.trim());
                    }
                }
            }
            order.addItem(item);
        }

        Payment payment = new Payment();
        payment.setMethod(request.paymentMethod());
        payment.setMethodLabel(request.paymentMethod().getLabel());
        payment.setAmount(totals.total());
        order.attachPayment(payment);

        Order saved = orderRepository.save(order);
        cart.clear();
        events.publishEvent(new OrderPlacedEvent(saved));
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByCode(String orderCode) {
        return OrderResponse.from(getOrder(orderCode));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> search(String customerId, OrderStatus status, int page, int size) {
        return PageResponse.of(orderRepository.search(customerId, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"))), OrderSummaryResponse::from);
    }

    /** Customers can cancel only before the kitchen starts on the order. */
    public OrderResponse cancel(String orderCode) {
        Order order = getOrder(orderCode);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BusinessRuleException("Order " + orderCode + " is '" + order.getStatus().getCode()
                    + "' and can no longer be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(order);
    }

    /** Marks the payment as received (cash at pickup, or ABA/KHQR confirmation). */
    public OrderResponse confirmPayment(String orderCode) {
        Order order = getOrder(orderCode);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot take payment for a cancelled order");
        }
        Payment payment = order.getPayment();
        if (payment.getPaidAt() != null) {
            throw new BusinessRuleException("Order " + orderCode + " is already paid");
        }
        payment.setPaidAt(Instant.now());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PromoCodeResponse validatePromo(String code) {
        return PromoCodeResponse.from(promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new NotFoundException("Promo code '" + code + "' is invalid or expired")));
    }

    @Transactional(readOnly = true)
    public List<PromoCodeResponse> activePromos() {
        return promoCodeRepository.findByActiveTrueOrderByAmountAsc().stream().map(PromoCodeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> discounts() {
        return discountRepository.findAllByOrderByAmountAsc().stream().map(DiscountResponse::from).toList();
    }

    Order getOrder(String orderCode) {
        return orderRepository.findByOrderCode(orderCode).orElseThrow(() -> NotFoundException.of("Order", orderCode));
    }

    private Totals computeTotals(Cart cart, QuoteRequest request) {
        if (request.discountId() != null && request.customDiscountAmount() != null) {
            throw BusinessRuleException.badRequest("Use either discountId or customDiscountAmount, not both");
        }
        BigDecimal subtotal = cart.subtotal();
        BigDecimal tax = Money.scale(subtotal.multiply(properties.taxRate()));
        BigDecimal tip = Money.orZero(request.tip());

        PromoCode promo = null;
        BigDecimal promoAmount = Money.ZERO;
        if (request.promoCode() != null && !request.promoCode().isBlank()) {
            promo = promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(request.promoCode().trim())
                    .orElseThrow(() -> BusinessRuleException.badRequest("Promo code '" + request.promoCode() + "' is invalid or expired"));
            promoAmount = Money.scale(promo.getAmount());
        }

        Discount discount = null;
        BigDecimal discountAmount = Money.orZero(request.customDiscountAmount());
        if (request.discountId() != null) {
            discount = discountRepository.findById(request.discountId())
                    .orElseThrow(() -> NotFoundException.of("Discount", request.discountId()));
            discountAmount = Money.scale(discount.getAmount());
        }

        // Same formula as checkout-view.tsx; an empty cart always totals 0.
        BigDecimal total = cart.getItems().isEmpty() ? Money.ZERO
                : Money.scale(subtotal.add(tip).add(tax).subtract(discountAmount).subtract(promoAmount).max(BigDecimal.ZERO));
        return new Totals(subtotal, tax, tip, promo, promoAmount, discount, discountAmount, total);
    }

    private record Totals(BigDecimal subtotal, BigDecimal tax, BigDecimal tip, PromoCode promo, BigDecimal promoAmount,
                          Discount discount, BigDecimal discountAmount, BigDecimal total) {
        QuoteResponse toResponse(int itemCount) {
            return new QuoteResponse(itemCount, subtotal, tax, tip, promo == null ? null : promo.getCode(), promoAmount,
                    discount == null ? null : discount.getId(), discountAmount, total);
        }
    }
}
