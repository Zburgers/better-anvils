package io.github.zburgers.betteranvils.config;

import java.util.ArrayList;
import java.util.List;

public record BetterAnvilsConfig(
        int maximumAnvilCost,
        int anvilDiscountPercent,
        int minimumAnvilCost,
        boolean preservePriorWorkPenalty
) {
    public static BetterAnvilsConfig defaults() {
        return new BetterAnvilsConfig(100, 50, 1, true);
    }

    public List<String> validationErrors() {
        var errors = new ArrayList<String>();
        if (maximumAnvilCost < 1 || maximumAnvilCost > 500) {
            errors.add("maximumAnvilCost must be between 1 and 500");
        }
        if (anvilDiscountPercent < 0 || anvilDiscountPercent > 100) {
            errors.add("anvilDiscountPercent must be between 0 and 100");
        }
        if (minimumAnvilCost < 1 || minimumAnvilCost > 10) {
            errors.add("minimumAnvilCost must be between 1 and 10");
        }
        if (minimumAnvilCost > maximumAnvilCost) {
            errors.add("minimumAnvilCost must not exceed maximumAnvilCost");
        }
        return List.copyOf(errors);
    }

    public BetterAnvilsConfig requireValid() {
        var errors = validationErrors();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        return this;
    }

    public BetterAnvilsConfig snapshot() {
        return new BetterAnvilsConfig(maximumAnvilCost, anvilDiscountPercent, minimumAnvilCost, preservePriorWorkPenalty);
    }
}
