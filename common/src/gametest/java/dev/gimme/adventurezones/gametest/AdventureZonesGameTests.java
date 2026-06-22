package dev.gimme.adventurezones.gametest;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import dev.gimme.adventurezones.domain.config.ServerConfig.StructureRule;
import dev.gimme.adventurezones.infrastructure.ConfigTestSupport;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/**
 * Loader-agnostic game test bodies. Each {@code static void(GameTestHelper)} method is one test;
 * a test passes by calling {@link GameTestHelper#succeed()} and fails by throwing.
 *
 * <p>To add a test: write the method here, then wire it into {@code FabricGameTests} and
 * {@code NeoForgeGameTests}.
 */
public final class AdventureZonesGameTests {

    private AdventureZonesGameTests() {
    }

    /**
     * The shared {@link ServerConfig} is initialized by the loader and exposes its documented defaults,
     * exercising the cross-loader Forge Config API Port + night-config TOML parsing path on either loader.
     */
    public static void configDefaultsLoaded(GameTestHelper helper) {
        ServerConfig config = ServerConfig.INSTANCE;
        helper.assertTrue(config != null, "ServerConfig should be initialized by the loader");

        List<StructureRule> rules = config.getProtectedStructures();
        helper.assertFalse(rules.isEmpty(), "the default config should define at least one protected structure");
        helper.assertTrue(rules.stream().anyMatch(rule -> !rule.breachable()),
                "the defaults should include an always-protected (breachable=false) structure");
        helper.assertTrue(rules.stream().anyMatch(StructureRule::breachable),
                "the defaults should include a breachable structure (e.g. stronghold)");
        helper.assertTrue(rules.stream().anyMatch(rule -> !rule.canPlaceOn().isEmpty()),
                "the defaults should grant at least one per-structure canPlaceOn exception");
        helper.assertTrue(rules.stream().allMatch(rule -> rule.canBreak().isEmpty()),
                "the default per-structure canBreak allow-lists should be empty (breaking is the escape vector)");
        helper.succeed();
    }

    /**
     * Config values are live and per-structure: a change to a rule's {@code canBreak} is reflected through
     * {@link ServerConfig}, confirming the test-support handle binds to the loaded spec on either loader.
     */
    public static void protectedStructuresConfigurable(GameTestHelper helper) {
        List<? extends Config> original = ConfigTestSupport.PROTECTED_STRUCTURE.get();
        try {
            Config rule = TomlFormat.newConfig();
            rule.set("structures", "minecraft:stronghold");
            rule.set("breachable", true);
            rule.set("canBreak", List.of("diamond_pickaxe=obsidian"));

            ConfigTestSupport.PROTECTED_STRUCTURE.set(List.of(rule));

            List<StructureRule> rules = ServerConfig.INSTANCE.getProtectedStructures();
            helper.assertTrue(rules.size() == 1, "expected exactly one configured rule but got " + rules.size());
            StructureRule configured = rules.getFirst();
            helper.assertTrue(configured.breachable(), "configured rule should be breachable");
            helper.assertTrue("obsidian".equals(configured.canBreak().get("diamond_pickaxe")),
                    "per-structure canBreak should follow the configured value but was " + configured.canBreak());
        } finally {
            ConfigTestSupport.PROTECTED_STRUCTURE.set(original);
        }
        helper.succeed();
    }
}
