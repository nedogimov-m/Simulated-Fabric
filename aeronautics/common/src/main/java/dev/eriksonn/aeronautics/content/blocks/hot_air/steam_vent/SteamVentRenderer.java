package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.LightCoordsUtil;

public class SteamVentRenderer extends NativePartsRenderer<SteamVentBlockEntity> {
    public SteamVentRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(SteamVentBlockEntity entity, float partial, Parts parts) {
        parts.add(AeroPartialModels.STEAM_VENT_REDSTONE, new PoseStack(),
                SimColors.redstone(Math.max(0, entity.signalStrength / 15f)), ChunkSectionLayer.CUTOUT, false, parts.light());
        final var handler = entity.getRenderHandler();
        final int alpha = handler.getAlpha(partial);
        if (alpha <= 2) return;
        final var pose = new PoseStack();
        final int color = alpha << 24 | 0xFFFFFF;
        parts.add(AeroPartialModels.STEAM_VENT_BASE, pose, color, ChunkSectionLayer.TRANSLUCENT, true, LightCoordsUtil.FULL_BRIGHT);
        pose.translate(0, (handler.getPosition(partial) - 1) / 3f, 0);
        parts.add(AeroPartialModels.STEAM_VENT_JET, pose, color, ChunkSectionLayer.TRANSLUCENT, true, LightCoordsUtil.FULL_BRIGHT);
    }
}
