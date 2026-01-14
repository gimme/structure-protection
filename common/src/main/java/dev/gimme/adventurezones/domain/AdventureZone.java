package dev.gimme.adventurezones.domain;

import dev.gimme.adventurezones.domain.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

class AdventureZone {

    private final ChunkPos chunkPos;
    private final int radius;
    private final int minY;
    private final int maxY;

    AdventureZone(ChunkPos chunkPos, int radius, @Nullable Integer minY, @Nullable Integer maxY) {
        this.chunkPos = chunkPos;
        this.radius = radius;
        this.minY = minY != null ? minY : Integer.MIN_VALUE;
        this.maxY = maxY != null ? maxY : Integer.MAX_VALUE;
    }

    AdventureZone(BlockPos blockPos, int radius, @Nullable Integer minY, @Nullable Integer maxY) {
        this.chunkPos = new ChunkPos(blockPos);
        this.radius = radius;

        var sectionPos = SectionPos.of(blockPos);
        this.minY = Math.max(minY != null ? minY : Integer.MIN_VALUE, sectionPos.minBlockY() - radius);
        this.maxY = Math.min(maxY != null ? maxY : Integer.MAX_VALUE, sectionPos.maxBlockY() + radius);
    }

    /**
     * Checks if the given position is inside this zone.
     */
    boolean covers(BlockPos pos) {
        var distance = getChessboardDistance(pos);
        return distance <= 0;
    }

    /**
     * Returns the maximum distance out of the x, y and z axis.
     */
    private int getChessboardDistance(BlockPos pos) {
        var dx = Math.min(pos.getX() - getMinX(), pos.getX() - getMaxX());
        var dz = Math.min(pos.getZ() - getMinZ(), pos.getZ() - getMaxZ());
        var dy = Math.min(pos.getY() - minY, pos.getY() - maxY);

        return Math.max(Math.max(dx, dz), dy);
    }

    private int getMinX() {
        return chunkPos.getMinBlockX() - radius;
    }
    private int getMaxX() {
        return chunkPos.getMaxBlockX() + radius;
    }
    private int getMinZ() {
        return chunkPos.getMinBlockZ() - radius;
    }
    private int getMaxZ() {
        return chunkPos.getMaxBlockZ() + radius;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    static int getMaxPossibleChunkZoneRadius() {
        return ServerConfig.INSTANCE.getZoneRadius();
    }
}
