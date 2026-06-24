package dev.gimme.structureprotection.gametest;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.gimme.structureprotection.domain.BlockEdit;
import dev.gimme.structureprotection.domain.IdPattern;
import dev.gimme.structureprotection.domain.StructureRule;
import dev.gimme.structureprotection.domain.config.ServerConfig;
import dev.gimme.structureprotection.infrastructure.ConfigTestSupport;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

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
            helper.assertTrue("obsidian".equals(configured.canBreak().raw()),
                    "canBreak should follow the configured value but was " + configured.canBreak().raw());
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
            helper.assertTrue(".*".equals(rules.get(1).protect().raw()),
                    "the protect regex should follow the configured value but was " + rules.get(1).protect().raw());
        } finally {
            ConfigTestSupport.STRUCTURE_PROTECTION.set(original);
        }
        helper.succeed();
    }

    /**
     * The policy logic on {@link StructureRule} and {@link IdPattern} reads the way the docs describe, exercised directly
     * (no world needed): {@code appliesTo} honors colon-vs-path matching, {@code protects} combines the block pattern
     * with {@code protectStructural} + structural-ness, and the allow-lists override per edit kind.
     */
    public static void rulePolicy(GameTestHelper helper) {
        Identifier fortress = Identifier.fromNamespaceAndPath("minecraft", "fortress");
        Identifier endCity = Identifier.fromNamespaceAndPath("minecraft", "end_city");
        Identifier spawner = Identifier.fromNamespaceAndPath("minecraft", "spawner");
        Identifier stone = Identifier.fromNamespaceAndPath("minecraft", "stone");
        Identifier pot = Identifier.fromNamespaceAndPath("minecraft", "decorated_pot");

        // A shape rule (path regex) that still lets players break decorated pots to loot them.
        StructureRule shape = new StructureRule(
                IdPattern.of("fortress"), IdPattern.NONE, true, false,
                IdPattern.NONE, IdPattern.of("decorated_pot"));

        helper.assertTrue(shape.appliesTo(fortress), "a path regex should match the fortress id");
        helper.assertFalse(shape.appliesTo(endCity), "a fortress rule should not apply to end_city");
        helper.assertTrue(shape.protects(stone, true), "protectStructural should protect a structural block");
        helper.assertFalse(shape.protects(spawner, false),
                "protectStructural should leave a non-structural block editable");
        helper.assertTrue(shape.allowsBreaking(pot), "canBreak should allow breaking the decorated pot");
        helper.assertFalse(shape.allowsPlacing(pot), "an empty canPlace should allow nothing");

        // A targeted rule that protects one named block, matched against the full namespaced id.
        StructureRule named = new StructureRule(
                IdPattern.of("minecraft:fortress"), IdPattern.of("spawner"), false, false,
                IdPattern.NONE, IdPattern.NONE);

        helper.assertTrue(named.appliesTo(fortress), "a namespaced regex should match the full id");
        helper.assertTrue(named.protects(spawner, false), "the protect regex should protect the named block");
        helper.assertFalse(named.protects(stone, true), "the protect regex should not protect an unlisted block");

        // The empty pattern matches nothing.
        helper.assertFalse(IdPattern.NONE.matches(stone), "the empty pattern should match nothing");
        helper.assertTrue(IdPattern.NONE.isEmpty(), "the empty pattern should report empty");

        helper.succeed();
    }

    /**
     * A block's structural-ness is a property of its type, judged by its default state, identically for placing and
     * breaking. Solid blocks are structural; non-physical decoration is not; and a fence gate is structural by its
     * default (closed) state, even though an open gate blocks no motion — so it cannot be opened to dodge shape
     * protection. {@code BlockEdit} enforces this by construction: it holds a block, never a particular block state.
     */
    public static void blockEditStructural(GameTestHelper helper) {
        helper.assertTrue(BlockEdit.breaking(Blocks.STONE).isStructural(),
                "a solid block should be structural");
        helper.assertFalse(BlockEdit.breaking(Blocks.TORCH).isStructural(),
                "a non-physical decoration block should not be structural");
        helper.assertTrue(BlockEdit.breaking(Blocks.OAK_FENCE_GATE).isStructural(),
                "a fence gate should be structural by its default (closed) state, so it can't be opened to dodge it");

        // Placing and breaking agree: structural-ness is a property of the block, not the edit kind.
        helper.assertTrue(BlockEdit.placing(Blocks.STONE).isStructural() == BlockEdit.breaking(Blocks.STONE).isStructural(),
                "placing and breaking should judge structural-ness identically");

        BlockEdit place = BlockEdit.placing(Blocks.STONE);
        helper.assertTrue(place.isPlacing() && !place.isBreaking(), "a placing edit should report placing");
        BlockEdit breakEdit = BlockEdit.breaking(Blocks.STONE);
        helper.assertTrue(breakEdit.isBreaking() && !breakEdit.isPlacing(), "a breaking edit should report breaking");

        helper.succeed();
    }
}
