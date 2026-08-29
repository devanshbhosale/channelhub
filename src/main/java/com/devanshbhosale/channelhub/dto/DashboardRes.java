package com.devanshbhosale.channelhub.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardRes(
    StatCards statCards,
    List<SeriesPoint> series14d,
    List<TopSku> topSkus,
    List<InventoryRow> lowStock,
    List<QueueRow> queue) {

    public record StatCards(long pending, long approved, long shippedToday, long lowStock, BigDecimal revenueThisMonth) {
    }

    public record SeriesPoint(String date, long orders, BigDecimal revenue) {
    }

    public record TopSku(String sku, String name, long qtySold) {
    }

    /**
     * Shared by GET /api/admin/inventory and the dashboard low-stock block.
     */
    public record InventoryRow(long productId, String sku, String name, String warehouse, int quantity, int reorderLevel) {
    }

    public record QueueRow(String orderNumber, String companyName, BigDecimal totalAmount, String status, LocalDateTime createdAt) {
    }
}
