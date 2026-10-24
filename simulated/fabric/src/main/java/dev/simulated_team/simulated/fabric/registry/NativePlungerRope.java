package dev.simulated_team.simulated.fabric.registry;

import dev.simulated_team.simulated.util.CatmulRomSpline;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.List;

/** Original four-sided tube/arc-length UVs and taper, extracted in double precision near its owner. */
public final class NativePlungerRope {
    public static NativeVertexSnapshot extract(List<Vec3> positions,BlockAndTintGetter level,Vec3 renderOrigin) {
        return extract(positions,renderOrigin,pos->net.minecraft.util.LightCoordsUtil.getLightCoords(level,pos));
    }
    public static NativeVertexSnapshot extract(List<Vec3> positions,Vec3 renderOrigin,java.util.function.ToIntFunction<BlockPos> lights) {
        final var builder=new NativeVertexSnapshot.Capture();
        if(positions.size()<4)return builder.finish();
        final var relative=positions.stream().map(point->point.subtract(renderOrigin)).toList();
        final var ORIENTATION=new Quaternionf();final var NEXT_ORIENTATION=new Quaternionf();
        final var POS=new Vector3f();final var NORMAL=new Vector3f();final var NEXT_NORMAL=new Vector3f();
        final var LIGHT_POS=new BlockPos.MutableBlockPos();
        final List<Vec3> splinePoints = new java.util.ArrayList<>(CatmulRomSpline.generateSpline(relative, 4));
        if(splinePoints.isEmpty())return builder.finish();
        if(splinePoints.getFirst().distanceToSqr(relative.getFirst())>1E-12)splinePoints.addFirst(relative.getFirst());
        if(splinePoints.getLast().distanceToSqr(relative.getLast())>1E-12)splinePoints.add(relative.getLast());
        for(int i=0;i+1<splinePoints.size();i++)if(splinePoints.get(i).distanceToSqr(splinePoints.get(i+1))>1E-12){var p=splinePoints.get(i);calculateOrientation(ORIENTATION,p.x,p.y,p.z,splinePoints.get(i+1));break;}

        final int color = 0xFFFFFFFF;
        final float constantRadius = (2f / 16.0f) / 2.0f;
        final float u = 2f / 16F;
        float v = 0;
        float nextV;

        for (int i = 0; i < splinePoints.size() - 1; i++) {
            final float delta = (float) i / (splinePoints.size() - 1);
            final float nextDelta = (float) (i + 1) / (splinePoints.size() - 1);
            final float cableRadius = constantRadius - (0.001f * delta);
            final float nextCableRadius = constantRadius - (0.001f * nextDelta);
            final Vec3 point = splinePoints.get(i);
            final Vec3 nextPoint = splinePoints.get(i + 1);

            final double x = point.x;
            final double y = point.y;
            final double z = point.z;
            final double nextX = nextPoint.x;
            final double nextY = nextPoint.y;
            final double nextZ = nextPoint.z;

            if (i < splinePoints.size() - 2) {
                calculateOrientation(NEXT_ORIENTATION, nextX, nextY, nextZ, splinePoints.get(i + 2));
            } else {
                NEXT_ORIENTATION.set(ORIENTATION);
            }

            final int lightStart = lights.applyAsInt(LIGHT_POS.set(x + renderOrigin.x, y + renderOrigin.y, z + renderOrigin.z));
            final int lightEnd = lights.applyAsInt(LIGHT_POS.set(nextX + renderOrigin.x, nextY + renderOrigin.y, nextZ + renderOrigin.z));

            final double length = Math.sqrt((nextX - x) * (nextX - x) + (nextY - y) * (nextY - y) + (nextZ - z) * (nextZ - z));
            if(length < 1E-8)continue;
            nextV = v + (float) (length * (17 / 16f));

            // Down
            ORIENTATION.transform(NORMAL.set(0, -1, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0, -1, 0));

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            // Up
            ORIENTATION.transform(NORMAL.set(0, 1, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0, 1, 0));

            ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            // West
            ORIENTATION.transform(NORMAL.set(-1, 0, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(-1, 0, 0));

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            // East
            ORIENTATION.transform(NORMAL.set(1, 0, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(1, 0, 0));

            ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0));
            builder.addVertex((float) (x + POS.x), (float) (y + POS.y), (float) (z + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX + POS.x), (float) (nextY + POS.y), (float) (nextZ + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.set(NEXT_ORIENTATION);
            v = nextV;
        }
        return builder.finish();
    }
    private static void calculateOrientation(Quaternionf target,double x,double y,double z,Vec3 next) {
        final double dx=next.x-x,dy=next.y-y,dz=next.z-z,length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(length<1E-8)return;
        target.identity().rotateY((float)Math.atan2(dx,dz)).rotateX((float)(Math.acos(Math.clamp(dy/length,-1,1))-Math.PI/2));
    }
    private NativePlungerRope(){ }
}
