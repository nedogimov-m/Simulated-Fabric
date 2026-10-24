package dev.simulated_team.simulated;

import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.end_sea.EndSeaFadeTransformer;
import dev.simulated_team.simulated.content.end_sea.EndSeaShadowRenderer;
import dev.simulated_team.simulated.content.items.merging_glue.MergingGlueItemHandler;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.index.ponder.SimPonderPlugin;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.createmod.ponder.foundation.PonderIndex;

public class SimulatedClient {

    public static final PhysicsStaffClientHandler PHYSICS_STAFF_CLIENT_HANDLER = new PhysicsStaffClientHandler();
    public static PlungerLauncherItemRenderer.RenderHandler PLUNGER_LAUNCHER_RENDER_HANDLER = new PlungerLauncherItemRenderer.RenderHandler();
	public static final MergingGlueItemHandler MERGING_GLUE_ITEM_HANDLER = new MergingGlueItemHandler();

    public static void init() {
        net.minecraft.client.renderer.entity.EntityRenderers.register(
                dev.simulated_team.simulated.index.SimEntityTypes.HONEY_GLUE,
                dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(
                dev.simulated_team.simulated.index.SimEntityTypes.PLUNGER,
                dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(
                dev.simulated_team.simulated.index.SimEntityTypes.CONTRAPTION_DIAGRAM,
                dev.simulated_team.simulated.content.entities.diagram.DiagramEntityRenderer::new);
        dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItem.CLIENT_SHOT =
                hand -> PLUNGER_LAUNCHER_RENDER_HANDLER.dontAnimateItem(hand);
        dev.simulated_team.simulated.fabric.client.NativeSimWorldShaderBindings.register();
        dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer.register();
        dev.simulated_team.simulated.fabric.client.render.NativeDiagramPass.register();
        dev.simulated_team.simulated.fabric.client.NativeSimMenuScreens.register();
        dev.simulated_team.simulated.fabric.client.NativeSimClientEvents.register();
        dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterItemBindHandler.register();
        dev.simulated_team.simulated.fabric.client.NativeAssistedPlacementClient.register();
        dev.simulated_team.simulated.fabric.client.NativeSimParticles.register();
        dev.simulated_team.simulated.index.SimSpriteShifts.init();
        SimPartialModels.init();
        dev.simulated_team.simulated.fabric.client.NativeSimAnimatedItems.register();
        dev.simulated_team.simulated.fabric.registry.NativeCatalogClient.register();
        dev.simulated_team.simulated.fabric.registry.NativeBlockEntityRenderers.register();
        BlockPropertiesTooltip.init();
        SimResourceManagers.init();

        PonderIndex.addPlugin(new SimPonderPlugin());

        EndSeaShadowRenderer.register();
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(EndSeaShadowRenderer::renderShadowMap);
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(dev.simulated_team.simulated.content.end_sea.EndSeaRenderer::render);

        dev.simulated_team.simulated.fabric.client.NativeEndSeaShaders.register();
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(SimulatedCommonClientEvents::onRenderLevelStage);

    }
}
