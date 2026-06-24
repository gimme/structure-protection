package dev.gimme.structureprotection.client;

import dev.gimme.structureprotection.domain.ProtectedPiece;
import dev.gimme.structureprotection.domain.StructureRule;
import dev.gimme.structureprotection.domain.StructureSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side {@link StructureSource}: mirrors {@code ServerStructureSource}'s semantics against the pieces and rules
 * the server has streamed into {@link ClientProtectedRegions}, rather than a live StructureManager (which the client
 * does not have). The streamed set is reach-range only, which is all the outline/break/place feel ever needs.
 */
public final class ClientStructureSource implements StructureSource {

    private final ClientProtectedRegions regions;

    public ClientStructureSource(ClientProtectedRegions regions) {
        this.regions = regions;
    }

    @Override
    public List<Match> matchesAt(Level level, BlockPos pos) {
        List<ProtectedPiece> pieces = regions.pieces();
        if (pieces.isEmpty()) return List.of();

        Set<Identifier> here = new LinkedHashSet<>();
        for (ProtectedPiece piece : pieces) {
            if (piece.contains(pos)) here.add(piece.structure());
        }
        if (here.isEmpty()) return List.of();

        List<StructureRule> rules = regions.rules();
        List<Match> matches = new ArrayList<>();
        for (Identifier structureId : here) {
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
        for (ProtectedPiece piece : regions.pieces()) {
            if (structure.equals(piece.structure()) && piece.contains(pos)) return true;
        }
        return false;
    }
}
