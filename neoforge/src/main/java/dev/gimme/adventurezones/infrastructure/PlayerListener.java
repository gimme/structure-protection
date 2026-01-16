package dev.gimme.adventurezones.infrastructure;

import dev.gimme.adventurezones.application.PlayerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerListener {

    private final PlayerHandler playerHandler;

    public PlayerListener(@NotNull PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        playerHandler.onPlayerTick(serverPlayer);
    }

    @SubscribeEvent
    public void onPlayerCombat(LivingDamageEvent.Post event) {
        var sourceEntity = event.getSource().getEntity();
        var defender = event.getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) return;

        playerHandler.onLivingEntityAttack(attacker, defender);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAggroMob(LivingChangeTargetEvent event) {
        var target = event.getNewAboutToBeSetTarget();
        var mob = event.getEntity();

        if (event.getTargetType() != LivingChangeTargetEvent.LivingTargetType.MOB_TARGET) return;
        if (!(target instanceof ServerPlayer targetPlayer)) return;

        playerHandler.onAggroMob(mob, targetPlayer);
    }
}
