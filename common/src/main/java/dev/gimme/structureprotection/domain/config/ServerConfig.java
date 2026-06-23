package dev.gimme.structureprotection.domain.config;

import java.util.List;

public abstract class ServerConfig {

    public static ServerConfig INSTANCE;

    /**
     * The structure rules: each applies to a set of structures and carries what it protects and what it still allows.
     * A rule that protects nothing acts as a "library" rule, contributing only its allow-lists to structures that
     * other rules protect.
     */
    public abstract List<StructureRule> getStructureRules();

    /**
     * A rule for a set of structures. A block at a matched position is <em>protected</em> for an edit if {@code protect}
     * matches its name <em>or</em> {@code protectStructural} is set and the block blocks motion; a protected block is
     * blocked unless the relevant allow-list ({@code canPlace}/{@code canBreak}) matches it, or — when breaking — a
     * breachable rule lets the player break in from outside. A structure's effective policy is the union of every rule
     * that matches it.
     *
     * @param structures        a regex matching the structures this rule applies to
     * @param protect           a regex matching block names this rule protects; {@code ".*"} protects everything, an
     *                          empty string protects nothing. Composes additively with {@code protectStructural}.
     * @param protectStructural if {@code true}, additionally protect every block that blocks motion (walls, floors,
     *                          stairs, fences, doors…) — the structure's shape — while leaving non-physical blocks such
     *                          as torches, carpets, and flowers editable. "Blocks motion" means
     *                          {@code BlockState#blocksMotion()}: a non-empty, motion-blocking collision box. For
     *                          placement it is judged by the block being placed, not the block it rests on.
     * @param breachable        if {@code true}, a block this rule protects may still be <em>broken</em> while the player
     *                          stands outside this structure's own pieces (breach in from outside, locked inside) —
     *                          standing inside an unrelated protected structure does not grant it. It never permits
     *                          placing, only breaking a way in. Only meaningful where the rule protects something.
     * @param canPlace          a regex matching block names that may still be placed inside the structure, overriding
     *                          protection. Empty allows nothing.
     * @param canBreak          a regex matching block names that may still be broken inside the structure, overriding
     *                          protection. Empty allows nothing (breaking is the main way to escape a protected
     *                          structure).
     */
    public record StructureRule(
            String structures,
            String protect,
            boolean protectStructural,
            boolean breachable,
            String canPlace,
            String canBreak
    ) {
    }
}
