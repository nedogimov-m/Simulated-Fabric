package dev.simulated_team.simulated.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.resources.Identifier;
import foundry.veil.api.client.render.gpu.GpuUniformBuffer;
import foundry.veil.api.client.render.shader.NativePipelineBridge;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import java.util.Map;

/** Six actual dimension shade values prepared before any render pass; binding creates no encoder. */
public final class NativeSimWorldShaderBindings {
    public static final String BLOCK = "SimulatedBlockLighting";
    public static final String FIELD = "VeilBlockFaceBrightness";
    public static final GpuUniformBuffer.Layout LAYOUT = GpuUniformBuffer.layoutBuilder()
            .array(FIELD, GpuUniformBuffer.ValueType.FLOAT, 6).build();
    private static GpuUniformBuffer lighting;
    private static boolean registered;
    public static void register() {
        if (registered) return;
        NativePipelineBridge.register(path("native/rope_lighting"),
                pipeline -> pipeline.getShaders().containsValue(path("native/rope")),
                Map.of(BLOCK, UniformType.UNIFORM_BUFFER), () -> ShaderPreProcessor.NOOP, pipeline -> {
                    if (lighting == null || lighting.isClosed()) throw new IllegalStateException("Simulated rope lighting was not prepared before the caller pass");
                    return new NativePipelineBridge.Bindings(Map.of(BLOCK, lighting.buffer().slice()), Map.of());
                });
        registered = true;
    }
    public static void prepareFrame() {
        if (!registered) return;
        final var device = RenderSystem.getDevice();
        if (lighting == null || lighting.isClosed()) lighting = new GpuUniformBuffer(device, "Simulated dimension face lighting", LAYOUT);
        final var level = Minecraft.getInstance().level;
        for (final var direction : Direction.values()) {
            final float shade = level != null ? level.cardinalLighting().byFace(direction) : switch (direction) {
                case DOWN -> .5f; case UP -> 1; case NORTH, SOUTH -> .8f; case WEST, EAST -> .6f;
            };
            lighting.setFloats(FIELD, direction.get3DDataValue(), shade);
        }
        final var encoder = device.createCommandEncoder();
        lighting.upload(encoder);
        encoder.submit();
    }
    public static void close() { if (lighting != null) { lighting.close(); lighting = null; } }
    private static Identifier path(String name) { return Identifier.fromNamespaceAndPath("simulated", name); }
    private NativeSimWorldShaderBindings() {}
}
