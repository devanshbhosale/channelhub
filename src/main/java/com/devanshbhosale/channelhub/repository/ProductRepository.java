package com.devanshbhosale.channelhub.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devanshbhosale.channelhub.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Active-catalog search. A null or blank q / brand / category means
     * "no filter"; q is matched against SKU, name and brand case-insensitively.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (COALESCE(:q, '') = ''
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (COALESCE(:brand, '') = '' OR p.brand = :brand)
              AND (COALESCE(:category, '') = '' OR p.category = :category)
            ORDER BY p.name, p.id
            """)
    Page<Product> search(@Param("q") String q,
                         @Param("brand") String brand,
                         @Param("category") String category,
                         Pageable pageable);

    List<Product> findByActiveTrue();
}
