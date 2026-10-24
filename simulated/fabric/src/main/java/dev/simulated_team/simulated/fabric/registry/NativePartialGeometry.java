package dev.simulated_team.simulated.fabric.registry;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.PartialModelRenderState;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;

/** Actual baked partial quads for custom native shader types; all live model access stays in extraction. */
public final class NativePartialGeometry {
    public static NativeVertexSnapshot extract(ExtraModelKey<BlockStateModel> key,int color,int light) {
        final var model=new PartialModelRenderState();AllPartialModels.extract(key,model);return extract(model.mesh,color,light);
    }
    public static NativeVertexSnapshot extract(Mesh mesh,int color,int light) {
        final var out=new NativeVertexSnapshot.Capture();
        mesh.forEach(quad->{for(int vertex=0;vertex<4;vertex++) {
            final var normal=quad.faceNormal();
            out.addVertex(quad.x(vertex),quad.y(vertex),quad.z(vertex)).setColor(color).setUv(quad.u(vertex),quad.v(vertex)).setLight(light)
                    .setNormal(quad.hasNormal(vertex)?quad.normalX(vertex):normal.x(),quad.hasNormal(vertex)?quad.normalY(vertex):normal.y(),quad.hasNormal(vertex)?quad.normalZ(vertex):normal.z());
        }});
        return out.finish();
    }
    private NativePartialGeometry() { }
}
