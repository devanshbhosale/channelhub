package com.devanshbhosale.channelhub.dto;

import java.math.BigDecimal;

public record ProductReq(
    String sku,
    String name,
    String brand,
    String category,
    String description,
    BigDecimal unitPrice,
    Boolean active) {
}
