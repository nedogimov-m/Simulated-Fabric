package dev.simulated_team.simulated.ponder.elements.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.ponder.instructions.ModifyRopeInstruction;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.PonderSceneElement;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class RopeStrandElement extends AnimatedSceneElementBase implements AnimatedSceneElement, PonderSceneElement {

    public final PonderRopePose pose;
    public final PonderRopePose lastPose;
    public final PonderRopePose startPose;
    public final PonderRopePose scenePose;

    public RopeStrandElement(final Vec3 from, final Vec3 to, final double length, final double sog, final float floorHeight) {
        this.pose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, floorHeight);
        this.lastPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, floorHeight);
        this.startPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, floorHeight);
        this.scenePose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, floorHeight);
    }

    public RopeStrandElement(final Vec3 from, final Vec3 to, final double length, final double sog) {
        this.pose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
        this.lastPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
        this.startPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
        this.scenePose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
    }

    @Override
    public void reset(final PonderScene scene) {
        super.reset(scene);

        this.pose.set(this.startPose);
        this.lastPose.set(this.startPose);
    }

    public void lerp(final Vec3 from, final Vec3 to, final double length, final double sog, final double t) {
        this.lastPose.set(this.pose);
        this.pose.lerp(from, to, length, sog, t);
    }

    public void set(final PonderRopePose pose) {
        this.lastPose.set(this.pose);
        this.pose.set(pose);
    }

    public ModifyRopeInstruction modify(final int duration) {
        return new ModifyRopeInstruction(duration, this);
    }

    @Override
    public PonderSceneElement.RenderState extractSceneRenderState(final PonderScene scene, final float partial) {
        final var current = new PonderRopePose();
        current.set(this.lastPose);
        current.lerp(this.pose, partial);
        final List<Vector3d> points = new ObjectArrayList<>();
        final var position = new Vector3d();
        final int knots = (int) Math.ceil(current.length) + 1;
        final double extra = current.length - knots;
        for (int i = 0; i < knots; i++) {
            final double t = 1 - (double) i / (knots + extra);
            current.start.lerp(current.end, Math.max(0, t), position);
            final double y = Math.pow(t - .5, 2) * 4;
            position.sub(0, Mth.clamp(1 - y, 0, 1) * current.sog, 0);
            position.y = Math.max(position.y, current.floorHeight);
            points.add(new Vector3d(position));
        }
        final var renderPoints = buildRenderPoints(partial, points);
        final var parts = new NativePartsRenderer.Parts(0xf0); // Original FULL_BLOCK packed light.
        final var pose = new PoseStack();
        this.applyFade(pose, partial);
        pose.translate(current.start.x, current.start.y, current.start.z);
        for (int i = 1; i < renderPoints.size(); i++) {
            final var first = renderPoints.get(i - 1);
            final var second = renderPoints.get(i);
            final double length = second.position().distance(first.position());
            pose.pushPose();
            pose.translate(first.position().x - current.start.x, first.position().y - current.start.y, first.position().z - current.start.z);
            pose.rotate(first.orientation());
            pose.translate(-.5, -.5, -.5);
            pose.pushPose();
            NativePartTransforms.partialFacing(pose, Direction.NORTH);
            parts.add(SimPartialModels.ROPE_KNOT, pose);
            pose.popPose();
            pose.pushPose();
            pose.translate(0, .5, 0);
            pose.scale(1, (float) length, 1);
            NativePartTransforms.partialFacing(pose, Direction.NORTH);
            parts.add(SimPartialModels.ROPE, pose);
            pose.popPose();
            if (i == renderPoints.size() - 1) {
                pose.translate(0, length, 0);
                NativePartTransforms.partialFacing(pose, Direction.NORTH);
                parts.add(SimPartialModels.ROPE_KNOT, pose);
            }
            pose.popPose();
        }
        final var captured = NativePartsRenderer.State.from(parts);
        return (phase, callerPose, collector, camera) -> {
            if (phase == PonderSceneElement.Phase.AFTER_WORLD) NativePartsRenderer.submitParts(captured, callerPose, collector);
        };
    }

    private static @NotNull ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> buildRenderPoints(final float partialTick, final List<Vector3d> inputPoints) {
        final ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> ropeRenderPoints = new ObjectArrayList<>();
        final ObjectArrayList<Vector3d> points = new ObjectArrayList<>(inputPoints);

        while (points.size() >= 2 && points.getFirst().distanceSquared(points.get(1)) < 1e-6) {
            points.removeFirst();
        }

        if (points.size() <= 1) {
            return new ObjectArrayList<>();
        }

        final Vector3dc pointZeroPosition = points.get(0);
        final Vector3dc pointOnePosition = points.get(1);

        final Vector3d normal = pointOnePosition.sub(pointZeroPosition, new Vector3d()).normalize();

        final Quaternionf runningRotation;
        if (normal.dot(OrientedBoundingBox3d.UP) < 0) {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), normal);
            runningRotation.rotateZ((float) Math.PI);
        } else {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), normal);
        }

        ropeRenderPoints.add(new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), new Vector3d(pointZeroPosition)));

        final Vector3d runningNormal = new Vector3d();

        for (int i = 2; i < points.size(); i++) {
            final Vector3d pointA = points.get(i - 1);
            final Vector3d pointB = points.get(i);

            runningNormal.set(pointB)
                    .sub(pointA)
                    .normalize();

            if (runningNormal.dot(OrientedBoundingBox3d.UP) < -0.15) {
                runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), runningNormal));
                runningRotation.rotateZ((float) Math.PI);
            } else {
                runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), runningNormal));
            }

            ropeRenderPoints.add(new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), pointA));
            normal.set(runningNormal);
        }

        ropeRenderPoints.add(new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), points.getLast()));
        return ropeRenderPoints;
    }

}
