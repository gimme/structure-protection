package dev.gimme.adventurezones;

import dev.gimme.adventurezones.domain.config.ServerConfig;
import dev.gimme.adventurezones.domain.util.Constants;
import dev.gimme.adventurezones.infrastructure.FcapServerConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Constants.MOD_ID)
public class NeoForgeMod {

    public NeoForgeMod(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, FcapServerConfig.SPEC, FcapServerConfig.FILE_NAME);
        ServerConfig.INSTANCE = new FcapServerConfig();

        // Protection is stateless and driven by mixins that read Main.INSTANCE, so wire it once at mod load.
        Main.init();
    }
}
