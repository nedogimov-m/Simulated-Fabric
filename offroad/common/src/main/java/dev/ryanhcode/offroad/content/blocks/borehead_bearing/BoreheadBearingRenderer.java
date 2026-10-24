package dev.ryanhcode.offroad.content.blocks.borehead_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BoreheadBearingRenderer extends NativePartsRenderer<BoreheadBearingBlockEntity> {
    public BoreheadBearingRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(BoreheadBearingBlockEntity entity, float partial, Parts parts) {
        final var axis = rotationAxis(entity);
        final double time = entity.getLevel().getGameTime() + partial;
        for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
            final Direction direction = Direction.fromAxisAndDirection(axis, sign);
            final var shaft = new PoseStack();
            final float angle = (float) ((sign.getStep() * time * entity.getSpeed() * 0.3) % 360) + rotationOffset(entity, axis);
            rotateCentered(shaft, axis, angle);
            faceSouth(shaft, direction);
            parts.add(AllPartialModels.SHAFT_HALF, shaft);
        }
        final var facing = entity.getBlockState().getValue(BlockStateProperties.FACING);
        final var plate = new PoseStack();
        rotateCentered(plate, facing.getAxis(), entity.getInterpolatedAngle(partial - 1));
        if (facing.getAxis().isHorizontal()) rotateCentered(plate, Direction.Axis.Y, AngleHelper.horizontalAngle(facing.getOpposite()));
        rotateCentered(plate, Direction.Axis.X, -90 - AngleHelper.verticalAngle(facing));
        parts.add(AllPartialModels.BEARING_TOP, plate);
    }
}
