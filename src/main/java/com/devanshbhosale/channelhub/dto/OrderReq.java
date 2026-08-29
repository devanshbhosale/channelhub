package com.devanshbhosale.channelhub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderReq(
    @NotEmpty(message = "At least one item is required") @Valid List<OrderItemReq> items,
    String notes) {

    public record OrderItemReq(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1") Integer quantity) {
    }
}
