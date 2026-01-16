package dev.gimme.adventurezones.application;

import dev.gimme.adventurezones.domain.PlayerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class PlayerHandler {

    private final PlayerManager playerManager;

    public PlayerHandler(@NotNull PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public void onPlayerTick(@NotNull ServerPlayer player) {
        playerManager.updatePlayer(player);
    }

    public void onLivingEntityAttack(@NotNull LivingEntity attacker, @NotNull LivingEntity defender) {
        playerManager.onAttack(attacker, defender);
    }

    public void onAggroMob(@NotNull LivingEntity mob, @NotNull ServerPlayer targetPlayer) {
        playerManager.onAggroMob(mob, targetPlayer);
    }
}
