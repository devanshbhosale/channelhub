package com.devanshbhosale.channelhub.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyFormatTest {

    @Test
    void formatsIndianLakhGrouping() {
        assertEquals("₹4,86,500.00", MoneyFormat.inr(new BigDecimal("486500")));
        assertEquals("₹12,34,567.89", MoneyFormat.inr(new BigDecimal("1234567.89")));
        assertEquals("4,86,500.00", MoneyFormat.inrPlain(new BigDecimal("486500")));
        assertEquals("₹1,200.00", MoneyFormat.inr(new BigDecimal("1200")));
    }
}
