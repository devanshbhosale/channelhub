package com.devanshbhosale.channelhub.dto;

public record AccountRes(
    String companyName,
    String gstin,
    String phone,
    CreditRes credit,
    long orderCount,
    long pendingCount) {
}
