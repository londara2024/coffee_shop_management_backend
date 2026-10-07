package com.aura.coffeeshop.product.dto;

import com.aura.coffeeshop.catalog.Tone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create / replace a menu item (admin "Add New Menu Item" form).
 * On update, a {@code null} list leaves that collection unchanged; an empty list clears it.
 */
public record ProductUpsertRequest(
        @Schema(description = "Optional URL slug. Generated from the name when omitted.", example = "lavender-honey-latte")
        @Size(max = 150) @Pattern(regexp = "^[\\p{L}\\p{M}0-9]+(?:-[\\p{L}\\p{M}0-9]+)*$", message = "must be lowercase words joined by '-'")
        String slug,
        @Schema(example = "coffee") @NotBlank @Size(max = 20) String categoryId,
        @Schema(example = "Lavender Honey Latte") @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String nameKm,
        @Size(max = 150) String kicker,
        @Size(max = 150) String kickerKm,
        String description,
        String descriptionKm,
        @Schema(example = "6.50") @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal basePrice,
        @Schema(example = "/images/latte-cup.jpg") @NotBlank @Size(max = 300) String imageUrl,
        @Size(max = 80) String imageTag,
        @Schema(example = "ក្ដៅ") @Size(max = 40) String badge,
        @Size(max = 40) String extra,
        @Schema(description = "Shows the size / temperature / milk / topping configurator") boolean customizable,
        @Schema(description = "Visible on the customer menu", defaultValue = "true") Boolean live,
        String createdByStaffId,
        @Valid List<SizeRequest> sizes,
        @Valid List<ToppingRequest> toppings,
        @Valid List<TagRequest> tags) {

    public record SizeRequest(
            @Schema(example = "ធម្មតា") @NotBlank @Size(max = 20) String sizeLabel,
            @Schema(example = "6.50") @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price) {
    }

    public record ToppingRequest(
            @NotBlank @Size(max = 80) String label,
            @Size(max = 80) String labelKm,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal priceDelta,
            boolean locked) {
    }

    public record TagRequest(
            @NotBlank @Size(max = 60) String label,
            @Size(max = 60) String labelKm,
            @NotNull Tone tone) {
    }
}
