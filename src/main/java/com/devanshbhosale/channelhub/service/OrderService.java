package com.devanshbhosale.channelhub.service;

import com.devanshbhosale.channelhub.domain.Inventory;
import com.devanshbhosale.channelhub.domain.Invoice;
import com.devanshbhosale.channelhub.domain.OrderItem;
import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.SalesOrder;
import com.devanshbhosale.channelhub.dto.AdminOrderRes;
import com.devanshbhosale.channelhub.dto.CreditRes;
import com.devanshbhosale.channelhub.dto.OrderReq;
import com.devanshbhosale.channelhub.dto.OrderRes;
import com.devanshbhosale.channelhub.dto.StockShortfall;
import com.devanshbhosale.channelhub.exception.ApiException;
import com.devanshbhosale.channelhub.exception.CreditExceededException;
import com.devanshbhosale.channelhub.exception.IllegalTransitionException;
import com.devanshbhosale.channelhub.exception.InsufficientStockException;
import com.devanshbhosale.channelhub.repository.InventoryRepository;
import com.devanshbhosale.channelhub.repository.InvoiceRepository;
import com.devanshbhosale.channelhub.repository.OrderItemRepository;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.repository.SalesOrderRepository;
import com.devanshbhosale.channelhub.util.SequenceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    /** Statuses that count against the reseller's credit line. */
    public static final List<OrderStatus> OPEN_STATUSES =
        List.of(OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.SHIPPED);
    private static final BigDecimal CGST_RATE = new BigDecimal("0.09");

    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final ResellerRepository resellerRepository;
    private final SequenceService sequenceService;

    public OrderService(SalesOrderRepository salesOrderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        InvoiceRepository invoiceRepository,
                        ResellerRepository resellerRepository,
                        SequenceService sequenceService) {
        this.salesOrderRepository = salesOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.resellerRepository = resellerRepository;
        this.sequenceService = sequenceService;
    }

    @Transactional
    public OrderRes place(Long resellerUserId, List<OrderReq.OrderItemReq> items, String notes) {
        if (items == null || items.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one order item is required");
        }
        Reseller reseller = requireReseller(resellerUserId);

        // Merge repeated product lines so stock and totals are computed per product.
        Map<Long, Integer> requestedByProduct = new LinkedHashMap<>();
        for (OrderReq.OrderItemReq item : items) {
            if (item.productId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "productId is required");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "quantity must be at least 1");
            }
            requestedByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        // Load active products, snapshot prices, collect ALL stock shortfalls before failing.
        Map<Long, Product> products = new LinkedHashMap<>();
        Map<Long, List<Inventory>> stockByProduct = new HashMap<>();
        List<StockShortfall> shortfalls = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> request : requestedByProduct.entrySet()) {
            Product product = productRepository.findById(request.getKey())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Product not found: " + request.getKey()));
            if (!product.isActive()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not available: " + product.getSku());
            }
            products.put(product.getId(), product);
            total = total.add(product.getUnitPrice().multiply(BigDecimal.valueOf(request.getValue())));

            List<Inventory> rows = inventoryRepository.findByProductId(product.getId());
            rows.sort(Comparator.comparingInt(Inventory::getQuantity).reversed()); // fullest-first
            stockByProduct.put(product.getId(), rows);
            int available = rows.stream().mapToInt(Inventory::getQuantity).sum();
            if (available < request.getValue()) {
                shortfalls.add(new StockShortfall(product.getSku(), request.getValue(), available));
            }
        }
        if (!shortfalls.isEmpty()) {
            throw new InsufficientStockException(shortfalls);
        }

        // Credit check: outstanding + new total must not exceed the limit (boundary == passes).
        BigDecimal outstanding = outstandingForReseller(reseller.getId());
        if (outstanding.add(total).compareTo(reseller.getCreditLimit()) > 0) {
            throw new CreditExceededException(reseller.getCreditLimit(), outstanding, total);
        }

        SalesOrder order = new SalesOrder();
        order.setOrderNumber(sequenceService.nextOrderNumber());
        order.setResellerId(reseller.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setNotes(notes);
        salesOrderRepository.save(order);

        for (Map.Entry<Long, Integer> request : requestedByProduct.entrySet()) {
            Product product = products.get(request.getKey());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setQuantity(request.getValue());
            item.setUnitPrice(product.getUnitPrice()); // price snapshot
            orderItemRepository.save(item);

            int remaining = request.getValue();
            for (Inventory row : stockByProduct.get(request.getKey())) {
                if (remaining <= 0) {
                    break;
                }
                int take = Math.min(remaining, row.getQuantity());
                if (take > 0) {
                    row.setQuantity(row.getQuantity() - take);
                    inventoryRepository.save(row);
                    remaining -= take;
                }
            }
        }
        return toRes(order);
    }

    @Transactional
    public OrderRes transition(Long orderId, OrderStatus target, String rejectionReason) {
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        OrderStatus current = order.getStatus();
        boolean allowed = switch (current) {
            case PENDING -> target == OrderStatus.APPROVED || target == OrderStatus.REJECTED;
            case APPROVED -> target == OrderStatus.SHIPPED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED, REJECTED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalTransitionException("Cannot move order from " + current + " to " + target);
        }
        if (target == OrderStatus.REJECTED) {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
            }
            order.setRejectionReason(rejectionReason.trim());
        }
        order.setStatus(target);
        if (target == OrderStatus.APPROVED && invoiceRepository.findByOrderId(order.getId()).isEmpty()) {
            Invoice invoice = new Invoice();
            invoice.setOrderId(order.getId());
            invoice.setInvoiceNumber(sequenceService.nextInvoiceNumber());
            invoice.setAmount(grandTotal(order.getTotalAmount()));
            invoiceRepository.save(invoice);
        }
        return toRes(order);
    }

    @Transactional
    public OrderRes cancel(Long orderId, Long resellerUserId) {
        Reseller reseller = requireReseller(resellerUserId);
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getResellerId().equals(reseller.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalTransitionException("Only PENDING orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toRes(order);
    }

    @Transactional(readOnly = true)
    public SalesOrder ownOrder(Long orderId, Long resellerUserId) {
        Reseller reseller = requireReseller(resellerUserId);
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getResellerId().equals(reseller.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderRes> listForReseller(Long resellerUserId) {
        Reseller reseller = requireReseller(resellerUserId);
        return salesOrderRepository.findByResellerIdOrderByCreatedAtDesc(reseller.getId()).stream()
            .map(this::toRes)
            .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal resellerOutstanding(Long resellerUserId) {
        return outstandingForReseller(requireReseller(resellerUserId).getId());
    }

    @Transactional(readOnly = true)
    public CreditRes creditSummary(Long resellerUserId) {
        Reseller reseller = requireReseller(resellerUserId);
        BigDecimal outstanding = outstandingForReseller(reseller.getId());
        return new CreditRes(reseller.getCreditLimit(), outstanding,
            reseller.getCreditLimit().subtract(outstanding));
    }

    @Transactional(readOnly = true)
    public List<AdminOrderRes> adminOrders(OrderStatus status) {
        List<SalesOrder> orders = status == null
            ? salesOrderRepository.findAll()
            : salesOrderRepository.findByStatusOrderByCreatedAtAsc(status);
        List<SalesOrder> sorted = orders.stream()
            .sorted(Comparator.comparing(SalesOrder::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .toList();
        Map<Long, Reseller> resellersById = resellerRepository.findAllById(
                sorted.stream().map(SalesOrder::getResellerId).distinct().toList()).stream()
            .collect(Collectors.toMap(Reseller::getId, r -> r));
        return sorted.stream()
            .map(order -> toAdminRes(order, resellersById.get(order.getResellerId())))
            .toList();
    }

    public AdminOrderRes toAdminRes(SalesOrder order, Reseller reseller) {
        OrderRes res = toRes(order);
        return new AdminOrderRes(res.id(), res.orderNumber(), res.status(), res.totalAmount(), res.notes(),
            res.rejectionReason(), res.createdAt(), res.items(), res.invoiceNumber(),
            reseller == null ? null : reseller.getCompanyName());
    }

    public OrderRes toRes(SalesOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId()).stream()
            .sorted(Comparator.comparing(OrderItem::getId))
            .toList();
        Map<Long, Product> products = productRepository.findAllById(
                items.stream().map(OrderItem::getProductId).distinct().toList()).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
        List<OrderRes.OrderItemRes> itemRes = items.stream()
            .map(item -> {
                Product product = products.get(item.getProductId());
                return new OrderRes.OrderItemRes(
                    product == null ? null : product.getSku(),
                    product == null ? null : product.getName(),
                    item.getQuantity(),
                    item.getUnitPrice());
            })
            .toList();
        String invoiceNumber = invoiceRepository.findByOrderId(order.getId())
            .map(Invoice::getInvoiceNumber)
            .orElse(null);
        return new OrderRes(order.getId(), order.getOrderNumber(), order.getStatus().name(),
            order.getTotalAmount(), order.getNotes(), order.getRejectionReason(),
            order.getCreatedAt(), itemRes, invoiceNumber);
    }

    private Reseller requireReseller(Long resellerUserId) {
        return resellerRepository.findByUserId(resellerUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reseller profile not found"));
    }

    private BigDecimal outstandingForReseller(Long resellerId) {
        return salesOrderRepository.findByResellerIdAndStatusIn(resellerId, OPEN_STATUSES).stream()
            .map(SalesOrder::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Tax-exclusive GST math shared with the invoice: grand = subtotal + CGST 9% + SGST 9%. */
    private BigDecimal grandTotal(BigDecimal subtotal) {
        BigDecimal cgst = subtotal.multiply(CGST_RATE).setScale(2, RoundingMode.HALF_UP);
        return subtotal.add(cgst).add(cgst);
    }
}
