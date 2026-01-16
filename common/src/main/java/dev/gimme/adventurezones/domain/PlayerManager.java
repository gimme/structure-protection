package dev.gimme.adventurezones.domain;

import com.mojang.logging.LogUtils;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {

    private final static Logger LOG = LogUtils.getLogger();

    private final AdventureZones adventureZones;
    private final Map<ServerPlayer, CombatModeState> playersInCombat = new HashMap<>();

    public PlayerManager(@NotNull AdventureZones adventureZones) {
        this.adventureZones = adventureZones;
    }

    /**
     * Updates the player's status regarding adventure zones and combat.
     */
    public void updatePlayer(ServerPlayer player) {
        cleanUpInactiveCombatStates();
        updateAdventureModeStatus(player);
    }

    private void cleanUpInactiveCombatStates() {
        playersInCombat.values().forEach(CombatModeState::removeInactiveMobs);
        playersInCombat.entrySet().removeIf(entry -> !entry.getValue().isInActiveCombat());
    }

    /**
     * Updates the player's game mode based on if they are in combat or in an adventure zone.
     */
    private void updateAdventureModeStatus(ServerPlayer player) {
        if (shouldBeInAdventureMode(player)) {
            if (!isInAdventureMode(player)) {
                enterAdventureMode(player);
            }
        } else {
            if (isInAdventureMode(player)) {
                exitAdventureMode(player);
            }
        }
    }

    public void onAttack(LivingEntity attacker, LivingEntity defender) {
        if (attacker instanceof ServerPlayer attackerPlayer) {
            if (!defender.getType().getCategory().isFriendly() || defender instanceof ServerPlayer) {
                LOG.debug("{} entered combat with entity {}", attackerPlayer.getName().getString(), defender.getName().getString());
                enterCombat(attackerPlayer, defender);
            }
        }

        if (defender instanceof ServerPlayer defenderPlayer) {
            if (attacker instanceof LivingEntity) {
                LOG.debug("Entity {} entered combat with {}", attacker.getName().getString(), defenderPlayer.getName().getString());
                enterCombat(defenderPlayer, attacker);
            }
        }
    }

    public void onAggroMob(LivingEntity angryMob, ServerPlayer targetPlayer) {
        if (angryMob.getType().getCategory().isFriendly()) {
            LOG.debug("{} angered {}", targetPlayer.getName(), angryMob.getType());
            enterCombat(targetPlayer, angryMob);
        }
    }

    private void enterCombat(ServerPlayer player, LivingEntity withMob) {
        if (ServerConfig.INSTANCE.getCombatModeSeconds() <= 0) return;

        var combatModeState = playersInCombat.computeIfAbsent(player, CombatModeState::new);
        combatModeState.addEntityInvolvement(withMob);
    }

    private boolean shouldBeInAdventureMode(ServerPlayer player) {
        return isInCombat(player) || adventureZones.isInAdventureZone(player.blockPosition());
    }

    private boolean isInCombat(ServerPlayer player) {
        var combatModeState = playersInCombat.get(player);
        return combatModeState != null && combatModeState.isInActiveCombat();
    }

    private boolean isInAdventureMode(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
    }

    /**
     * Forces the player into Adventure mode.
     */
    private void enterAdventureMode(ServerPlayer player) {
        player.setGameMode(GameType.ADVENTURE);

        if (ServerConfig.INSTANCE.displayModeText()) {
            Component title = Component.translatableWithFallback("message.adventurezones.enter_zone_title", "Adventure Mode")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA));
            ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(Component.literal(""));
            ClientboundSetSubtitleTextPacket subtitlePacket = new ClientboundSetSubtitleTextPacket(title);
            player.connection.send(titlePacket);
            player.connection.send(subtitlePacket);
        }
    }

    /**
     * Returns the player to survival mode.
     */
    private void exitAdventureMode(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);

        if (ServerConfig.INSTANCE.displayModeText()) {
            Component title = Component.translatableWithFallback("message.adventurezones.leave_zone_title", "Survival Mode")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(Component.literal(""));
            ClientboundSetSubtitleTextPacket subtitlePacket = new ClientboundSetSubtitleTextPacket(title);
            player.connection.send(titlePacket);
            player.connection.send(subtitlePacket);
        }
    }

    /**
     * Tracks what entities a player is in combat with and when those expire.
     */
    private static class CombatModeState {
        private final ServerPlayer player;
        private final Map<LivingEntity, Integer> entitiesInCombatWith = new HashMap<>();

        public CombatModeState(ServerPlayer player) {
            this.player = player;
        }

        public void addEntityInvolvement(LivingEntity entity) {
            var endTick = getNewEndTickForEntityInvolvement(entity);
            if (endTick <= player.tickCount) return;
            entitiesInCombatWith.put(entity, endTick);
        }

        public void removeInactiveMobs() {
            entitiesInCombatWith.entrySet().removeIf(entry -> {
                var isTimeExpired = entry.getValue() <= player.tickCount;
                var mobStillTargetsPlayer = entry.getKey() instanceof Mob mob && mob.isAlive() && mob.getTarget() == player;
                return isTimeExpired && !mobStillTargetsPlayer;
            });
        }

        private int getNewEndTickForEntityInvolvement(LivingEntity entity) {
            return player.tickCount + ServerConfig.INSTANCE.getCombatModeSeconds() * 20;
        }

        public boolean isInActiveCombat() {
            return !entitiesInCombatWith.isEmpty();
        }
    }
}
