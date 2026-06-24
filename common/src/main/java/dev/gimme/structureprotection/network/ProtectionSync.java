package dev.gimme.structureprotection.network;

import dev.gimme.structureprotection.domain.ProtectedPiece;
import dev.gimme.structureprotection.domain.StructureRule;
import dev.gimme.structureprotection.domain.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * Server-side driver that streams the reach-range protected pieces around each capable client, so the client can make
 * protected blocks feel untouchable. Runs from the player's tick; recomputes only when the player has moved to a new
 * block, and sends only when the resulting set changes. It reads structure data already resident on the server (no
 * generation), bounded to the handful of chunks within reach, so the added cost is marginal. Vanilla (and other modless)
 * clients are skipped entirely via {@link Sender#canSend} — protection itself stays fully server-authoritative.
 */
public final class ProtectionSync {

    /** Blocks within this radius of the player are all the outline/break/place feel can ever target. */
    private static final int REACH = 7;

    /** Loader-supplied bridge: whether a client can receive our payload (i.e. has the mod) and how to send it. */
    public interface Sender {
        boolean canSend(ServerPlayer player);

        void send(ServerPlayer player, ProtectionUpdatePayload payload);
    }

    public static Sender SENDER;

    private ProtectionSync() {
    }

    // Per-player last-sent snapshot. WeakHashMap so logged-out players are collected without a disconnect hook; only
    // ever touched from the single server thread.
    private record Snapshot(long blockPos, Set<ProtectedPiece> pieces) {
    }

    private static final Map<ServerPlayer, Snapshot> STATE = new WeakHashMap<>();

    public static void tick(ServerPlayer player) {
        Sender sender = SENDER;
        if (sender == null || !sender.canSend(player)) return;

        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        long packed = pos.asLong();

        Snapshot previous = STATE.get(player);
        if (previous != null && previous.blockPos == packed) return; // unmoved since last check

        Set<ProtectedPiece> pieces = nearbyPieces(level, pos);
        if (previous != null && pieces.equals(previous.pieces)) {
            STATE.put(player, new Snapshot(packed, previous.pieces)); // moved, but same pieces: just track the position
            return;
        }

        List<StructureRule> rules = pieces.isEmpty() ? List.of() : ServerConfig.INSTANCE.getStructureRules();
        sender.send(player, new ProtectionUpdatePayload(rules, List.copyOf(pieces)));
        STATE.put(player, new Snapshot(packed, pieces));
    }

    private static Set<ProtectedPiece> nearbyPieces(ServerLevel level, BlockPos center) {
        List<StructureRule> rules = ServerConfig.INSTANCE.getStructureRules();
        if (rules.isEmpty()) return Set.of();

        StructureManager structureManager = level.structureManager();
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        BoundingBox reach = new BoundingBox(
                center.getX() - REACH, center.getY() - REACH, center.getZ() - REACH,
                center.getX() + REACH, center.getY() + REACH, center.getZ() + REACH);

        // Only enumerate structures some rule mentions, so the common "nothing nearby" case stays cheap.
        Predicate<Structure> configured = structure -> {
            Identifier id = registry.getKey(structure);
            if (id == null) return false;
            for (StructureRule rule : rules) {
                if (rule.appliesTo(id)) return true;
            }
            return false;
        };

        Set<ProtectedPiece> pieces = new LinkedHashSet<>();
        for (int chunkX = reach.minX() >> 4; chunkX <= reach.maxX() >> 4; chunkX++) {
            for (int chunkZ = reach.minZ() >> 4; chunkZ <= reach.maxZ() >> 4; chunkZ++) {
                for (StructureStart start : structureManager.startsForStructure(new ChunkPos(chunkX, chunkZ), configured)) {
                    Identifier id = registry.getKey(start.getStructure());
                    if (id == null) continue;
                    for (StructurePiece piece : start.getPieces()) {
                        BoundingBox box = piece.getBoundingBox();
                        if (box.intersects(reach)) {
                            pieces.add(ProtectedPiece.of(id, box));
                        }
                    }
                }
            }
        }
        return pieces;
    }
}
