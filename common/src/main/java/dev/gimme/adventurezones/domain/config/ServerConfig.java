package dev.gimme.adventurezones.domain.config;

import java.util.List;
import java.util.Map;

public abstract class ServerConfig {

    public static ServerConfig INSTANCE;

    /**
     * The protection rules: each applies to a set of structures and carries its own breach mode and place/break
     * allow-lists.
     */
    public abstract List<StructureRule> getProtectedStructures();

    /**
     * A protection rule for a set of structures.
     *
     * @param structures a regex matching the structures this rule applies to
     * @param breachable if {@code false}, the structure's blocks can never be edited; if {@code true}, they can be
     *                   edited only while the player stands outside all protected pieces (breach from outside, locked
     *                   inside)
     * @param canPlaceOn exceptions for this rule: maps an item-name regex to a block-name regex of blocks the item may
     *                   still be placed on
     * @param canBreak   exceptions for this rule: maps an item-name regex to a block-name regex of blocks the item may
     *                   still break
     */
    public record StructureRule(
            String structures,
            boolean breachable,
            Map<String, String> canPlaceOn,
            Map<String, String> canBreak
    ) {
    }
}
