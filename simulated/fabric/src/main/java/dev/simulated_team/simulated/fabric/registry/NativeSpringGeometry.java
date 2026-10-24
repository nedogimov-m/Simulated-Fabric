package dev.simulated_team.simulated.fabric.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

/** Source spline, parallel-transport frame, quarter-turn twist and both textured tube surfaces. */
public final class NativeSpringGeometry {
    public record Frame(Vector3dc delta,Direction facing,Direction partnerFacing,Vector3dc partnerNormal,
                        Quaterniondc ownerOrientation,Quaterniondc partnerOrientation,double renderLength,float width,float textureWidth,int light,int color) {
        public Frame {delta=new Vector3d(delta);partnerNormal=new Vector3d(partnerNormal);ownerOrientation=new Quaterniond(ownerOrientation);partnerOrientation=new Quaterniond(partnerOrientation);}
    }
    private final Vector3d controlPointA = new Vector3d();
    private final Vector3d controlPointB = new Vector3d();
    private final Vector3d segmentALerp = new Vector3d();
    private final Vector3d segmentBLerp = new Vector3d();
    private final Vector3d segmentCLerp = new Vector3d();
    private final Vector3d startUp = new Vector3d();
    private final Vector3d endUp = new Vector3d();
    private final Vector3d startLeft = new Vector3d();
    private final Vector3d endLeft = new Vector3d();
    private final Vector3d normalizedNormal = new Vector3d();
    private final Vector3d vertex = new Vector3d();

    public static NativeVertexSnapshot extract(Frame frame){return new NativeSpringGeometry().build(frame);}
    public static int stressColor(double distance,double renderLength,double snapDistance,double renderTime,int stressedRed) {
        final double start=Mth.lerp(.7,renderLength-.75,snapDistance);float alpha=0;
        if(distance>start) {
            alpha=Mth.clamp((float)((distance-start)/(snapDistance-start)),0,1)*.3f;
            alpha*=Mth.lerp(.25f,(float)Math.sin(renderTime/3)*.5f+.5f,1);
        }
        return stressedRed&0xffffff|(int)(alpha*255)<<24;
    }
    private NativeVertexSnapshot build(Frame frame) {
        final var out=new NativeVertexSnapshot.Capture();
        if(!frame.delta.isFinite()||frame.delta.lengthSquared()<1E-16||!Double.isFinite(frame.renderLength)||frame.width<=0||frame.textureWidth<=0)return out.finish();
        final var normalA=new Vector3d(frame.facing.getStepX(),frame.facing.getStepY(),frame.facing.getStepZ());
        final var normalB=new Vector3d(frame.partnerNormal);if(!normalB.isFinite()||normalB.lengthSquared()<1E-16)normalB.set(normalA).negate();else normalB.normalize();
        final var points=generateSpline(new Vector3d(),frame.delta,normalA,normalB,frame.delta.length()/5+.25);
        final int total=points.size();final var up=upDirection(frame.facing,frame.delta);final var firstNormal=points.getFirst().normal;
        final var matrix=new Matrix3d(up,firstNormal,up.cross(firstNormal,new Vector3d()));
        double totalLength=0;
        for(int i=0;i<total-1;i++){final var point=points.get(i);final var next=points.get(i+1);totalLength+=point.point.distance(next.point);matrix.rotateLocal(rotation(point.normal,next.normal));}
        if(!Double.isFinite(totalLength)||totalLength<1E-12)return out.finish();
        final var blockA=new Quaterniond(frame.facing.getRotation());
        final var blockB=new Quaterniond(frame.partnerFacing.getRotation()).premul(frame.partnerOrientation).premul(frame.ownerOrientation.conjugate(new Quaterniond()));
        final var orientation=new Quaterniond(blockA).div(blockB).mul(matrix.getNormalizedRotation(new Quaterniond()));
        if(Math.abs(orientation.y())<1E-5)orientation.rotateLocalX(Math.PI);
        final double angle=2*Math.atan2(-orientation.y(),orientation.w()),halfPi=Math.PI/2;
        final double twist=Math.floor((angle+halfPi/2)/halfPi)*halfPi-angle;
        final float uvScale=(float)((frame.renderLength-.75)/totalLength);double running=0;
        matrix.set(up,firstNormal,up.cross(firstNormal,new Vector3d()));final var pose=new PoseStack();
        for(int i=0;i<total-1;i++) {
            final var point=points.get(i);final var next=points.get(i+1);final var startUp=matrix.getColumn(0,new Vector3d());
            matrix.rotateLocal(rotation(point.normal,next.normal));matrix.rotateY(-twist/(total-1));final var endUp=matrix.getColumn(0,new Vector3d());
            final double length=point.point.distance(next.point);
            renderSegment(pose,point.normal,next.normal,startUp,endUp,point.point,next.point,false,(float)running*uvScale,(float)(running+length)*uvScale,frame.light,frame.color,out,frame.width,frame.textureWidth);
            renderSegment(pose,point.normal.negate(new Vector3d()),next.normal.negate(new Vector3d()),startUp.negate(new Vector3d()),endUp.negate(new Vector3d()),point.point,next.point,true,-(float)running*uvScale,-(float)(running+length)*uvScale,frame.light,frame.color,out,frame.width,frame.textureWidth);
            running+=length;
        }
        return out.finish();
    }
    private static Vector3d upDirection(Direction facing,Vector3dc delta) {
        final var normal=new Vector3d(facing.getStepX(),facing.getStepY(),facing.getStepZ());final var projected=new Vector3d(delta).fma(-delta.dot(normal),normal);
        if(projected.lengthSquared()<1E-6)return facing.getAxis().isHorizontal()?new Vector3d(0,1,0):new Vector3d(0,0,-1);
        final var direction=Direction.getApproximateNearest(projected.x,projected.y,projected.z).getOpposite();return new Vector3d(direction.getStepX(),direction.getStepY(),direction.getStepZ());
    }
    private static Quaternionf rotation(Vector3dc start,Vector3dc end) {
        final var cross=start.cross(end,new Vector3d());final var rotation=new Quaternionf((float)cross.x,(float)cross.y,(float)cross.z,1+(float)start.dot(end));
        if(rotation.lengthSquared()<1E-12f)return new Quaternionf().rotationTo((float)start.x(),(float)start.y(),(float)start.z(),(float)end.x(),(float)end.y(),(float)end.z());
        return rotation.normalize();
    }
    /**
     * Generate a spline with equally spaced points (0.5 units)
     */
    private List<SplinePoint> generateSpline(final Vector3dc pointA, final Vector3dc pointB, final Vector3dc normalA, final Vector3dc normalB, final double controlPointLength) {
        final List<SplinePoint> list = new ArrayList<>();

        final double influence = controlPointLength;
        pointA.fma(influence, normalA, this.controlPointA);
        pointB.fma(influence, normalB, this.controlPointB);

        final double len = pointA.distance(pointB);
        final int initialPointCount = Mth.clamp(Mth.ceil(len), 5, 8);
        for (int i = 0; i <= initialPointCount; i++) {
            final double t = (double) i / initialPointCount;
            pointA.lerp(this.controlPointA, t, this.segmentALerp);
            this.controlPointA.lerp(this.controlPointB, t, this.segmentBLerp);
            this.controlPointB.lerp(pointB, t, this.segmentCLerp);

            final Vector3d point = new Vector3d(this.segmentALerp
                    .lerp(this.segmentBLerp, t)
                    .lerp(this.segmentBLerp.lerp(this.segmentCLerp, t), t));

            final Vector3d normal = new Vector3d();

            if (list.isEmpty()) {
                normal.set(normalA);
            } else if (list.size() == initialPointCount) {
                normal.set(normalB).negate();
            } else {
                point.sub(list.get(list.size() - 1).point, normal);
                if(normal.lengthSquared()<1E-16)normal.set(list.getLast().normal);else normal.normalize();
            }

            list.add(new SplinePoint(point, normal));
        }

        return list;
    }

    private void renderSegment(final PoseStack ms,
                               final Vector3dc startDirection,
                               final Vector3dc endDirection,
                               final Vector3dc inputStartUp,
                               final Vector3dc inputEndUp,
                               final Vector3dc startPos,
                               final Vector3dc endPos,
                               final boolean second,
                               final float uvStart,
                               final float uvEnd,
                               final int light,
                               final int color,
                               final VertexConsumer a,
                               final float width,
                               final float textureWidth) {
        inputStartUp.cross(startDirection, this.startLeft).normalize();
        inputEndUp.cross(endDirection, this.endLeft).normalize();

        final float texW = width / textureWidth;
        final double scale = width / 16.0 / 2.0;

        this.startLeft.mul(scale);
        inputStartUp.mul(scale, this.startUp);
        this.endLeft.mul(scale);
        inputEndUp.mul(scale, this.endUp);

        final Vector3d startDown = this.startUp.negate(new Vector3d());
        final Vector3d endDown = this.endUp.negate(new Vector3d());
        final Vector3d startRight = this.startLeft.negate(new Vector3d());
        final Vector3d endRight = this.endLeft.negate(new Vector3d());

        final float uvScale = 16.0f / textureWidth;
        final float uvXOffset = second ? width / textureWidth : 0.0f;
        this.vert(ms, a, startPos.add(this.startLeft, this.vertex).sub(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, startDown, light);
        this.vert(ms, a, endPos.add(this.endLeft, this.vertex).sub(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, endDown, light);
        this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).sub(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, endDown, light);
        this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).sub(this.startUp), color, texW + uvXOffset, uvStart * uvScale, startDown, light);

        this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).add(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, this.startUp, light);
        this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).add(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, this.endUp, light);
        this.vert(ms, a, endPos.add(this.endLeft, this.vertex).add(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, this.endUp, light);
        this.vert(ms, a, startPos.add(this.startLeft, this.vertex).add(this.startUp), color, texW + uvXOffset, uvStart * uvScale, this.startUp, light);

        this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).sub(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, startRight, light);
        this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).sub(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, endRight, light);
        this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).add(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, endRight, light);
        this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).add(this.startUp), color, texW + uvXOffset, uvStart * uvScale, startRight, light);

        this.vert(ms, a, startPos.add(this.startLeft, this.vertex).add(this.startUp), color, 0.0f + uvXOffset, uvStart * uvScale, this.startLeft, light);
        this.vert(ms, a, endPos.add(this.endLeft, this.vertex).add(this.endUp), color, 0.0f + uvXOffset, uvEnd * uvScale, this.endLeft, light);
        this.vert(ms, a, endPos.add(this.endLeft, this.vertex).sub(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, this.endLeft, light);
        this.vert(ms, a, startPos.add(this.startLeft, this.vertex).sub(this.startUp), color, texW + uvXOffset, uvStart * uvScale, this.startLeft, light);
    }

    private void vert(final PoseStack ms, final VertexConsumer a, final Vector3dc pos, final int color, final float u1, final float v1, final Vector3dc normal, final int light) {
        normal.normalize(this.normalizedNormal);
        a.addVertex(ms.last().pose(), (float) pos.x(), (float) pos.y(), (float) pos.z())
                .setColor(color)
                .setUv(u1, v1)
                .setLight(light)
                .setNormal(ms.last(), (float) this.normalizedNormal.x(), (float) this.normalizedNormal.y(), (float) this.normalizedNormal.z());
    }

    private record SplinePoint(Vector3dc point,Vector3dc normal) { }
}
