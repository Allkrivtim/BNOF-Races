package dev.oneframe.races.tick;

import java.util.function.Consumer;

/** intervalPasses = 1 means "every pass" (every 1s / 20 ticks); N means every Nth pass. */
public record TickTask(int intervalPasses, Consumer<Long> action) {
}
