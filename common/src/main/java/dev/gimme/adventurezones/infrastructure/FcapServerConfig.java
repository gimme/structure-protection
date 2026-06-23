package dev.gimme.adventurezones.infrastructure;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import dev.gimme.adventurezones.domain.util.Constants;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * {@link ServerConfig} backed by the NeoForge config system. The spec is defined once here in the common module and
 * registered per loader (natively on NeoForge, via Forge Config API Port on Fabric) as a {@code COMMON} config.
 */
public class FcapServerConfig extends ServerConfig {

    public static final String FILE_NAME = Constants.MOD_ID + "-server.toml";

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec.ConfigValue<List<? extends Config>> STRUCTURE_PROTECTION = BUILDER
            .comment("""
                    Rules controlling which structures' generated pieces are protected from block placing/breaking and
                    what is still allowed inside them. A structure's policy is the union of every rule that matches it,
                    so common exceptions can live in one shared rule instead of being repeated. Properties:
                      "structures": A regex matching the structures this rule applies to.
                      "protect" (optional): A regex matching block names this rule protects from placing/breaking. ".*"
                        protects everything; the default (empty) protects nothing by itself. Combines additively with
                        "protectStructural".
                      "protectStructural" (optional, default false): If true, additionally protect every block that
                        blocks motion (walls, floors, stairs, fences, doors…) — the structure's shape — while leaving
                        non-physical blocks such as torches, carpets, and flowers editable. For placement it is judged by
                        the block being placed, not the block it rests on.
                      "breachable" (optional, default false): If true, a block this rule protects may still be edited
                        while the player stands outside this structure's own pieces (breach a wall from outside, but you
                        cannot dig once inside; standing in an unrelated protected structure does not grant it). Use this
                        for sealed structures with no natural entrance, e.g. strongholds.
                      "canPlace" (optional): A regex matching block names that may still be placed inside the structure,
                        overriding protection. E.g. "ladder" so players can climb/escape.
                      "canBreak" (optional): A regex matching block names that may still be broken inside the structure,
                        overriding protection. Empty by default, since breaking is the main way to escape a protected
                        structure. E.g. "decorated_pot" to let players loot pots without otherwise touching the shape.
                    A rule that protects nothing (no "protect"/"protectStructural") only contributes its allow-lists, so a
                    shared ".*" rule can grant common exceptions without protecting every structure. If a regex contains
                    a colon (":") it is matched against the full namespaced id (e.g. minecraft:fortress); otherwise only
                    against the path.
                    """)
            .defineList(
                    "structureProtection",
                    () -> {
                        // Shared base: a few navigation/loot exceptions granted in every matched structure, so the
                        // protecting rules below need not repeat them. It protects nothing on its own.
                        Config base = TomlFormat.newConfig();
                        base.set("structures", ".*");
                        base.set("canPlace", "ladder");
                        base.set("canBreak", "decorated_pot|ladder|gilded_blackstone|gold_block|.*_ore");

                        // Guard the structure's shape, not its decoration: torches, carpets, flowers, etc. stay
                        // editable because they do not block motion.
                        Config structural = TomlFormat.newConfig();
                        structural.set("structures", "bastion_remnant|end_city|fortress|jungle_pyramid|mansion|pillager_outpost");
                        structural.set("protectStructural", true);

                        // Sealed structures with no natural entrance: breach a wall from outside, locked once inside.
                        Config sealed = TomlFormat.newConfig();
                        sealed.set("structures", "ancient_city|stronghold|trial_chambers");
                        sealed.set("protectStructural", true);
                        sealed.set("breachable", true);

                        return List.of(base, structural, sealed);
                    },
                    () -> {
                        Config cfg = TomlFormat.newConfig();
                        cfg.set("structures", "minecraft:fortress");
                        cfg.set("protectStructural", true);
                        return cfg;
                    },
                    FcapServerConfig::validateStructureProtection
            );

    /**
     * Validates that the given object is a valid structure rule.
     */
    private static boolean validateStructureProtection(Object o) {
        if (!(o instanceof Config cfg)) return false;

        if (!(cfg.get("structures") instanceof String s) || !isValidRegex(s)) return false;

        if (!isOptionalRegex(cfg, "protect")) return false;
        if (!isOptionalRegex(cfg, "canPlace")) return false;
        if (!isOptionalRegex(cfg, "canBreak")) return false;

        if (cfg.contains("protectStructural") && !(cfg.get("protectStructural") instanceof Boolean)) return false;
        return !cfg.contains("breachable") || cfg.get("breachable") instanceof Boolean;
    }

    /**
     * Validates that an optional key is either absent or a valid-regex string.
     */
    private static boolean isOptionalRegex(Config cfg, String key) {
        if (!cfg.contains(key)) return true;
        return cfg.get(key) instanceof String s && isValidRegex(s);
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
    public List<StructureRule> getStructureRules() {
        return STRUCTURE_PROTECTION.get().stream()
                .map(cfg -> new StructureRule(
                        cfg.get("structures"),
                        str(cfg, "protect"),
                        bool(cfg, "protectStructural"),
                        bool(cfg, "breachable"),
                        str(cfg, "canPlace"),
                        str(cfg, "canBreak")))
                .toList();
    }

    /**
     * Reads a string config value, defaulting to empty when absent.
     */
    private static String str(Config cfg, String key) {
        return cfg.get(key) instanceof String s ? s : "";
    }

    /**
     * Reads a boolean config value, defaulting to false when absent.
     */
    private static boolean bool(Config cfg, String key) {
        return cfg.get(key) instanceof Boolean b && b;
    }
}
