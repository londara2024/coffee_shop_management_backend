package com.aura.coffeeshop.kitchen.dto;

import com.aura.coffeeshop.kitchen.KitchenEnums.FlagTone;
import com.aura.coffeeshop.kitchen.KitchenEnums.TicketStatus;
import com.aura.coffeeshop.kitchen.entity.KitchenTicket;
import com.aura.coffeeshop.kitchen.entity.KitchenTicketItem;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentTiming;
import com.aura.coffeeshop.order.OrderEnums.Station;
import com.aura.coffeeshop.order.entity.Order;
import com.aura.coffeeshop.order.entity.OrderItemNote;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class KitchenDtos {

    private KitchenDtos() {
    }

    public record TicketItemResponse(String id, int qty, String name, String nameKm, Station station,
                                     List<String> notes, boolean done) {
        public static TicketItemResponse from(KitchenTicketItem i) {
            var orderItem = i.getOrderItem();
            return new TicketItemResponse(i.getId(), orderItem.getQty(), orderItem.getProduct().getName(),
                    orderItem.getProduct().getNameKm(), i.getStation(),
                    orderItem.getNotes().stream().map(OrderItemNote::getNote).toList(), i.isDone());
        }
    }

    public record FlagResponse(FlagTone tone, String text) {
    }

    /** One card on the KDS board (kds-board.tsx {@code Ticket}). */
    public record TicketResponse(String id, String orderCode, String guest, String where, String channel,
                                 TicketStatus status, int slaMinutes, Instant startedAt, long elapsedSeconds,
                                 boolean overSla, Instant readyAt, Instant scheduledFor, FlagResponse flag,
                                 String assignedStaffId, String assignedStaffName, List<TicketItemResponse> items) {
        public static TicketResponse from(KitchenTicket t, Instant now) {
            Order order = t.getOrder();
            boolean active = t.getStatus() == TicketStatus.NEW || t.getStatus() == TicketStatus.BREWING;
            long elapsed = active ? Math.max(0, Duration.between(t.getStartedAt(), now).toSeconds()) : 0;
            String where = order.getFulfillmentTiming() == FulfillmentTiming.LATER && order.getScheduledFor() != null
                    ? "Scheduled for " + order.getScheduledFor()
                    : order.pickupLabel();
            var staff = t.getAssignedStaff();
            return new TicketResponse(t.getId(), order.getOrderCode(),
                    order.getCustomer() == null ? "Guest" : order.getCustomer().getName(), where, t.getChannelLabel(),
                    t.getStatus(), t.getSlaMinutes(), t.getStartedAt(), elapsed, active && elapsed > t.getSlaMinutes() * 60L,
                    t.getReadyAt(), order.getScheduledFor(),
                    t.getFlagText() == null ? null : new FlagResponse(t.getFlagTone(), t.getFlagText()),
                    staff == null ? null : staff.getId(), staff == null ? null : staff.getName(),
                    t.getItems().stream().map(TicketItemResponse::from).toList());
        }
    }

    /** Counts for the board's filter tabs; {@code queued} = new + scheduled. */
    public record BoardSummaryResponse(int all, int queued, int brewing, int ready, long completedToday) {
    }

    public record UpdateTicketRequest(
            @Schema(description = "Assign a barista / cook") String assignedStaffId,
            @Schema(description = "Clear the flag by sending an empty flagText") FlagTone flagTone,
            @Size(max = 300) String flagText,
            @Min(1) @Max(120) Integer slaMinutes) {
    }

    public record ItemDoneRequest(@NotNull Boolean done) {
    }
}
