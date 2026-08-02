package dev.oneframe.races.races.merman;

final class MermanAirCycle {

    private static final int DAMAGE_THRESHOLD = -19;

    private MermanAirCycle() {
    }

    static int nextDryAir(int current) {
        return causesDamage(current) ? 0 : current - 1;
    }

    static boolean causesDamage(int current) {
        return current <= DAMAGE_THRESHOLD;
    }
}
