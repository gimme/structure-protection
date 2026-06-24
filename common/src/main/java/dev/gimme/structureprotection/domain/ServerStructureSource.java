package dev.gimme.structureprotection.domain;

import dev.gimme.structureprotection.domain.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side {@link StructureSource}: stateless queries against the live {@link StructureManager}. Bounds come from
 * each piece's bounding box, so protection follows the real structure shape rather than chunk/radius math. This is the
 * authoritative source — the client only ever mirrors a subset of what this produces.
 */
public final class ServerStructureSource implements StructureSource {

    @Override
    public List<Match> matchesAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return List.of();

        StructureManager structureManager = serverLevel.structureManager();
        if (!structureManager.hasAnyStructureAt(pos)) return List.of();

        List<StructureRule> rules = ServerConfig.INSTANCE.getStructureRules();
        if (rules.isEmpty()) return List.of();

        Registry<Structure> structureRegistry = serverLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        List<Match> matches = new ArrayList<>();
        for (Structure structure : structureManager.getAllStructuresAt(pos).keySet()) {
            // Confirm the position is actually inside a generated piece of this structure, not just its chunk.
            if (!structureManager.getStructureWithPieceAt(pos, structure).isValid()) continue;

            Identifier structureId = structureRegistry.getKey(structure);
            if (structureId == null) continue;

            List<StructureRule> matchingRules = new ArrayList<>();
            for (StructureRule rule : rules) {
                if (rule.appliesTo(structureId)) {
                    matchingRules.add(rule);
                }
            }
            if (!matchingRules.isEmpty()) {
                matches.add(new Match(structureId, matchingRules));
            }
        }
        return matches;
    }

    @Override
    public boolean isInsidePiece(Level level, BlockPos pos, Identifier structure) {
        if (!(level instanceof ServerLevel serverLevel)) return false;

        StructureManager structureManager = serverLevel.structureManager();
        if (!structureManager.hasAnyStructureAt(pos)) return false;

        Registry<Structure> structureRegistry = serverLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (Structure s : structureManager.getAllStructuresAt(pos).keySet()) {
            if (structure.equals(structureRegistry.getKey(s))
                    && structureManager.getStructureWithPieceAt(pos, s).isValid()) {
                return true;
            }
        }
        return false;
    }
}
