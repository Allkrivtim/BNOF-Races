package dev.oneframe.races.breathing;

final class BreathingCycle {

    static final int DROWNING_THRESHOLD = -20;

    private BreathingCycle() {
    }

    static Step drainVanillaHud(int current) {
        int next = current - 1;
        return next <= DROWNING_THRESHOLD ? new Step(0, true) : new Step(next, false);
    }

    record Step(int air, boolean damage) {
    }
}
