package dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block;

import com.mojang.math.Axis;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class SwivelBearingPlateBlockRenderer extends NativePartsRenderer<SwivelBearingPlateBlockEntity> {
    public SwivelBearingPlateBlockRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(SwivelBearingPlateBlockEntity entity, float partial, Parts parts) {
        final var pose = rotating(entity, partial, rotationAxis(entity));
        final var facing = entity.getBlockState().getValue(SwivelBearingBlock.FACING);
        pose.translate(.5, .5, .5);
        pose.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
        pose.rotate(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing)));
        pose.translate(-.5, -.5, -.5);
        parts.add(SimPartialModels.SHAFT_SIXTEENTH, pose);
    }
}
