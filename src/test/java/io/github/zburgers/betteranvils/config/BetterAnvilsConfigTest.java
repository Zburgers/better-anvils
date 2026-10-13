package io.github.zburgers.betteranvils.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterAnvilsConfigTest {
    @Test
    void defaultsMatchTheProductContract() {
        assertEquals(new BetterAnvilsConfig(100, 50, 1, true), BetterAnvilsConfig.defaults());
    }

    @Test
    void validBoundsAreAccepted() {
        assertTrue(new BetterAnvilsConfig(1, 0, 1, false).validationErrors().isEmpty());
        assertTrue(new BetterAnvilsConfig(500, 100, 10, true).validationErrors().isEmpty());
    }

    @Test
    void rejectsOutOfRangeValuesAndMinimumAboveMaximum() {
        assertTrue(new BetterAnvilsConfig(0, 50, 1, true).validationErrors().stream().anyMatch(e -> e.contains("maximumAnvilCost")));
        assertTrue(new BetterAnvilsConfig(100, -1, 1, true).validationErrors().stream().anyMatch(e -> e.contains("anvilDiscountPercent")));
        assertTrue(new BetterAnvilsConfig(100, 101, 1, true).validationErrors().stream().anyMatch(e -> e.contains("anvilDiscountPercent")));
        assertTrue(new BetterAnvilsConfig(100, 50, 0, true).validationErrors().stream().anyMatch(e -> e.contains("minimumAnvilCost")));
        assertTrue(new BetterAnvilsConfig(100, 50, 11, true).validationErrors().stream().anyMatch(e -> e.contains("minimumAnvilCost")));
        assertTrue(new BetterAnvilsConfig(5, 50, 6, true).validationErrors().stream().anyMatch(e -> e.contains("minimumAnvilCost")));
    }

    @Test
    void snapshotIsAnIndependentImmutableValue() {
        var config = BetterAnvilsConfig.defaults();
        var snapshot = config.snapshot();
        assertNotSame(config, snapshot);
        assertEquals(config, snapshot);
    }

    @Test
    void requireValidReturnsTheValidatedConfigOrExplainsTheProblem() {
        var valid = BetterAnvilsConfig.defaults();
        assertEquals(valid, valid.requireValid());
        var invalid = new BetterAnvilsConfig(100, 50, 11, true);
        assertThrows(IllegalArgumentException.class, invalid::requireValid);
    }
}
