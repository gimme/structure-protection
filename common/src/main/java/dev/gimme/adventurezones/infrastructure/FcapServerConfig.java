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

    static final ModConfigSpec.ConfigValue<List<? extends Config>> STRUCTURE_PROTECTION = BUILDER
            .comment("""
                    Rules controlling which structures' generated pieces are protected from block placing/breaking and
                    what is still allowed inside them. A structure's allowed edits are the union of every rule that
                    matches it, so common exceptions can live in one shared rule instead of being repeated. Properties:
                      "structures": A regex matching the structures this rule applies to.
                      "protected": If true (default), a matching structure is in scope and its blocks are protected. If
                        false, the rule never protects anything on its own; it only contributes its allow-lists to
                        structures that some other rule protects. Use a non-protecting ".*" rule as a shared base so you
                        do not have to repeat common exceptions in every structure. A ".*" rule must be non-protecting,
                        since most structures should not be touched at all.
                      "breachable": If false (default), the structure's blocks can never be placed/broken by
                        non-creative players. If true, they can be edited only while the player stands outside this
                        structure's own pieces (you can breach a wall from outside, but cannot dig once inside; standing
                        in an unrelated protected structure does not block it). Use this for sealed structures with no
                        natural entrance, e.g. strongholds. Only meaningful on a protecting rule.
                      "protectsOnlyPhysical": If false (default), every block in scope is protected. If true, this rule
                        guards only blocks that block motion (walls, floors, stairs, fences, doors…) and leaves
                        non-physical blocks such as torches, carpets, and flowers freely editable — useful when the
                        point is to preserve the structure's shape rather than every decoration. Only meaningful on a
                        protecting rule.
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
                    "structureProtection",
                    () -> {
                        // Shared base: light/navigation sources may be placed in any protected structure, so the
                        // protecting rules below need not repeat it. Non-protecting, so it never pulls a structure
                        // into scope on its own.
                        Config base = TomlFormat.newConfig();
                        base.set("structures", ".*");
                        base.set("protected", false);
                        base.set("canPlaceOn", lightSources());

                        Config alwaysProtected = TomlFormat.newConfig();
                        alwaysProtected.set("structures", "fortress|bastion_remnant|end_city|mansion|.*_pyramid|ancient_city|trial_chambers|pillager_outpost");
                        alwaysProtected.set("protected", true);
                        alwaysProtected.set("breachable", false);
                        // Guard the structure's shape, not its decoration: torches, carpets, flowers, etc. stay editable.
                        alwaysProtected.set("protectsOnlyPhysical", true);
                        alwaysProtected.set("canBreak", List.of());

                        Config stronghold = TomlFormat.newConfig();
                        stronghold.set("structures", "stronghold");
                        stronghold.set("protected", true);
                        stronghold.set("breachable", true);
                        stronghold.set("protectsOnlyPhysical", true);
                        stronghold.set("canBreak", List.of());

                        return List.of(base, alwaysProtected, stronghold);
                    },
                    () -> {
                        Config cfg = TomlFormat.newConfig();
                        cfg.set("structures", "minecraft:fortress");
                        cfg.set("protected", true);
                        cfg.set("breachable", false);
                        cfg.set("canPlaceOn", lightSources());
                        cfg.set("canBreak", List.of());
                        return cfg;
                    },
                    FcapServerConfig::validateStructureProtection
            );

    /**
     * The default {@code canPlaceOn} allow-list: light/navigation sources may be placed on anything.
     */
    private static List<String> lightSources() {
        return List.of(".*torch|.*lantern" + ALLOW_LIST_SEPARATOR + ".*");
    }

    /**
     * Validates that the given object is a valid structure rule.
     */
    private static boolean validateStructureProtection(Object o) {
        if (!(o instanceof Config cfg)) return false;

        Object structures = cfg.get("structures");
        if (!(structures instanceof String s) || !isValidRegex(s)) return false;

        if (cfg.contains("protected") && !(cfg.get("protected") instanceof Boolean)) return false;
        if (cfg.contains("breachable") && !(cfg.get("breachable") instanceof Boolean)) return false;
        if (cfg.contains("protectsOnlyPhysical") && !(cfg.get("protectsOnlyPhysical") instanceof Boolean)) return false;

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
    public List<StructureRule> getStructureRules() {
        return STRUCTURE_PROTECTION.get().stream()
                .map(cfg -> {
                    Object protectedVal = cfg.get("protected");
                    boolean isProtected = !(protectedVal instanceof Boolean pb) || pb; // absent defaults to protected
                    boolean breachable = cfg.get("breachable") instanceof Boolean bb && bb;
                    boolean protectsOnlyPhysical = cfg.get("protectsOnlyPhysical") instanceof Boolean pp && pp;
                    return new StructureRule(
                            cfg.get("structures"),
                            isProtected,
                            breachable,
                            protectsOnlyPhysical,
                            parseAllowList(cfg, "canPlaceOn"),
                            parseAllowList(cfg, "canBreak"));
                })
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
