import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.fabric.client.NativeSimWorldShaderBindings;
import foundry.veil.api.client.registry.RenderTypeLayerRegistry;
import foundry.veil.api.client.render.gpu.*;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.NativePipelineBridge;
import foundry.veil.api.client.render.texture.TextureFilter;
import foundry.veil.impl.client.render.rendertype.NativeRenderTypeDraws;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Only new Simulated shader/render-type behavior; no complete Simulated/Veil initialization. */
public final class SimNativeShaderGpuCheck implements ModInitializer, ClientModInitializer {
    private boolean ran;
    private int ticks;
    @Override public void onInitialize() { RenderTypeLayerRegistry.bootstrap(); NativeSimWorldShaderBindings.register(); }
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.ran || ++this.ticks < 5 || !client.isGameLoadFinished()) return;
            this.ran = true;
            var report = new LinkedHashMap<String, Object>();
            var device = RenderSystem.getDevice(); report.put("backend", device.getDeviceInfo().backendName()); report.put("device", device.getDeviceInfo().name());
            try {
                check(device.getDeviceInfo().backendName().toLowerCase(Locale.ROOT).contains("vulkan"), "Actual Vulkan required");
                var types = List.of(SimRenderTypes.staffOverlay(), SimRenderTypes.laser(), SimRenderTypes.lens(), SimRenderTypes.lock(), SimRenderTypes.rope(),
                        SimRenderTypes.spring(Identifier.parse("veil:textures/special/white.png")), SimRenderTypes.itemGlowingSolid(false), SimRenderTypes.itemGlowingTranslucent(false));
                // The installed production GameRenderer.render HEAD mixin must prepare this frame's UBO.
                // No explicit prepareFrame call here can hide a missing production lifecycle hook.
                for (var type : types) {
                    var state = VeilRenderType.getShards(type);
                    check(state.veilShaderId() == null && state.outputState() == null, "Simulated types require actual native caller pass ownership");
                    check(!RenderSystem.getCompiledPipeline(type.pipeline()).isClosed(), "Every native shader pair and both glowing state variants must genuinely compile");
                }
                check(NativeSimWorldShaderBindings.LAYOUT.size() == 96 && NativeSimWorldShaderBindings.LAYOUT.fields().get(NativeSimWorldShaderBindings.FIELD).offset(5) == 80,
                        "Six face brightness scalars require std140 stride 16 and 96 real bytes");
                check(NativePipelineBridge.actualDefinition(RenderSystem.getCompiledPipeline(SimRenderTypes.rope().pipeline())).orElseThrow()
                        .getBindGroupLayouts().stream().flatMap(layout -> layout.uniforms().stream()).anyMatch(uniform -> uniform.name().equals(NativeSimWorldShaderBindings.BLOCK)),
                        "Rope must receive the actual production face-lighting UBO through the native bridge");
                try (var fixture = new Fixture()) {
                    fixture.check(SimRenderTypes.rope(), new Vertex(0xffffffff, 0, 1, 0, .5f, .5f), new Vector4f(1), 0, 255, 255, 255, 255);
                    fixture.check(SimRenderTypes.rope(), new Vertex(0xffffffff, 0, -1, 0, .5f, .5f), new Vector4f(1), 0, 128, 128, 128, 128);
                    int oblique = Math.round(255 * ((float) Math.pow(76 / 127.0, 3) + .8f * (float) Math.pow(101 / 127.0, 2)));
                    fixture.check(SimRenderTypes.rope(), new Vertex(0xffffffff, 0, .6f, .8f, .5f, .5f), new Vector4f(1), 0, oblique, oblique, oblique, oblique);
                    fixture.check(types.get(5), new Vertex(0x80ff0000, 0, 1, 0, .5f, .5f), new Vector4f(1), 0, 255, 127, 127, 255);
                    fixture.check(SimRenderTypes.lens(), new Vertex(0x40ff0000, 0, 1, 0, .5f, .5f), new Vector4f(128 / 255.0f, 0, 0, .5f), 0, 242, 14, 14, 255);
                    fixture.check(SimRenderTypes.lens(), new Vertex(0x40ff0000, 0, 1, 0, .5f, .5f), new Vector4f(.5f, .5f, .5f, 0), 0, 0, 0, 0, 0);
                    fixture.check(SimRenderTypes.laser(), new Vertex(0xffff0000, 0, 1, 0, 1, 5), new Vector4f(1), 0, 255, 0, 0, 255);
                    fixture.check(SimRenderTypes.laser(), new Vertex(0xffff0000, 0, 1, 0, 3, 5), new Vector4f(1), 0, 128, 0, 0, 128);
                    fixture.check(SimRenderTypes.laser(), new Vertex(0xffff0000, 0, 1, 0, 5, 5), new Vector4f(1), 0, 0, 0, 0, 0);
                    fixture.check(SimRenderTypes.laser(), new Vertex(0xffff0000, 0, 1, 0, 1, 5), new Vector4f(1), .75f, 0, 0, 0, 0);
                }
                report.put("checks", List.of("all seven native shader pairs/eight real render pipelines compile", "real 96-byte face UBO and exact cubic-Y/squared-XZ rope shading",
                        "spring stress color blend", "lens tint/opaque-alpha and zero-alpha discard", "laser taper/blend and reverse-depth rejection", "actual PreparedRenderType hooks draw into the caller target without deferred ownership"));
                report.put("status", "passed");
            } catch (Throwable failure) { failure.printStackTrace(); report.put("status", "failed"); report.put("error", failure.toString()); }
            finally {
                NativeSimWorldShaderBindings.close();
                try { Files.writeString(Path.of(System.getProperty("simulated.native.shaders.report")), new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n"); }
                catch (Exception failure) { throw new RuntimeException(failure); }
                client.stop();
            }
        });
    }
    private record Vertex(int color, float nx, float ny, float nz, float u, float v) { }
    private static final class Fixture implements AutoCloseable {
        final com.mojang.renderpearl.api.device.GpuDevice device = RenderSystem.getDevice();
        final GpuUniformBuffer transform = new GpuUniformBuffer(device, "Sim native transforms", GpuUniformBuffer.layoutBuilder()
                .field("ModelViewMat", GpuUniformBuffer.ValueType.MAT4).field("TextureMat", GpuUniformBuffer.ValueType.MAT4).field("ColorModulator", GpuUniformBuffer.ValueType.VEC4).field("ModelOffset", GpuUniformBuffer.ValueType.VEC3).build());
        final GpuUniformBuffer projection = new GpuUniformBuffer(device, "Sim native projection", GpuUniformBuffer.layoutBuilder().field("ProjMat", GpuUniformBuffer.ValueType.MAT4).build());
        final GpuUniformBuffer lighting = new GpuUniformBuffer(device, "Sim native lighting", GpuUniformBuffer.layoutBuilder().field("Light0_Direction", GpuUniformBuffer.ValueType.VEC3).field("Light1_Direction", GpuUniformBuffer.ValueType.VEC3).build());
        final GpuUniformBuffer fog = new GpuUniformBuffer(device, "Sim native fog", GpuUniformBuffer.layoutBuilder().field("FogColor", GpuUniformBuffer.ValueType.VEC4)
                .field("FogEnvironmentalStart", GpuUniformBuffer.ValueType.FLOAT).field("FogEnvironmentalEnd", GpuUniformBuffer.ValueType.FLOAT).field("FogRenderDistanceStart", GpuUniformBuffer.ValueType.FLOAT)
                .field("FogRenderDistanceEnd", GpuUniformBuffer.ValueType.FLOAT).field("FogSkyEnd", GpuUniformBuffer.ValueType.FLOAT).field("FogCloudsEnd", GpuUniformBuffer.ValueType.FLOAT).build());
        final GpuFramebuffer output = GpuFramebuffer.builder("Sim native caller output", 2, 2).color(GpuFormat.RGBA8_UNORM).depth(GpuFormat.D32_FLOAT).build(device);
        final GpuFramebuffer scratch = GpuFramebuffer.builder("Sim untouched caller sibling", 2, 2).color(GpuFormat.RGBA8_UNORM).build(device);
        final GpuFramebuffer texture = GpuFramebuffer.builder("Sim shader sample", 16, 16).color(GpuFormat.RGBA8_UNORM).build(device);
        final GpuFramebuffer lightmap = GpuFramebuffer.builder("Sim shader lightmap", 16, 16).color(GpuFormat.RGBA8_UNORM).build(device);
        final com.mojang.renderpearl.api.textures.GpuSampler sampler = TextureFilter.CLAMP.createSampler(device);
        final com.mojang.renderpearl.api.buffers.GpuBufferSlice oldFog = RenderSystem.getShaderFog(), oldLights = RenderSystem.getShaderLights(), oldProjection = RenderSystem.getProjectionMatrixBuffer();
        final ProjectionType oldProjectionType = RenderSystem.getProjectionType();
        Fixture() {
            transform.setMatrix("ModelViewMat", new Matrix4f()); transform.setMatrix("TextureMat", new Matrix4f()); transform.setVector("ColorModulator", 1, 1, 1, 1); transform.setVector("ModelOffset", 0, 0, 0);
            projection.setMatrix("ProjMat", new Matrix4f()); lighting.setVector("Light0_Direction", 0, 1, 0); lighting.setVector("Light1_Direction", 0, 1, 0);
            fog.setVector("FogColor", 0, 0, 0, 0); fog.setFloat("FogEnvironmentalStart", 100); fog.setFloat("FogEnvironmentalEnd", 200); fog.setFloat("FogRenderDistanceStart", 100); fog.setFloat("FogRenderDistanceEnd", 200);
            fog.setFloat("FogSkyEnd", 200); fog.setFloat("FogCloudsEnd", 200);
            var encoder = device.createCommandEncoder(); transform.upload(encoder); projection.upload(encoder); lighting.upload(encoder); fog.upload(encoder);
            lightmap.clear(encoder, new Vector4f(1), 0); scratch.clear(encoder, new Vector4f(0, 1, 0, 1), 0); encoder.submit();
            RenderSystem.setProjectionMatrix(projection.buffer().slice(), ProjectionType.ORTHOGRAPHIC); RenderSystem.setShaderFog(fog.buffer().slice()); RenderSystem.setShaderLights(lighting.buffer().slice());
        }
        void check(RenderType type, Vertex input, Vector4f sample, float clearDepth, int... expected) {
            try (var memory = new ByteBufferBuilder(2048)) {
                var vertices = new BufferBuilder(memory, type.primitiveTopology(), type.format());
                for (float[] xy : new float[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
                    vertices.addVertex(xy[0], xy[1], .5f).setColor(input.color);
                    if (type.format().contains("UV0")) vertices.setUv(input.u, input.v);
                    if (type.format().contains("UV1")) vertices.setUv1(0, 0);
                    if (type.format().contains("UV2")) vertices.setUv2(0, 0);
                    if (type.format().contains("Normal")) vertices.setNormal(input.nx, input.ny, input.nz);
                }
                try (var mesh = vertices.buildOrThrow(); var vertexBuffer = device.createBuffer(() -> "Sim real MeshData vertices", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, mesh.vertexBuffer())) {
                    var indexBytes = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder());
                    for (int index : new int[]{0, 1, 2, 2, 3, 0}) indexBytes.putShort((short) index);
                    indexBytes.flip();
                    try (var indexBuffer = device.createBuffer(() -> "Sim real mesh indices", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, indexBytes)) {
                        var textures = new ArrayList<PreparedRenderType.Texture>();
                        var declarations = BindGroupLayout.flattenUniforms(type.pipeline().getBindGroupLayouts());
                        for (String name : List.of("Sampler0", "Sampler1", "Sampler2")) if (declarations.stream().anyMatch(uniform -> uniform.name().equals(name)))
                            textures.add(new PreparedRenderType.Texture(name, name.equals("Sampler0") ? texture.color(0).view() : lightmap.color(0).view(), sampler));
                        var prepared = new PreparedRenderType(type.toString(), type.pipeline(), null, transform.buffer().slice(), new ScissorState(), textures);
                        NativeRenderTypeDraws.capture(type, prepared);
                        var geometry = new StagedVertexBuffer.ExecuteInfo(vertexBuffer, indexBuffer, IndexType.SHORT, 0, 0, 6, type.primitiveTopology());
                        var encoder = device.createCommandEncoder(); texture.clear(encoder, sample, 0); output.clear(encoder, new Vector4f(0), clearDepth);
                        try (var pass = output.beginPass(encoder)) { prepared.drawFromBuffer(geometry, pass); }
                        try (var submission = NativeRenderTypeDraws.flush(encoder)) { encoder.submit(); }
                        assertColor(output, expected); assertColor(scratch, 0, 255, 0, 255);
                    }
                }
            }
        }
        void assertColor(GpuFramebuffer target, int... expected) {
            try (var bytes = device.createBuffer(() -> "Sim shader RGBA readback", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, 16)) {
                var encoder = device.createCommandEncoder(); encoder.copyTextureToBuffer(target.color(0).texture(), bytes, 0, () -> {}, 0);
                try (var fence = encoder.createFence()) {
                    encoder.submit(); SimNativeShaderGpuCheck.check(fence.awaitCompletion(10_000_000_000L), "Sim shader readback timed out");
                    try (var mapped = bytes.map(true, false)) {
                        for (int index = 0; index < 16; index++) SimNativeShaderGpuCheck.check(Math.abs((mapped.data().get(index) & 255) - expected[index % 4]) <= 1,
                                "Sim shader RGBA component " + index + " was " + (mapped.data().get(index) & 255) + ", expected " + expected[index % 4]);
                    }
                }
            }
        }
        @Override public void close() {
            RenderSystem.setProjectionMatrix(oldProjection, oldProjectionType); RenderSystem.setShaderFog(oldFog); RenderSystem.setShaderLights(oldLights);
            transform.close(); projection.close(); lighting.close(); fog.close(); sampler.close(); output.close(); scratch.close(); texture.close(); lightmap.close();
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
