package com.devanshbhosale.channelhub.dto;

/**
 * One per-product shortfall line: available = total stock across all warehouses.
 */
public record StockShortfall(String sku, int requested, int available) {
}
