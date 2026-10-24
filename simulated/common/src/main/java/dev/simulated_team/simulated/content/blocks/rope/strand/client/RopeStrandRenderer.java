package dev.simulated_team.simulated.content.blocks.rope.strand.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Objects;

public class RopeStrandRenderer {
    public record RopeRenderPoint(Quaternionf orientation, Vector3d position) { }

    public static void extract(final SmartBlockEntity be, final RopeStrandHolderBehavior holder, final float partial, final NativePartsRenderer.Parts parts) {
        final Level level = be.getLevel();
        final ClientRopeStrand strand = holder.getClientStrand();
        if (level == null || !holder.ownsRope() || strand == null || strand.getPoints().size() <= 1) return;
        final var global = buildRenderPoints(partial, strand.getPoints());
        if (global.isEmpty()) return;
        final BlockPos owner = be.getBlockPos();
        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        final Pose3dc containing = subLevel instanceof ClientSubLevel client ? client.renderPose() : null;
        final var local = new ObjectArrayList<RopeRenderPoint>(global.size());
        for (final var point : global) {
            final var position = new Vector3d(point.position());
            final var orientation = new Quaternionf(point.orientation());
            if (containing != null) {
                containing.transformPositionInverse(position);
                orientation.premul(new Quaternionf(containing.orientation()).conjugate());
            }
            local.add(new RopeRenderPoint(orientation, position));
        }
        for (int i = 1; i < local.size(); i++) {
            final var point = local.get(i - 1);
            final var worldPosition = global.get(i - 1).position();
            final double length = worldPosition.distance(global.get(i).position());
            final int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(worldPosition.x, worldPosition.y, worldPosition.z));
            final var pose = new PoseStack();
            pose.translate(point.position().x - owner.getX(), point.position().y - owner.getY(), point.position().z - owner.getZ());
            pose.rotate(point.orientation());
            pose.translate(-.5, -.5, -.5);
            if (i > 1) {
                pose.pushPose();
                NativePartTransforms.partialFacing(pose, Direction.NORTH);
                parts.add(SimPartialModels.ROPE_KNOT, pose, -1, null, false, light);
                pose.popPose();
            }
            pose.translate(0, .5, 0);
            pose.scale(1, (float) length, 1);
            NativePartTransforms.partialFacing(pose, Direction.NORTH);
            parts.add(SimPartialModels.ROPE, pose, -1, null, false, light);
        }
        if (Objects.equals(ZiplineClientManager.hoveringRope, strand.getUuid())) extractOutline(parts, 3f / 16, local, owner);
    }

    private record OutlineLine(org.joml.Vector3fc start, org.joml.Vector3fc end) {
        private OutlineLine { start = new org.joml.Vector3f(start); end = new org.joml.Vector3f(end); }
    }
    private static void extractOutline(NativePartsRenderer.Parts parts, float radius, List<RopeRenderPoint> points, BlockPos owner) {
        final var lines = new java.util.ArrayList<OutlineLine>();
        final Vector3d[] corners = {new Vector3d(-radius, 0, -radius), new Vector3d(-radius, 0, radius),
                new Vector3d(radius, 0, radius), new Vector3d(radius, 0, -radius)};
        for (int i = 0; i <= points.size(); i++) {
            final var first = points.get(Math.max(0, i - 1));
            final var second = points.get(Math.min(points.size() - 1, i));
            for (final var corner : corners) {
                final var a = new Vector3d(corner);
                final var b = new Vector3d(corner);
                if (i == 0) a.rotateY(Math.PI / 2);
                if (i == points.size()) b.rotateY(Math.PI / 2);
                first.orientation().transform(a).add(first.position()).sub(owner.getX(), owner.getY(), owner.getZ());
                second.orientation().transform(b).add(second.position()).sub(owner.getX(), owner.getY(), owner.getZ());
                lines.add(new OutlineLine(new org.joml.Vector3f(a), new org.joml.Vector3f(b)));
            }
        }
        final var captured = List.copyOf(lines);
        parts.addGeometry(RenderTypes.lines(), (pose, vertices) -> {
            for (final var line : captured) {
                vertices.addVertex(pose, line.start()).setColor(0f, 0f, 0f, .4f).setLineWidth(1);
                vertices.addVertex(pose, line.end()).setColor(0f, 0f, 0f, .4f).setLineWidth(1);
            }
        }, new PoseStack());
    }

    private static @NotNull ObjectArrayList<RopeRenderPoint> buildRenderPoints(final float partialTick, final List<ClientRopePoint> inputPoints) {
        final ObjectArrayList<RopeRenderPoint> ropeRenderPoints = new ObjectArrayList<>();
        final ObjectArrayList<ClientRopePoint> points = new ObjectArrayList<>(inputPoints);

        while (points.size() >= 2 && points.getFirst().position().distanceSquared(points.get(1).position()) < 1e-3) {
            points.removeFirst();
        }

        if (points.size() <= 1) {
            return new ObjectArrayList<>();
        }

        final Vector3dc pointZeroPosition = points.get(0).renderPos(partialTick, new Vector3d());
        final Vector3dc pointOnePosition = points.get(1).renderPos(partialTick, new Vector3d());

        final Vector3d normal = pointOnePosition.sub(pointZeroPosition, new Vector3d()).normalize();

        final Quaternionf runningRotation;
        if (normal.dot(OrientedBoundingBox3d.UP) < 0) {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), normal);
            runningRotation.rotateZ((float) Math.PI);
        } else {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), normal);
        }

        ropeRenderPoints.add(new RopeRenderPoint(new Quaternionf(runningRotation), new Vector3d(pointZeroPosition)));

        final Vector3d runningNormal = new Vector3d();

        final Vector3d bPos = new Vector3d();
        final Vector3d aPos = new Vector3d();

        for (int i = 2; i < points.size(); i++) {
            final ClientRopePoint pointA = points.get(i - 1);
            final ClientRopePoint pointB = points.get(i);

            runningNormal.set(pointB.renderPos(partialTick, bPos))
                    .sub(pointA.renderPos(partialTick, aPos))
                    .normalize();

            if (runningNormal.dot(OrientedBoundingBox3d.UP) < -0.15) {
                runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), runningNormal));
                runningRotation.rotateZ((float) Math.PI);
            } else {
                runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), runningNormal));
            }

            ropeRenderPoints.add(new RopeRenderPoint(new Quaternionf(runningRotation), pointA.renderPos(partialTick, new Vector3d())));
            normal.set(runningNormal);
        }

        ropeRenderPoints.add(new RopeRenderPoint(new Quaternionf(runningRotation), points.getLast().renderPos(partialTick, new Vector3d())));
        return ropeRenderPoints;
    }
}
