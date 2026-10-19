package com.dai.mcbraziers.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Behaviors slice 2: the per-tick cook-step arithmetic behind tiered cooking speed. */
class BrazierCookingStepsTest {

    @Test
    void tierSpeedsMatchThePrd() {
        assertEquals(1.0, BrazierTier.COPPER.cookingSpeed());
        assertEquals(1.25, BrazierTier.IRON.cookingSpeed());
        assertEquals(1.5, BrazierTier.DIAMOND.cookingSpeed());
        assertEquals(1.5, BrazierTier.OBSIDIAN.cookingSpeed());
        assertEquals(1.5, BrazierTier.PRISMARINE.cookingSpeed());
        assertEquals(2.0, BrazierTier.NETHERITE.cookingSpeed());
    }

    /** Over any run of ticks, a tier performs speed × ticks cook steps (to within one). */
    @Test
    void stepsAddUpToTheTierSpeed() {
        for (BrazierTier tier : BrazierTier.values()) {
            for (long start : new long[] {0, 7, 123_456_789L}) {
                int steps = 0;
                for (long tick = start; tick < start + 600; tick++) {
                    steps += tier.cookingSteps(tick);
                }
                double expected = 600 * tier.cookingSpeed();
                assertTrue(Math.abs(steps - expected) <= 1, tier + " from tick " + start + ": " + steps + " steps, expected " + expected);
            }
        }
    }

    @Test
    void stepsPerTickFollowThePattern() {
        assertEquals("1111", pattern(BrazierTier.COPPER, 4));
        assertEquals("1112", pattern(BrazierTier.IRON, 4));
        assertEquals("1212", pattern(BrazierTier.DIAMOND, 4));
        assertEquals("2222", pattern(BrazierTier.NETHERITE, 4));
    }

    private static String pattern(BrazierTier tier, int ticks) {
        StringBuilder out = new StringBuilder();
        for (long tick = 0; tick < ticks; tick++) {
            out.append(tier.cookingSteps(tick));
        }
        return out.toString();
    }
}
