package dev.simulated_team.simulated.fabric.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;

/** Source Flywheel NORTH-facing and CachedBuffers baked-facing conventions expressed as native poses. */
public final class NativePartTransforms {
    public static void faceNorth(PoseStack pose, Direction direction) {
        switch (direction) {
            case NORTH -> { }
            case SOUTH -> NativePartsRenderer.rotateCentered(pose, Direction.Axis.Y, 180);
            case WEST -> NativePartsRenderer.rotateCentered(pose, Direction.Axis.Y, 90);
            case EAST -> NativePartsRenderer.rotateCentered(pose, Direction.Axis.Y, 270);
            case UP -> NativePartsRenderer.rotateCentered(pose, Direction.Axis.X, 90);
            case DOWN -> NativePartsRenderer.rotateCentered(pose, Direction.Axis.X, -90);
        }
    }
    public static void partialFacing(PoseStack pose, Direction direction) {
        NativePartsRenderer.rotateCentered(pose, Direction.Axis.Y, AngleHelper.horizontalAngle(direction));
        NativePartsRenderer.rotateCentered(pose, Direction.Axis.X, AngleHelper.verticalAngle(direction));
    }
    private NativePartTransforms() {}
}
