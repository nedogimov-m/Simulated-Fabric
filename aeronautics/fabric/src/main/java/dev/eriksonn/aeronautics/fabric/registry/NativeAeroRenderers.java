package dev.eriksonn.aeronautics.fabric.registry;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerRenderer;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerRenderer;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonRenderer;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** Native client bindings; the original moving BE parts use extracted native submissions. */
public final class NativeAeroRenderers {
    private static boolean registered;
    public static void register() {
        if (registered) return;
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.STEAM_VENT, SteamVentRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.HOT_AIR_BURNER, HotAirBurnerRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.PROPELLER_BEARING, PropellerBearingRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.GYROSCOPIC_PROPELLER_BEARING, GyroscopicPropellerBearingRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.ENVELOPE_ENCASED_SHAFT, KineticBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.ANDESITE_PROPELLER, AndesitePropellerRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.WOODEN_PROPELLER, WoodenPropellerRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.SMART_PROPELLER, SmartPropellerRenderer::new);
        BlockEntityRendererRegistry.register(AeroBlockEntityTypes.MOUNTED_POTATO_CANNON, MountedPotatoCannonRenderer::new);
        EntityRenderers.register(AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION, NativePropellerContraptionRenderer::new);
        EntityRenderers.register(AeroEntityTypes.GUST, NoopRenderer::new);
        registered = true;
    }
    private NativeAeroRenderers() {}
}
