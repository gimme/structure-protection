package dev.gimme.adventurezones.domain;

import com.mojang.logging.LogUtils;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Set;

public class AdventureZones {

    private static final Logger LOG = LogUtils.getLogger();

    private final ZoneCollection adventureZones = new ZoneCollection();

    /**
     * Scans a chunk and creates adventure zones for any matching blocks.
     */
    public void loadChunk(LevelChunk chunk) {
        Set<Structure> structuresInChunk = chunk.getAllReferences().keySet();
        if (structuresInChunk.isEmpty()) return;

        var zoneConfgs = ServerConfig.INSTANCE.getZoneConfigs();

        Registry<Structure> structureRegistry = chunk.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Registry<Block> blockRegistry = chunk.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);

        structuresInChunk.forEach(structure -> {
            Identifier structureRL = structureRegistry.getKey(structure);
            if (structureRL == null) return;

            zoneConfgs.forEach(zoneConfig -> {
                var isStructureWhitelisted = matchesRegex(structureRL, zoneConfig.structures());
                if (!isStructureWhitelisted) return;

                if (zoneConfig.blocks() != null) {
                    chunk.findBlocks(
                            (blockState) -> true,
                            (pos, blockState) -> {
                                Identifier blockRL = blockRegistry.getKey(blockState.getBlock());
                                if (blockRL == null) return;

                                var isBlockWhitelisted = matchesRegex(blockRL, zoneConfig.blocks());
                                if (!isBlockWhitelisted) return;

                                adventureZones.add(new AdventureZone(pos, zoneConfig.radius(), zoneConfig.minY(), zoneConfig.maxY()));
                                LOG.debug("Loaded adventure zone for structure {} in chunk {} and block {} at [{}]", structureRL, chunk.getPos(), blockRL, pos.toShortString());
                            }
                    );
                } else {
                    adventureZones.add(new AdventureZone(chunk.getPos(), zoneConfig.radius(), zoneConfig.minY(), zoneConfig.maxY()));
                    LOG.debug("Loaded adventure zone for structure {} in chunk {}", structureRL, chunk.getPos());
                }
            });
        });
    }

    /**
     * Checks if the given resource location matches the specified regex.
     */
    private static boolean matchesRegex(@NotNull Identifier resourceLocation, @NotNull String regex) {
        if (regex.contains(":")) {
            return resourceLocation.toString().matches(regex);
        } else {
            return resourceLocation.getPath().matches(regex);
        }
    }

    /**
     * Unloads a chunk and removes any adventure zones associated with it.
     */
    public void unloadChunk(ChunkPos chunkPos) {
        adventureZones.clear(chunkPos);
    }

    /**
     * Checks if the given position is inside an adventure zone.
     */
    public boolean isInAdventureZone(BlockPos pos) {
        return adventureZones.hasAnyZoneInRange(pos);
    }
}
