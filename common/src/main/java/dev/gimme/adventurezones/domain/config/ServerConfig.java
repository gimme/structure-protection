package dev.gimme.adventurezones.domain.config;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ServerConfig {

    public static ServerConfig INSTANCE;

    public abstract boolean displayModeText();

    public abstract int getCombatModeSeconds();

    public abstract List<ZoneConfig> getZoneConfigs();

    /**
     * Returns the maximum extra radius among all defined zones.
     */
    public abstract int getMaxZoneRadius();

    public record ZoneConfig(
            String structures,
            int radius,
            @Nullable String blocks,
            @Nullable Integer minY,
            @Nullable Integer maxY
    ) {
    }
}
