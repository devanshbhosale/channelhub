package com.devanshbhosale.channelhub.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderRes plus the reseller's company name for admin listings.
 */
public record AdminOrderRes(
    Long id,
    String orderNumber,
    String status,
    BigDecimal totalAmount,
    String notes,
    String rejectionReason,
    LocalDateTime createdAt,
    List<OrderRes.OrderItemRes> items,
    String invoiceNumber,
    String companyName) {
}
