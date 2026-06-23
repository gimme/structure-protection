package dev.gimme.adventurezones.application;

import dev.gimme.adventurezones.domain.ProtectedStructures;
import dev.gimme.adventurezones.domain.ProtectedStructures.Match;
import dev.gimme.adventurezones.domain.config.ServerConfig.StructureRule;
import dev.gimme.adventurezones.domain.util.Identifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Decides, statelessly, whether a block place/break should be prevented because it targets a protected structure piece.
 * Queries the live {@link ProtectedStructures} and each matching {@link StructureRule} at the moment of the interaction
 * — there is no per-server or per-player state.
 */
public final class BlockProtection {

    /**
     * Whether breaking the block at {@code targetPos} should be prevented for the given player.
     */
    public boolean preventsBreak(ServerLevel level, BlockPos targetPos, ServerPlayer player) {
        // When breaking, the edited block is the target itself, so its real in-world state answers everything.
        BlockState state = level.getBlockState(targetPos);
        return prevented(level, targetPos, player, state.getBlock(), state.blocksMotion(), false);
    }

    /**
     * Whether placing a block via the given context should be prevented.
     */
    public boolean preventsPlace(Level level, BlockPlaceContext ctx) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return false;
        // Only block items place a block; the rules match against the block being placed, not the block it rests on.
        if (!(ctx.getItemInHand().getItem() instanceof BlockItem blockItem)) return false;

        Block placed = blockItem.getBlock();
        // The default state is a sound approximation: every motion-blocking block blocks motion in its default state.
        return prevented(serverLevel, ctx.getClickedPos(), player, placed, placed.defaultBlockState().blocksMotion(),
                true);
    }

    private boolean prevented(ServerLevel level, BlockPos targetPos, ServerPlayer player, Block editedBlock,
                              boolean editedBlocksMotion, boolean placing) {
        if (player.isCreative()) return false; // Creative bypass

        List<Match> matches = ProtectedStructures.matchesAt(level, targetPos);
        if (matches.isEmpty()) return false;

        Identifier blockId = level.registryAccess().lookupOrThrow(Registries.BLOCK).getKey(editedBlock);
        if (blockId == null) return false;

        // Walk every matching rule. The edit is blocked only if some rule protects this block and no rule grants an
        // exception: an allow-list match (subtractive), or — for a breachable protecting rule — the player standing
        // outside that rule's own structure (breach from outside, locked inside). Exceptions compose by union.
        boolean protectedHere = false;
        for (Match match : matches) {
            for (StructureRule rule : match.rules()) {
                if (protects(rule, blockId, editedBlocksMotion)) {
                    protectedHere = true;
                    if (rule.breachable()
                            && !ProtectedStructures.isInsidePiece(level, player.blockPosition(), match.structure())) {
                        return false; // breachable rule and the player is outside its structure: breach permitted
                    }
                }

                String allowList = placing ? rule.canPlace() : rule.canBreak();
                if (Identifiers.matches(blockId, allowList)) return false;
            }
        }

        return protectedHere; // protected, and no matching rule permits the edit
    }

    /**
     * Whether the given rule protects the block: its {@code protect} regex matches the block, or it protects the
     * structure's shape ({@code protectStructural}) and the block blocks motion.
     */
    private static boolean protects(StructureRule rule, Identifier blockId, boolean blocksMotion) {
        return Identifiers.matches(blockId, rule.protect()) || (rule.protectStructural() && blocksMotion);
    }
}
