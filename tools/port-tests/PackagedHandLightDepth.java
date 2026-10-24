import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.ext.RenderTargetExtension;
import foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer;
import foundry.veil.impl.client.render.shader.injection.NativeShaderViews;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/** New production-path regression; contains no production classes, runtime replacements or mixins. */
public final class PackagedHandLightDepth {
    private static final Identifier OWNER = Identifier.parse("veil_test:packaged_hand_light_depth");
    private static final Identifier LIGHTS = Identifier.parse("veil:lights");
    private static final Identifier POINT = Identifier.parse("veil:light/point");

    /** Run once on the real client/render thread after an initialized world and initial resource reload. */
    public static void run(Minecraft client, List<String> checks) {
        RenderSystem.assertOnRenderThread();
        var camera = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        check(client.level != null && client.player != null && camera.initialized, "Hand light regression requires an actual extracted world camera");
        var renderer = VeilRenderSystem.renderer();
        var buffers = renderer.getDynamicBufferManger();
        var lights = renderer.getLightRenderer();
        int previousOwner = buffers.getActiveBuffers(OWNER), previousLights = buffers.getActiveBuffers(LIGHTS);
        var main = client.gameRenderer.mainRenderTarget();
        var extension = (RenderTargetExtension) main;
        var previousTarget = extension.veil$getWrapper();
        var worldDepth = main.getDepthTextureView();
        var previousView = NativeShaderViews.currentView().orElse(null);
        check(worldDepth != null && !worldDepth.isClosed(), "Production main depth view is missing");

        renderer.enableBuffers(OWNER, DynamicBufferType.ALBEDO, DynamicBufferType.NORMAL);
        LightRenderHandle<PointLightData> point = null;
        try {
            point = lights.addLight(new PointLightData().setPosition(camera.pos.x, camera.pos.y, camera.pos.z)
                    .setRadius(16).setColor(1, .5f, .25f).setBrightness(1));
            int required = DynamicBufferType.encode(DynamicBufferType.ALBEDO, DynamicBufferType.NORMAL);
            check((buffers.getAvailableBuffers() & required) == required, "Real production light buffers were not enabled");
            check(point.getLightData().isVisible(VeilRenderSystem.getCullingFrustum()), "Actual camera-centered point light was not visible");
            var lightTarget = renderer.getFramebufferManager().getFramebuffer(VeilFramebuffers.LIGHT);
            check(lightTarget != null && lightTarget.hasDepth(), "Production light framebuffer has no depth attachment");
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.clearDepthTexture(worldDepth.texture(), .875); encoder.submit();
            GpuTextureView handDepth;
            try (var handView = NativeShaderViews.beginHand(hudProjection(client), camera)) {
                try (var hand = VeilFirstPersonRenderer.begin()) {
                    handDepth = hand.framebuffer().depth().view();
                    check(handDepth != worldDepth && handDepth.texture() != worldDepth.texture(), "Production hand depth aliases world depth");
                    check(main.getDepthTextureView() == handDepth, "Production first-person scope did not install its actual hand target");
                    encoder = RenderSystem.getDevice().createCommandEncoder();
                    encoder.clearDepthTexture(handDepth.texture(), .375); encoder.submit();
                }
                // Scope.close invokes the real production completion callback after restoring its caller.
                check(extension.veil$getWrapper() == previousTarget && main.getDepthTextureView() == worldDepth,
                        "Production hand completion did not restore the outer target");
                depth(lightTarget.depth().texture(), .375f, "Production light depth must copy the completed hand scene");
                // The authored core/first_person post pipeline deliberately composites
                // hand depth after lighting: legacy GEQUAL maps to reverse-Z LEQUAL.
                depth(worldDepth.texture(), .375f, "Authored first-person post must composite hand depth using reverse-Z LEQUAL");
                check(pointSampler(renderer.getShaderManager(), lights, handDepth), "Production point shader did not bind the actual hand DepthSampler");
                check(field(LightRenderer.class, lights, "activeSource") == null, "Production light scene source leaked after completion");
            }
            check(NativeShaderViews.currentView().orElse(null) == previousView, "Production hand shader view did not restore its caller");
            checks.add("production first-person completion restores the outer world target before real point-light preparation; light depth=.375 and DepthSampler retain the independent hand scene instead of world depth=.875; authored hand post composites depth using reverse-Z LEQUAL and activeSource/view owners restore");
        } finally {
            if (point != null) point.free();
            buffers.setActiveBuffers(LIGHTS, previousLights);
            buffers.setActiveBuffers(OWNER, previousOwner);
        }
    }

    private static Matrix4f hudProjection(Minecraft client) {
        return ((Projection) field(GameRenderer.class, client.gameRenderer, "hudProjection")).getMatrix(new Matrix4f());
    }
    @SuppressWarnings("unchecked")
    private static boolean pointSampler(foundry.veil.api.client.render.shader.ShaderManager shaders, LightRenderer renderer, GpuTextureView handDepth) {
        var pipelines = (Map<?, RenderPipeline>) field(LightRenderer.class, renderer, "pipelines");
        boolean found = false;
        for (var pipeline : pipelines.values()) {
            if (!POINT.equals(pipeline.getShaders().get(ShaderType.FRAGMENT))) continue;
            var binding = shaders.getShader(POINT, pipeline, Runnable::run).snapshotTextures().get("DepthSampler");
            check(binding != null && binding.view() == handDepth, "Production point DepthSampler differs from the completed hand view");
            found = true;
        }
        return found;
    }
    private static void depth(GpuTexture texture, float expected, String message) {
        check(texture.getFormat() == GpuFormat.D32_FLOAT, "Expected actual production D32 depth for exact regression readback");
        var device = RenderSystem.getDevice();
        try (var read = device.createBuffer(() -> "packaged hand/world light depth regression", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, 4)) {
            var encoder = device.createCommandEncoder();
            encoder.copyTextureToBuffer(texture, read, 0, () -> {}, 0, texture.getWidth(0) / 2, texture.getHeight(0) / 2, 1, 1);
            try (var fence = encoder.createFence()) {
                encoder.submit(); check(fence.awaitCompletion(10_000_000_000L), "Packaged hand light depth readback timed out");
            }
            try (var mapped = read.map(true, false)) {
                float actual = mapped.data().order(ByteOrder.nativeOrder()).getFloat();
                check(Math.abs(actual - expected) < .000001f, message + ": actual=" + actual + ", expected=" + expected);
            }
        }
    }
    private static Object field(Class<?> owner, Object instance, String name) {
        try { var field = owner.getDeclaredField(name); field.setAccessible(true); return field.get(instance); }
        catch (ReflectiveOperationException failure) { throw new AssertionError("Cannot inspect genuine production owner " + owner.getName() + "." + name, failure); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedHandLightDepth() { }
}
