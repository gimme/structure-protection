package dev.gimme.adventurezones.infrastructure;

import dev.gimme.adventurezones.domain.util.Constants;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModAttachmentTypes {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);

    /**
     * Attachment type for storing a list of blocks placed by players in a chunk.
     */
    static final Supplier<AttachmentType<List<BlockPos>>> PLAYER_PLACED_BLOCKS = ATTACHMENT_TYPES.register(
            "player_placed_blocks", () -> AttachmentType
                    .builder(() -> (List<BlockPos>) new ArrayList<BlockPos>())
                    .serialize(BlockPos.CODEC.listOf()).build()
    );
}
