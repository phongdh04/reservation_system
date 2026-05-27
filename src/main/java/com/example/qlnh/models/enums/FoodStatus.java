package com.example.qlnh.models.enums;

public enum FoodStatus {
    AVAILABLE("available"),
    UNAVAILABLE("unavailable");

    private final String value;

    FoodStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FoodStatus fromValue(String value) {
        for (FoodStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown FoodStatus: " + value);
    }
}
