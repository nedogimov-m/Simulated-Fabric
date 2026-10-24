package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

public final class VelocitySensorRenderer extends NativePartsRenderer<VelocitySensorBlockEntity> {
    public VelocitySensorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(VelocitySensorBlockEntity entity, float partial, Parts parts) {
        final var state = entity.getBlockState();
        final boolean axis = state.getValue(VelocitySensorBlock.AXIS_ALONG_FIRST_COORDINATE);
        boolean front = state.getValue(VelocitySensorBlock.POWERED) == 1;
        front = switch (state.getValue(VelocitySensorBlock.FACING)) {
            case NORTH, EAST -> !front;
            case SOUTH, DOWN -> axis == front;
            case WEST -> axis != front;
            case UP -> front;
        };
        final int color = SimColors.redstone(entity.getRedstoneStrength() / 15f);
        final var diode = new PoseStack();
        transform(diode, state);
        parts.add(SimPartialModels.VELOCITY_SENSOR_DIODE, diode, front ? color : SimColors.REDSTONE_OFF,
                ChunkSectionLayer.CUTOUT, false, parts.light());
        final var back = new PoseStack();
        transform(back, state);
        rotateCentered(back, Direction.Axis.Y, 180);
        parts.add(SimPartialModels.VELOCITY_SENSOR_DIODE, back, front ? SimColors.REDSTONE_OFF : color,
                ChunkSectionLayer.CUTOUT, false, parts.light());
        final var fan = new PoseStack();
        final Direction direction = AbstractDirectionalAxisBlock.getDirectionOfAxis(state);
        rotateCentered(fan, new Quaternionf().rotationAxis(entity.getFanAngle(partial), direction.getStepX(), direction.getStepY(), direction.getStepZ()));
        transform(fan, state);
        parts.add(SimPartialModels.VELOCITY_SENSOR_FAN, fan, -1, ChunkSectionLayer.CUTOUT, false, parts.light());
    }
    private static void transform(PoseStack pose, BlockState state) {
        final var facing = state.getValue(VelocitySensorBlock.FACING);
        if (state.getValue(VelocitySensorBlock.AXIS_ALONG_FIRST_COORDINATE) == (facing.getStepX() == 0))
            rotateCentered(pose, new Quaternionf().rotationAxis((float) (Math.PI / 2), facing.getStepX(), facing.getStepY(), facing.getStepZ()));
        rotateCentered(pose, facing.getRotation());
    }
}
