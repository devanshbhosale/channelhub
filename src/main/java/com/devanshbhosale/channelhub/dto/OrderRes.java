package com.devanshbhosale.channelhub.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderRes(
    Long id,
    String orderNumber,
    String status,
    BigDecimal totalAmount,
    String notes,
    String rejectionReason,
    LocalDateTime createdAt,
    List<OrderItemRes> items,
    String invoiceNumber) {

    public record OrderItemRes(String sku, String name, Integer quantity, BigDecimal unitPrice) {
    }
}
