package dev.eriksonn.aeronautics.fabric.registry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import foundry.veil.mixin.rendertype.accessor.RenderTypeAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** Per-flame values travel in native vertices, preserving different burners inside one batched draw. */
public final class NativeBurnerFlame {
    public static final Identifier SHADER = Identifier.fromNamespaceAndPath("aeronautics", "burner_flame");
    public static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("UV3", GpuFormat.RG32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM).build();

    public record Parameters(float time, float intensity, boolean soul) implements SubmitNodeCollector.CustomGeometryRenderer {
        @Override public void render(PoseStack.Pose pose, VertexConsumer vertices) {
            vertex(vertices, pose, 0, 0, 0, 1);
            vertex(vertices, pose, 2, 0, 1, 1);
            vertex(vertices, pose, 2, 2, 1, 0);
            vertex(vertices, pose, 0, 2, 0, 0);
        }
        private void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float u, float v) {
            vertices.addVertex(pose, x, y, 0).setUv(u, v).setUv3(this.time, this.intensity)
                    .setColor(this.soul ? 0xFFFF0000 : 0xFF000000);
        }
    }
    public static void extract(NativePartsRenderer.Parts parts, PoseStack pose, float time, float intensity, boolean soul) {
        parts.addGeometry(type(), new Parameters(time, intensity, soul), pose);
    }
    public static RenderType type() { return TypeHolder.TYPE; }
    private static final class TypeHolder {
        private static final RenderType TYPE = create();
        private static RenderType create() {
            final var layout = BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("Fog", UniformType.UNIFORM_BUFFER)
                    .withUniform("FirePalette", UniformType.COMBINED_IMAGE_SAMPLER).build();
            final var pipeline = RenderPipeline.builder().withLocation(SHADER).withVertexShader(SHADER).withFragmentShader(SHADER)
                    .withVertexBinding(0, FORMAT).withPrimitiveTopology(PrimitiveTopology.QUADS).withCull(false)
                    .withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(DepthStencilState.DEFAULT)
                    .withBindGroupLayout(layout).build();
            final var setup = RenderSetup.builder(pipeline).withTexture("FirePalette",
                    Identifier.fromNamespaceAndPath("aeronautics", "textures/effects/fire_palette.png"),
                    () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, false)).createRenderSetup();
            return RenderTypeAccessor.veil$create("aeronautics:burner_flame", setup);
        }
    }
    private NativeBurnerFlame() {}
}
