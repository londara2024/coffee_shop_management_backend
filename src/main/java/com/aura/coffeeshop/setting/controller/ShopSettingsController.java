package com.aura.coffeeshop.setting.controller;

import com.aura.coffeeshop.setting.dto.SettingsDtos.CreateStaffRequest;
import com.aura.coffeeshop.setting.dto.SettingsDtos.ShopSettingsResponse;
import com.aura.coffeeshop.setting.dto.SettingsDtos.ShopStatusRequest;
import com.aura.coffeeshop.setting.dto.SettingsDtos.UpdateShopRequest;
import com.aura.coffeeshop.setting.dto.SettingsDtos.UpdateStaffRequest;
import com.aura.coffeeshop.setting.service.ShopSettingsService;
import com.aura.coffeeshop.store.dto.StaffResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin - Settings")
public class ShopSettingsController {

    private static final String STORE_PARAM = "Store to manage; defaults to the flagship store";

    private final ShopSettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get shop settings",
            description = "Shop info, branding (logo, brand colour + preset palette), open/closed status, staff list and the selectable staff roles.")
    public ShopSettingsResponse get(@Parameter(description = STORE_PARAM) @RequestParam(required = false) String storeId) {
        return settingsService.get(storeId);
    }

    @PatchMapping
    @Operation(summary = "Update shop info",
            description = "Name, address, opening hours, map coordinates and brand colour (one of `brandColors`). Only fields sent are changed.")
    public ShopSettingsResponse update(@Parameter(description = STORE_PARAM) @RequestParam(required = false) String storeId,
                                       @Valid @RequestBody UpdateShopRequest request) {
        return settingsService.updateShop(storeId, request);
    }

    @PatchMapping("/status")
    @Operation(summary = "Open or close the shop", description = "While closed, placing an order returns 409.")
    public ShopSettingsResponse setStatus(@Parameter(description = STORE_PARAM) @RequestParam(required = false) String storeId,
                                          @Valid @RequestBody ShopStatusRequest request) {
        return settingsService.setOpen(storeId, request.open());
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace the shop logo",
            description = "Multipart field `file`: JPEG, PNG, WebP or GIF up to 5 MB (a square ~256 px image is ideal). "
                    + "The previous uploaded logo is deleted. 413 when too large.")
    public ShopSettingsResponse uploadLogo(@Parameter(description = STORE_PARAM) @RequestParam(required = false) String storeId,
                                           @RequestPart("file") MultipartFile file) {
        return settingsService.uploadLogo(storeId, file);
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Remove the shop logo", description = "The shop falls back to the default Aura mark.")
    public ShopSettingsResponse removeLogo(@Parameter(description = STORE_PARAM) @RequestParam(required = false) String storeId) {
        return settingsService.removeLogo(storeId);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a staff member")
    public StaffResponse addStaff(@Parameter(description = STORE_PARAM) @RequestParam(required = false) String storeId,
                                  @Valid @RequestBody CreateStaffRequest request) {
        return settingsService.addStaff(storeId, request);
    }

    @PatchMapping("/staff/{staffId}")
    @Operation(summary = "Rename or change the role of a staff member", description = "409 when demoting the store's last manager.")
    public StaffResponse updateStaff(@PathVariable String staffId, @Valid @RequestBody UpdateStaffRequest request) {
        return settingsService.updateStaff(staffId, request);
    }

    @DeleteMapping("/staff/{staffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a staff member",
            description = "History is kept (tickets, purchase orders, menu items lose only the staff link). 409 for the store's last manager.")
    public void removeStaff(@PathVariable String staffId) {
        settingsService.removeStaff(staffId);
    }
}
