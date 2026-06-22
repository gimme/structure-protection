package dev.gimme.adventurezones;

import dev.gimme.adventurezones.application.BlockProtection;

/**
 * Composition root. Wires the stateless services and exposes them through {@link #INSTANCE}, which the loader-agnostic
 * mixins read. {@link #init()} runs once at mod load — there is no per-server state to rebuild.
 */
public class Main {

    public static Main INSTANCE;

    public static Main init() {
        INSTANCE = new Main();
        return INSTANCE;
    }

    private final BlockProtection blockProtection;

    private Main() {
        this.blockProtection = new BlockProtection();
    }

    public BlockProtection getBlockProtection() {
        return blockProtection;
    }
}
