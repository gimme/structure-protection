package dev.gimme.adventurezones.domain;

import dev.gimme.adventurezones.domain.config.ServerConfig;
import dev.gimme.adventurezones.domain.config.ServerConfig.StructureRule;
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
     * Returns the configured rules whose structures have a generated piece containing {@code pos}. Empty if the
     * position is not inside any protected structure piece.
     */
    public static List<StructureRule> rulesAt(ServerLevel level, BlockPos pos) {
        StructureManager structureManager = level.structureManager();
        if (!structureManager.hasAnyStructureAt(pos)) return List.of();

        List<StructureRule> rules = ServerConfig.INSTANCE.getProtectedStructures();
        if (rules.isEmpty()) return List.of();

        Registry<Structure> structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        List<StructureRule> matched = new ArrayList<>();
        for (Structure structure : structureManager.getAllStructuresAt(pos).keySet()) {
            // Confirm the position is actually inside a generated piece of this structure, not just its chunk.
            if (!structureManager.getStructureWithPieceAt(pos, structure).isValid()) continue;

            Identifier structureId = structureRegistry.getKey(structure);
            if (structureId == null) continue;

            for (StructureRule rule : rules) {
                if (matchesRegex(structureId, rule.structures())) {
                    matched.add(rule);
                }
            }
        }
        return matched;
    }

    /**
     * Checks whether the given position is inside any protected structure piece.
     */
    public static boolean isInsideAnyProtected(ServerLevel level, BlockPos pos) {
        return !rulesAt(level, pos).isEmpty();
    }

    /**
     * Checks if the given resource location matches the specified regex.
     */
    private static boolean matchesRegex(@NotNull Identifier resourceLocation, @NotNull String regex) {
        if (regex.contains(":")) {
            return resourceLocation.toString().matches(regex);
        } else {
            return resourceLocation.getPath().matches(regex);
        }
    }
}
