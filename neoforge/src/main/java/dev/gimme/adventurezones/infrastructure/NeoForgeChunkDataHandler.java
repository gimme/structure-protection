package dev.gimme.adventurezones.infrastructure;

import dev.gimme.adventurezones.application.ChunkDataHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public class NeoForgeChunkDataHandler implements ChunkDataHandler {

    @Override
    public void addPlayerPlacedBlock(LevelChunk chunk, BlockPos pos) {
        var playerPlacedBlocks = chunk.getData(ModAttachmentTypes.PLAYER_PLACED_BLOCKS);
        var newList = new ArrayList<>(playerPlacedBlocks);
        newList.add(pos);
        chunk.setData(ModAttachmentTypes.PLAYER_PLACED_BLOCKS, newList);
    }

    @Override
    public List<BlockPos> getPlayerPlacedBlocks(LevelChunk chunk) {
        return chunk.getData(ModAttachmentTypes.PLAYER_PLACED_BLOCKS);
    }
}
