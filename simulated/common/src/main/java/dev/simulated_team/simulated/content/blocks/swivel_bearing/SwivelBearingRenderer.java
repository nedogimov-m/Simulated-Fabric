package dev.simulated_team.simulated.content.blocks.swivel_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class SwivelBearingRenderer extends NativePartsRenderer<SwivelBearingBlockEntity> {
    public SwivelBearingRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(SwivelBearingBlockEntity entity, float partial, Parts parts) {
        final var facing = entity.getBlockState().getValue(SwivelBearingBlock.FACING);
        final var input = rotating(entity, partial, rotationAxis(entity));
        face(input, facing.getOpposite(), false);
        parts.add(SimPartialModels.SHAFT_SIXTEENTH, input);
        final var cog = rotating(entity.getExtraKinetics(), partial, rotationAxis(entity));
        face(cog, facing.getOpposite(), true);
        parts.add(SimPartialModels.SWIVEL_BEARING_COG, cog);
        if (!entity.isAssembled()) {
            final var output = rotating(entity, partial, rotationAxis(entity));
            face(output, facing, false);
            parts.add(SimPartialModels.SHAFT_SIXTEENTH, output);
        }
    }
    private static void face(PoseStack pose, Direction facing, boolean verticalModel) {
        pose.translate(.5, .5, .5);
        pose.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
        pose.rotate(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing) + (verticalModel ? 90 : 0)));
        pose.translate(-.5, -.5, -.5);
    }
}
