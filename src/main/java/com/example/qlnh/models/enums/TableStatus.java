package com.example.qlnh.models.enums;

public enum TableStatus {
    AVAILABLE("available"),
    OCCUPIED("occupied"),
    RESERVED("reserved");

    private final String value;

    TableStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TableStatus fromValue(String value) {
        for (TableStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TableStatus: " + value);
    }
}
