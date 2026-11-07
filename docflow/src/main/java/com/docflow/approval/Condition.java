package com.docflow.approval;

import java.math.BigDecimal;
import java.util.Map;

/** Declarative rule on document metadata, e.g. {@code amount GT 5000}. A missing field never matches. */
public record Condition(String field, Operator op, String value) {

    public Condition {
        if (field == null || field.isBlank() || op == null || value == null) {
            throw new IllegalArgumentException("field, op and value are required");
        }
        if (op.isNumeric()) {
            toNumber(value); // fail fast on a malformed rule
        }
    }

    public boolean matches(Map<String, String> metadata) {
        String actual = metadata.get(field);
        if (actual == null) {
            return false;
        }
        if (!op.isNumeric()) {
            boolean equal = actual.equals(value);
            return op == Operator.EQ ? equal : !equal;
        }
        BigDecimal a;
        try {
            a = toNumber(actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
        int cmp = a.compareTo(toNumber(value));
        return switch (op) {
            case GT -> cmp > 0;
            case GTE -> cmp >= 0;
            case LT -> cmp < 0;
            case LTE -> cmp <= 0;
            default -> throw new IllegalStateException();
        };
    }

    private static BigDecimal toNumber(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + s, e);
        }
    }
}
