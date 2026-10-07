package com.aura.coffeeshop.catalog.service;

import com.aura.coffeeshop.catalog.Temperature;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.CatalogOptionsResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.CategoryResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.MilkOptionResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.OptionResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.PairingResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.ProductSummaryResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.ToppingResponse;
import com.aura.coffeeshop.catalog.entity.Product;
import com.aura.coffeeshop.catalog.repository.CategoryRepository;
import com.aura.coffeeshop.catalog.repository.MilkOptionRepository;
import com.aura.coffeeshop.catalog.repository.PairingRepository;
import com.aura.coffeeshop.catalog.repository.ProductRepository;
import com.aura.coffeeshop.catalog.repository.ProductToppingRepository;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private static final List<OptionResponse> SWEETNESS_LEVELS = List.of(
            new OptionResponse("None", "None", "គ្មាន"),
            new OptionResponse("50% Half", "50% Half", "៥០% កន្លះ"),
            new OptionResponse("100% Std", "100% Std", "១០០% ស្តង់ដារ"));

    private static final Sort MENU_ORDER = Sort.by(Sort.Order.asc("category.sortOrder"), Sort.Order.asc("createdAt"),
            Sort.Order.asc("name"));

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MilkOptionRepository milkOptionRepository;
    private final ProductToppingRepository toppingRepository;
    private final PairingRepository pairingRepository;
    private final PricingService pricingService;

    public List<CategoryResponse> listCategories() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : productRepository.countLiveByCategory()) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(c -> CategoryResponse.from(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /** {@code live} / {@code custom} null = no filter. */
    public PageResponse<ProductSummaryResponse> searchProducts(String categoryId, String query, Boolean live,
                                                               Boolean custom, int page, int size) {
        Specification<Product> spec = Specification.where(ProductRepository.inCategory(categoryId))
                .and(ProductRepository.liveEquals(live))
                .and(ProductRepository.customOnly(custom))
                .and(ProductRepository.matching(query));
        return PageResponse.of(productRepository.findAll(spec, PageRequest.of(page, size, MENU_ORDER)),
                ProductSummaryResponse::from);
    }

    /** Customer-facing lookup: withdrawn items are hidden. */
    public ProductDetailResponse getLiveProduct(String slug) {
        Product product = productRepository.findBySlug(slug)
                .filter(Product::isLive)
                .orElseThrow(() -> NotFoundException.of("Product", slug));
        return toDetail(product);
    }

    public ProductDetailResponse getProductDetail(String id) {
        return toDetail(getProduct(id));
    }

    public Product getProduct(String id) {
        return productRepository.findById(id).orElseThrow(() -> NotFoundException.of("Product", id));
    }

    public ProductDetailResponse toDetail(Product product) {
        return ProductDetailResponse.from(product, pricingService.effectiveToppings(product));
    }

    public CatalogOptionsResponse options() {
        return new CatalogOptionsResponse(
                milkOptionRepository.findAllByOrderBySortOrderAsc().stream().map(MilkOptionResponse::from).toList(),
                toppingRepository.findByProductIsNullOrderByLockedDescPriceDeltaAsc().stream()
                        .map(ToppingResponse::from).toList(),
                Arrays.stream(Temperature.values())
                        .map(t -> new OptionResponse(t.getCode(), t.getLabel(), t.getLabelKm())).toList(),
                SWEETNESS_LEVELS);
    }

    public List<PairingResponse> listPairings() {
        return pairingRepository.findAllByOrderByPriceAsc().stream().map(PairingResponse::from).toList();
    }
}
