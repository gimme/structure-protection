package dev.gimme.adventurezones.infrastructure;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import dev.gimme.adventurezones.domain.util.Constants;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * {@link ServerConfig} backed by the NeoForge config system. The spec is defined once here in the common module and
 * registered per loader (natively on NeoForge, via Forge Config API Port on Fabric) as a {@code COMMON} config.
 */
public class FcapServerConfig extends ServerConfig {

    public static final String FILE_NAME = Constants.MOD_ID + "-server.toml";

    /**
     * Separates the item regex from the block regex in a single allow-list entry, e.g. {@code "ladder|torch=.*"}.
     */
    private static final String ALLOW_LIST_SEPARATOR = "=";

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec.ConfigValue<List<? extends Config>> PROTECTED_STRUCTURE = BUILDER
            .comment("""
                    Structures whose generated pieces are protected from block placing/breaking. Each entry is a set of
                    rules for the structures it matches. Properties:
                      "structures": A regex matching the structures this rule applies to.
                      "breachable": If false (default), the structure's blocks can never be placed/broken by
                        non-creative players. If true, they can be edited only while the player stands outside all
                        protected pieces (you can breach a wall from outside, but cannot dig once inside). Use this for
                        sealed structures with no natural entrance, e.g. strongholds.
                      "canPlaceOn" (optional): Exceptions for this rule. A list of "<itemRegex>=<blockRegex>" entries;
                        each lets the matched item still be placed on the matched block inside the structure.
                        E.g. ["ladder|torch=.*"].
                      "canBreak" (optional): Exceptions for this rule. A list of "<itemRegex>=<blockRegex>" entries;
                        each lets the matched item still break the matched block inside the structure. Empty by default,
                        since breaking is the main way to escape a protected structure.
                    If a regex contains a colon (":") it is matched against the full namespaced id
                    (e.g. minecraft:fortress); otherwise only against the path.
                    """)
            .defineList(
                    "protectedStructure",
                    () -> {
                        Config alwaysProtected = TomlFormat.newConfig();
                        alwaysProtected.set("structures", "fortress|bastion_remnant|end_city|mansion|.*_pyramid|ancient_city|trial_chambers|pillager_outpost");
                        alwaysProtected.set("breachable", false);
                        alwaysProtected.set("canPlaceOn", lightSources());
                        alwaysProtected.set("canBreak", List.of());

                        Config stronghold = TomlFormat.newConfig();
                        stronghold.set("structures", "stronghold");
                        stronghold.set("breachable", true);
                        stronghold.set("canPlaceOn", lightSources());
                        stronghold.set("canBreak", List.of());

                        return List.of(alwaysProtected, stronghold);
                    },
                    () -> {
                        Config cfg = TomlFormat.newConfig();
                        cfg.set("structures", "minecraft:fortress");
                        cfg.set("breachable", false);
                        cfg.set("canPlaceOn", lightSources());
                        cfg.set("canBreak", List.of());
                        return cfg;
                    },
                    FcapServerConfig::validateProtectedStructure
            );

    /**
     * The default {@code canPlaceOn} allow-list: light/navigation sources may be placed on anything.
     */
    private static List<String> lightSources() {
        return List.of(".*torch|.*lantern" + ALLOW_LIST_SEPARATOR + ".*");
    }

    /**
     * Validates that the given object is a valid protected-structure rule.
     */
    private static boolean validateProtectedStructure(Object o) {
        if (!(o instanceof Config cfg)) return false;

        Object structures = cfg.get("structures");
        if (!(structures instanceof String s) || !isValidRegex(s)) return false;

        if (!(cfg.get("breachable") instanceof Boolean)) return false;

        if (cfg.contains("canPlaceOn") && !validateAllowList(cfg.get("canPlaceOn"))) return false;
        return !cfg.contains("canBreak") || validateAllowList(cfg.get("canBreak"));
    }

    /**
     * Validates that the given object is a list of "&lt;itemRegex&gt;=&lt;blockRegex&gt;" entries.
     */
    private static boolean validateAllowList(Object o) {
        if (!(o instanceof List<?> list)) return false;

        for (Object element : list) {
            if (!(element instanceof String entry)) return false;
            int sep = entry.indexOf(ALLOW_LIST_SEPARATOR);
            if (sep < 0) return false;
            if (!isValidRegex(entry.substring(0, sep))) return false;
            if (!isValidRegex(entry.substring(sep + ALLOW_LIST_SEPARATOR.length()))) return false;
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
    public List<StructureRule> getProtectedStructures() {
        return PROTECTED_STRUCTURE.get().stream()
                .map(cfg -> new StructureRule(
                        cfg.get("structures"),
                        cfg.get("breachable") instanceof Boolean b && b,
                        parseAllowList(cfg, "canPlaceOn"),
                        parseAllowList(cfg, "canBreak")
                ))
                .toList();
    }

    /**
     * Reads a list of "&lt;itemRegex&gt;=&lt;blockRegex&gt;" entries into an item-regex to block-regex map. Entries
     * sharing an item regex are unioned. Returns an empty map if the key is absent.
     */
    private static Map<String, String> parseAllowList(Config cfg, String key) {
        Object value = cfg.get(key);
        if (!(value instanceof List<?> list)) return Map.of();

        Map<String, String> map = new LinkedHashMap<>();
        for (Object element : list) {
            if (!(element instanceof String entry)) continue;
            int sep = entry.indexOf(ALLOW_LIST_SEPARATOR);
            if (sep < 0) continue;
            String item = entry.substring(0, sep);
            String block = entry.substring(sep + ALLOW_LIST_SEPARATOR.length());
            map.merge(item, block, (a, b) -> a + "|" + b);
        }
        return map;
    }
}
