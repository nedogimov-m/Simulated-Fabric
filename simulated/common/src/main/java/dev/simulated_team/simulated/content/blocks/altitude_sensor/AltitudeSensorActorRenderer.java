package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorRenderer;
import com.simibubi.create.content.contraptions.render.VirtualRenderWorld;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

/** Actual Create26.3 actor API; submission reads only the immutable gauge snapshot. */
public final class AltitudeSensorActorRenderer implements ActorRenderer<NativePartsRenderer.State> {
    @Override public NativePartsRenderer.State extract(MovementContext context, VirtualRenderWorld world, float partial, Vec3 camera) {
        if (context.position == null) return null;
        final float low = context.blockEntityData.getFloatOr("low_signal", 0f);
        final float high = context.blockEntityData.getFloatOr("high_signal", 0f);
        final float visualHeight;
        if (context.temporaryData instanceof Pair<?, ?> heights
                && heights.getLeft() instanceof Number previous && heights.getRight() instanceof Number current)
            visualHeight = Mth.lerp(partial, previous.floatValue(), current.floatValue());
        else visualHeight = (float) Sable.HELPER.projectOutOfSubLevel(context.world, JOMLConversion.toJOML(context.position)).y;
        final var level = context.contraption.entity.level();
        final float y = (float) Mth.map(context.position.y, level.getMinY(), level.getMaxY() + 1, 0, 1);
        final float value = Mth.clampedMap(y, 0, 1, low, high);
        final var parts = new NativePartsRenderer.Parts(LightCoordsUtil.getLightCoords(world, context.localPos));
        AltitudeSensorRenderer.extractGauge(context.state, 1000, value, visualHeight, partial, parts);
        return NativePartsRenderer.State.from(parts);
    }
    @Override public void submit(NativePartsRenderer.State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        NativePartsRenderer.submitParts(state, pose, collector);
    }
}
