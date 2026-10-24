package dev.ryanhcode.offroad.fabric.registry;

import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import com.simibubi.create.content.contraptions.render.ActorRenderer;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelBlock;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingRenderer;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelRenderer;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountRenderer;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import dev.ryanhcode.offroad.index.OffroadEntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Actual native BER and actor bindings, isolated from the common entity registrations. */
public final class NativeOffroadRenderers {
    private static boolean registered;
    public static void register() {
        if (registered) return;
        // Native item defaults bind after client entrypoints. Register the declared tire models
        // now; server-provided component models are baked by exact ID when first extracted.
        NativeTireModels.init(java.util.List.of(
                dev.ryanhcode.offroad.content.components.TireLike.ROCKCUTTING_WHEEL,
                dev.ryanhcode.offroad.content.components.TireLike.MECHANICAL_ROLLER).stream()
                .flatMap(tire -> tire.model().stream()).toList());
        BlockEntityRendererRegistry.register(OffroadBlockEntityTypes.WHEEL_MOUNT, WheelMountRenderer::new);
        BlockEntityRendererRegistry.register(OffroadBlockEntityTypes.BOREHEAD_BEARING, BoreheadBearingRenderer::new);
        BlockEntityRendererRegistry.register(OffroadBlockEntityTypes.ROCKCUTTING_WHEEL_BLOCK_ENTITY, RockCuttingWheelRenderer::new);
        EntityRenderers.register(OffroadEntityTypes.BOREHEAD_CONTRAPTION_ENTITY, ContraptionEntityRenderer::new);
        ActorRenderer.REGISTRY.register(OffroadBlocks.ROCK_CUTTER_BLOCK, new NativeRockCuttingWheelGeometry.Actor(RockCuttingWheelBlock.AXIS_ALONG_FIRST_COORDINATE));
        registered = true;
    }
    private NativeOffroadRenderers() {}
}
