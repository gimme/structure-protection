package dev.gimme.adventurezones.mixin;

import dev.gimme.adventurezones.Main;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels block placement that targets a protected structure piece.
 */
@Mixin(BlockItem.class)
public class MixinBlockItem {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true, require = 1)
    private void onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (Main.INSTANCE == null) return;
        if (context.getLevel().isClientSide()) return;
        if (Main.INSTANCE.getBlockProtection().preventsPlace(context.getLevel(), context)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
