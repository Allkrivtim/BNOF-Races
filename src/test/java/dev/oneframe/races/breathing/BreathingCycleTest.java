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
        assertFalse(BreathingCycle.isAtOrAboveAltitude(999.999, 1000));
        assertTrue(BreathingCycle.isAtOrAboveAltitude(1000.0, 1000));
        assertTrue(BreathingCycle.isAtOrAboveAltitude(1001.0, 1000));
    }
}
