package dev.oneframe.races.races.merman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MermanAirCycleTest {

    @Test
    void drainsOnePointOnDryLand() {
        assertEquals(298, MermanAirCycle.nextDryAir(299));
        assertFalse(MermanAirCycle.causesDamage(299));
    }

    @Test
    void resetsAndDamagesAtVanillaDrowningThreshold() {
        assertEquals(0, MermanAirCycle.nextDryAir(-19));
        assertTrue(MermanAirCycle.causesDamage(-19));
        assertEquals(0, MermanAirCycle.nextDryAir(-20));
    }
}
