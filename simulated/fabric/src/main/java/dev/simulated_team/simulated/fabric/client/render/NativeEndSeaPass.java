package dev.simulated_team.simulated.fabric.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.*;
import foundry.veil.api.client.render.gpu.*;
import foundry.veil.api.client.render.vertex.VertexArray;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import java.util.*;

/** Explicit End Sea depth spreading, additive layers and anchor cracks; caller owns every target. */
public final class NativeEndSeaPass implements AutoCloseable {
    private static final GpuUniformBuffer.Layout TRANSFORMS = GpuUniformBuffer.layoutBuilder()
            .field("ProjMat", GpuUniformBuffer.ValueType.MAT4).field("ModelViewMat", GpuUniformBuffer.ValueType.MAT4)
            .field("CameraY", GpuUniformBuffer.ValueType.FLOAT).field("ShadowVolumeSize", GpuUniformBuffer.ValueType.FLOAT)
            .field("StartY", GpuUniformBuffer.ValueType.FLOAT).build();
    private final GpuSampler clamp, repeat;
    private final GpuShaderSource sources;
    private final Map<GpuFormat, GpuShaderProgram> layers = new HashMap<>(), cracks = new HashMap<>();
    private GpuShaderProgram spread;
    public NativeEndSeaPass() {
        var device = RenderSystem.getDevice();
        clamp = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        repeat = device.createSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        sources = new GpuShaderSource(Minecraft.getInstance().getResourceManager());
    }
    /** Exactly five original two-pass iterations, with original .85 strength decay and .0005 depth bias. */
    public void spread(GpuFramebuffer target, GpuFramebuffer swap) {
        if (target.width()!=swap.width() || target.height()!=swap.height()) throw new IllegalArgumentException("End Sea spread targets must have equal size");
        if (spread == null) {
            var bindings=BindGroupLayout.builder().withUniform("DiffuseSampler0",UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("DiffuseDepthSampler",UniformType.COMBINED_IMAGE_SAMPLER).build();
            spread=new GpuShaderProgram(RenderSystem.getDevice(), RenderPipeline.builder().withLocation(path("pipeline/end_sea_spread"))
                    .withVertexShader(path("native/end_sea_spread")).withFragmentShader(path("native/end_sea_spread"))
                    .withBindGroupLayout(bindings).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,true))
                    .withColorTargetState(new ColorTargetState(Optional.empty(),GpuFormat.RGBA8_UNORM,ColorTargetState.WRITE_ALL)).build(),sources,Runnable::run);
        }
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        try(var lease=spread.retain()) {
            for(int i=0;i<10;i++) {
                var from=(i&1)==0?target:swap;var to=(i&1)==0?swap:target;
                spread.setTexture("DiffuseSampler0",from.color(0).view(),clamp);
                spread.setTexture("DiffuseDepthSampler",from.depth().view(),clamp);
                spread.prepare(encoder);
                try(var pass=to.beginPass(encoder)){spread.bind(pass);pass.draw(3,1,0,0);}
            }
            encoder.submit();
        }
    }
    public void layers(GpuFramebuffer target, GpuFramebuffer shadows, Matrix4fc projection, Matrix4fc view,
                       float cameraY, float startY, float radius, VertexArray.Draw mesh) {
        var program=layers.computeIfAbsent(target.color(0).texture().getFormat(), format -> createWorld(format,false));
        program.setTexture("ShadowDepthSampler",shadows.depth().view(),clamp);
        program.setTexture("ShadowStrengthSampler",shadows.color(0).view(),clamp);
        program.setTexture("SkySampler",Minecraft.getInstance().getTextureManager().getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION).getTextureView(),repeat);
        draw(target,projection,view,cameraY,startY,radius,mesh,program);
    }
    public void cracks(GpuFramebuffer target, Matrix4fc projection, Matrix4fc view, VertexArray.Draw mesh) {
        var program=cracks.computeIfAbsent(target.color(0).texture().getFormat(),format -> createWorld(format,true));
        var texture=Minecraft.getInstance().getTextureManager().getTexture(path("textures/effects/cracks.png"));
        program.setTexture("Sampler0",texture.getTextureView(),clamp);
        draw(target,projection,view,0,0,0,mesh,program);
    }
    private void draw(GpuFramebuffer target, Matrix4fc projection, Matrix4fc view, float cameraY, float startY,
                      float radius, VertexArray.Draw mesh, GpuShaderProgram program) {
        var block=program.getUniformBlock("EndSeaTransforms");
        block.setMatrix("ProjMat",projection);block.setMatrix("ModelViewMat",view);
        block.setFloat("CameraY",cameraY);block.setFloat("StartY",startY);block.setFloat("ShadowVolumeSize",radius);
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        try(var lease=program.retain()) {
            program.prepare(encoder);mesh.validatePipeline(program.definition());
            try(var pass=target.beginPass(encoder)){program.bind(pass);mesh.draw(pass);}
            encoder.submit();
        }
    }
    private GpuShaderProgram createWorld(GpuFormat format, boolean crack) {
        var shader=path(crack?"native/end_sea_cracks":"native/end_sea");
        var bindings=BindGroupLayout.builder().withUniform("EndSeaTransforms",UniformType.UNIFORM_BUFFER);
        for(String name:crack?new String[]{"Sampler0"}:new String[]{"ShadowDepthSampler","ShadowStrengthSampler","SkySampler"})
            bindings.withUniform(name,UniformType.COMBINED_IMAGE_SAMPLER);
        var additive=new BlendFunction(BlendFactor.SRC_ALPHA,BlendFactor.ONE,BlendOp.ADD,BlendFactor.ONE,BlendFactor.ONE,BlendOp.ADD);
        var definition=RenderPipeline.builder().withLocation(path("pipeline/"+(crack?"end_sea_cracks_":"end_sea_")+format.name().toLowerCase(Locale.ROOT)))
                .withVertexShader(shader).withFragmentShader(shader).withBindGroupLayout(bindings.build())
                .withVertexBinding(0,crack?DefaultVertexFormat.POSITION_TEX_COLOR:DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
                .withPrimitiveTopology(PrimitiveTopology.QUADS).withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,crack))
                .withColorTargetState(new ColorTargetState(crack?Optional.empty():Optional.of(additive),format,ColorTargetState.WRITE_ALL)).build();
        var program=new GpuShaderProgram(RenderSystem.getDevice(),definition,sources,Runnable::run);
        try { program.createUniformBlock("EndSeaTransforms",TRANSFORMS);return program; }
        catch(RuntimeException|Error failure){program.close();throw failure;}
    }
    private static Identifier path(String name){return Identifier.fromNamespaceAndPath("simulated",name);}
    @Override public void close(){if(spread!=null)spread.close();layers.values().forEach(GpuShaderProgram::close);cracks.values().forEach(GpuShaderProgram::close);sources.close();clamp.close();repeat.close();}
}
