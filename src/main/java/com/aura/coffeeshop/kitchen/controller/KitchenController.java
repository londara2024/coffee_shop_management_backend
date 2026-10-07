package com.aura.coffeeshop.kitchen.controller;

import com.aura.coffeeshop.kitchen.KitchenEnums.TicketStatus;
import com.aura.coffeeshop.kitchen.dto.KitchenDtos.BoardSummaryResponse;
import com.aura.coffeeshop.kitchen.dto.KitchenDtos.ItemDoneRequest;
import com.aura.coffeeshop.kitchen.dto.KitchenDtos.TicketResponse;
import com.aura.coffeeshop.kitchen.dto.KitchenDtos.UpdateTicketRequest;
import com.aura.coffeeshop.kitchen.service.KitchenService;
import com.aura.coffeeshop.order.OrderEnums.Station;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/kitchen")
@RequiredArgsConstructor
@Tag(name = "Kitchen (KDS)")
public class KitchenController {

    private final KitchenService kitchenService;

    @GetMapping("/tickets")
    @Operation(summary = "Kitchen board", description = "Open tickets (order not completed/cancelled), oldest first.")
    public List<TicketResponse> board(
            @Parameter(description = "scheduled | new | brewing | ready") @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Bar A | Bar B | Hearth | Bakery | Cold Lab | Tea Bar") @RequestParam(required = false) Station station) {
        return kitchenService.board(status, station);
    }

    @GetMapping("/summary")
    @Operation(summary = "Board counters", description = "Counts per status tab plus tickets completed today.")
    public BoardSummaryResponse summary() {
        return kitchenService.summary();
    }

    @GetMapping("/tickets/{ticketId}")
    @Operation(summary = "Get one ticket")
    public TicketResponse get(@PathVariable String ticketId) {
        return kitchenService.get(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/advance")
    @Operation(summary = "Bump a ticket to its next stage",
            description = "new → brewing → ready → archived (order completed; loyalty stars awarded).")
    public TicketResponse advance(@PathVariable String ticketId) {
        return kitchenService.advance(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/recall")
    @Operation(summary = "Send a brewing ticket back to the queue")
    public TicketResponse recall(@PathVariable String ticketId) {
        return kitchenService.recall(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/release")
    @Operation(summary = "Release a scheduled ticket into the live queue now")
    public TicketResponse release(@PathVariable String ticketId) {
        return kitchenService.release(ticketId);
    }

    @PatchMapping("/tickets/{ticketId}")
    @Operation(summary = "Assign staff, set a flag or adjust the SLA")
    public TicketResponse update(@PathVariable String ticketId, @Valid @RequestBody UpdateTicketRequest request) {
        return kitchenService.update(ticketId, request);
    }

    @PatchMapping("/tickets/{ticketId}/items/{itemId}")
    @Operation(summary = "Mark one station's item done / not done")
    public TicketResponse setItemDone(@PathVariable String ticketId, @PathVariable String itemId,
                                      @Valid @RequestBody ItemDoneRequest request) {
        return kitchenService.setItemDone(ticketId, itemId, request.done());
    }
}
