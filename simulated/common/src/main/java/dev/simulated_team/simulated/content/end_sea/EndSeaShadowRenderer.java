package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.void_anchor.VoidAnchorBlockEntity;
import dev.simulated_team.simulated.fabric.client.render.NativeEndSeaPass;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.gpu.GpuFramebuffer;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public final class EndSeaShadowRenderer {
    public static final float SHADOW_VOLUME_RADIUS=128;
    private static final Vector3d SHADOW_CAMERA_POSITION=new Vector3d();
    private static boolean rendering,registered;
    private static final List<Vector3d> voidAnchors=new ArrayList<>();
    private static GpuFramebuffer target,swap;
    private static NativeEndSeaPass pass;
    public static boolean isEnabled(){return true;}
    public static void register(){
        if(registered)return;registered=true;
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->close());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Simulated.path("end_sea"),
                (state,workers,barrier,game)->barrier.wait(null).thenRunAsync(EndSeaShadowRenderer::close,game));
    }
    public static void renderShadowMap(VeilRenderLevelStageEvent.Stage stage,VeilRenderLevelStageEvent.Context context){
        if(stage!=VeilRenderLevelStageEvent.Stage.END_MAIN||!isEnabled())return;
        var level=Minecraft.getInstance().level;
        var physics=level==null?null:EndSeaPhysicsData.of(level);
        if(physics==null){voidAnchors.clear();return;}
        if(target==null){
            var device=RenderSystem.getDevice();
            target=GpuFramebuffer.builder("End Sea shadows",1024,1024).color(GpuFormat.RGBA8_UNORM).depth(GpuFormat.D32_FLOAT).build(device);
            swap=GpuFramebuffer.builder("End Sea spread",1024,1024).color(GpuFormat.RGBA8_UNORM).depth(GpuFormat.D32_FLOAT).build(device);
        }
        var camera=context.camera();
        SHADOW_CAMERA_POSITION.set(Math.floor(camera.pos.x),physics.startY()-SHADOW_VOLUME_RADIUS,Math.floor(camera.pos.z));
        var selected=new ArrayList<ClientSubLevel>();
        for(var owner:Sable.HELPER.getAllIntersecting(level,new BoundingBox3d(-30_000_000,-10_000,-30_000_000,30_000_000,10_000,30_000_000)))
            if(owner instanceof ClientSubLevel client)selected.add(client);
        var encoder=context.createCommandEncoder();target.clear(encoder,new Vector4f(0),0);encoder.submit();
        // Caller projection retains diagram/plot GL coordinates; group renderer converts once to native reversed depth.
        var projection=new Matrix4f().ortho(-SHADOW_VOLUME_RADIUS,SHADOW_VOLUME_RADIUS,-SHADOW_VOLUME_RADIUS,SHADOW_VOLUME_RADIUS,.5f,SHADOW_VOLUME_RADIUS);
        rendering=true;
        try{
            SimpleSubLevelGroupRenderer.renderGroup(level,selected,target,new Matrix4f(),projection,SHADOW_CAMERA_POSITION,
                    new Quaternionf().rotateX(-(float)Math.PI/2),camera.cameraEntityPartialTicks,false);
        }finally{rendering=false;}
        nativePass().spread(target,swap);
    }
    public static NativeEndSeaPass nativePass(){if(pass==null)pass=new NativeEndSeaPass();return pass;}
    public static void renderVoidAnchors(VeilRenderLevelStageEvent.Context context){
        if(voidAnchors.isEmpty())return;
        var device=RenderSystem.getDevice();var camera=context.camera();
        try(var target=GpuFramebuffer.borrow(device,"End Sea cracks world target",context.mainTarget());
            var mesh=VertexArray.create(device,"End Sea cracks");
            var memory=new ByteBufferBuilder(Math.max(1024,voidAnchors.size()*4*DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize()))){
            var vertices=new BufferBuilder(memory,PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            for(var position:List.copyOf(voidAnchors)){
                var pose=new Matrix4f().translation((float)(position.x-camera.pos.x),(float)(position.y-camera.pos.y),(float)(position.z-camera.pos.z));
                vertices.addVertex(pose,-60,0,-60).setUv(0,0).setColor(.5f,0,0,1);
                vertices.addVertex(pose,60,0,-60).setUv(1,0).setColor(.5f,0,0,1);
                vertices.addVertex(pose,60,0,60).setUv(1,1).setColor(.5f,0,0,1);
                vertices.addVertex(pose,-60,0,60).setUv(0,1).setColor(.5f,0,0,1);
            }
            var encoder=context.createCommandEncoder();mesh.upload(encoder,vertices.buildOrThrow(),VertexArray.DrawUsage.STATIC);encoder.submit();
            try(var draw=mesh.retain()){nativePass().cracks(target,camera.projectionMatrix,camera.viewRotationMatrix,draw);}
        }finally{voidAnchors.clear();}
    }
    public static @Nullable GpuFramebuffer getShadowsFramebuffer(){return target;}
    public static boolean renderingShadowMap(){return rendering;}
    public static Vector3dc getLastRenderOrigin(){return new Vector3d(SHADOW_CAMERA_POSITION);}
    public static void addVoidAnchor(VoidAnchorBlockEntity anchor){
        if(rendering||SimpleSubLevelGroupRenderer.isRendering())return;
        addVoidAnchor(Sable.HELPER.projectOutOfSubLevel(anchor.getLevel(),JOMLConversion.atCenterOf(anchor.getBlockPos())));
    }
    public static void addVoidAnchor(Vector3dc position){
        if(rendering||SimpleSubLevelGroupRenderer.isRendering())return;
        var copy=new Vector3d(position);if(!voidAnchors.contains(copy))voidAnchors.add(copy);
    }
    public static void close(){
        if(target!=null){target.close();target=null;}if(swap!=null){swap.close();swap=null;}
        if(pass!=null){pass.close();pass=null;}voidAnchors.clear();
    }
    private EndSeaShadowRenderer(){}
}
