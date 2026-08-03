package dev.oneframe.races.breathing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreathingCycleTest {

    @Test
    void vanillaHudCountsDownAndResetsOnDamage() {
        assertEquals(new BreathingCycle.Step(299, false), BreathingCycle.drainVanillaHud(300));
        assertEquals(new BreathingCycle.Step(298, false), BreathingCycle.drainVanillaHud(299));
        assertEquals(new BreathingCycle.Step(0, true), BreathingCycle.drainVanillaHud(-19));
    }

    @Test
    void altitudeThresholdIsInclusive() {
        int configuredThreshold = 640;
        assertFalse(BreathingCycle.isAtOrAboveAltitude(639.999, configuredThreshold));
        assertTrue(BreathingCycle.isAtOrAboveAltitude(640.0, configuredThreshold));
        assertTrue(BreathingCycle.isAtOrAboveAltitude(641.0, configuredThreshold));
    }
}
