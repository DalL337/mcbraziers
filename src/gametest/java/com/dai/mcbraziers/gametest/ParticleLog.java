package com.dai.mcbraziers.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;

/**
 * Test-only log of the particles the client is asked to spawn, filled by
 * {@code ClientLevelParticleLogMixin} between {@link #start()} and {@link #stop()}.
 */
public final class ParticleLog {

    /** One particle request: its type, the block it spawned in and its vertical speed. */
    public record Entry(ParticleType<?> type, BlockPos block, double yd) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean recording;

    private ParticleLog() {}

    public static synchronized void start() {
        ENTRIES.clear();
        recording = true;
    }

    public static synchronized List<Entry> stop() {
        recording = false;
        return List.copyOf(ENTRIES);
    }

    public static synchronized void record(ParticleType<?> type, double x, double y, double z, double yd) {
        if (recording) {
            ENTRIES.add(new Entry(type, BlockPos.containing(x, y, z), yd));
        }
    }
}
