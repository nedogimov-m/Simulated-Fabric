package dev.simulated_team.simulated.content.blocks.directional_gearshift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Barrel mechanism plus both independently driven native split-shaft ports. */
public final class DirectionalGearshiftRenderer extends NativePartsRenderer<DirectionalGearshiftBlockEntity> {
    public DirectionalGearshiftRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(DirectionalGearshiftBlockEntity entity, float partial, Parts parts) {
        final var state = entity.getBlockState();
        final var axis = rotationAxis(entity);
        final var direction = state.getValue(DirectionalGearshiftBlock.FACING);
        final boolean left = state.getValue(DirectionalGearshiftBlock.LEFT_POWERED);
        final boolean right = state.getValue(DirectionalGearshiftBlock.RIGHT_POWERED);
        final float raw = entity.getAngle(partial);
        float angle = 0, shaftAngle = 0;
        if (entity.hasSource() && !left && right) shaftAngle = raw;
        if (entity.hasSource() && left && !right)
            angle = raw * entity.getRotationSpeedModifier(entity.getSourceFacing().getOpposite()) + rotationOffset(entity, axis);
        final boolean vertical = axis.isVertical() || (direction.getAxis().isVertical()
                && !state.getValue(DirectionalGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE));
        final var barrel = new PoseStack();
        rotateCentered(barrel, axis, angle);
        NativePartTransforms.faceNorth(barrel, direction);
        if (vertical) rotateCentered(barrel, Direction.Axis.Z, 90);
        parts.add(SimPartialModels.DIRECTIONAL_GEARSHIFT_CENTER, barrel);
        for (int side = 0; side < 2; side++) {
            final var pose = new PoseStack();
            pose.mulPose(barrel.last().pose());
            if (side == 0) rotateCentered(pose, Direction.Axis.Z, 180);
            rotateCentered(pose, Direction.Axis.Y, shaftAngle);
            parts.add(SimPartialModels.DIRECTIONAL_GEARSHIFT_BARREL_SHAFT, pose);
        }
        for (final var face : Direction.values()) {
            if (face.getAxis() != axis) continue;
            final var pose = new PoseStack();
            rotateCentered(pose, axis, raw * entity.getRotationSpeedModifier(face) + rotationOffset(entity, axis));
            NativePartTransforms.partialFacing(pose, face);
            parts.add(AllPartialModels.SHAFT_HALF, pose);
        }
    }
}
