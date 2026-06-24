package dev.gimme.structureprotection.domain;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * A regex over namespaced ids ({@link Identifier}) — the shared vocabulary a {@link StructureRule} uses to name sets of
 * structures and blocks. Matching mirrors how a player writes the rule: a pattern containing a colon is tested against
 * the full id (e.g. {@code minecraft:fortress}), otherwise against the path alone (e.g. {@code fortress}). The empty
 * pattern, {@link #NONE}, matches nothing — an unset rule field protects or allows nothing.
 *
 * <p>An immutable value object: the regex is compiled once at construction (rather than per edit) and two patterns are
 * equal when their source text is equal.
 */
public final class IdPattern {

    /** The empty pattern: matches no id. The value of every unset {@link StructureRule} field. */
    public static final IdPattern NONE = new IdPattern("", null, false);

    private final String raw;
    private final Pattern compiled; // null exactly for NONE (empty source)
    private final boolean matchFullId;

    private IdPattern(String raw, Pattern compiled, boolean matchFullId) {
        this.raw = raw;
        this.compiled = compiled;
        this.matchFullId = matchFullId;
    }

    /**
     * Compiles the given regex into a pattern. A null or empty source yields {@link #NONE}. Throws
     * {@link java.util.regex.PatternSyntaxException} if the source is not a valid regex, so callers that accept untrusted
     * input should validate first (the config spec does, in {@code FcapServerConfig}).
     */
    public static IdPattern of(String regex) {
        if (regex == null || regex.isEmpty()) return NONE;
        return new IdPattern(regex, Pattern.compile(regex), regex.contains(":"));
    }

    /** Whether this pattern matches the given id. {@link #NONE} matches nothing. */
    public boolean matches(@NotNull Identifier id) {
        if (compiled == null) return false;
        return compiled.matcher(matchFullId ? id.toString() : id.getPath()).matches();
    }

    /** Whether this is the empty pattern that matches nothing. */
    public boolean isEmpty() {
        return compiled == null;
    }

    /** The original regex source, for serialization and display. */
    public String raw() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IdPattern other && raw.equals(other.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public String toString() {
        return raw;
    }
}
