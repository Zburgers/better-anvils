package io.github.zburgers.betteranvils;

import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;

import java.util.OptionalInt;

public final class AnvilCostPolicy {
    private AnvilCostPolicy() {}

    public static OptionalInt discountedCost(int originalCost, BetterAnvilsConfig config) {
        config.requireValid();
        if (originalCost <= 0) {
            return OptionalInt.empty();
        }
        long scaledCost = (long) originalCost * (100L - config.anvilDiscountPercent());
        long roundedCost = (scaledCost + 99L) / 100L;
        long effectiveCost = Math.max(config.minimumAnvilCost(), roundedCost);
        return OptionalInt.of(Math.toIntExact(effectiveCost));
    }

    public static boolean permits(int effectiveCost, BetterAnvilsConfig config) {
        config.requireValid();
        return effectiveCost > 0 && effectiveCost <= config.maximumAnvilCost();
    }
}
