package dev.oneframe.races.util;

import org.bukkit.World;

/**
 * Day/night check based on the raw relative time ({@code World#getTime()}, 0-23999, "hours *
 * 1000"). Deliberately NOT {@code World#isDayTime()}: that convenience method's exact semantics
 * vary across server implementations/versions, and this is the same manual boundary
 * (night = [12000, 24000)) used throughout the Bukkit plugin ecosystem for exactly that reason -
 * unambiguous and stable regardless of server jar quirks.
 */
public final class WorldTimeUtil {

    private static final long NIGHT_START = 12000L;
    private static final long NIGHT_END = 24000L;

    private WorldTimeUtil() {
    }

    public static boolean isNight(World world) {
        long time = world.getTime();
        return time >= NIGHT_START && time < NIGHT_END;
    }
}
