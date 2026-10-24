package dev.eriksonn.aeronautics.fabric.registry;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.vanilla.PlotSectionMesh;
import dev.ryanhcode.sable.sublevel.render.vanilla.PlotSectionMeshExtension;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.gpu.GpuFramebuffer;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.Supplier;

/** Captures actual native section compilation and draws immutable world/plot snapshots at END_MAIN. */
public final class NativeLevititeRenderer {
    private static final NativeLevititePass PASS=new NativeLevititePass();
    private static final ThreadLocal<NativeLevititeMesh.Collector> CAPTURE=new ThreadLocal<>();
    private static final List<NativeLevititePass.Request> PENDING=new ArrayList<>();
    public static <T> T compile(SectionPos section,Supplier<T> action) {
        final var previous=CAPTURE.get();final var capture=new NativeLevititeMesh.Collector(section,false);CAPTURE.set(capture);
        try {T result=action.get();((NativeLevititeGeometryHolder)result).aeronautics$levititeMesh(capture.finish());return result;}
        finally {if(previous==null)CAPTURE.remove();else CAPTURE.set(previous);}
    }
    public static NativeLevititeMesh.Collector capture(){return CAPTURE.get();}
    public static PlotSectionMeshExtension.Snapshot extract(NativeLevititeMesh mesh,ClientSubLevel owner,PlotSectionMesh.Snapshot snapshot,float partialTick) {
        final var pose=owner.renderPose(partialTick);final var pivot=pose.rotationPoint();final var origin=mesh.section().origin();
        var transform=new Matrix4f().translation((float)snapshot.cameraRelativeOrigin().x,(float)snapshot.cameraRelativeOrigin().y,(float)snapshot.cameraRelativeOrigin().z)
                .rotate(snapshot.orientation()).scale(snapshot.scale());
        var offset=new Vector3f((float)(origin.getX()-pivot.x()),(float)(origin.getY()-pivot.y()),(float)(origin.getZ()-pivot.z()));
        var light=snapshot.lighting();
        var lighting=new NativeLevititePass.Lighting(light.down(),light.up(),light.north(),light.south(),light.west(),light.east(),snapshot.skyLightScale(),mesh.dynamicShading());
        final var request=new NativeLevititePass.Request(mesh,mesh.vertices(),transform,offset,LevititeShaderManager.getInstance(owner).extract(owner,partialTick),lighting);
        return nodes->PENDING.add(request);
    }
    public static void register() {
        PlotSectionMeshExtension.register(NativeLevititeMesh.Collector::new);
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(NativeLevititeRenderer::onStage);
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->free());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->free());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId(){return Identifier.parse("aeronautics:levitite_meshes");}
            @Override public void onResourceManagerReload(ResourceManager resources){free();}
        });
    }
    private static void onStage(VeilRenderLevelStageEvent.Stage stage,VeilRenderLevelStageEvent.Context context) {
        final var client=Minecraft.getInstance();
        if(client.level==null){free();return;}
        if(stage==VeilRenderLevelStageEvent.Stage.COLLECT_SUBMITS) {
            final var camera=context.camera().pos;final var motion=LevititeShaderManager.world();
            for(var section:context.levelRenderer().visibleSections()) {
                if(!(section.getSectionMesh() instanceof NativeLevititeGeometryHolder holder))continue;
                final var mesh=holder.aeronautics$levititeMesh();if(mesh==null)continue;
                final var origin=mesh.section().origin();final Vec3 relative=Vec3.atLowerCornerOf(origin).subtract(camera);
                var transform=new Matrix4f().translation((float)relative.x,(float)relative.y,(float)relative.z);
                var offset=new Vector3f(origin.getX()%10000,origin.getY()%10000,origin.getZ()%10000);
                PENDING.add(new NativeLevititePass.Request(mesh,mesh.vertices(),transform,offset,motion,new NativeLevititePass.Lighting(1,1,1,1,1,1,15,false)));
            }
        }
        if(stage!=VeilRenderLevelStageEvent.Stage.END_MAIN||PENDING.isEmpty())return;
        final var requests=PENDING.stream().map(request->new NativeLevititePass.Request(request.key(),request.vertices(),
                new Matrix4f(context.camera().viewRotationMatrix).mul(request.modelView()),request.sectionOffset(),request.motion(),request.lighting())).toList();
        PENDING.clear();final var encoder=context.createCommandEncoder();final var device=RenderSystem.getDevice();
        final var atlas=client.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        final var noise=client.getTextureManager().getTexture(Identifier.parse("aeronautics:textures/special/noise_composed.png"));
        try(var main=GpuFramebuffer.borrow(device,"Levitite main",context.mainTarget());
            var submission=PASS.record(encoder,VeilRenderSystem.renderer().getShaderManager(),main,context.camera().projectionMatrix,
                    context.levelState().gameTime%100000+context.levelState().worldPartialTicks,requests,
                    new NativeLevititePass.Texture(atlas.getTextureView(),atlas.getSampler()),
                    new NativeLevititePass.Texture(context.gameRenderer().levelLightmap(),RenderSystem.getSamplerCache().getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.LINEAR,false)),
                    new NativeLevititePass.Texture(noise.getTextureView(),noise.getSampler()),RenderSystem.getShaderFog())) {encoder.submit();}
    }
    public static void free(){PENDING.clear();PASS.close();LevititeShaderManager.clear();}
    private NativeLevititeRenderer(){ }
}
