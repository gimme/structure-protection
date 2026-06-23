package dev.gimme.adventurezones.domain;

import dev.gimme.adventurezones.domain.config.ServerConfig;
import dev.gimme.adventurezones.domain.config.ServerConfig.StructureRule;
import dev.gimme.adventurezones.domain.util.Identifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless queries against the live {@link StructureManager}: decides whether a position sits inside a generated piece
 * of a configured, protected structure. Bounds come from each piece's bounding box, so protection follows the real
 * structure shape rather than chunk/radius math.
 */
public final class ProtectedStructures {

    private ProtectedStructures() {
    }

    /**
     * A structure with a generated piece at the queried position, paired with the configured rules that match it
     * (including non-protecting "library" rules).
     */
    public record Match(Structure structure, List<StructureRule> rules) {
    }

    /**
     * Returns, for each structure with a generated piece containing {@code pos}, that structure paired with the
     * configured rules that match it. Empty if the position is not inside any matched structure piece. Whether a block
     * at the position is actually protected depends on what the matching rules protect (see {@link StructureRule}); the
     * structure handles let callers reason about breaching relative to the specific structures the position belongs to.
     */
    public static List<Match> matchesAt(ServerLevel level, BlockPos pos) {
        StructureManager structureManager = level.structureManager();
        if (!structureManager.hasAnyStructureAt(pos)) return List.of();

        List<StructureRule> rules = ServerConfig.INSTANCE.getStructureRules();
        if (rules.isEmpty()) return List.of();

        Registry<Structure> structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        List<Match> matches = new ArrayList<>();
        for (Structure structure : structureManager.getAllStructuresAt(pos).keySet()) {
            // Confirm the position is actually inside a generated piece of this structure, not just its chunk.
            if (!structureManager.getStructureWithPieceAt(pos, structure).isValid()) continue;

            Identifier structureId = structureRegistry.getKey(structure);
            if (structureId == null) continue;

            List<StructureRule> matchingRules = new ArrayList<>();
            for (StructureRule rule : rules) {
                if (Identifiers.matches(structureId, rule.structures())) {
                    matchingRules.add(rule);
                }
            }
            if (!matchingRules.isEmpty()) {
                matches.add(new Match(structure, matchingRules));
            }
        }
        return matches;
    }

    /**
     * Checks whether {@code pos} is inside a generated piece of the given structure. Used to test whether the player
     * stands inside the very structure a breach targets, rather than inside any protected structure at all.
     */
    public static boolean isInsidePiece(ServerLevel level, BlockPos pos, @NotNull Structure structure) {
        return level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
    }
}
