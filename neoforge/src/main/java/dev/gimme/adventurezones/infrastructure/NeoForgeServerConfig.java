package dev.gimme.adventurezones.infrastructure;

import com.electronwill.nightconfig.core.Config;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.stream.Stream;

public class NeoForgeServerConfig extends ServerConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue ZONE_RADIUS = BUILDER
            .comment("""
                    The radius of adventure zones, measured in chunk sections (horizontally and vertically in sections of 16 blocks).
                    The center of the zone is the chunk section that the whitelisted block is found in.
                    A radius of 2 means the zone extends 2 chunks in each direction.""")
            .defineInRange("zoneRadius", 2, 0, 5);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_WHITELIST = BUILDER
            .comment("The blocks to create adventure zones around")
            .defineList(
                    "blockWhitelist",
                    Stream.of(
                            Blocks.SPAWNER,
                            Blocks.CHEST,
                            Blocks.TRAPPED_CHEST,
                            Blocks.BARREL
                    ).map(block -> BuiltInRegistries.BLOCK.getKey(block).toString()).toList(),
                    () -> "minecraft:chest",
                    o -> {
                        if (!(o instanceof String str)) return false;
                        ResourceLocation rl = ResourceLocation.tryParse(str);
                        return rl != null && BuiltInRegistries.BLOCK.containsKey(rl);
                    }
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_WHITELIST = BUILDER
            .comment("""
                    The only structures to look for the blocks in. If empty, all structures are considered.
                    For modded structures, make sure to include the namespace (e.g., "betterdungeons:small_nether_dungeon").""")
            .defineList(
                    "structureWhitelist",
                    Stream.of(
                            BuiltinStructures.BASTION_REMNANT,
                            BuiltinStructures.DESERT_PYRAMID,
                            BuiltinStructures.END_CITY,
                            BuiltinStructures.FORTRESS,
                            BuiltinStructures.JUNGLE_TEMPLE,
                            BuiltinStructures.NETHER_FOSSIL,
                            BuiltinStructures.OCEAN_MONUMENT,
                            BuiltinStructures.PILLAGER_OUTPOST,
                            BuiltinStructures.TRIAL_CHAMBERS,
                            BuiltinStructures.WOODLAND_MANSION
                    ).map(structure -> structure.location().getPath()).toList(),
                    () -> "minecraft:fortress",
                    o -> o instanceof String str && ResourceLocation.tryParse(str) != null
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLACKLIST = BUILDER
            .comment("Structures to ignore")
            .defineList(
                    "structureBlacklist",
                    Stream.of(
                            BuiltinStructures.ANCIENT_CITY,
                            BuiltinStructures.BURIED_TREASURE,
                            BuiltinStructures.IGLOO,
                            BuiltinStructures.MINESHAFT,
                            BuiltinStructures.MINESHAFT_MESA,
                            BuiltinStructures.OCEAN_RUIN_COLD,
                            BuiltinStructures.OCEAN_RUIN_WARM,
                            BuiltinStructures.RUINED_PORTAL_DESERT,
                            BuiltinStructures.RUINED_PORTAL_JUNGLE,
                            BuiltinStructures.RUINED_PORTAL_MOUNTAIN,
                            BuiltinStructures.RUINED_PORTAL_NETHER,
                            BuiltinStructures.RUINED_PORTAL_OCEAN,
                            BuiltinStructures.RUINED_PORTAL_STANDARD,
                            BuiltinStructures.RUINED_PORTAL_SWAMP,
                            BuiltinStructures.SHIPWRECK,
                            BuiltinStructures.SHIPWRECK_BEACHED,
                            BuiltinStructures.STRONGHOLD,
                            BuiltinStructures.SWAMP_HUT,
                            BuiltinStructures.TRAIL_RUINS,
                            BuiltinStructures.VILLAGE_DESERT,
                            BuiltinStructures.VILLAGE_PLAINS,
                            BuiltinStructures.VILLAGE_SAVANNA,
                            BuiltinStructures.VILLAGE_SNOWY,
                            BuiltinStructures.VILLAGE_TAIGA
                    ).map(structure -> structure.location().getPath()).toList(),
                    () -> "minecraft:fortress",
                    o -> o instanceof String str && ResourceLocation.tryParse(str) != null
            );

    private static final ModConfigSpec.BooleanValue DISPLAY_ZONE_TEXT = BUILDER
            .comment("If a text should be displayed on screen when entering/leaving an adventure zone")
            .define("displayTitleOnZoneEntry", true);

    /**
     * Validates that the given object is a map of string keys to list of string values.
     */
    private static boolean validateStringMap(Object o) {
        if (!(o instanceof Config cfg)) return false;

        for (Config.Entry e : cfg.entrySet()) {
            Object v = e.getValue();
            if (!(v instanceof List<?> list)) return false;
            for (Object el : list) {
                if (!(el instanceof String)) return false;
            }
        }
        return true;
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @Override
    public int getZoneRadius() {
        return ZONE_RADIUS.get();
    }

    @Override
    public List<ResourceLocation> getBlockWhitelist() {
        return BLOCK_WHITELIST.get().stream()
                .map(ResourceLocation::parse)
                .toList();
    }

    @Override
    public List<ResourceLocation> getStructureWhitelist() {
        return STRUCTURE_WHITELIST.get().stream()
                .map(ResourceLocation::parse)
                .toList();
    }

    @Override
    public List<ResourceLocation> getStructureBlacklist() {
        return STRUCTURE_BLACKLIST.get().stream()
                .map(ResourceLocation::parse)
                .toList();
    }

    @Override
    public boolean displayZoneText() {
        return DISPLAY_ZONE_TEXT.get();
    }
}
