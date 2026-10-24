package dev.ryanhcode.offroad.fabric.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorRenderer;
import com.simibubi.create.content.contraptions.render.VirtualRenderWorld;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Detached geometry shared by the stationary and contraption paths. */
public final class NativeRockCuttingWheelGeometry {
    public static void extract(NativePartsRenderer.Parts parts, Direction facing, boolean alongFirst, float angle) {
        final var pose = new PoseStack();
        NativePartsRenderer.rotateCentered(pose, facing.getRotation());
        if ((facing.getAxis() == Direction.Axis.Z || facing.getAxis() == Direction.Axis.Y) ^ alongFirst) {
            NativePartsRenderer.rotateCentered(pose, Direction.Axis.Z, 90);
            pose.translate(.625, .5, 0);
        } else {
            NativePartsRenderer.rotateCentered(pose, Direction.Axis.X, 90);
            pose.translate(0, .5, -.625);
        }
        NativePartsRenderer.rotateCentered(pose, Direction.Axis.Y, angle);
        parts.add(OffroadPartialModels.ROCK_CUTTING_WHEEL_WHEEL, pose);
    }
    public static final class Actor implements ActorRenderer<NativePartsRenderer.State> {
        private final BooleanProperty alongFirst;
        public Actor(BooleanProperty alongFirst) { this.alongFirst = alongFirst; }
        @Override public NativePartsRenderer.State extract(MovementContext context, VirtualRenderWorld world, float partial, Vec3 camera) {
            if (!(context.temporaryData instanceof LerpedFloat)) context.temporaryData = LerpedFloat.angular();
            final var parts = new NativePartsRenderer.Parts(LightCoordsUtil.getLightCoords(world, context.localPos));
            NativeRockCuttingWheelGeometry.extract(parts, context.state.getValue(BlockStateProperties.FACING),
                    context.state.getValue(this.alongFirst), ((LerpedFloat) context.temporaryData).getValue(partial));
            return NativePartsRenderer.State.from(parts);
        }
        @Override public void submit(NativePartsRenderer.State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            NativePartsRenderer.submitParts(state, pose, collector);
        }
    }
    private NativeRockCuttingWheelGeometry() {}
}
