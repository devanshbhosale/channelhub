package com.devanshbhosale.channelhub.dto;

import java.math.BigDecimal;

public record ResellerAdminRes(
    String companyName,
    String gstin,
    String phone,
    BigDecimal creditLimit,
    BigDecimal outstanding,
    BigDecimal available,
    long orderCount) {
}
