package com.devanshbhosale.channelhub.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Indian-format money: 486500 -> 4,86,500.00 (lakh/crore grouping).
 * Core JDK DecimalFormat ignores secondary grouping sizes (ICU-only feature),
 * verified empirically on JDK 24 — so grouping is done manually: last 3 digits,
 * then pairs.
 */
public final class MoneyFormat {

    private static final String RUPEE = "₹";

    private MoneyFormat() {
    }

    public static String inr(BigDecimal value) {
        return RUPEE + inrPlain(value);
    }

    public static String inrPlain(BigDecimal value) {
        BigDecimal v = (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
        boolean negative = v.signum() < 0;
        String[] parts = v.abs().toPlainString().split("\\.", 2);
        String grouped = groupIndian(parts[0]) + "." + parts[1];
        return (negative ? "-" : "") + grouped;
    }

    private static String groupIndian(String digits) {
        int n = digits.length();
        if (n <= 3) {
            return digits;
        }
        String last3 = digits.substring(n - 3);
        String rest = digits.substring(0, n - 3);
        Deque<String> pairs = new ArrayDeque<>();
        for (int i = rest.length(); i > 0; i -= 2) {
            pairs.push(rest.substring(Math.max(0, i - 2), i));
        }
        return String.join(",", pairs) + "," + last3;
    }
}
