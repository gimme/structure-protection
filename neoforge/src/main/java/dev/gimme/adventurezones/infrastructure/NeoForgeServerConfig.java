package dev.gimme.adventurezones.infrastructure;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NeoForgeServerConfig extends ServerConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue DISPLAY_MODE_TEXT = BUILDER
            .comment("If a text should be displayed on screen when entering/leaving adventure mode")
            .define("displayTextOnModeSwitch", true);

    private static final ModConfigSpec.IntValue COMBAT_MODE_SECONDS = BUILDER
            .comment("How long (in seconds) combat mode should last after a player attacks or is attacked. 0 to disable.")
            .defineInRange("combatModeSeconds", 10, 0, 300);

    private static final ModConfigSpec.ConfigValue<List<? extends Config>> ZONE_SPECS = BUILDER
            .comment("""
                    Adventure zone specification. Defines what chunks should be considered part of the zone. Properties:
                      "structures": A regex pattern matching structure that have to be present in the chunk.
                      "radius": The extra radius (in blocks) to include around the targeted chunks.
                      "blocks" (optional): A regex pattern matching block names required to be present in the chunk section (16x16x16 areas).
                      "aboveY" (optional): Minimum Y level for the zone to exist.
                      "belowY" (optional): Maximum Y level for the zone to exist.
                    """)
            .defineList(
                    "zone",
                    () -> {
                        Config overworld = TomlFormat.newConfig();
                        overworld.set("structures", "pillager_outpost|mansion");
                        overworld.set("radius", 5);
                        overworld.set("aboveY", 62);

                        Config pyramids = TomlFormat.newConfig();
                        pyramids.set("structures", "minecraft:.*_pyramid");
                        pyramids.set("radius", 8);
                        pyramids.set("aboveY", 62);

                        Config pyramidChests = TomlFormat.newConfig();
                        pyramidChests.set("structures", "minecraft:.*_pyramid");
                        pyramidChests.set("radius", 5);
                        pyramidChests.set("aboveY", 40);
                        pyramidChests.set("blocks", "chest");

                        Config nether = TomlFormat.newConfig();
                        nether.set("structures", "fortress|bastion_remnant|end_city");
                        nether.set("radius", 16);

                        return List.of(overworld, pyramids, pyramidChests, nether);
                    },
                    () -> {
                        Config cfg = TomlFormat.newConfig();
                        cfg.set("structures", "minecraft:fortress");
                        cfg.set("radius", 8);
                        return cfg;
                    },
                    NeoForgeServerConfig::validateZoneConfig
            );

    /**
     * Validates that the given object is a valid zone config.
     */
    private static boolean validateZoneConfig(Object o) {
        if (!(o instanceof Config cfg)) return false;

        String structures = cfg.get("structures");
        if (!isValidRegex(structures)) return false;

        cfg.getInt("radius");

        if (cfg.contains("blocks")) {
            String blocks = cfg.get("blocks");
            if (!isValidRegex(blocks)) return false;
        }

        if (cfg.contains("aboveY")) {
            cfg.getInt("aboveY");
        }

        if (cfg.contains("belowY")) {
            cfg.getInt("belowY");
        }

        return true;
    }

    /**
     * Checks if the given string is a valid regex pattern.
     */
    private static boolean isValidRegex(@NotNull String regex) {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException ex) {
            return false;
        }
        return true;
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @Override
    public boolean displayModeText() {
        return DISPLAY_MODE_TEXT.get();
    }

    @Override
    public int getCombatModeSeconds() {
        return COMBAT_MODE_SECONDS.get();
    }

    @Override
    public List<ZoneConfig> getZoneConfigs() {
        return ZONE_SPECS.get().stream()
                .map(cfg -> new ZoneConfig(
                        cfg.get("structures"),
                        cfg.getInt("radius"),
                        cfg.contains("blocks") ? cfg.get("blocks") : null,
                        cfg.contains("aboveY") ? cfg.getInt("aboveY") : null,
                        cfg.contains("belowY") ? cfg.getInt("belowY") : null
                ))
                .toList();
    }

    @Override
    public int getMaxZoneRadius() {
        return getZoneConfigs().stream()
                .map(ZoneConfig::radius)
                .max(Integer::compareTo)
                .orElse(0);
    }
}
