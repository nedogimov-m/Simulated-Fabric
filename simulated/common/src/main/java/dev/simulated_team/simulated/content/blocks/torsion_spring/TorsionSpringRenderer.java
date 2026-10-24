package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Input, spring and independently driven output are captured as separate parts. */
public final class TorsionSpringRenderer extends NativePartsRenderer<TorsionSpringBlockEntity> {
    public TorsionSpringRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(TorsionSpringBlockEntity entity, float partial, Parts parts) {
        final var facing = entity.getBlockState().getValue(TorsionSpringBlock.FACING);
        final var input = rotating(entity, partial, facing.getAxis());
        face(input, facing.getOpposite());
        parts.add(AllPartialModels.SHAFT_HALF, input);
        final var spring = new PoseStack();
        rotateCentered(spring, facing.getAxis(), entity.interpolatedSpring(partial));
        if (facing.getAxis().isHorizontal())
            rotateCentered(spring, Direction.Axis.Y, AngleHelper.horizontalAngle(facing.getOpposite()));
        rotateCentered(spring, Direction.Axis.X, -90 - AngleHelper.verticalAngle(facing));
        parts.add(SimPartialModels.TORSION_SPRING, spring);
        final var output = rotating(entity.getExtraKinetics(), partial, facing.getAxis());
        face(output, facing);
        parts.add(AllPartialModels.SHAFT_HALF, output);
    }
    private static void face(PoseStack pose, Direction facing) {
        pose.translate(.5, .5, .5);
        pose.rotate(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
        pose.rotate(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing)));
        pose.translate(-.5, -.5, -.5);
    }
}
