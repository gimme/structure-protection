package dev.gimme.adventurezones.application;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Handles storing and retrieving persistent chunk data.
 */
public interface ChunkDataHandler {

    /**
     * Records a block as being placed by a player in the given chunk.
     */
    void addPlayerPlacedBlock(LevelChunk chunk, BlockPos pos);

    /**
     * Gets all blocks marked as placed by players in the given chunk.
     */
    List<BlockPos> getPlayerPlacedBlocks(LevelChunk chunk);
}
