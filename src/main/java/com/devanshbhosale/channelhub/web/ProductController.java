package com.devanshbhosale.channelhub.web;

import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.exception.ApiException;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/api/products")
    public Page<Product> list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String brand,
                              @RequestParam(required = false) String category) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        return productRepository.search(blankToNull(q), blankToNull(brand), blankToNull(category),
            PageRequest.of(Math.max(page, 0), pageSize));
    }

    @GetMapping("/api/products/{id}")
    public Product get(@PathVariable Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!product.isActive()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return product;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
