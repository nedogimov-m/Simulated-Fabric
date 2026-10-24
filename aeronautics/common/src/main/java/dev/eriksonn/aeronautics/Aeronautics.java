package dev.eriksonn.aeronautics;

import dev.eriksonn.aeronautics.events.AeronauticsCommonEvents;
import dev.eriksonn.aeronautics.index.*;
import dev.eriksonn.aeronautics.network.AeroPacketManager;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Aeronautics {
    public static final String MOD_ID = "aeronautics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        dev.eriksonn.aeronautics.fabric.service.FabricAeroConfigService.register();
        AeroRegistries.init();
        AeroLiftingGasTypes.init();
        AeroDataComponents.init();

        AeroBlocks.init();
        AeroBlockEntityTypes.init();
        AeroItems.init();
        AeroAdvancements.init();
        dev.eriksonn.aeronautics.data.AeroAdvancementTriggers.register();
        AeroEntityTypes.init();
        AeroArmorMaterials.init();
        AeroSoundEvents.init();
        dev.eriksonn.aeronautics.fabric.fluid.NativeAeroFluids.register();
        AeroParticleTypes.init();
        AeroBlockMovementChecks.init();
        AeroPacketManager.init();
        AeroLevititeBlendPropagationContexts.init();

        listenCommonEvents();
    }

    private static void listenCommonEvents() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(AeronauticsCommonEvents::onServerTickEnd);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(AeronauticsCommonEvents::onServerStopped);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.UNLOAD.register((server, level) ->
                dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeCrystallizerManager.saveAndClearLevel(level));
        SableEventPlatform.INSTANCE.onPhysicsTick(AeronauticsCommonEvents::physicsTick);
        SableEventPlatform.INSTANCE.onSubLevelContainerReady(AeronauticsCommonEvents::onSubLevelContainerReady);
    }

    public static Identifier path(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
    private Aeronautics() {}
}
