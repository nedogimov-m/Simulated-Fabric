package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.joml.Quaternionf;
import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

public final class AltitudeSensorRenderer extends NativePartsRenderer<AltitudeSensorBlockEntity> {
    public AltitudeSensorRenderer(BlockEntityRendererProvider.Context context) {}
    public static float calculateLinearDial(float minHeight, float maxHeight, float height) {
        return Math.min(Math.max((height - minHeight) / (maxHeight - minHeight), 0), 1);
    }
    @Override protected void extractParts(AltitudeSensorBlockEntity entity, float partial, Parts parts) {
        extractGauge(entity.getBlockState(), entity.tickCount, entity.getValue(), entity.getVisualHeight(partial), partial, parts);
    }
    /** Shared extraction for placed and moving gauges; all world inputs are resolved by the caller. */
    public static void extractGauge(BlockState state, int ticks, float value, float visualHeight, float partial, Parts parts) {
        final var direction = state.getValue(HORIZONTAL_FACING);
        final boolean radial = state.getValue(AltitudeSensorBlock.DIAL) == AltitudeSensorBlock.FaceType.RADIAL;
        final var face = new PoseStack();
        final var dial = new PoseStack();
        final var indicator = new PoseStack();
        if (radial) rotateCentered(dial, new Quaternionf().rotationAxis(-(float) (visualHeight * Math.PI / 2),
                direction.getStepX(), direction.getStepY(), direction.getStepZ()));
        else dial.translate(0, (value * 8 - 4) / 16f, 0);
        final float time = ticks + partial;
        final float wobble = (float) (-Math.sin(time * .8) * Math.exp(-time / 3.5)) * .7f;
        final float y = direction.getAxis() != Direction.Axis.Z ? direction.getOpposite().toYRot() : direction.toYRot();
        rotateCentered(face, Direction.Axis.Y, y + 180);
        rotateCentered(face, Direction.Axis.X, -(float) Math.toDegrees(wobble));
        rotateCentered(dial, Direction.Axis.Y, y + 180);
        rotateCentered(dial, Direction.Axis.X, -(float) Math.toDegrees(wobble));
        rotateCentered(indicator, Direction.Axis.Y, y + 180);
        final var attach = state.getValue(AltitudeSensorBlock.FACE);
        rotateCentered(indicator, Direction.Axis.X, attach == AttachFace.WALL ? -90 : attach == AttachFace.CEILING ? -180 : 0);
        parts.add(radial ? SimPartialModels.ALTITUDE_SENSOR_RADIAL_CASE : SimPartialModels.ALTITUDE_SENSOR_LINEAR_CASE, face);
        parts.add(radial ? SimPartialModels.ALTITUDE_SENSOR_RADIAL_HAND : SimPartialModels.ALTITUDE_SENSOR_LINEAR_HAND, dial);
        parts.add(SimPartialModels.ALTITUDE_SENSOR_INDICATOR, indicator, SimColors.redstone(value),
                ChunkSectionLayer.CUTOUT, false, parts.light());
    }
}
