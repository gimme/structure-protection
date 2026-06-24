package dev.gimme.structureprotection.network;

import dev.gimme.structureprotection.domain.IdPattern;
import dev.gimme.structureprotection.domain.ProtectedPiece;
import dev.gimme.structureprotection.domain.StructureRule;
import dev.gimme.structureprotection.domain.util.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client snapshot of the protection state around a player: the effective rules and the reach-range protected
 * pieces. Sent only when that set changes (see {@link ProtectionSync}), and never to vanilla clients. The client mirrors
 * it into {@link dev.gimme.structureprotection.client.ClientProtectedRegions}. Encoded by hand (small, flat data) so the
 * exact same wire shape works on both loaders.
 */
public record ProtectionUpdatePayload(List<StructureRule> rules, List<ProtectedPiece> pieces)
        implements CustomPacketPayload {

    public static final Type<ProtectionUpdatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "protection_update"));

    public static final StreamCodec<FriendlyByteBuf, ProtectionUpdatePayload> STREAM_CODEC =
            StreamCodec.of(ProtectionUpdatePayload::write, ProtectionUpdatePayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, ProtectionUpdatePayload payload) {
        buf.writeVarInt(payload.rules.size());
        for (StructureRule rule : payload.rules) {
            buf.writeUtf(rule.structures().raw());
            buf.writeUtf(rule.protect().raw());
            buf.writeBoolean(rule.protectStructural());
            buf.writeBoolean(rule.breachable());
            buf.writeUtf(rule.canPlace().raw());
            buf.writeUtf(rule.canBreak().raw());
        }
        buf.writeVarInt(payload.pieces.size());
        for (ProtectedPiece piece : payload.pieces) {
            buf.writeUtf(piece.structure().toString());
            buf.writeInt(piece.minX());
            buf.writeInt(piece.minY());
            buf.writeInt(piece.minZ());
            buf.writeInt(piece.maxX());
            buf.writeInt(piece.maxY());
            buf.writeInt(piece.maxZ());
        }
    }

    private static ProtectionUpdatePayload read(FriendlyByteBuf buf) {
        int ruleCount = buf.readVarInt();
        List<StructureRule> rules = new ArrayList<>(ruleCount);
        for (int i = 0; i < ruleCount; i++) {
            rules.add(new StructureRule(
                    IdPattern.of(buf.readUtf()), IdPattern.of(buf.readUtf()),
                    buf.readBoolean(), buf.readBoolean(),
                    IdPattern.of(buf.readUtf()), IdPattern.of(buf.readUtf())));
        }
        int pieceCount = buf.readVarInt();
        List<ProtectedPiece> pieces = new ArrayList<>(pieceCount);
        for (int i = 0; i < pieceCount; i++) {
            Identifier structure = Identifier.parse(buf.readUtf());
            pieces.add(new ProtectedPiece(structure,
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()));
        }
        return new ProtectionUpdatePayload(rules, pieces);
    }
}
