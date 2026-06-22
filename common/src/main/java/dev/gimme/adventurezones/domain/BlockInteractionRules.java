package dev.gimme.adventurezones.domain;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Per-block place/break allow-list matching, ported from the sibling mod Adventure Mode Adjust. The allow-list is a map
 * of item-name regex to block-name regex; a configured exception lets a specific item still act on a specific block
 * even inside a protected structure.
 */
public final class BlockInteractionRules {

    private BlockInteractionRules() {
    }

    /**
     * Checks if the given itemStack is allowed to act on the specified block according to the given allow-list (an
     * item-name regex to block-name regex map).
     */
    public static boolean isAllowed(@NotNull ItemStack itemStack, @NotNull BlockInWorld block, @NotNull Map<String, String> blocksByItems) {
        if (blocksByItems.isEmpty()) return false;

        var itemRegistry = block.getLevel().registryAccess().lookupOrThrow(Registries.ITEM);
        var itemResourceLocation = itemRegistry.getKey(itemStack.getItem());
        if (itemResourceLocation == null) return false;

        return blocksByItems.entrySet().stream().anyMatch(entry ->
                matchesRegex(itemResourceLocation, entry.getKey()) && matchesBlockRegex(block, entry.getValue())
        );
    }

    /**
     * Checks if the given block matches the specified block regex.
     */
    private static boolean matchesBlockRegex(@NotNull BlockInWorld block, @NotNull String blockRegex) {
        var blockRegistry = block.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        Identifier blockResourceLocation = blockRegistry.getKey(block.getState().getBlock());
        if (blockResourceLocation == null) return false;

        return matchesRegex(blockResourceLocation, blockRegex);
    }

    /**
     * Checks if the given resource location matches the specified regex.
     */
    private static boolean matchesRegex(@NotNull Identifier resourceLocation, @NotNull String regex) {
        if (regex.contains(":")) {
            return resourceLocation.toString().matches(regex);
        } else {
            return resourceLocation.getPath().matches(regex);
        }
    }
}
