package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class RedstoneInductorRenderer extends NativePartsRenderer<RedstoneInductorBlockEntity> {
    public RedstoneInductorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(RedstoneInductorBlockEntity entity, float partial, Parts parts) {
        final var pose = new PoseStack();
        final var facing = entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.translate(.5, 0, .5);
        pose.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
        parts.add(SimPartialModels.REDSTONE_INDUCTOR_INDICATOR, pose,
                SimColors.redstone(entity.lerpedState.getValue(partial) / 15F),
                ChunkSectionLayer.CUTOUT, false, parts.light());
    }
}
