package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eriksonn.aeronautics.fabric.registry.NativeBurnerFlame;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Extracts the indicator and complete billboard payload before native batched submission. */
public class HotAirBurnerRenderer extends NativePartsRenderer<HotAirBurnerBlockEntity> {
    public HotAirBurnerRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(HotAirBurnerBlockEntity entity, float partial, Parts parts) {
        final float strength = Math.max(0, entity.getSignalStrength() / 15f);
        parts.add(AeroPartialModels.HOT_AIR_BURNER_INDICATOR, new PoseStack(), SimColors.redstone(strength),
                ChunkSectionLayer.CUTOUT, false, parts.light());
        if (strength <= 0) return;
        final var client = Minecraft.getInstance();
        Vec3 camera = client.gameRenderer.mainCamera().position();
        if (entity.getLevel() instanceof PonderLevel && client.getCameraEntity() != null)
            camera = client.getCameraEntity().getPosition(partial);
        final var sublevel = Sable.HELPER.getContaining(entity);
        if (sublevel != null) camera = sublevel.logicalPose().transformPositionInverse(camera);
        final var center = Vec3.atCenterOf(entity.getBlockPos());
        final float angle = (float) Math.atan2(camera.z() - center.z(), camera.x() - center.x());
        final var pose = new PoseStack();
        pose.translate(-.5, .35, .5);
        pose.rotateAround(Axis.YP.rotation((float) (-angle + Math.PI * .5)), 1, 0, 0);
        final float time = (float) Mth.lerp(partial, entity.lastRenderTime, entity.renderTime) + entity.getTimeOffset();
        final boolean soul = entity.getBlockState().getValue(HotAirBurnerBlock.VARIANT) != HotAirBurnerBlock.Variant.FIRE;
        NativeBurnerFlame.extract(parts, pose, time, entity.getFlameIntensity(partial), soul);
    }
}
