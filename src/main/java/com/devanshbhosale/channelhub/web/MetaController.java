package com.devanshbhosale.channelhub.web;

import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.dto.MetaRes;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
public class MetaController {

    private static final List<String> WAREHOUSES = List.of("BOM-01", "MAA-01", "DEL-01");

    private final ProductRepository productRepository;

    public MetaController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/api/meta")
    public MetaRes meta() {
        List<Product> products = productRepository.findByActiveTrue();
        List<String> brands = products.stream()
            .map(Product::getBrand)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
        List<String> categories = products.stream()
            .map(Product::getCategory)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
        return new MetaRes(brands, categories, WAREHOUSES);
    }
}
