package dev.simulated_team.simulated.fabric.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.resources.Identifier;
import dev.simulated_team.simulated.fabric.client.gui.NativeGuiAccess;
import dev.simulated_team.simulated.fabric.client.gui.NativeGuiQuads;
import foundry.veil.api.client.render.gpu.GpuFramebuffer;
import foundry.veil.api.client.render.gpu.GpuShaderProgram;
import foundry.veil.api.client.render.gpu.GpuShaderSource;
import foundry.veil.api.client.render.gpu.GpuUniformBuffer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.server.packs.PackType;
import java.util.Optional;
import java.util.OptionalDouble;

/** Original diagram palette/dither/outline in a real caller-owned native color pass. */
public final class NativeDiagramPass implements AutoCloseable {
    private static NativeDiagramPass instance;
    private static boolean registered;
    private final GpuSampler clamp, repeat;
    private final GpuShaderProgram program;
    private final GpuUniformBuffer settings;
    public static void register() {
        if (registered) return;
        registered = true;
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> dispose());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(path("diagram_post"),
                (state, workers, barrier, game) -> barrier.wait(null).thenRunAsync(NativeDiagramPass::dispose, game));
    }
    private static NativeDiagramPass get() {
        if (instance == null) instance = new NativeDiagramPass();
        return instance;
    }
    private NativeDiagramPass() {
        var device = RenderSystem.getDevice();
        clamp = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        repeat = device.createSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        var bindings = BindGroupLayout.builder().withUniform("DiagramSettings", UniformType.UNIFORM_BUFFER);
        for (String name : new String[]{"DiffuseSampler0", "DiffuseDepthSampler", "Palette", "Dither"}) bindings.withUniform(name, UniformType.COMBINED_IMAGE_SAMPLER);
        var shader = path("native/diagram");
        var pipeline = RenderPipeline.builder().withLocation(path("pipeline/diagram"))
                .withVertexShader(shader).withFragmentShader(shader).withBindGroupLayout(bindings.build())
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(Optional.empty())
                .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL)).build();
        try (var source = new GpuShaderSource(Minecraft.getInstance().getResourceManager())) {
            program = new GpuShaderProgram(device, pipeline, source, Runnable::run);
        }
        settings = program.createUniformBlock("DiagramSettings", GpuUniformBuffer.layoutBuilder()
                .field("LineColor", GpuUniformBuffer.ValueType.VEC4).field("LineShadowColor", GpuUniformBuffer.ValueType.VEC4)
                .field("PaletteOffset", GpuUniformBuffer.ValueType.FLOAT).field("FadeScale", GpuUniformBuffer.ValueType.FLOAT).build());
    }
    public static void draw(GpuFramebuffer source, GpuFramebuffer target, float paletteOffset, float fadeScale, int line, int shadow) {
        var pass = get();
        if (source.color(0).texture() == target.color(0).texture()) throw new IllegalArgumentException("Diagram must use distinct source and output");
        if (fadeScale <= 0) throw new IllegalArgumentException("Diagram fade scale must be positive");
        pass.settings.setVector("LineColor", (line >> 16 & 255) / 255f, (line >> 8 & 255) / 255f, (line & 255) / 255f, 1);
        pass.settings.setVector("LineShadowColor", (shadow >> 16 & 255) / 255f, (shadow >> 8 & 255) / 255f, (shadow & 255) / 255f, 1);
        pass.settings.setFloat("PaletteOffset", paletteOffset); pass.settings.setFloat("FadeScale", fadeScale);
        pass.program.setTexture("DiffuseSampler0", source.color(0).view(), pass.clamp);
        pass.program.setTexture("DiffuseDepthSampler", source.depth().view(), pass.clamp);
        var textures = Minecraft.getInstance().getTextureManager();
        pass.program.setTexture("Palette", textures.getTexture(path("textures/effects/diagram_palette.png")).getTextureView(), pass.clamp);
        pass.program.setTexture("Dither", textures.getTexture(path("textures/effects/dither.png")).getTextureView(), pass.repeat);
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        pass.program.prepare(encoder);
        try (var draw = target.beginPass(encoder)) { pass.program.bind(draw); draw.draw(3, 1, 0, 0); }
        encoder.submit();
    }
    public static void extract(GuiGraphicsExtractor gui, GpuFramebuffer framebuffer, int width, int height) {
        final var access = (NativeGuiAccess) gui;
        access.simulated$guiState().addGuiElement(new NativeGuiQuads.TexturedQuad(
                TextureSetup.singleTexture(framebuffer.color(0).view(), get().clamp), gui.pose(),
                0, 0, width, height, 0, 0, 1, 1, -1, -1, -1, -1, access.simulated$scissor()));
    }
    /** One initial read for decoration placement, rather than one blocking read per candidate. */
    public static byte[] readAlpha(GpuFramebuffer framebuffer) {
        int pixels = Math.multiplyExact(framebuffer.width(), framebuffer.height());
        var device = RenderSystem.getDevice();
        try (var buffer = device.createBuffer(() -> "Diagram decoration coverage", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, (long) pixels * 4)) {
            var encoder = device.createCommandEncoder();
            encoder.copyTextureToBuffer(framebuffer.color(0).texture(), buffer, 0, () -> {}, 0);
            try (var fence = encoder.createFence()) {
                encoder.submit();
                if (!fence.awaitCompletion(10_000_000_000L)) throw new IllegalStateException("Diagram coverage read timed out");
                try (var mapped = buffer.map(true, false)) {
                    byte[] alpha = new byte[pixels];
                    for (int i = 0; i < pixels; i++) alpha[i] = mapped.data().get(i * 4 + 3);
                    return alpha;
                }
            }
        }
    }
    private static Identifier path(String name) { return Identifier.fromNamespaceAndPath("simulated", name); }
    private static void dispose() { if (instance != null) { instance.close(); instance = null; } }
    @Override public void close() { program.close(); clamp.close(); repeat.close(); }
}
