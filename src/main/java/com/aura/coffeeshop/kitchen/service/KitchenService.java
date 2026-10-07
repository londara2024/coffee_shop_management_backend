package com.aura.coffeeshop.kitchen.service;

import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.customer.service.CustomerService;
import com.aura.coffeeshop.kitchen.KitchenEnums.TicketStatus;
import com.aura.coffeeshop.kitchen.KitchenEnums;
import com.aura.coffeeshop.kitchen.dto.KitchenDtos.BoardSummaryResponse;
import com.aura.coffeeshop.kitchen.dto.KitchenDtos.TicketResponse;
import com.aura.coffeeshop.kitchen.dto.KitchenDtos.UpdateTicketRequest;
import com.aura.coffeeshop.kitchen.entity.KitchenTicket;
import com.aura.coffeeshop.kitchen.entity.KitchenTicketItem;
import com.aura.coffeeshop.kitchen.repository.KitchenTicketRepository;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentMethod;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentTiming;
import com.aura.coffeeshop.order.OrderEnums.OrderStatus;
import com.aura.coffeeshop.order.OrderEnums.Station;
import com.aura.coffeeshop.order.OrderPlacedEvent;
import com.aura.coffeeshop.order.entity.Order;
import com.aura.coffeeshop.order.entity.OrderItem;
import com.aura.coffeeshop.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Kitchen display workflow. Ticket transitions keep the order status in sync:
 * <pre>
 *   scheduled --release--> new --start--> brewing --ready--> ready --archive--> (order completed)
 *                           ^                |
 *                           +----recall------+
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class KitchenService {

    private static final Set<OrderStatus> OPEN = EnumSet.of(OrderStatus.NEW, OrderStatus.BREWING, OrderStatus.READY);
    private static final int BASE_SLA_MINUTES = 4;
    private static final int MAX_SLA_MINUTES = 15;

    private final KitchenTicketRepository ticketRepository;
    private final CustomerService customerService;
    private final StoreService storeService;

    /** Opens a ticket for every placed order; runs inside the checkout transaction. */
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        Order order = event.order();
        boolean scheduled = order.getFulfillmentTiming() == FulfillmentTiming.LATER;

        KitchenTicket ticket = new KitchenTicket();
        ticket.setOrder(order);
        ticket.setStatus(scheduled ? TicketStatus.SCHEDULED : TicketStatus.NEW);
        ticket.setChannelLabel(scheduled ? "ការកម្ម៉ង់តាមកាលវិភាគ"
                : order.getFulfillmentMethod() == FulfillmentMethod.CURBSIDE ? "ចំណតទទួលក្រៅរថយន្ត" : "ទទួលនៅកន្លែងបញ្ជរ");
        int totalQty = order.getItems().stream().mapToInt(OrderItem::getQty).sum();
        ticket.setSlaMinutes(Math.min(MAX_SLA_MINUTES, BASE_SLA_MINUTES + 2 * totalQty));
        ticket.setStartedAt(Instant.now());
        for (OrderItem orderItem : order.getItems()) {
            KitchenTicketItem item = new KitchenTicketItem();
            item.setOrderItem(orderItem);
            item.setStation(orderItem.getStation() != null ? orderItem.getStation() : Station.BAR_A);
            ticket.addItem(item);
        }
        ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> board(TicketStatus status, Station station) {
        Instant now = Instant.now();
        return ticketRepository.findByOrderStatusIn(OPEN).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> station == null || t.getItems().stream().anyMatch(i -> i.getStation() == station))
                .map(t -> TicketResponse.from(t, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardSummaryResponse summary() {
        List<KitchenTicket> open = ticketRepository.findByOrderStatusIn(OPEN);
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new BoardSummaryResponse(open.size(),
                count(open, TicketStatus.NEW) + count(open, TicketStatus.SCHEDULED),
                count(open, TicketStatus.BREWING), count(open, TicketStatus.READY),
                ticketRepository.countByOrderStatusSince(OrderStatus.COMPLETED, startOfDay));
    }

    @Transactional(readOnly = true)
    public TicketResponse get(String ticketId) {
        return TicketResponse.from(getTicket(ticketId), Instant.now());
    }

    /** The board's primary button: start → mark ready → archive. */
    public TicketResponse advance(String ticketId) {
        KitchenTicket ticket = getOpenTicket(ticketId);
        Order order = ticket.getOrder();
        switch (ticket.getStatus()) {
            case NEW -> {
                ticket.setStatus(TicketStatus.BREWING);
                order.setStatus(OrderStatus.BREWING);
            }
            case BREWING -> {
                ticket.setStatus(TicketStatus.READY);
                ticket.setReadyAt(Instant.now());
                ticket.getItems().forEach(i -> i.setDone(true));
                order.setStatus(OrderStatus.READY);
            }
            case READY -> {
                order.setStatus(OrderStatus.COMPLETED);
                if (order.getCustomer() != null) {
                    customerService.awardStars(order.getCustomer().getId(), order.getTotal());
                }
            }
            case SCHEDULED -> throw new BusinessRuleException("Ticket is scheduled; release it to the queue first");
        }
        return TicketResponse.from(ticket, Instant.now());
    }

    /** Send a brewing ticket back to the queue. */
    public TicketResponse recall(String ticketId) {
        KitchenTicket ticket = getOpenTicket(ticketId);
        requireStatus(ticket, TicketStatus.BREWING, "recall");
        ticket.setStatus(TicketStatus.NEW);
        ticket.getOrder().setStatus(OrderStatus.NEW);
        return TicketResponse.from(ticket, Instant.now());
    }

    /** Move a scheduled ticket into the live queue now. */
    public TicketResponse release(String ticketId) {
        KitchenTicket ticket = getOpenTicket(ticketId);
        requireStatus(ticket, TicketStatus.SCHEDULED, "release");
        ticket.setStatus(TicketStatus.NEW);
        ticket.setStartedAt(Instant.now());
        return TicketResponse.from(ticket, Instant.now());
    }

    public TicketResponse setItemDone(String ticketId, String itemId, boolean done) {
        KitchenTicket ticket = getOpenTicket(ticketId);
        KitchenTicketItem item = ticket.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> NotFoundException.of("Ticket item", itemId));
        item.setDone(done);
        return TicketResponse.from(ticket, Instant.now());
    }

    public TicketResponse update(String ticketId, UpdateTicketRequest request) {
        KitchenTicket ticket = getOpenTicket(ticketId);
        if (request.assignedStaffId() != null) {
            ticket.setAssignedStaff(storeService.getStaff(request.assignedStaffId()));
        }
        if (request.flagText() != null) {
            boolean clear = request.flagText().isBlank();
            ticket.setFlagText(clear ? null : request.flagText().trim());
            ticket.setFlagTone(clear ? null : (request.flagTone() != null ? request.flagTone() : KitchenEnums.FlagTone.NEUTRAL));
        } else if (request.flagTone() != null && ticket.getFlagText() != null) {
            ticket.setFlagTone(request.flagTone());
        }
        if (request.slaMinutes() != null) {
            ticket.setSlaMinutes(request.slaMinutes());
        }
        return TicketResponse.from(ticket, Instant.now());
    }

    private KitchenTicket getTicket(String ticketId) {
        return ticketRepository.findById(ticketId).orElseThrow(() -> NotFoundException.of("Kitchen ticket", ticketId));
    }

    private KitchenTicket getOpenTicket(String ticketId) {
        KitchenTicket ticket = getTicket(ticketId);
        if (!ticket.getOrder().getStatus().isOpen()) {
            throw new BusinessRuleException("Order " + ticket.getOrder().getOrderCode() + " is already "
                    + ticket.getOrder().getStatus().getCode());
        }
        return ticket;
    }

    private static void requireStatus(KitchenTicket ticket, TicketStatus expected, String action) {
        if (ticket.getStatus() != expected) {
            throw new BusinessRuleException("Cannot " + action + " a ticket that is '" + ticket.getStatus().getCode()
                    + "' (expected '" + expected.getCode() + "')");
        }
    }

    private static int count(List<KitchenTicket> tickets, TicketStatus status) {
        return (int) tickets.stream().filter(t -> t.getStatus() == status).count();
    }
}
