package com.devanshbhosale.channelhub.dto;

import java.math.BigDecimal;

public record CreditRes(BigDecimal creditLimit, BigDecimal outstanding, BigDecimal available) {
}
