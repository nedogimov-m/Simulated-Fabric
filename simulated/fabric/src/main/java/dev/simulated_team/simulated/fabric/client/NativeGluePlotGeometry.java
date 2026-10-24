package dev.simulated_team.simulated.fabric.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.createmod.catnip.outliner.Outline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Copies plot-local glue outlines into world coordinates during native extraction. */
public final class NativeGluePlotGeometry {
    public static ClientSubLevel owner(AABB bounds) {
        final var owner = Sable.HELPER.getContainingClient(bounds.getCenter());
        return owner != null && !owner.isRemoved() ? owner : null;
    }
    public static float partial() { return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false); }
    public static Outline.Geometry project(AABB bounds, Outline.Geometry geometry) {
        final var owner = owner(bounds);
        if (owner == null) return geometry;
        final var pose = owner.renderPose(partial());
        final var segments = geometry.segments().stream().map(line -> new Outline.Segment(
                pose.transformPosition(line.start()), pose.transformPosition(line.end()), false)).toList();
        final var faces = geometry.faces().stream().map(face -> {
            final var normal = new Vector3f(face.normal().getStepX(), face.normal().getStepY(), face.normal().getStepZ());
            pose.orientation().transform(normal);
            return new Outline.Face(pose.transformPosition(face.a()), pose.transformPosition(face.b()),
                    pose.transformPosition(face.c()), pose.transformPosition(face.d()),
                    Direction.getNearest((int) (normal.x * 10000), (int) (normal.y * 10000), (int) (normal.z * 10000), Direction.UP),
                    face.texture(), face.color(), face.maxU(), face.maxV(), face.cull());
        }).toList();
        return new Outline.Geometry(segments, faces, geometry.items(), geometry.color(), geometry.width(), geometry.light(), geometry.disableNormals());
    }
    public static int box(DrawableGizmoPrimitives output, AABB bounds, int color, float width, ClientSubLevel owner) {
        final var pose = owner.renderPose(partial());
        final var corners = new Vec3[8];
        for (int i = 0; i < 8; i++) corners[i] = pose.transformPosition(new Vec3(
                (i & 1) == 0 ? bounds.minX : bounds.maxX, (i & 2) == 0 ? bounds.minY : bounds.maxY,
                (i & 4) == 0 ? bounds.minZ : bounds.maxZ));
        for (int i = 0; i < 8; i++) for (int bit : new int[]{1, 2, 4})
            if ((i & bit) == 0) output.addLine(corners[i], corners[i | bit], color, width);
        return 12;
    }
    private NativeGluePlotGeometry() {}
}
