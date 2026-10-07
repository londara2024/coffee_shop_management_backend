package com.aura.coffeeshop.store.controller;

import com.aura.coffeeshop.store.dto.StaffResponse;
import com.aura.coffeeshop.store.dto.StoreResponse;
import com.aura.coffeeshop.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
@Tag(name = "Stores")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    @Operation(summary = "List store locations")
    public List<StoreResponse> list() {
        return storeService.listStores();
    }

    @GetMapping("/{storeId}")
    @Operation(summary = "Get a store location")
    public StoreResponse get(@PathVariable String storeId) {
        return StoreResponse.from(storeService.getStore(storeId));
    }

    @GetMapping("/{storeId}/staff")
    @Operation(summary = "List staff working at a store")
    public List<StaffResponse> staff(@PathVariable String storeId) {
        return storeService.listStaff(storeId);
    }
}
