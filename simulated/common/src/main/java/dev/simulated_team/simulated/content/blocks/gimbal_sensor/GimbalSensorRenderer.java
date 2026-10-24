package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

/** Filter/value boxes are handled by Create's native global behaviour renderer. */
public final class GimbalSensorRenderer extends NativePartsRenderer<GimbalSensorBlockEntity> {
    public GimbalSensorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override protected void extractParts(GimbalSensorBlockEntity entity, float partial, Parts parts) {
        for (final var direction : SimDirectionUtil.Y_AXIS_PLANE) {
            final var pose = new PoseStack();
            pose.translate(.5, 0, .5);
            // Original Flywheel Rotate.rotateToFace uses NORTH as its identity orientation.
            if (direction == Direction.SOUTH) pose.rotate(Axis.YP.rotationDegrees(180));
            else if (direction == Direction.WEST) pose.rotate(Axis.YP.rotationDegrees(90));
            else if (direction == Direction.EAST) pose.rotate(Axis.YP.rotationDegrees(270));
            pose.translate(0, 0, -.5);
            parts.add(SimPartialModels.GIMBAL_SENSOR_INDICATOR, pose,
                    SimColors.redstone(Math.max(entity.getPower(direction), 0) / 15f),
                    ChunkSectionLayer.CUTOUT, false, parts.light());
        }
        final var rotation = new Quaternionf(entity.getBaseQuaternion());
        entity.applyPrimaryQuaternion(rotation, partial);
        add(parts, SimPartialModels.GIMBAL_SENSOR_GIMBAL, rotation);
        entity.applySecondaryQuaternion(rotation, partial);
        add(parts, SimPartialModels.GIMBAL_SENSOR_COMPASS, rotation);
        entity.applyCompassQuaternion(rotation, partial);
        add(parts, SimPartialModels.GIMBAL_SENSOR_NEEDLE, rotation);
    }

    private static void add(Parts parts, ExtraModelKey<BlockStateModel> key, Quaternionf rotation) {
        final var pose = new PoseStack();
        rotateCentered(pose, new Quaternionf(rotation));
        pose.translate(.5, .5, .5);
        parts.add(key, pose, -1, ChunkSectionLayer.CUTOUT, false, parts.light());
    }
}
