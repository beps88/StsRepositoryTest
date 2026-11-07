package com.docflow.approval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConditionTest {

    @Test
    void numericComparisons() {
        Map<String, String> m = Map.of("amount", "5000.50");
        assertTrue(new Condition("amount", Operator.GT, "5000").matches(m));
        assertTrue(new Condition("amount", Operator.GTE, "5000.5").matches(m));
        assertFalse(new Condition("amount", Operator.LT, "5000").matches(m));
        assertTrue(new Condition("amount", Operator.LTE, "5000.50").matches(m));
    }

    @Test
    void stringEquality() {
        Map<String, String> m = Map.of("dept", "HR");
        assertTrue(new Condition("dept", Operator.EQ, "HR").matches(m));
        assertTrue(new Condition("dept", Operator.NE, "IT").matches(m));
        assertFalse(new Condition("dept", Operator.EQ, "IT").matches(m));
    }

    @Test
    void missingOrNonNumericFieldNeverMatchesNumericRule() {
        assertFalse(new Condition("amount", Operator.GT, "1").matches(Map.of()));
        assertFalse(new Condition("amount", Operator.GT, "1").matches(Map.of("amount", "abc")));
    }

    @Test
    void malformedNumericRuleFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> new Condition("amount", Operator.GT, "many"));
    }
}
