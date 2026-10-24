package dev.simulated_team.simulated.fabric.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector4fc;

/** Source four-sided tapered beam, copied before native submission. Colours retain the source R/B/G convention. */
public final class NativeLaserGeometry {
    public static PoseStack pose(Direction facing,float scale) {
        final var pose=new PoseStack();pose.translate(.5,.5,.5);pose.rotate(facing.getRotation());
        pose.rotate(Axis.XP.rotationDegrees(-90));pose.translate(0,0,.5-.0625);pose.scale(scale,scale,1);pose.translate(-.5,-.5,0);
        return pose;
    }
    public static NativeVertexSnapshot extract(Vector4fc color,float maximum,float length) {
        final var out=new NativeVertexSnapshot.Capture();
        if(!Float.isFinite(maximum)||!Float.isFinite(length)||maximum<=0||length<=0||color.w()<=0)return out.finish();
        final float fraction=Math.clamp(length/maximum,0,1),offset=fraction/10,endU=1+1/length;
        final var pose=new PoseStack();
        for(int face=0;face<4;face++) {
            vertex(out,pose,0,0,0,color,color.w(),0,endU);
            vertex(out,pose,1,0,0,color,color.w(),0,endU);
            vertex(out,pose,1+offset,-offset,length+.5f,color,color.w()*(1-fraction),endU,endU);
            vertex(out,pose,-offset,-offset,length+.5f,color,color.w()*(1-fraction),endU,endU);
            pose.translate(.5,.5,.5);pose.rotate(Axis.ZN.rotationDegrees(90));pose.translate(-.5,-.5,-.5);
        }
        return out.finish();
    }
    private static void vertex(NativeVertexSnapshot.Capture out,PoseStack pose,float x,float y,float z,Vector4fc color,float alpha,float u,float v) {
        out.addVertex(pose.last(),x,y,z).setColor(color.x(),color.z(),color.y(),alpha).setUv(u,v).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(0,1,0);
    }
    private NativeLaserGeometry() { }
}
