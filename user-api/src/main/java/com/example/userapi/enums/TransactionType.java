package com.example.userapi.enums;

public enum TransactionType {
    TOP_UP("儲值"),
    PAYMENT("付款");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
