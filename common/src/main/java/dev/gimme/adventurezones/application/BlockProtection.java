package dev.gimme.adventurezones.application;

import dev.gimme.adventurezones.domain.BlockInteractionRules;
import dev.gimme.adventurezones.domain.ProtectedStructures;
import dev.gimme.adventurezones.domain.ProtectedStructures.Match;
import dev.gimme.adventurezones.domain.config.ServerConfig.StructureRule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Decides, statelessly, whether a block place/break should be prevented because it targets a protected structure piece.
 * Queries the live {@link ProtectedStructures} and each matching rule's own allow-list ({@link BlockInteractionRules})
 * at the moment of the interaction — there is no per-server or per-player state.
 */
public final class BlockProtection {

    /**
     * Whether breaking the block at {@code targetPos} should be prevented for the given player.
     */
    public boolean preventsBreak(ServerLevel level, BlockPos targetPos, ServerPlayer player) {
        ItemStack item = player.getMainHandItem();
        BlockInWorld ruleBlock = new BlockInWorld(level, targetPos, false);
        // When breaking, the edited block is the target itself, so its real in-world state answers "is this physical?".
        boolean editedBlocksMotion = ruleBlock.getState().blocksMotion();
        return prevented(level, targetPos, player, item, ruleBlock, false, editedBlocksMotion);
    }

    /**
     * Whether placing a block via the given context should be prevented.
     */
    public boolean preventsPlace(Level level, BlockPlaceContext ctx) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return false;

        BlockPos targetPos = ctx.getClickedPos();
        ItemStack item = ctx.getItemInHand();
        // The block the item is being placed on (its support), opposite the clicked face from the new block position.
        // This drives canPlaceOn (the right-hand side of "<item>=<block>" matches the support).
        BlockPos supportPos = targetPos.relative(ctx.getClickedFace().getOpposite());
        BlockInWorld ruleBlock = new BlockInWorld(serverLevel, supportPos, false);
        // The "physical?" question is about the block being *placed* (the item's block), not the support it rests on.
        // The default state is a sound approximation: every motion-blocking block blocks motion in its default state.
        boolean editedBlocksMotion = item.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().blocksMotion();
        return prevented(serverLevel, targetPos, player, item, ruleBlock, true, editedBlocksMotion);
    }

    private boolean prevented(ServerLevel level, BlockPos targetPos, @Nullable ServerPlayer player, ItemStack item,
                              BlockInWorld ruleBlock, boolean placing, boolean editedBlocksMotion) {
        if (player == null || player.isCreative()) return false; // Creative bypass

        List<Match> matches = ProtectedStructures.matchesAt(level, targetPos);

        // Membership: the position is in scope only if a protecting rule matches it. Non-protecting "library" rules
        // (e.g. a ".*" base) contribute allow-lists but never put a structure into scope on their own.
        if (matches.stream().flatMap(m -> m.rules().stream()).noneMatch(StructureRule::isProtected)) return false;

        // Every matching rule is a whitelist: the edit is allowed the moment any rule permits it, no matter what other
        // rules say. A rule permits it through its allow-list, or — on a protecting rule — by guarding only physical
        // blocks while this edit is non-physical, or — for a breachable rule — by the player standing outside that
        // rule's own structure (breach from outside, locked inside).
        for (Match match : matches) {
            for (StructureRule rule : match.rules()) {
                Map<String, String> allowList = placing ? rule.canPlaceOn() : rule.canBreak();
                if (BlockInteractionRules.isAllowed(item, ruleBlock, allowList)) return false;

                if (rule.isProtected()) {
                    // A "physical only" rule guards walls/floors/stairs/etc. but leaves non-physical blocks editable.
                    if (rule.protectsOnlyPhysical() && !editedBlocksMotion) return false;

                    if (rule.breachable()
                            && !ProtectedStructures.isInsidePiece(level, player.blockPosition(), match.structure())) {
                        return false; // breachable rule and the player is outside its structure: breach permitted
                    }
                }
            }
        }

        return true; // protected, and no matching rule permits the edit
    }
}
