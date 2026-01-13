package dev.gimme.adventurezones.infrastructure;

import dev.gimme.adventurezones.application.ChunkHandler;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.NotNull;

public class ChunkListener {

    private final ChunkHandler chunkHandler;

    public ChunkListener(@NotNull ChunkHandler chunkHandler) {
        this.chunkHandler = chunkHandler;
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        chunkHandler.onChunkLoad(chunk);
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        chunkHandler.onChunkUnload(chunk);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel().getChunk(event.getPos()) instanceof LevelChunk chunk)) return;

        chunkHandler.onBlockPlace(chunk, event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public void onMultiBlockPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (!(event.getLevel().getChunk(event.getPos()) instanceof LevelChunk chunk)) return;

        chunkHandler.onBlockPlace(chunk, event.getPos(), event.getPlacedBlock());
    }
}
