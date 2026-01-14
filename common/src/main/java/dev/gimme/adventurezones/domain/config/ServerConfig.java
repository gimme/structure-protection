package dev.gimme.adventurezones.domain.config;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ServerConfig {

    public static ServerConfig INSTANCE;

    public abstract int getZoneRadius();

    public abstract List<ResourceLocation> getBlockWhitelist();

    public abstract List<ResourceLocation> getStructureWhitelist();

    public abstract List<ResourceLocation> getStructureBlacklist();

    public abstract boolean displayZoneText();

    public abstract List<ZoneConfig> getZoneConfigs();

    public record ZoneConfig(
            String structures,
            int radius,
            @Nullable String blocks,
            @Nullable Integer minY,
            @Nullable Integer maxY
    ) {
    }
}
