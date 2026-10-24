package dev.eriksonn.aeronautics.fabric.registry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import foundry.veil.api.client.render.gpu.GpuFramebuffer;
import foundry.veil.api.client.render.gpu.GpuShaderProgram;
import foundry.veil.api.client.render.gpu.GpuUniformBuffer;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.vertex.VertexArray;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

/** Explicit native heat pass. Each balloon owns a detached uniform snapshot through submission. */
public final class NativeBalloonHeatPass implements AutoCloseable {
    public static final Identifier SHADER = Identifier.fromNamespaceAndPath("aeronautics", "hot_air_overlay");
    public static final GpuUniformBuffer.Layout UNIFORMS = GpuUniformBuffer.layoutBuilder()
            .field("ModelViewMat", GpuUniformBuffer.ValueType.MAT4)
            .field("ProjMat", GpuUniformBuffer.ValueType.MAT4)
            .field("Scroll", GpuUniformBuffer.ValueType.FLOAT)
            .field("CutoffY", GpuUniformBuffer.ValueType.FLOAT)
            .field("ColorModulator", GpuUniformBuffer.ValueType.VEC4).build();
    private static final RenderPipeline PIPELINE = RenderPipeline.builder().withLocation(SHADER)
            .withVertexShader(SHADER).withFragmentShader(SHADER)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
            .withPrimitiveTopology(PrimitiveTopology.QUADS).withCull(true)
            .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL))
            // Native depth is reversed: the original (-.5,-30) moves toward the camera as (+.5,+30).
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, .5f, 30f))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("HeatUniforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler1", UniformType.COMBINED_IMAGE_SAMPLER).build()).build();
    private GpuFramebuffer target;
    private ShaderManager owner;

    public record Geometry(VertexArray mesh, Matrix4fc modelView, float cutoffY) {
        public Geometry { modelView = new Matrix4f(modelView); }
    }
    public GpuFramebuffer target(int width, int height) {
        if (this.target == null) this.target = GpuFramebuffer.builder("Aeronautics balloon heat", width, height)
                .color(GpuFormat.RGBA8_UNORM).depth(GpuFormat.D32_FLOAT).build(RenderSystem.getDevice());
        else this.target.resize(width, height);
        return this.target;
    }
    public Submission record(CommandEncoder encoder, ShaderManager shaders, Executor workers, Matrix4fc projection,
                             float scroll, List<Geometry> geometries,
                             GpuTextureView side, GpuSampler sideSampler, GpuTextureView top, GpuSampler topSampler) {
        if (this.target == null) throw new IllegalStateException("Heat target has not been allocated");
        if (this.owner != null && this.owner != shaders) this.owner.releaseShader(SHADER, PIPELINE);
        this.owner = shaders;
        var program = shaders.getShader(SHADER, PIPELINE, workers);
        var lease = program.retain();
        var draws = new ArrayList<VertexArray.Draw>();
        try {
            var uniforms = program.getUniformBlock("HeatUniforms");
            if (uniforms == null) uniforms = program.createUniformBlock("HeatUniforms", UNIFORMS);
            uniforms.setMatrix("ProjMat", projection);
            uniforms.setFloat("Scroll", (float) (Math.floor(scroll * 16f) / 16f));
            uniforms.setVector("ColorModulator", .85f, .85f, .85f, 1f);
            var textures = Map.of("Sampler0", new GpuShaderProgram.TextureBinding(side, sideSampler, null),
                    "Sampler1", new GpuShaderProgram.TextureBinding(top, topSampler, null));
            this.target.clear(encoder, new Vector4f(0, 0, 0, 0), RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE);
            for (var geometry : geometries) {
                var draw = geometry.mesh.retain();
                draws.add(draw);
                draw.validatePipeline(PIPELINE);
                uniforms.setMatrix("ModelViewMat", geometry.modelView);
                uniforms.setFloat("CutoffY", geometry.cutoffY);
                var snapshot = program.prepareUniformSnapshots(encoder, program.snapshotOwnedUniforms());
                try (var pass = this.target.beginPass(encoder)) {
                    program.bind(pass, snapshot, textures);
                    draw.draw(pass);
                }
            }
            return new Submission(draws, lease);
        } catch (RuntimeException | Error failure) {
            draws.forEach(VertexArray.Draw::close); lease.close(); throw failure;
        }
    }
    /** Close after the caller submits the encoder containing these draws. */
    public record Submission(List<VertexArray.Draw> draws, GpuShaderProgram.Lease program) implements AutoCloseable {
        public Submission { draws = List.copyOf(draws); }
        @Override public void close() { draws.forEach(VertexArray.Draw::close); program.close(); }
    }
    @Override public void close() {
        if (this.target != null) this.target.close();
        this.target = null;
        if (this.owner != null) this.owner.releaseShader(SHADER, PIPELINE);
        this.owner = null;
    }
}
