package com.aura.coffeeshop.setting.dto;

import com.aura.coffeeshop.setting.entity.BrandColor;
import com.aura.coffeeshop.store.StaffRole;
import com.aura.coffeeshop.store.dto.StaffResponse;
import com.aura.coffeeshop.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Arrays;
import java.util.List;

/** Admin settings page (shop-settings-manager.tsx). */
public final class SettingsDtos {

    private SettingsDtos() {
    }

    public record RoleOption(StaffRole code, String label, String labelKm) {
        public static List<RoleOption> all() {
            return Arrays.stream(StaffRole.values()).map(r -> new RoleOption(r, r.getLabel(), r.getLabelKm())).toList();
        }
    }

    /** A preset theme colour for the colour picker. */
    public record BrandColorOption(String code, String label, String labelKm, String hex, boolean isDefault) {
        public static BrandColorOption from(BrandColor c) {
            return new BrandColorOption(c.getCode(), c.getLabel(), c.getLabelKm(), c.getHex(), c.isDefaultColor());
        }
    }

    /** Everything the settings page renders in one call. */
    public record ShopSettingsResponse(String storeId, String shopName, String address, String hoursLabel,
                                       Double latitude, Double longitude, String mapsUrl, boolean open,
                                       @Schema(description = "Uploaded logo, or null for the default mark") String logoUrl,
                                       @Schema(example = "#8d4f06") String brandColor,
                                       List<BrandColorOption> brandColors,
                                       List<StaffResponse> staff, List<RoleOption> staffRoles) {
        public static ShopSettingsResponse from(Store store, List<StaffResponse> staff, List<BrandColorOption> colors) {
            return new ShopSettingsResponse(store.getId(), store.getName(), store.getAddress(), store.getHoursLabel(),
                    store.getLatitude(), store.getLongitude(), store.mapsUrl(), store.isOpen(), store.getLogoUrl(),
                    store.getBrandColor(), colors, staff, RoleOption.all());
        }
    }

    /** Partial update: only non-null fields change. Coordinates must be sent together. */
    public record UpdateShopRequest(
            @Schema(example = "Free Shop Coffee") @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 150) String shopName,
            @Schema(example = "742 Pine Hill Court, Suite 100") @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 250) String address,
            @Schema(example = "Mon – Sun • 6:30 AM – 7:00 PM") @Size(max = 100) String hoursLabel,
            @Schema(example = "13.640854603672658") @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @Schema(example = "103.1190348130734") @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Schema(description = "Hex of one of the brandColors presets", example = "#3a5a40")
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex colour like #3a5a40") String brandColor) {
    }

    public record ShopStatusRequest(@Schema(description = "true = open, false = closed") @NotNull Boolean open) {
    }

    public record CreateStaffRequest(
            @Schema(example = "Sophia Vance") @NotBlank @Size(max = 150) String name,
            @Schema(example = "staff") @NotNull StaffRole role) {
    }

    /** Partial update: only non-null fields change. */
    public record UpdateStaffRequest(
            @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 150) String name,
            @Schema(example = "manager") StaffRole role) {
    }
}
