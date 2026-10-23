package de.frostbound.tweaks.warmth;

import com.momosoftworks.coldsweat.util.world.WorldHelper;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Vermisst den Raum um eine Position: Flutfüllung durch Luft nach denselben Regeln wie die Hearth-Luft
 * (Cold Sweats isSpreadBlocked: Wände, geschlossene Türen, Glasscheiben halten auf, offene Türen nicht).
 * Luftblöcke mit Himmelssicht (über der Heightmap) sind Lecks und werden nicht weiter gefüllt.
 * Ungeladene Chunks gelten ebenfalls als Leck; es werden nie Chunks geladen.
 */
final class RoomScanner {
    private static final Direction[] DIRECTIONS = Direction.values();

    /** volume = Luftblöcke im Raum, leaks = berührte Himmelsblöcke, sources = Positionen möglicher Wärmequellen. */
    record Room(int volume, int leaks, boolean open, boolean tooBig, LongArrayList sources) {
        static Room outside() {
            return new Room(0, 0, true, false, new LongArrayList());
        }

        boolean heatable() {
            return !open && !tooBig;
        }

        double leakFactor() {
            return Math.max(0, 1 - (double) leaks / RoomHeatConfig.LEAK_LIMIT.get());
        }
    }

    private RoomScanner() {}

    static Room scan(ServerLevel level, BlockPos start) {
        int maxVolume = RoomHeatConfig.MAX_VOLUME.get();
        int leakLimit = RoomHeatConfig.LEAK_LIMIT.get();
        LongOpenHashSet seen = new LongOpenHashSet(512);
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue(256);
        LongArrayList sources = new LongArrayList();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ChunkLookup chunks = new ChunkLookup(level);

        LevelChunk startChunk = chunks.get(start.getX(), start.getZ());
        if (startChunk == null) return Room.outside();
        long startKey = start.asLong();
        seen.add(startKey);
        queue.enqueue(startKey);
        // Unter einem Loch im Dach zählt die eigene Position als Leck, der Raum drumherum trotzdem;
        // findet sich gar kein überdachter Luftblock, steht man im Freien
        boolean startSky = seesSky(startChunk, start);
        if (!startSky && isSource(startChunk.getBlockState(start))) sources.add(startKey);
        int volume = startSky ? 0 : 1;
        int leaks = startSky ? 1 : 0;

        while (!queue.isEmpty()) {
            long cur = queue.dequeueLong();
            for (Direction dir : DIRECTIONS) {
                long next = BlockPos.offset(cur, dir);
                if (!seen.add(next)) continue;
                pos.set(next);
                if (level.isOutsideBuildHeight(pos)) {
                    if (pos.getY() >= level.getMaxBuildHeight()) leaks++;
                    continue;
                }
                LevelChunk chunk = chunks.get(pos.getX(), pos.getZ());
                if (chunk == null) {
                    if (++leaks >= leakLimit) return new Room(volume, leaks, true, false, sources);
                    continue;
                }
                BlockState state = chunk.getBlockState(pos);
                if (WorldHelper.isSpreadBlocked(level, state, pos, dir, dir)) {
                    // Wand: Öfen, Herde usw. sind feste Blöcke und stehen hier
                    if (isSource(state)) sources.add(next);
                    continue;
                }
                if (seesSky(chunk, pos)) {
                    if (++leaks >= leakLimit) return new Room(volume, leaks, true, false, sources);
                    continue;
                }
                if (isSource(state)) sources.add(next);
                if (++volume > maxVolume) return new Room(volume, leaks, false, true, sources);
                queue.enqueue(next);
            }
        }
        if (volume == 0) return Room.outside();
        return new Room(volume, leaks, false, false, sources);
    }

    private static boolean isSource(BlockState state) {
        return !state.isAir() && !RoomHeatConfig.sourcesFor(state).isEmpty();
    }

    private static boolean seesSky(LevelChunk chunk, BlockPos pos) {
        return pos.getY() > chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX() & 15, pos.getZ() & 15);
    }

    /** Merkt sich den zuletzt benutzten Chunk; liefert nur bereits geladene Chunks. */
    private static final class ChunkLookup {
        private final ServerLevel level;
        private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
        private LevelChunk last;

        ChunkLookup(ServerLevel level) {
            this.level = level;
        }

        LevelChunk get(int blockX, int blockZ) {
            int cx = blockX >> 4, cz = blockZ >> 4;
            if (cx != lastX || cz != lastZ) {
                lastX = cx;
                lastZ = cz;
                last = level.getChunkSource().getChunkNow(cx, cz);
            }
            return last;
        }
    }
}
