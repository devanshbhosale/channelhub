package com.devanshbhosale.channelhub.web;

import com.devanshbhosale.channelhub.domain.Inventory;
import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.dto.AdminOrderRes;
import com.devanshbhosale.channelhub.dto.DashboardRes;
import com.devanshbhosale.channelhub.dto.InventoryAdjustReq;
import com.devanshbhosale.channelhub.dto.OrderRes;
import com.devanshbhosale.channelhub.dto.OrderStatusReq;
import com.devanshbhosale.channelhub.dto.ProductReq;
import com.devanshbhosale.channelhub.dto.ResellerAdminRes;
import com.devanshbhosale.channelhub.exception.ApiException;
import com.devanshbhosale.channelhub.repository.InventoryRepository;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.repository.SalesOrderRepository;
import com.devanshbhosale.channelhub.service.DashboardService;
import com.devanshbhosale.channelhub.service.OrderService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class AdminController {

    private final OrderService orderService;
    private final DashboardService dashboardService;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ResellerRepository resellerRepository;
    private final SalesOrderRepository salesOrderRepository;

    public AdminController(OrderService orderService,
                           DashboardService dashboardService,
                           ProductRepository productRepository,
                           InventoryRepository inventoryRepository,
                           ResellerRepository resellerRepository,
                           SalesOrderRepository salesOrderRepository) {
        this.orderService = orderService;
        this.dashboardService = dashboardService;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.resellerRepository = resellerRepository;
        this.salesOrderRepository = salesOrderRepository;
    }

    @GetMapping("/api/admin/orders")
    public List<AdminOrderRes> orders(@RequestParam(required = false) String status) {
        OrderStatus parsed = status == null || status.isBlank() ? null : parseStatus(status);
        return orderService.adminOrders(parsed);
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    public OrderRes updateStatus(@PathVariable Long id, @RequestBody OrderStatusReq request) {
        OrderStatus target = parseStatus(request.status());
        return orderService.transition(id, target, request.reason());
    }

    @PostMapping("/api/admin/products")
    public Product createProduct(@RequestBody ProductReq request) {
        Product product = new Product();
        applyProduct(product, request);
        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU already exists");
        }
    }

    @PutMapping("/api/admin/products/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody ProductReq request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        applyProduct(product, request);
        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU already exists");
        }
    }

    @PostMapping("/api/admin/inventory/adjust")
    public DashboardRes.InventoryRow adjust(@RequestBody InventoryAdjustReq request) {
        if (request.productId() == null || request.warehouse() == null || request.warehouse().isBlank()
            || request.delta() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "productId, warehouse and delta are required");
        }
        Inventory inventory = inventoryRepository
            .findByProductIdAndWarehouse(request.productId(), request.warehouse())
            .orElseGet(() -> {
                Inventory created = new Inventory();
                created.setProductId(request.productId());
                created.setWarehouse(request.warehouse());
                created.setQuantity(0);
                created.setReorderLevel(0);
                return created;
            });
        inventory.setQuantity(Math.max(0, inventory.getQuantity() + request.delta()));
        inventoryRepository.save(inventory);
        Product product = productRepository.findById(request.productId()).orElse(null);
        return toRow(inventory, product);
    }

    @GetMapping("/api/admin/inventory")
    public List<DashboardRes.InventoryRow> inventory(
        @RequestParam(name = "lowStock", required = false) Boolean lowStock) {
        Map<Long, Product> products = productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<DashboardRes.InventoryRow> rows = inventoryRepository.findAll().stream()
            .map(inventory -> toRow(inventory, products.get(inventory.getProductId())))
            .toList();
        if (Boolean.TRUE.equals(lowStock)) {
            rows = rows.stream()
                .filter(row -> row.quantity() <= row.reorderLevel())
                .toList();
        }
        return rows;
    }

    @GetMapping("/api/admin/resellers")
    public List<ResellerAdminRes> resellers() {
        return resellerRepository.findAll().stream()
            .map(reseller -> {
                long orderCount = salesOrderRepository
                    .findByResellerIdOrderByCreatedAtDesc(reseller.getId()).size();
                BigDecimal outstanding = orderService.resellerOutstanding(reseller.getUserId());
                return new ResellerAdminRes(reseller.getCompanyName(), reseller.getGstin(), reseller.getPhone(),
                    reseller.getCreditLimit(), outstanding,
                    reseller.getCreditLimit().subtract(outstanding), orderCount);
            })
            .toList();
    }

    @GetMapping("/api/admin/dashboard")
    public DashboardRes dashboard() {
        return dashboardService.load();
    }

    private OrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status: " + raw);
        }
    }

    private void applyProduct(Product product, ProductReq request) {
        if (request.sku() == null || request.sku().isBlank() || request.name() == null || request.name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sku and name are required");
        }
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setBrand(request.brand());
        product.setCategory(request.category());
        product.setDescription(request.description());
        product.setUnitPrice(request.unitPrice() == null ? BigDecimal.ZERO : request.unitPrice());
        product.setActive(request.active() == null || request.active());
    }

    private DashboardRes.InventoryRow toRow(Inventory inventory, Product product) {
        return new DashboardRes.InventoryRow(inventory.getProductId(),
            product == null ? null : product.getSku(),
            product == null ? null : product.getName(),
            inventory.getWarehouse(), inventory.getQuantity(), inventory.getReorderLevel());
    }
}
