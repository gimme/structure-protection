package dev.gimme.structureprotection.gametest;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.gimme.structureprotection.domain.config.ServerConfig;
import dev.gimme.structureprotection.domain.config.ServerConfig.StructureRule;
import dev.gimme.structureprotection.infrastructure.ConfigTestSupport;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/**
 * Loader-agnostic game test bodies. Each {@code static void(GameTestHelper)} method is one test;
 * a test passes by calling {@link GameTestHelper#succeed()} and fails by throwing.
 *
 * <p>To add a test: write the method here, then wire it into {@code FabricGameTests} and
 * {@code NeoForgeGameTests}.
 */
public final class StructureProtectionGameTests {

    private StructureProtectionGameTests() {
    }

    /**
     * The shared {@link ServerConfig} is initialized by the loader and exposes its documented defaults,
     * exercising the cross-loader Forge Config API Port + night-config TOML parsing path on either loader.
     */
    public static void configDefaultsLoaded(GameTestHelper helper) {
        ServerConfig config = ServerConfig.INSTANCE;
        helper.assertTrue(config != null, "ServerConfig should be initialized by the loader");

        List<StructureRule> rules = config.getStructureRules();
        helper.assertFalse(rules.isEmpty(), "the default config should define at least one structure rule");
        helper.assertTrue(rules.stream().anyMatch(StructureRule::protectStructural),
                "the defaults should protect at least one structure's shape (protectStructural)");
        helper.assertTrue(rules.stream().anyMatch(rule -> rule.protectStructural() && !rule.breachable()),
                "the defaults should include an always-protected (non-breachable) structure");
        helper.assertTrue(rules.stream().anyMatch(StructureRule::breachable),
                "the defaults should include a breachable structure (e.g. stronghold)");
        helper.assertTrue(rules.stream().anyMatch(rule ->
                        rule.protect().isEmpty() && !rule.protectStructural()
                                && !rule.canBreak().isEmpty()),
                "the defaults should include a non-protecting base rule contributing a canBreak allow-list");
        helper.assertTrue(rules.stream().filter(StructureRule::protectStructural)
                        .allMatch(rule -> rule.canPlace().isEmpty() && rule.canBreak().isEmpty()),
                "the protecting rules should carry no own allow-lists (exceptions live on the shared base)");
        helper.succeed();
    }

    /**
     * Config values are live and per-structure: a change to a rule's {@code canBreak} is reflected through
     * {@link ServerConfig}, confirming the test-support handle binds to the loaded spec on either loader.
     */
    public static void protectedStructuresConfigurable(GameTestHelper helper) {
        List<? extends Config> original = ConfigTestSupport.STRUCTURE_PROTECTION.get();
        try {
            Config rule = TomlFormat.newConfig();
            rule.set("structures", "minecraft:stronghold");
            rule.set("breachable", true);
            rule.set("canBreak", "obsidian");

            ConfigTestSupport.STRUCTURE_PROTECTION.set(List.of(rule));

            List<StructureRule> rules = ServerConfig.INSTANCE.getStructureRules();
            helper.assertTrue(rules.size() == 1, "expected exactly one configured rule but got " + rules.size());
            StructureRule configured = rules.getFirst();
            helper.assertTrue(configured.breachable(), "configured rule should be breachable");
            helper.assertTrue("obsidian".equals(configured.canBreak()),
                    "canBreak should follow the configured value but was " + configured.canBreak());
            helper.assertFalse(configured.protectStructural(),
                    "a rule with no protectStructural key should default to false");
            helper.assertTrue(configured.protect().isEmpty(),
                    "a rule with no protect key should default to protecting nothing");
        } finally {
            ConfigTestSupport.STRUCTURE_PROTECTION.set(original);
        }
        helper.succeed();
    }

    /**
     * The optional {@code protectStructural} flag and {@code protect} regex round-trip through {@link ServerConfig}:
     * present values are honored, and absent keys fall back to the documented defaults (false / empty).
     */
    public static void protectStructuralConfigurable(GameTestHelper helper) {
        List<? extends Config> original = ConfigTestSupport.STRUCTURE_PROTECTION.get();
        try {
            Config structural = TomlFormat.newConfig();
            structural.set("structures", "minecraft:ancient_city");
            structural.set("protectStructural", true);

            Config full = TomlFormat.newConfig();
            full.set("structures", "minecraft:fortress");
            full.set("protect", ".*");

            ConfigTestSupport.STRUCTURE_PROTECTION.set(List.of(structural, full));

            List<StructureRule> rules = ServerConfig.INSTANCE.getStructureRules();
            helper.assertTrue(rules.size() == 2, "expected exactly two configured rules but got " + rules.size());
            helper.assertTrue(rules.getFirst().protectStructural(),
                    "a rule with protectStructural=true should report it");
            helper.assertTrue(rules.getFirst().protect().isEmpty(),
                    "a rule with no protect key should default to empty");
            helper.assertFalse(rules.get(1).protectStructural(),
                    "a rule with no protectStructural key should default to false");
            helper.assertTrue(".*".equals(rules.get(1).protect()),
                    "the protect regex should follow the configured value but was " + rules.get(1).protect());
        } finally {
            ConfigTestSupport.STRUCTURE_PROTECTION.set(original);
        }
        helper.succeed();
    }
}
