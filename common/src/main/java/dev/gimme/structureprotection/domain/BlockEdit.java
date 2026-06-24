package dev.gimme.structureprotection.domain;

import net.minecraft.world.level.block.Block;

/**
 * A player's attempt to edit a single block — placing it or breaking it — the unit
 * {@link dev.gimme.structureprotection.application.BlockProtection} weighs against the policy.
 */
public record BlockEdit(Kind kind, Block block) {

    /** Whether the player is placing a block or breaking one. */
    public enum Kind {PLACE, BREAK}

    public static BlockEdit placing(Block block) {
        return new BlockEdit(Kind.PLACE, block);
    }

    public static BlockEdit breaking(Block block) {
        return new BlockEdit(Kind.BREAK, block);
    }

    public boolean isPlacing() {
        return kind == Kind.PLACE;
    }

    public boolean isBreaking() {
        return kind == Kind.BREAK;
    }

    /**
     * Whether the edited block is <em>structural</em> — part of the structure's shape that
     * {@link StructureRule#protectStructural()} guards, as opposed to non-physical decoration (torches, carpets,
     * flowers) which stays editable. A block is structural when its default state blocks motion (a non-empty,
     * motion-blocking collision box). Judged by the block type via its default state, so it reads the same whether
     * placing or breaking, and regardless of the state a particular instance is in — e.g. an open fence gate blocks no
     * motion, but is still structural, so it can't be opened to dodge shape protection.
     */
    public boolean isStructural() {
        return block.defaultBlockState().blocksMotion();
    }
}
