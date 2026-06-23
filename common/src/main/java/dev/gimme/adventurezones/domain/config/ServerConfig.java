package dev.gimme.adventurezones.domain.config;

import java.util.List;
import java.util.Map;

public abstract class ServerConfig {

    public static ServerConfig INSTANCE;

    /**
     * The structure rules: each applies to a set of structures and carries whether it protects, its breach mode, and
     * its place/break allow-lists. Includes non-protecting "library" rules, which only contribute allow-lists.
     */
    public abstract List<StructureRule> getStructureRules();

    /**
     * A rule for a set of structures.
     *
     * @param structures  a regex matching the structures this rule applies to
     * @param isProtected if {@code true} (the default), a matching structure is in scope and its blocks are protected.
     *                    If {@code false}, the rule never puts a structure into scope on its own; it only contributes
     *                    its allow-lists to structures that some other rule protects. Use {@code false} for a shared
     *                    "library" rule, e.g. a {@code ".*"} base granting common exceptions to every protected
     *                    structure without having to repeat them per structure.
     * @param breachable  if {@code false}, the structure's blocks can never be edited; if {@code true}, they can be
     *                    edited only while the player stands outside this structure's own pieces (breach from outside,
     *                    locked inside) — standing inside an unrelated protected structure does not block it. Only
     *                    meaningful on a protecting rule.
     * @param protectsOnlyPhysical if {@code false} (the default), every block in scope is protected; if {@code true},
     *                    this rule guards only blocks that block motion (walls, floors, stairs, fences, doors…) and
     *                    permits edits to non-physical blocks such as torches, carpets, and flowers. "Physical" means
     *                    {@code BlockState#blocksMotion()}: a non-empty, motion-blocking collision box. Like the other
     *                    permissions it composes by union — a rule permitting a non-physical edit allows it regardless
     *                    of what other matching rules say. Only meaningful on a protecting rule.
     * @param canPlaceOn  exceptions for this rule: maps an item-name regex to a block-name regex of blocks the item may
     *                    still be placed on
     * @param canBreak    exceptions for this rule: maps an item-name regex to a block-name regex of blocks the item may
     *                    still break
     */
    public record StructureRule(
            String structures,
            boolean isProtected,
            boolean breachable,
            boolean protectsOnlyPhysical,
            Map<String, String> canPlaceOn,
            Map<String, String> canBreak
    ) {
    }
}
