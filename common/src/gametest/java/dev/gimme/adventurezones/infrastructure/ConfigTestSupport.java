package dev.gimme.adventurezones.infrastructure;

import com.electronwill.nightconfig.core.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Test-only handles to package-private {@link FcapServerConfig} values, exposed so game tests can
 * read and modify them.
 */
public final class ConfigTestSupport {

    public static final ModConfigSpec.ConfigValue<List<? extends Config>> PROTECTED_STRUCTURE = FcapServerConfig.PROTECTED_STRUCTURE;

    private ConfigTestSupport() {
    }
}
