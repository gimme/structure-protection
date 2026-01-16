package dev.gimme.adventurezones;

import dev.gimme.adventurezones.domain.AdventureZones;
import dev.gimme.adventurezones.application.ChunkHandler;
import dev.gimme.adventurezones.application.PlayerHandler;
import dev.gimme.adventurezones.domain.PlayerManager;
import dev.gimme.adventurezones.domain.config.ServerConfig;
import dev.gimme.adventurezones.domain.util.Constants;
import dev.gimme.adventurezones.infrastructure.ChunkListener;
import dev.gimme.adventurezones.infrastructure.ModAttachmentTypes;
import dev.gimme.adventurezones.infrastructure.NeoForgeServerConfig;
import dev.gimme.adventurezones.infrastructure.PlayerListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeMod {

    public NeoForgeMod(ModContainer modContainer, IEventBus eventBus) {
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeServerConfig.SPEC, Constants.MOD_ID + "-server.toml");
        ServerConfig.INSTANCE = new NeoForgeServerConfig();

        ModAttachmentTypes.ATTACHMENT_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var adventureZones = new AdventureZones();
        var playerManager = new PlayerManager(adventureZones);

        NeoForge.EVENT_BUS.register(new ChunkListener(new ChunkHandler(adventureZones)));
        NeoForge.EVENT_BUS.register(new PlayerListener(new PlayerHandler(playerManager)));
    }
}
