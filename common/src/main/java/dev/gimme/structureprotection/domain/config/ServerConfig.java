package dev.gimme.structureprotection.domain.config;

import dev.gimme.structureprotection.domain.StructureRule;

import java.util.List;

/**
 * Outbound port supplying the protection policy — the {@link StructureRule}s that decide what is protected and what is
 * still allowed. The domain reads rules through this; an infrastructure adapter ({@code FcapServerConfig}) backs it with
 * the on-disk config.
 */
public abstract class ServerConfig {

    public static ServerConfig INSTANCE;

    /**
     * The structure rules: each applies to a set of structures and carries what it protects and what it still allows.
     * A rule that protects nothing acts as a "library" rule, contributing only its allow-lists to structures that
     * other rules protect. A structure's effective policy is the union of every rule that matches it.
     */
    public abstract List<StructureRule> getStructureRules();
}
