package dev.gimme.structureprotection.fabric.gametest;

import dev.gimme.structureprotection.gametest.StructureProtectionGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric test wiring, scanned via the {@code fabric-gametest} entrypoint. One {@link GameTest}
 * delegate per shared test; the structure defaults to Fabric API's built-in empty 8x8x8.
 */
public final class FabricGameTests {

    @GameTest
    public void configDefaultsLoaded(GameTestHelper helper) {
        StructureProtectionGameTests.configDefaultsLoaded(helper);
    }

    @GameTest
    public void protectedStructuresConfigurable(GameTestHelper helper) {
        StructureProtectionGameTests.protectedStructuresConfigurable(helper);
    }

    @GameTest
    public void protectStructuralConfigurable(GameTestHelper helper) {
        StructureProtectionGameTests.protectStructuralConfigurable(helper);
    }

    @GameTest
    public void rulePolicy(GameTestHelper helper) {
        StructureProtectionGameTests.rulePolicy(helper);
    }

    @GameTest
    public void blockEditStructural(GameTestHelper helper) {
        StructureProtectionGameTests.blockEditStructural(helper);
    }
}
