package dev.eriksonn.aeronautics.fabric.registry;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.vanilla.PlotSectionMesh;
import dev.ryanhcode.sable.sublevel.render.vanilla.PlotSectionMeshExtension;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import dev.eriksonn.aeronautics.fabric.registry.LevititeQuadSubdivision.Vertex;
import java.util.ArrayList;
import java.util.List;

/** Fixed four-by-four bilinear quad subdivision replaces actual tessellation stages. */
public record NativeLevititeMesh(SectionPos section, List<Vertex> vertices, boolean dynamicShading)
        implements PlotSectionMeshExtension.Geometry {
    public NativeLevititeMesh { vertices=List.copyOf(vertices); if(vertices.size()%4!=0)throw new IllegalArgumentException("Incomplete levitite quad"); }
    public static boolean accepts(BlockState state) {
        final var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("aeronautics") && (id.getPath().equals("levitite") || id.getPath().equals("pearlescent_levitite"));
    }
    @Override public PlotSectionMeshExtension.Snapshot extract(ClientSubLevel owner, PlotSectionMesh.Snapshot section, float partialTick) {
        return NativeLevititeRenderer.extract(this, owner, section, partialTick);
    }
    public static final class Collector implements PlotSectionMeshExtension.Collector {
        private final SectionPos section;
        private final boolean dynamic;
        private final List<Vertex> vertices=new ArrayList<>();
        public Collector(SectionPos section,boolean dynamic){this.section=section;this.dynamic=dynamic;}
        @Override public void quad(BlockState state,float x,float y,float z,BakedQuad quad,QuadInstance instance) {
            if(!accepts(state))return;
            final var capture=new Capture(quad.direction().getStepX(),quad.direction().getStepY(),quad.direction().getStepZ());
            capture.putBlockBakedQuad(x,y,z,quad,instance);
            vertices.addAll(LevititeQuadSubdivision.subdivide(capture.finish()));
        }
        @Override public NativeLevititeMesh finish(){return vertices.isEmpty()?null:new NativeLevititeMesh(section,vertices,dynamic);}
    }
    private static final class Capture implements VertexConsumer {
        private final List<Vertex> vertices=new ArrayList<>(4);
        private float x,y,z,u,v,nx,ny,nz;private int color,light;private boolean pending;
        Capture(float nx,float ny,float nz){this.nx=nx;this.ny=ny;this.nz=nz;}
        private void flush(){if(pending){vertices.add(new Vertex(x,y,z,color,u,v,light,nx,ny,nz));pending=false;}}
        List<Vertex> finish(){flush();return List.copyOf(vertices);}
        @Override public VertexConsumer addVertex(float x,float y,float z){flush();this.x=x;this.y=y;this.z=z;color=-1;light=0;u=v=0;pending=true;return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return setColor(a<<24|r<<16|g<<8|b);}
        @Override public VertexConsumer setColor(int color){this.color=color;return this;}
        @Override public VertexConsumer setUv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){light=u&0xffff|v<<16;return this;}
        @Override public VertexConsumer setUv3(float u,float v){return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){nx=x;ny=y;nz=z;return this;}
        @Override public VertexConsumer setLineWidth(float width){return this;}
    }
}
