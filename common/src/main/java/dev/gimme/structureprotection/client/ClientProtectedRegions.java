package dev.gimme.structureprotection.client;

import dev.gimme.structureprotection.domain.ProtectedPiece;
import dev.gimme.structureprotection.domain.StructureRule;

import java.util.List;

/**
 * Client-side store of the protection data the server has streamed for the area around the player: the effective rules
 * and the reach-range protected pieces. Replaced wholesale by the network receiver; read by {@link ClientStructureSource}
 * on the render/interaction threads. The fields are volatile because those two touch points can differ by a frame.
 */
public final class ClientProtectedRegions {

    public static final ClientProtectedRegions INSTANCE = new ClientProtectedRegions();

    private volatile List<StructureRule> rules = List.of();
    private volatile List<ProtectedPiece> pieces = List.of();

    public void update(List<StructureRule> rules, List<ProtectedPiece> pieces) {
        this.rules = List.copyOf(rules);
        this.pieces = List.copyOf(pieces);
    }

    public void clear() {
        this.rules = List.of();
        this.pieces = List.of();
    }

    public List<StructureRule> rules() {
        return rules;
    }

    public List<ProtectedPiece> pieces() {
        return pieces;
    }
}
