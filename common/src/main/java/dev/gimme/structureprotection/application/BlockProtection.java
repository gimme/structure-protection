package dev.gimme.structureprotection.application;

import dev.gimme.structureprotection.domain.BlockEdit;
import dev.gimme.structureprotection.domain.StructureRule;
import dev.gimme.structureprotection.domain.StructureSource;
import dev.gimme.structureprotection.domain.StructureSource.Match;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Decides, statelessly, whether a {@link BlockEdit} should be prevented because it targets a protected structure piece.
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
        // When breaking, the edited block is the target itself.
        Block broken = level.getBlockState(targetPos).getBlock();
        return prevented(level, targetPos, player, BlockEdit.breaking(broken));
    }

    /**
     * Whether placing a block via the given context should be prevented.
     */
    public boolean preventsPlace(Level level, BlockPlaceContext ctx) {
        if (!(ctx.getPlayer() instanceof Player player)) return false;
        // Only block items place a block; the rules match against the block being placed, not the block it rests on.
        if (!(ctx.getItemInHand().getItem() instanceof BlockItem blockItem)) return false;

        return prevented(level, ctx.getClickedPos(), player, BlockEdit.placing(blockItem.getBlock()));
    }

    private boolean prevented(Level level, BlockPos targetPos, Player player, BlockEdit edit) {
        if (player.isCreative()) return false; // Creative bypass

        List<Match> matches = structures.matchesAt(level, targetPos);
        if (matches.isEmpty()) return false;

        Identifier blockId = level.registryAccess().lookupOrThrow(Registries.BLOCK).getKey(edit.block());
        if (blockId == null) return false;

        // Walk every matching rule. The edit is blocked only if some rule protects this block and no rule grants an
        // exception: an allow-list match (subtractive), or — when breaking, for a breachable protecting rule — the
        // player standing outside that rule's own structure (breach in from outside, locked inside). Breaching only
        // ever permits breaking a way in, never placing. Exceptions compose by union.
        boolean protectedHere = false;
        for (Match match : matches) {
            for (StructureRule rule : match.rules()) {
                if (rule.protects(blockId, edit.isStructural())) {
                    protectedHere = true;
                    if (edit.isBreaking() && rule.breachable()
                            && !structures.isInsidePiece(level, player.blockPosition(), match.structure())) {
                        return false; // breachable rule, breaking from outside its structure: breach permitted
                    }
                }

                if (edit.isPlacing() ? rule.allowsPlacing(blockId) : rule.allowsBreaking(blockId)) {
                    return false; // an allow-list grants this edit
                }
            }
        }

        return protectedHere; // protected, and no matching rule permits the edit
    }
}
