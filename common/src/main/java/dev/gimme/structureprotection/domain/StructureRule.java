package dev.gimme.structureprotection.domain;

import net.minecraft.resources.Identifier;

/**
 * One rule of the protection policy: a set of structures, what is protected inside them, and the exceptions that survive.
 * A block at a matched position is <em>protected</em> for an edit if {@link #protects(Identifier, boolean)} holds; a
 * protected block is blocked unless the relevant allow-list ({@link #allowsPlacing}/{@link #allowsBreaking}) permits it,
 * or — when breaking — the rule is {@link #breachable()} and the player stands outside this structure's own pieces. A
 * structure's effective policy is the union of every rule that {@link #appliesTo} it; a rule that protects nothing acts
 * as a "library" rule, contributing only its allow-lists.
 *
 * @param structures        which structures this rule applies to.
 * @param protect           block names this rule protects; {@code ".*"} protects everything, {@link IdPattern#NONE}
 *                          protects nothing. Composes additively with {@code protectStructural}.
 * @param protectStructural if {@code true}, additionally protect every <em>structural</em> block (walls, floors, stairs,
 *                          fences, doors…) — the structure's shape — while leaving non-physical blocks such as torches,
 *                          carpets, and flowers editable. See {@link BlockEdit#isStructural()} for what counts as
 *                          structural; it is a property of the block being placed or broken, not of the block it rests
 *                          on.
 * @param breachable        if {@code true}, a block this rule protects may still be <em>broken</em> while the player
 *                          stands outside this structure's own pieces (breach in from outside, locked inside) — standing
 *                          inside an unrelated protected structure does not grant it. It never permits placing, only
 *                          breaking a way in. Only meaningful where the rule protects something.
 * @param canPlace          block names that may still be placed inside the structure, overriding protection.
 *                          {@link IdPattern#NONE} allows nothing.
 * @param canBreak          block names that may still be broken inside the structure, overriding protection.
 *                          {@link IdPattern#NONE} allows nothing (breaking is the main way to escape a protected
 *                          structure).
 */
public record StructureRule(
        IdPattern structures,
        IdPattern protect,
        boolean protectStructural,
        boolean breachable,
        IdPattern canPlace,
        IdPattern canBreak
) {

    /** Whether this rule applies to the structure with the given id. */
    public boolean appliesTo(Identifier structure) {
        return structures.matches(structure);
    }

    /**
     * Whether this rule protects the given block: its {@link #protect()} pattern matches the block, or it protects the
     * structure's shape ({@link #protectStructural()}) and the block is structural.
     */
    public boolean protects(Identifier block, boolean structural) {
        return protect.matches(block) || (protectStructural && structural);
    }

    /** Whether this rule allows placing the given block, overriding protection. */
    public boolean allowsPlacing(Identifier block) {
        return canPlace.matches(block);
    }

    /** Whether this rule allows breaking the given block, overriding protection. */
    public boolean allowsBreaking(Identifier block) {
        return canBreak.matches(block);
    }
}
