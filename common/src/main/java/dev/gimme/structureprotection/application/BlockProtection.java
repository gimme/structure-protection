package dev.gimme.structureprotection.application;

import dev.gimme.structureprotection.domain.StructureSource;
import dev.gimme.structureprotection.domain.StructureSource.Match;
import dev.gimme.structureprotection.domain.config.ServerConfig.StructureRule;
import dev.gimme.structureprotection.domain.util.Identifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Decides, statelessly, whether a block place/break should be prevented because it targets a protected structure piece.
 * The decision is identical on both sides — only the {@link StructureSource} differs: the server reads the live
 * StructureManager, the client reads the pieces streamed to it. The server's verdict is authoritative; the client's is
 * used purely to make protected blocks <em>feel</em> unbreakable (no outline, no mining, no place flicker).
 */
public final class BlockProtection {

    private final StructureSource structures;

    public BlockProtection(StructureSource structures) {
        this.structures = structures;
    }

    /**
     * Whether breaking the block at {@code targetPos} should be prevented for the given player.
     */
    public boolean preventsBreak(Level level, BlockPos targetPos, Player player) {
        // When breaking, the edited block is the target itself, so its real in-world state answers everything.
        BlockState state = level.getBlockState(targetPos);
        return prevented(level, targetPos, player, state.getBlock(), state.blocksMotion(), false);
    }

    /**
     * Whether placing a block via the given context should be prevented.
     */
    public boolean preventsPlace(Level level, BlockPlaceContext ctx) {
        if (!(ctx.getPlayer() instanceof Player player)) return false;
        // Only block items place a block; the rules match against the block being placed, not the block it rests on.
        if (!(ctx.getItemInHand().getItem() instanceof BlockItem blockItem)) return false;

        Block placed = blockItem.getBlock();
        // The default state is a sound approximation: every motion-blocking block blocks motion in its default state.
        return prevented(level, ctx.getClickedPos(), player, placed, placed.defaultBlockState().blocksMotion(), true);
    }

    private boolean prevented(Level level, BlockPos targetPos, Player player, Block editedBlock,
                              boolean editedBlocksMotion, boolean placing) {
        if (player.isCreative()) return false; // Creative bypass

        List<Match> matches = structures.matchesAt(level, targetPos);
        if (matches.isEmpty()) return false;

        Identifier blockId = level.registryAccess().lookupOrThrow(Registries.BLOCK).getKey(editedBlock);
        if (blockId == null) return false;

        // Walk every matching rule. The edit is blocked only if some rule protects this block and no rule grants an
        // exception: an allow-list match (subtractive), or — when breaking, for a breachable protecting rule — the
        // player standing outside that rule's own structure (breach in from outside, locked inside). Breaching only
        // ever permits breaking a way in, never placing. Exceptions compose by union.
        boolean protectedHere = false;
        for (Match match : matches) {
            for (StructureRule rule : match.rules()) {
                if (protects(rule, blockId, editedBlocksMotion)) {
                    protectedHere = true;
                    if (rule.breachable() && !placing
                            && !structures.isInsidePiece(level, player.blockPosition(), match.structure())) {
                        return false; // breachable rule, breaking from outside its structure: breach permitted
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
