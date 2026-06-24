package dev.gimme.structureprotection.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Supplies the only world-state the {@link dev.gimme.structureprotection.application.BlockProtection} decision needs:
 * which protected structure pieces cover a position and the rules that match them. The server implementation reads the
 * live {@code StructureManager}; the client implementation reads structure pieces streamed from the server. Both
 * identify a structure by its registry id, so the two sides agree on breach checks ("is the player inside this
 * structure") without the client needing real structure-placement data.
 */
public interface StructureSource {

    /**
     * A structure (by registry id) with a generated piece at the queried position, paired with the configured rules
     * that match it (including non-protecting "library" rules).
     */
    record Match(Identifier structure, List<StructureRule> rules) {
    }

    /**
     * Returns, for each structure with a generated piece containing {@code pos}, that structure's registry id paired
     * with the configured rules that match it. Empty if the position is not inside any matched structure piece.
     */
    List<Match> matchesAt(Level level, BlockPos pos);

    /**
     * Whether {@code pos} lies inside a generated piece of the given structure. Used to test whether the player stands
     * inside the very structure a breach targets, rather than inside any protected structure at all.
     */
    boolean isInsidePiece(Level level, BlockPos pos, Identifier structure);
}
