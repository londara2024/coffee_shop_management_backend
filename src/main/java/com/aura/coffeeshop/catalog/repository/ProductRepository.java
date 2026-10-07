package com.aura.coffeeshop.catalog.repository;

import com.aura.coffeeshop.catalog.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("select p.category.id, count(p) from Product p where p.live = true group by p.category.id")
    List<Object[]> countLiveByCategory();

    static Specification<Product> inCategory(String categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Product> liveEquals(Boolean live) {
        return (root, query, cb) -> live == null ? null : cb.equal(root.get("live"), live);
    }

    static Specification<Product> customOnly(Boolean custom) {
        return (root, query, cb) -> custom == null ? null : cb.equal(root.get("custom"), custom);
    }

    /** Case-insensitive match on English name, Khmer name or kicker. */
    static Specification<Product> matching(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("nameKm"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("kicker"), "")), pattern));
        };
    }
}
