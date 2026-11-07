package com.docflow.approval;

public enum Operator {
    EQ, NE, GT, GTE, LT, LTE;

    boolean isNumeric() {
        return this != EQ && this != NE;
    }
}
