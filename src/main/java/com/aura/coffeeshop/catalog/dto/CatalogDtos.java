package com.aura.coffeeshop.catalog.dto;

import com.aura.coffeeshop.catalog.Tone;
import com.aura.coffeeshop.catalog.entity.Category;
import com.aura.coffeeshop.catalog.entity.MilkOption;
import com.aura.coffeeshop.catalog.entity.Pairing;
import com.aura.coffeeshop.catalog.entity.Product;
import com.aura.coffeeshop.catalog.entity.ProductSize;
import com.aura.coffeeshop.catalog.entity.ProductTag;
import com.aura.coffeeshop.catalog.entity.ProductTopping;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Read models for the catalog API. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryResponse(String id, String label, String labelKm, String eyebrow, String title,
                                   String titleKm, String note, String noteKm, int sortOrder, long productCount) {
        public static CategoryResponse from(Category c, long productCount) {
            return new CategoryResponse(c.getId(), c.getLabel(), c.getLabelKm(), c.getEyebrow(), c.getTitle(),
                    c.getTitleKm(), c.getNote(), c.getNoteKm(), c.getSortOrder(), productCount);
        }
    }

    public record TagResponse(String label, String labelKm, Tone tone) {
        public static TagResponse from(ProductTag t) {
            return new TagResponse(t.getLabel(), t.getLabelKm(), t.getTone());
        }
    }

    public record SizeResponse(String id, String sizeLabel, int sortOrder, BigDecimal price) {
        public static SizeResponse from(ProductSize s) {
            return new SizeResponse(s.getId(), s.getSizeLabel(), s.getSortOrder(), s.getPrice());
        }
    }

    public record ToppingResponse(String id, String label, String labelKm, BigDecimal priceDelta, boolean locked) {
        public static ToppingResponse from(ProductTopping t) {
            return new ToppingResponse(t.getId(), t.getLabel(), t.getLabelKm(), t.getPriceDelta(), t.isLocked());
        }
    }

    public record MilkOptionResponse(String code, String label, String labelKm, BigDecimal priceDelta) {
        public static MilkOptionResponse from(MilkOption m) {
            return new MilkOptionResponse(m.getCode(), m.getLabel(), m.getLabelKm(), m.getPriceDelta());
        }
    }

    public record OptionResponse(String code, String label, String labelKm) {
    }

    /** Everything the drink configurator needs that is not product-specific. */
    public record CatalogOptionsResponse(List<MilkOptionResponse> milks,
                                         List<ToppingResponse> defaultToppings,
                                         List<OptionResponse> temperatures,
                                         List<OptionResponse> sweetnessLevels) {
    }

    public record ProductSummaryResponse(String id, String slug, String categoryId, String name, String nameKm,
                                         String kicker, String kickerKm, BigDecimal price, String imageUrl,
                                         String imageTag, String badge, String extra, boolean customizable,
                                         boolean custom, boolean live, List<TagResponse> tags) {
        public static ProductSummaryResponse from(Product p) {
            return new ProductSummaryResponse(p.getId(), p.getSlug(), p.getCategory().getId(), p.getName(),
                    p.getNameKm(), p.getKicker(), p.getKickerKm(), p.getBasePrice(), p.getImageUrl(),
                    p.getImageTag(), p.getBadge(), p.getExtra(), p.isCustomizable(), p.isCustom(), p.isLive(),
                    p.getTags().stream().map(TagResponse::from).toList());
        }
    }

    /**
     * Full product view. {@code toppings} are the product's own toppings, or the global defaults
     * when it has none (and it is customizable).
     */
    public record ProductDetailResponse(String id, String slug, String categoryId, String name, String nameKm,
                                        String kicker, String kickerKm, String description, String descriptionKm,
                                        BigDecimal price, String imageUrl, String imageTag, String badge,
                                        String extra, boolean customizable, boolean custom, boolean live,
                                        List<TagResponse> tags, List<SizeResponse> sizes,
                                        List<ToppingResponse> toppings, Instant createdAt, Instant updatedAt) {
        public static ProductDetailResponse from(Product p, List<ProductTopping> effectiveToppings) {
            return new ProductDetailResponse(p.getId(), p.getSlug(), p.getCategory().getId(), p.getName(),
                    p.getNameKm(), p.getKicker(), p.getKickerKm(), p.getDescription(), p.getDescriptionKm(),
                    p.getBasePrice(), p.getImageUrl(), p.getImageTag(), p.getBadge(), p.getExtra(),
                    p.isCustomizable(), p.isCustom(), p.isLive(),
                    p.getTags().stream().map(TagResponse::from).toList(),
                    p.getSizes().stream().map(SizeResponse::from).toList(),
                    effectiveToppings.stream().map(ToppingResponse::from).toList(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    public record PairingResponse(String id, String slug, String name, String nameKm, String description,
                                  String descriptionKm, BigDecimal price, String imageUrl, String ctaLabel,
                                  String ctaLabelKm) {
        public static PairingResponse from(Pairing p) {
            return new PairingResponse(p.getId(), p.getSlug(), p.getName(), p.getNameKm(), p.getDescription(),
                    p.getDescriptionKm(), p.getPrice(), p.getImageUrl(), p.getCtaLabel(), p.getCtaLabelKm());
        }
    }
}
