package dev.simulated_team.simulated.fabric.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import java.util.ArrayList;
import java.util.List;

/** Copied procedural geometry; the deferred callback has no level/entity/camera dependency. */
public record NativeVertexSnapshot(List<Vertex> vertices) implements SubmitNodeCollector.CustomGeometryRenderer {
    public NativeVertexSnapshot{vertices=List.copyOf(vertices);}
    public record Vertex(float x,float y,float z,int color,float u,float v,int light,int overlay,float nx,float ny,float nz,boolean hasOverlay) { }
    @Override public void render(PoseStack.Pose pose,VertexConsumer out) {
        for(var v:vertices) {
            out.addVertex(pose,v.x,v.y,v.z).setColor(v.color).setUv(v.u,v.v).setLight(v.light).setNormal(pose,v.nx,v.ny,v.nz);
            if(v.hasOverlay)out.setOverlay(v.overlay);
        }
    }
    public static final class Capture implements VertexConsumer {
        private final List<Vertex> vertices=new ArrayList<>();
        private float x,y,z,u,v,nx,ny,nz;private int color,light,overlay;private boolean pending,hasOverlay;
        private void flush(){if(pending){vertices.add(new Vertex(x,y,z,color,u,v,light,overlay,nx,ny,nz,hasOverlay));pending=false;}}
        public NativeVertexSnapshot finish(){flush();return new NativeVertexSnapshot(vertices);}
        @Override public VertexConsumer addVertex(float x,float y,float z){flush();this.x=x;this.y=y;this.z=z;u=v=nx=nz=0;ny=1;color=-1;light=overlay=0;hasOverlay=false;pending=true;return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return setColor(a<<24|r<<16|g<<8|b);}
        @Override public VertexConsumer setColor(int color){this.color=color;return this;}
        @Override public VertexConsumer setUv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){overlay=u&0xffff|v<<16;hasOverlay=true;return this;}
        @Override public VertexConsumer setUv2(int u,int v){light=u&0xffff|v<<16;return this;}
        @Override public VertexConsumer setUv3(float u,float v){return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){nx=x;ny=y;nz=z;return this;}
        @Override public VertexConsumer setLineWidth(float width){return this;}
    }
}
