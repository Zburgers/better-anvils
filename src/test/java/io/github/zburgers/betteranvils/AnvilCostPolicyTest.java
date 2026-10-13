package io.github.zburgers.betteranvils;

import io.github.zburgers.betteranvils.config.BetterAnvilsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilCostPolicyTest {
    @ParameterizedTest
    @CsvSource({"1,1", "59,30", "79,40", "119,60", "197,99", "199,100", "200,100", "201,101"})
    void roundsDefaultFiftyPercentDiscountUp(int originalCost, int expected) {
        assertEquals(expected, AnvilCostPolicy.discountedCost(originalCost, BetterAnvilsConfig.defaults()).orElseThrow());
    }

    @ParameterizedTest
    @CsvSource({"0,60", "25,45", "75,15", "100,1"})
    void supportsEveryDiscountBoundary(int discount, int expected) {
        var config = new BetterAnvilsConfig(100, discount, 1, true);
        assertEquals(expected, AnvilCostPolicy.discountedCost(60, config).orElseThrow());
    }

    @Test
    void appliesMinimumFloorWithoutTurningZeroIntoAnOperation() {
        var config = new BetterAnvilsConfig(100, 100, 3, true);
        assertEquals(3, AnvilCostPolicy.discountedCost(60, config).orElseThrow());
        assertEquals(OptionalInt.empty(), AnvilCostPolicy.discountedCost(0, config));
        assertEquals(OptionalInt.empty(), AnvilCostPolicy.discountedCost(-1, config));
    }

    @Test
    void maximumIsInclusive() {
        var config = BetterAnvilsConfig.defaults();
        assertTrue(AnvilCostPolicy.permits(100, config));
        assertFalse(AnvilCostPolicy.permits(101, config));
        assertFalse(AnvilCostPolicy.permits(0, config));
    }

    @Test
    void integerMaximumDoesNotOverflow() {
        var original = Integer.MAX_VALUE;
        assertEquals(Integer.MAX_VALUE, AnvilCostPolicy.discountedCost(original, new BetterAnvilsConfig(500, 0, 1, true)).orElseThrow());
        assertEquals(1_073_741_824, AnvilCostPolicy.discountedCost(original, BetterAnvilsConfig.defaults()).orElseThrow());
        assertEquals(1, AnvilCostPolicy.discountedCost(original, new BetterAnvilsConfig(500, 100, 1, true)).orElseThrow());
    }

    @Test
    void rejectsInvalidSettingsAtThePolicyBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> AnvilCostPolicy.discountedCost(1, new BetterAnvilsConfig(0, 50, 1, true)));
    }
}
