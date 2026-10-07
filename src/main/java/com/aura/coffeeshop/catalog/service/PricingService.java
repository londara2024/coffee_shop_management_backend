package com.aura.coffeeshop.catalog.service;

import com.aura.coffeeshop.catalog.PricedItem;
import com.aura.coffeeshop.catalog.Temperature;
import com.aura.coffeeshop.catalog.dto.ItemCustomization;
import com.aura.coffeeshop.catalog.entity.MilkOption;
import com.aura.coffeeshop.catalog.entity.Product;
import com.aura.coffeeshop.catalog.entity.ProductSize;
import com.aura.coffeeshop.catalog.entity.ProductTopping;
import com.aura.coffeeshop.catalog.repository.MilkOptionRepository;
import com.aura.coffeeshop.catalog.repository.ProductToppingRepository;
import com.aura.coffeeshop.common.Money;
import com.aura.coffeeshop.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side price calculation for a configured menu item. Mirrors the rules in the frontend's
 * product-configurator.tsx, but the server is authoritative: clients never send prices.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PricingService {

    static final String DEFAULT_MILK = "oat";
    static final short REGULAR_SIZE_ORDER = 1;
    private static final String SEPARATOR = " · ";

    private final MilkOptionRepository milkOptionRepository;
    private final ProductToppingRepository toppingRepository;

    public PricedItem price(Product product, ItemCustomization customization) {
        ItemCustomization c = customization == null ? ItemCustomization.none() : customization;
        String notes = blankToNull(c.notes());

        if (!product.isCustomizable()) {
            String details = notes != null ? notes : "House standard preparation";
            String variantKey = notes == null ? "default" : "n-" + digest(notes);
            return new PricedItem(product, Money.scale(product.getBasePrice()), variantKey, details, tag(product));
        }

        ProductSize size = resolveSize(product, c.sizeId());
        Temperature temperature = c.temperature() != null ? c.temperature() : Temperature.HOT;
        String milkCode = c.milkCode() != null ? c.milkCode() : DEFAULT_MILK;
        MilkOption milk = milkOptionRepository.findById(milkCode)
                .orElseThrow(() -> BusinessRuleException.badRequest("Unknown milk option '" + milkCode + "'"));
        List<ProductTopping> selected = resolveToppings(product, c.toppingIds());
        String sweetness = blankToNull(c.sweetness());

        BigDecimal unit = (size != null ? size.getPrice() : product.getBasePrice()).add(milk.getPriceDelta());
        for (ProductTopping topping : selected) {
            unit = unit.add(topping.getPriceDelta());
        }

        List<String> details = new ArrayList<>();
        if (size != null) {
            details.add(size.getSizeLabel());
        }
        details.add(temperature.getLabel());
        details.add(milk.getLabel());
        // Locked toppings are implied by the drink itself, so the frontend leaves them out of the summary.
        selected.stream().filter(t -> !t.isLocked()).forEach(t -> details.add(t.getLabel()));
        if (sweetness != null) {
            details.add("Sweetness " + sweetness);
        }
        if (notes != null) {
            details.add(notes);
        }

        String config = String.join("|",
                size != null ? size.getId().toString() : "-", temperature.getCode(), milk.getCode(),
                selected.stream().map(t -> t.getId().toString()).sorted().reduce("", String::concat),
                sweetness == null ? "" : sweetness, notes == null ? "" : notes);
        String variantKey = (size != null ? size.getSortOrder() : "x") + "-" + temperature.getCode() + "-"
                + milk.getCode() + "-" + digest(config);

        return new PricedItem(product, Money.scale(unit), variantKey, truncate(String.join(SEPARATOR, details), 300),
                tag(product));
    }

    /** Product-specific toppings, or the global defaults when the product defines none. */
    public List<ProductTopping> effectiveToppings(Product product) {
        if (!product.isCustomizable()) {
            return List.of();
        }
        if (!product.getToppings().isEmpty()) {
            return product.getToppings().stream()
                    .sorted(Comparator.comparing(ProductTopping::isLocked).reversed()
                            .thenComparing(ProductTopping::getPriceDelta))
                    .toList();
        }
        return toppingRepository.findByProductIsNullOrderByLockedDescPriceDeltaAsc();
    }

    private ProductSize resolveSize(Product product, String sizeId) {
        List<ProductSize> sizes = product.getSizes();
        if (sizeId != null) {
            return sizes.stream().filter(s -> s.getId().equals(sizeId)).findFirst()
                    .orElseThrow(() -> BusinessRuleException.badRequest(
                            "Size '" + sizeId + "' does not belong to product '" + product.getSlug() + "'"));
        }
        return sizes.stream().filter(s -> s.getSortOrder() == REGULAR_SIZE_ORDER).findFirst()
                .orElse(sizes.isEmpty() ? null : sizes.get(0));
    }

    private List<ProductTopping> resolveToppings(Product product, List<String> requestedIds) {
        List<ProductTopping> available = effectiveToppings(product);
        Set<String> requested = requestedIds == null ? Set.of() : new HashSet<>(requestedIds);
        Set<String> availableIds = new HashSet<>();
        available.forEach(t -> availableIds.add(t.getId()));
        for (String id : requested) {
            if (!availableIds.contains(id)) {
                throw BusinessRuleException.badRequest(
                        "Topping '" + id + "' is not available for product '" + product.getSlug() + "'");
            }
        }
        return available.stream().filter(t -> t.isLocked() || requested.contains(t.getId())).toList();
    }

    private static String tag(Product product) {
        String kicker = blankToNull(product.getKicker());
        return kicker == null ? null : truncate(kicker.split("\\s+")[0], 60);
    }

    private static String digest(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().substring(0, 13);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
