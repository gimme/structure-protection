package dev.gimme.adventurezones.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A collection of adventure zones indexed by chunk for quick lookup.
 */
class ZoneCollection {

    private final Map<ChunkPos, Set<AdventureZone>> byChunk = new HashMap<>();

    void add(AdventureZone zone) {
        byChunk.computeIfAbsent(zone.getChunkPos(), k -> new HashSet<>()).add(zone);
    }

    void clear(ChunkPos chunkPos) {
        byChunk.remove(chunkPos);
    }

    /**
     * Checks if there are any zones covering the given position.
     * @param pos the position to check
     * @return true if there is at least one zone covering the position, false otherwise
     */
    boolean hasAnyZoneInRange(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        int searchRadius = AdventureZone.getMaxPossibleChunkZoneRadius();

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                ChunkPos nearbyChunkPos = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
                Set<AdventureZone> zonesInChunk = byChunk.get(nearbyChunkPos);
                if (zonesInChunk == null) continue;

                for (AdventureZone zone : zonesInChunk) {
                    if (zone.covers(pos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
