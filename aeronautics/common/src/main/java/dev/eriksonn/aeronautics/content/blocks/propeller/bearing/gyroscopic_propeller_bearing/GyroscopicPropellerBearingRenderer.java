package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class GyroscopicPropellerBearingRenderer extends NativePartsRenderer<GyroscopicPropellerBearingBlockEntity> {
    public GyroscopicPropellerBearingRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(GyroscopicPropellerBearingBlockEntity entity, float partial, Parts parts) {
        final Direction facing = entity.getBlockState().getValue(BlockStateProperties.FACING);
        final var shaft = rotating(entity, partial, rotationAxis(entity));
        faceSouth(shaft, facing.getOpposite());
        parts.add(AllPartialModels.SHAFT_HALF, shaft);
        final var normal = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        final var tilt = new Quaternionf(entity.previousTiltQuat).slerp(entity.tiltQuat, partial);
        final var transformed = new Quaternionf(tilt).conjugate()
                .mul(new Quaternionf((float) normal.x, (float) normal.y, (float) normal.z, 0)).mul(tilt);
        final var contraptionNormal = new Vec3(transformed.x(), transformed.y(), transformed.z());
        final var plate = new PoseStack();
        plate.translate(normal.scale(4 / 16f));
        rotateCentered(plate, tilt);
        plate.translate(normal.scale(-4 / 16f));
        rotateCentered(plate, facing.getAxis(), entity.getInterpolatedAngle(partial - 1));
        if (facing.getAxis().isHorizontal()) rotateCentered(plate, Direction.Axis.Y, AngleHelper.horizontalAngle(facing.getOpposite()));
        rotateCentered(plate, Direction.Axis.X, -90 - AngleHelper.verticalAngle(facing));
        parts.add(AeroPartialModels.BEARING_PLATE_METAL, plate);
        for (int index = 0; index < 4; index++) {
            Vec3 position = VecHelper.rotate(new Vec3(5.9 / 16, 0, 0), -90 * index, Direction.Axis.Y);
            if (facing.getAxis().isHorizontal()) {
                position = VecHelper.rotate(position, AngleHelper.horizontalAngle(facing), Direction.Axis.Z);
                position = VecHelper.rotate(position, -90 + AngleHelper.verticalAngle(facing), Direction.Axis.X);
            }
            final double distance = position.dot(contraptionNormal) / normal.dot(contraptionNormal);
            position = position.add(normal.scale(distance + 3 / 16d));
            final var head = new PoseStack();
            final var pole = new PoseStack();
            head.translate(position); pole.translate(position);
            head.translate(.5, .5, .5); pole.translate(.5, .5, .5);
            head.rotate(tilt);
            int orientation = index;
            if (facing == Direction.DOWN) {
                final var rotation = index % 2 == 0 ? Axis.XP.rotationDegrees(180) : Axis.ZP.rotationDegrees(180);
                head.rotate(rotation); pole.rotate(rotation);
            }
            if (facing.getAxis().isHorizontal()) {
                head.rotateDegrees(Axis.YP, AngleHelper.horizontalAngle(facing.getOpposite()));
                pole.rotateDegrees(Axis.YP, AngleHelper.horizontalAngle(facing.getOpposite()));
                head.rotateDegrees(Axis.XP, -90 + AngleHelper.verticalAngle(facing));
                pole.rotateDegrees(Axis.XP, -90 + AngleHelper.verticalAngle(facing));
                orientation = 2 - orientation;
            }
            pole.translate(0, .5 / 16, 0);
            head.rotateDegrees(Axis.YP, -90 * orientation);
            pole.rotateDegrees(Axis.YP, -90 * orientation);
            parts.add(AeroPartialModels.GYRO_BEARING_PISTON_HEAD, head);
            parts.add(AeroPartialModels.GYRO_BEARING_PISTON_POLE, pole);
        }
    }
}
