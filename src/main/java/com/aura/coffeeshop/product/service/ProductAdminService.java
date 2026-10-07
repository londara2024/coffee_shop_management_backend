package com.aura.coffeeshop.product.service;

import com.aura.coffeeshop.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.aura.coffeeshop.catalog.entity.Product;
import com.aura.coffeeshop.catalog.entity.ProductSize;
import com.aura.coffeeshop.catalog.entity.ProductTag;
import com.aura.coffeeshop.catalog.entity.ProductTopping;
import com.aura.coffeeshop.catalog.repository.CategoryRepository;
import com.aura.coffeeshop.catalog.repository.ProductRepository;
import com.aura.coffeeshop.catalog.service.CatalogService;
import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.common.storage.ImageCleanup;
import com.aura.coffeeshop.common.storage.ImageStorage;
import com.aura.coffeeshop.common.storage.ImageStorage.StoredImage;
import com.aura.coffeeshop.product.dto.ProductUpsertRequest;
import com.aura.coffeeshop.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogService catalogService;
    private final StoreService storeService;
    private final ImageStorage imageStorage;

    private static final String IMAGE_FOLDER = "products";

    public ProductDetailResponse create(ProductUpsertRequest request) {
        Product product = new Product();
        product.setCustom(true);
        product.setSlug(request.slug() != null ? requireFreeSlug(request.slug()) : uniqueSlug(request.name()));
        if (request.createdByStaffId() != null) {
            product.setCreatedByStaff(storeService.getStaff(request.createdByStaffId()));
        }
        apply(product, request, true);
        return catalogService.toDetail(productRepository.save(product));
    }

    public ProductDetailResponse update(String id, ProductUpsertRequest request) {
        Product product = catalogService.getProduct(id);
        if (request.slug() != null && !request.slug().equals(product.getSlug())) {
            product.setSlug(requireFreeSlug(request.slug()));
        }
        apply(product, request, false);
        return catalogService.toDetail(productRepository.saveAndFlush(product));
    }

    /** Publish to / withdraw from the customer menu. */
    public ProductDetailResponse setLive(String id, boolean live) {
        Product product = catalogService.getProduct(id);
        product.setLive(live);
        return catalogService.toDetail(productRepository.saveAndFlush(product));
    }

    /**
     * Hard delete. Products referenced by past orders or open carts cannot be deleted (409) —
     * withdraw them with {@link #setLive} instead so order history stays intact.
     */
    public void delete(String id) {
        Product product = catalogService.getProduct(id);
        String imageUrl = product.getImageUrl();
        productRepository.delete(product);
        productRepository.flush();
        ImageCleanup.afterCommit(imageStorage, imageUrl);
    }

    /** Upload an image before the product exists (e.g. from the "Add item" form); returns its URL for {@code imageUrl}. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StoredImage uploadImage(MultipartFile file) {
        return imageStorage.store(file, IMAGE_FOLDER);
    }

    /** Replace a product's photo. The previous uploaded file is removed once the change is committed. */
    public ProductDetailResponse replaceImage(String id, MultipartFile file) {
        Product product = catalogService.getProduct(id);
        String previousUrl = product.getImageUrl();
        StoredImage stored = imageStorage.store(file, IMAGE_FOLDER);
        ImageCleanup.onReplace(imageStorage, previousUrl, stored.url());
        product.setImageUrl(stored.url());
        return catalogService.toDetail(productRepository.saveAndFlush(product));
    }

    private void apply(Product product, ProductUpsertRequest r, boolean creating) {
        product.setCategory(categoryRepository.findById(r.categoryId())
                .orElseThrow(() -> NotFoundException.of("Category", r.categoryId())));
        product.setName(r.name().trim());
        product.setNameKm(r.nameKm());
        product.setKicker(r.kicker());
        product.setKickerKm(r.kickerKm());
        product.setDescription(r.description());
        product.setDescriptionKm(r.descriptionKm());
        product.setBasePrice(r.basePrice());
        product.setImageUrl(r.imageUrl());
        product.setImageTag(r.imageTag());
        product.setBadge(r.badge());
        product.setExtra(r.extra());
        product.setCustomizable(r.customizable());
        if (r.live() != null) {
            product.setLive(r.live());
        } else if (creating) {
            product.setLive(true);
        }

        if (r.sizes() != null) {
            Set<String> labels = new HashSet<>();
            r.sizes().forEach(s -> {
                if (!labels.add(s.sizeLabel())) {
                    throw BusinessRuleException.badRequest("Duplicate size label '" + s.sizeLabel() + "'");
                }
            });
            List<ProductUpsertRequest.SizeRequest> sizes = r.sizes();
            product.replaceSizes(IntStream.range(0, sizes.size())
                    .mapToObj(i -> new ProductSize(sizes.get(i).sizeLabel(), (short) i, sizes.get(i).price()))
                    .toList());
        }
        if (r.toppings() != null) {
            product.replaceToppings(r.toppings().stream()
                    .map(t -> new ProductTopping(t.label(), t.labelKm(), t.priceDelta(), t.locked()))
                    .toList());
        }
        if (r.tags() != null) {
            product.replaceTags(r.tags().stream().map(t -> new ProductTag(t.label(), t.labelKm(), t.tone())).toList());
        }
    }

    private String requireFreeSlug(String slug) {
        if (productRepository.existsBySlug(slug)) {
            throw new BusinessRuleException("Slug '" + slug + "' is already used by another product");
        }
        return slug;
    }

    /**
     * ASCII slug from the name. Khmer-only names transliterate to nothing, so they fall back to
     * a random suffix instead of colliding (the bug noted in DATABASE_DESIGN.md §4.1).
     */
    String uniqueSlug(String name) {
        String ascii = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        String base = ascii.isEmpty() ? "item-" + UUID.randomUUID().toString().substring(0, 8) : ascii;
        if (base.length() > 140) {
            base = base.substring(0, 140);
        }
        String candidate = base;
        for (int i = 2; productRepository.existsBySlug(candidate); i++) {
            candidate = base + "-" + i;
        }
        return candidate;
    }
}
