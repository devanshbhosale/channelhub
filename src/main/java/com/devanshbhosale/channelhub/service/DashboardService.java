package com.devanshbhosale.channelhub.service;

import com.devanshbhosale.channelhub.domain.Inventory;
import com.devanshbhosale.channelhub.domain.Invoice;
import com.devanshbhosale.channelhub.domain.OrderItem;
import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.SalesOrder;
import com.devanshbhosale.channelhub.dto.DashboardRes;
import com.devanshbhosale.channelhub.repository.InventoryRepository;
import com.devanshbhosale.channelhub.repository.InvoiceRepository;
import com.devanshbhosale.channelhub.repository.OrderItemRepository;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final List<OrderStatus> COUNTED_STATUSES =
        List.of(OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final ResellerRepository resellerRepository;

    public DashboardService(SalesOrderRepository salesOrderRepository,
                            OrderItemRepository orderItemRepository,
                            ProductRepository productRepository,
                            InventoryRepository inventoryRepository,
                            InvoiceRepository invoiceRepository,
                            ResellerRepository resellerRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.resellerRepository = resellerRepository;
    }

    @Transactional(readOnly = true)
    public DashboardRes load() {
        LocalDate today = LocalDate.now();

        List<SalesOrder> pending = salesOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);
        List<SalesOrder> approved = salesOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.APPROVED);
        long shippedToday = salesOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.SHIPPED).stream()
            .filter(order -> order.getUpdatedAt() != null && order.getUpdatedAt().toLocalDate().equals(today))
            .count();

        List<DashboardRes.InventoryRow> lowStockRows = lowStockRows(inventoryRepository.findAll());

        List<Invoice> invoices = invoiceRepository.findAll();
        YearMonth currentMonth = YearMonth.now();
        BigDecimal revenueThisMonth = invoices.stream()
            .filter(invoice -> invoice.getIssuedAt() != null
                && YearMonth.from(invoice.getIssuedAt()).equals(currentMonth))
            .map(Invoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SalesOrder> countedOrders = salesOrderRepository.findByStatusIn(COUNTED_STATUSES);

        // 14-day series, zero-filled.
        Map<LocalDate, Long> ordersByDay = countedOrders.stream()
            .filter(order -> order.getCreatedAt() != null)
            .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, BigDecimal> revenueByDay = invoices.stream()
            .filter(invoice -> invoice.getIssuedAt() != null)
            .collect(Collectors.groupingBy(invoice -> invoice.getIssuedAt().toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, Invoice::getAmount, BigDecimal::add)));
        List<DashboardRes.SeriesPoint> series = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            series.add(new DashboardRes.SeriesPoint(day.toString(),
                ordersByDay.getOrDefault(day, 0L),
                revenueByDay.getOrDefault(day, BigDecimal.ZERO)));
        }

        // Top SKUs by quantity sold across counted (non-cancelled / non-rejected) orders.
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (SalesOrder order : countedOrders) {
            for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                qtyByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }
        Map<Long, Product> countedProducts = productRepository.findAllById(qtyByProduct.keySet()).stream()
            .collect(Collectors.toMap(Product::getId, product -> product));
        List<DashboardRes.TopSku> topSkus = qtyByProduct.entrySet().stream()
            .filter(entry -> countedProducts.containsKey(entry.getKey()))
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Product product = countedProducts.get(entry.getKey());
                return new DashboardRes.TopSku(product.getSku(), product.getName(), entry.getValue());
            })
            .toList();

        Map<Long, Reseller> resellersById = resellerRepository.findAllById(
                pending.stream().map(SalesOrder::getResellerId).distinct().toList()).stream()
            .collect(Collectors.toMap(Reseller::getId, reseller -> reseller));
        List<DashboardRes.QueueRow> queue = pending.stream()
            .map(order -> {
                Reseller reseller = resellersById.get(order.getResellerId());
                return new DashboardRes.QueueRow(order.getOrderNumber(),
                    reseller == null ? null : reseller.getCompanyName(),
                    order.getTotalAmount(), order.getStatus().name(), order.getCreatedAt());
            })
            .toList();

        return new DashboardRes(
            new DashboardRes.StatCards(pending.size(), approved.size(), shippedToday, lowStockRows.size(), revenueThisMonth),
            series, topSkus, lowStockRows, queue);
    }

    private List<DashboardRes.InventoryRow> lowStockRows(List<Inventory> allInventory) {
        Map<Long, Product> products = productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getId, product -> product));
        return allInventory.stream()
            .filter(inventory -> inventory.getQuantity() <= inventory.getReorderLevel())
            .map(inventory -> {
                Product product = products.get(inventory.getProductId());
                return new DashboardRes.InventoryRow(inventory.getProductId(),
                    product == null ? null : product.getSku(),
                    product == null ? null : product.getName(),
                    inventory.getWarehouse(), inventory.getQuantity(), inventory.getReorderLevel());
            })
            .toList();
    }
}
