package com.aura.coffeeshop.catalog.dto;

import com.aura.coffeeshop.catalog.Temperature;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Configurator choices for a drink. Ignored (except {@code notes}) for non-customizable items. */
public record ItemCustomization(
        @Schema(description = "product_sizes.id; defaults to the regular size") String sizeId,
        @Schema(description = "Defaults to hot", example = "hot") Temperature temperature,
        @Schema(description = "milk_options.code; defaults to oat", example = "oat") @Size(max = 20) String milkCode,
        @Schema(description = "Topping ids; locked toppings are always included") List<String> toppingIds,
        @Schema(example = "50% Half") @Size(max = 40) String sweetness,
        @Schema(example = "Extra hot, 155°F") @Size(max = 200) String notes) {

    public static ItemCustomization none() {
        return new ItemCustomization(null, null, null, null, null, null);
    }
}
