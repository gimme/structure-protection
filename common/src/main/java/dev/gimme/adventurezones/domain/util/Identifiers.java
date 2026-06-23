package dev.gimme.adventurezones.domain.util;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Regex matching against namespaced ids ({@link Identifier}), shared by structure matching and block-protection rules.
 */
public final class Identifiers {

    private Identifiers() {
    }

    /**
     * Checks whether {@code id} matches {@code regex}. An empty regex matches nothing (so an unset rule field protects
     * or allows nothing). If the regex contains a colon it is matched against the full namespaced id
     * (e.g. {@code minecraft:fortress}); otherwise only against the path (e.g. {@code fortress}).
     */
    public static boolean matches(@NotNull Identifier id, @NotNull String regex) {
        if (regex.isEmpty()) return false;
        String target = regex.contains(":") ? id.toString() : id.getPath();
        return target.matches(regex);
    }
}
