package dev.gimme.adventurezones.domain;

import com.mojang.logging.LogUtils;
import dev.gimme.adventurezones.application.ChunkDataHandler;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

public class AdventureZones {

    private static final Logger LOG = LogUtils.getLogger();

    private final ChunkDataHandler chunkDataHandler;
    private final ZoneCollection adventureZones = new ZoneCollection();

    public AdventureZones(ChunkDataHandler chunkDataHandler) {
        this.chunkDataHandler = chunkDataHandler;
    }

    /**
     * Scans a chunk and creates adventure zones for any matching blocks.
     */
    public void loadChunk(LevelChunk chunk) {
        chunk.findBlocks(
                (blockState) -> true,
                (pos, blockState) -> {
                    loadBlock(pos, blockState, chunk);
                }
        );
    }

    /**
     * Unloads a chunk and removes any adventure zones associated with it.
     */
    public void unloadChunk(ChunkPos chunkPos) {
        adventureZones.clear(chunkPos);
    }

    /**
     * Updates the player's game mode based on if they are in an adventure zone.
     */
    public void updatePlayer(ServerPlayer player) {
        var pos = player.blockPosition();

        if (isInAdventureZone(pos)) {
            if (player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
                enterAdventureZone(player);
            }
        } else {
            if (player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                exitAdventureZone(player);
            }
        }
    }

    public void onPlayerPlaceBlock(LevelChunk chunk, BlockPos pos, BlockState blockState) {
        if (!isZoneBlock(blockState, chunk)) return;
        chunkDataHandler.addPlayerPlacedBlock(chunk, pos);
        LOG.debug("Player placed zone block [{}]", pos.toShortString());
    }

    /**
     * Checks if the given position is inside an adventure zone.
     */
    private boolean isInAdventureZone(BlockPos pos) {
        return adventureZones.hasAnyZoneInRange(pos);
    }

    /**
     * Forces the player into Adventure mode.
     */
    private void enterAdventureZone(ServerPlayer player) {
        player.setGameMode(GameType.ADVENTURE);

        if (ServerConfig.INSTANCE.displayZoneText()) {
            Component title = Component.translatableWithFallback("message.adventurezones.enter_zone_title", "Adventure Mode")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA));
            ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(Component.literal(""));
            ClientboundSetSubtitleTextPacket subtitlePacket = new ClientboundSetSubtitleTextPacket(title);
            player.connection.send(titlePacket);
            player.connection.send(subtitlePacket);
        }
    }

    /**
     * Returns the player to survival mode.
     */
    private void exitAdventureZone(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);

        if (ServerConfig.INSTANCE.displayZoneText()) {
            Component title = Component.translatableWithFallback("message.adventurezones.leave_zone_title", "Survival Mode")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(Component.literal(""));
            ClientboundSetSubtitleTextPacket subtitlePacket = new ClientboundSetSubtitleTextPacket(title);
            player.connection.send(titlePacket);
            player.connection.send(subtitlePacket);
        }
    }

    /**
     * Checks if the given block is defined as an adventure zone block.
     */
    private boolean isZoneBlock(BlockState blockState, LevelChunk chunk) {
        Set<Structure> structuresInChunk = chunk.getAllReferences().keySet();
        if (structuresInChunk.isEmpty()) return false;

        List<ResourceLocation> blockWhitelist = ServerConfig.INSTANCE.getBlockWhitelist();
        var isWhitelistedBlock = blockWhitelist.stream().anyMatch(whitelistedBlock -> blockState.getBlockHolder().is(whitelistedBlock));
        if (!isWhitelistedBlock) return false;

        List<ResourceLocation> structureWhitelist = ServerConfig.INSTANCE.getStructureWhitelist();
        List<ResourceLocation> structureBlacklist = ServerConfig.INSTANCE.getStructureBlacklist();
        Registry<Structure> structureRegistry = chunk.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (Structure structure : structuresInChunk) {
            var isWhitelisted = structureWhitelist.isEmpty() || structureWhitelist.stream().anyMatch(rl -> structureRegistry.get(rl) == structure);
            var isBlacklisted = structureBlacklist.stream().anyMatch(rl -> structureRegistry.get(rl) == structure);

            if (isWhitelisted && !isBlacklisted) return true;
        }

        return false;
    }

    /**
     * Checks a block and creates an adventure zone if it matches the criteria.
     */
    private void loadBlock(BlockPos pos, BlockState blockState, LevelChunk chunk) {
        if (!isZoneBlock(blockState, chunk)) return;

        if (chunkDataHandler.getPlayerPlacedBlocks(chunk).contains(pos)) {
            LOG.debug("Ignored player-placed block [{}]", pos.toShortString());
            return;
        }

        adventureZones.add(new AdventureZone(pos));
        LOG.debug("Loaded adventure zone block [{}]", pos.toShortString());
    }
}
