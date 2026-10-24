package dev.simulated_team.simulated;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import dev.simulated_team.simulated.index.*;
import dev.simulated_team.simulated.network.SimPacketManager;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class Simulated {
    public static final String MOD_ID = "simulated";
    public static final String MOD_NAME = "Create Simulated";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        dev.simulated_team.simulated.util.extra_kinetics.NativeKineticChannels.bootstrap();
        dev.simulated_team.simulated.fabric.registry.NativeExtendoGripRules.register();
        dev.simulated_team.simulated.fabric.service.FabricSimConfigService.register();
        dev.simulated_team.simulated.fabric.block.NativeWrenchable.register();
        SimRegistries.register();
        dev.simulated_team.simulated.fabric.registry.NativeSimStats.register();
        SimDataComponents.register();
        SimEntityDataSerializers.register();
        SimTags.register();
        SimBlocks.register();
        SimItems.register();
        dev.simulated_team.simulated.fabric.registry.NativePortableEngineDyeingRecipe.register();
        dev.simulated_team.simulated.fabric.registry.NativeSchematicRequirements.register();
        dev.simulated_team.simulated.data.advancements.SimAdvancements.register();
        dev.simulated_team.simulated.data.advancements.SimAdvancementTriggers.register();
        SimBlockEntityTypes.register();
        SimParticleTypes.register();
        SimSoundEvents.init();
        SimPacketManager.init();
        SimEntityTypes.register();
        SimMenuTypes.register();
        SimNavigationTargets.register();
        SimItemAttributeTypes.init();
        dev.simulated_team.simulated.fabric.tab.FabricSimTabService.register();

        DefaultItemComponentEvents.MODIFY.register(context -> SimulatedCommonEvents.modifyDefaultComponents(
                (item, modify) -> context.modify(item.asItem(), modify)));
        SimulatedCommonEvents.register();
        SimBlockMovementChecks.register();
        SimAssemblyHelper.register();
        SimModCompatibilityService.initLoaded();
        SableEventPlatform.INSTANCE.onPhysicsTick(SimulatedCommonEvents::onPhysicsTick);
        SableEventPlatform.INSTANCE.onPostPhysicsTick(SimulatedCommonEvents::onPostPhysicsTick);
    }

    public static Identifier path(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
    private Simulated() {}
}
