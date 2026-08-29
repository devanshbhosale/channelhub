package com.devanshbhosale.channelhub.dto;

public record InventoryAdjustReq(Long productId, String warehouse, Integer delta) {
}
