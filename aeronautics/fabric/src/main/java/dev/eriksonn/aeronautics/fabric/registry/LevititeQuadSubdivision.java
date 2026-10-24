package dev.eriksonn.aeronautics.fabric.registry;

import java.util.ArrayList;
import java.util.List;

/** Immutable bilinear CPU replacement for the original fixed four-by-four tessellation patches. */
public final class LevititeQuadSubdivision {
    public static final int SUBDIVISIONS=4;
    public record Vertex(float x,float y,float z,int color,float u,float v,int light,float nx,float ny,float nz) { }
    public static List<Vertex> subdivide(List<Vertex> corners) {
        if(corners.size()!=4)throw new IllegalArgumentException("A tessellation patch requires four corners");
        var output=new ArrayList<Vertex>(64);
        for(int y=0;y<SUBDIVISIONS;y++)for(int x=0;x<SUBDIVISIONS;x++) {
            float u=x/(float)SUBDIVISIONS,v=y/(float)SUBDIVISIONS,du=1f/SUBDIVISIONS;
            output.add(interpolate(corners,u,v));output.add(interpolate(corners,u+du,v));
            output.add(interpolate(corners,u+du,v+du));output.add(interpolate(corners,u,v+du));
        }
        return List.copyOf(output);
    }
    private static Vertex interpolate(List<Vertex> c,float u,float v) {
        float[] w={(1-u)*(1-v),u*(1-v),u*v,(1-u)*v};
        float x=0,y=0,z=0,tu=0,tv=0,nx=0,ny=0,nz=0;float[] color=new float[4];float block=0,sky=0;
        for(int i=0;i<4;i++) {
            var p=c.get(i);float a=w[i];x+=p.x*a;y+=p.y*a;z+=p.z*a;tu+=p.u*a;tv+=p.v*a;nx+=p.nx*a;ny+=p.ny*a;nz+=p.nz*a;
            for(int channel=0;channel<4;channel++)color[channel]+=((p.color>>>(channel*8))&255)*a;
            block+=(p.light&0xffff)*a;sky+=(p.light>>>16)*a;
        }
        int packed=0;for(int i=0;i<4;i++)packed|=Math.clamp(Math.round(color[i]),0,255)<<(i*8);
        return new Vertex(x,y,z,packed,tu,tv,Math.round(block)|Math.round(sky)<<16,nx,ny,nz);
    }
    private LevititeQuadSubdivision() { }
}
