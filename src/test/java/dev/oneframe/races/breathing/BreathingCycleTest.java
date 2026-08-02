package dev.oneframe.races.breathing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreathingCycleTest {

    @Test
    void vanillaHudCountsDownAndResetsOnDamage() {
        assertEquals(new BreathingCycle.Step(298, false), BreathingCycle.drainVanillaHud(299));
        assertEquals(new BreathingCycle.Step(0, true), BreathingCycle.drainVanillaHud(-19));
    }
}
