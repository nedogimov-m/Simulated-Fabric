package dev.simulated_team.simulated.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.UniformType;
import dev.ryanhcode.sable.sublevel.render.vanilla.PlotSectionMesh;
import dev.simulated_team.simulated.content.end_sea.EndSeaFadeTransformer;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import foundry.veil.api.client.render.gpu.GpuUniformBuffer;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import foundry.veil.api.client.render.shader.NativePipelineBridge;
import foundry.veil.mixin.rendertype.accessor.RenderTypeAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import java.util.EnumMap;
import java.util.Map;

/** Plot-only tint; normal world blocks and detached diagram geometry keep their own native pipelines. */
public final class NativeEndSeaShaders {
    public static final String BLOCK="SimulatedEndSea";
    public static final GpuUniformBuffer.Layout LAYOUT=GpuUniformBuffer.layoutBuilder()
            .field("EndSeaCameraToStartY",GpuUniformBuffer.ValueType.FLOAT).field("EndSeaActive",GpuUniformBuffer.ValueType.FLOAT).build();
    private static GpuUniformBuffer active,disabled;
    private static boolean registered;
    public static void register(){
        if(registered)return;registered=true;
        NativePipelineBridge.register(path("plot_end_sea"),pipeline->pipeline.getLocation().getNamespace().equals("simulated")
                        &&pipeline.getLocation().getPath().startsWith("pipeline/end_sea_plot_"),
                Map.of(BLOCK,UniformType.UNIFORM_BUFFER),EndSeaFadeTransformer::new,pipeline->{
                    if(active==null||disabled==null)throw new IllegalStateException("Prepare End Sea uniforms before opening native passes");
                    var value=dev.ryanhcode.sable.render.SableNativeWorldShaders.worldEffectsDisabled()?disabled:active;
                    return new NativePipelineBridge.Bindings(Map.of(BLOCK,value.buffer().slice()),Map.of());
                });
        PlotSectionMesh.setWorldRenderTypeFactory(NativeEndSeaPlotTypes::type);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->close());
    }
    public static void prepareFrame(){
        if(!registered)return;
        if(active==null){active=new GpuUniformBuffer(RenderSystem.getDevice(),"Simulated End Sea world",LAYOUT);disabled=new GpuUniformBuffer(RenderSystem.getDevice(),"Simulated End Sea disabled",LAYOUT);}
        active.setFloat("EndSeaActive",0);active.setFloat("EndSeaCameraToStartY",0);
        var encoder=RenderSystem.getDevice().createCommandEncoder();active.upload(encoder);disabled.upload(encoder);encoder.submit();
    }
    /** Called at LevelRenderer.render HEAD with the actual extracted camera, before its frame graph opens any pass. */
    public static void prepareWorld(LevelRenderState state){
        if(!registered)return;
        if(active==null)prepareFrame();
        var level=Minecraft.getInstance().level;var physics=level==null?null:EndSeaPhysicsData.of(level);
        float height=physics==null?0:(float)(state.cameraRenderState.pos.y-physics.startY());
        active.setFloat("EndSeaCameraToStartY",height);
        active.setFloat("EndSeaActive",physics!=null&&height!=0?1:0);
        var encoder=RenderSystem.getDevice().createCommandEncoder();active.upload(encoder);encoder.submit();
    }
    public static void close(){if(active!=null){active.close();active=null;}if(disabled!=null){disabled.close();disabled=null;}}
    private static Identifier path(String name){return Identifier.fromNamespaceAndPath("simulated",name);}
    private NativeEndSeaShaders(){}
}
