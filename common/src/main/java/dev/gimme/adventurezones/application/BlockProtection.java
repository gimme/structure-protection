package dev.gimme.adventurezones.application;

import dev.gimme.adventurezones.domain.BlockInteractionRules;
import dev.gimme.adventurezones.domain.ProtectedStructures;
import dev.gimme.adventurezones.domain.config.ServerConfig.StructureRule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        return prevented(level, targetPos, player, item, ruleBlock, false);
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
        BlockPos supportPos = targetPos.relative(ctx.getClickedFace().getOpposite());
        BlockInWorld ruleBlock = new BlockInWorld(serverLevel, supportPos, false);
        return prevented(serverLevel, targetPos, player, item, ruleBlock, true);
    }

    private boolean prevented(ServerLevel level, BlockPos targetPos, @Nullable ServerPlayer player, ItemStack item,
                              BlockInWorld ruleBlock, boolean placing) {
        if (player == null || player.isCreative()) return false; // Creative bypass

        List<StructureRule> rules = ProtectedStructures.rulesAt(level, targetPos);
        if (rules.isEmpty()) return false; // not inside a protected piece

        // The rules are whitelists: each matching structure independently says what it permits, so the edit is allowed
        // as soon as any matching rule permits it, and prevented only if none do. Being inside a protected piece only
        // matters for breachable rules, so compute it lazily.
        Boolean insideProtected = null;
        for (StructureRule rule : rules) {
            Map<String, String> allowList = placing ? rule.canPlaceOn() : rule.canBreak();
            if (BlockInteractionRules.isAllowed(item, ruleBlock, allowList)) return false; // this rule's allow-list permits it

            if (rule.breachable()) {
                if (insideProtected == null) {
                    insideProtected = ProtectedStructures.isInsideAnyProtected(level, player.blockPosition());
                }
                if (!insideProtected) return false; // breachable and the player is outside: breach permitted
            }
        }
        return true; // no matching rule permits the edit
    }
}
