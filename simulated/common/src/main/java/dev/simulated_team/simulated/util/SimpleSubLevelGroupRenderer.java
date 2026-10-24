package dev.simulated_team.simulated.util;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.fabric.client.render.NativeSubLevelGroupFrame;
import dev.simulated_team.simulated.fabric.client.render.NativeDiagramLightmap;
import foundry.veil.api.client.render.gpu.GpuFramebuffer;
import foundry.veil.api.client.render.gpu.GpuUniformBuffer;
import foundry.veil.api.client.render.gpu.NativeFeatureScene;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.util.Collection;
import java.util.LinkedHashSet;

/** Isolated native plot scenes; owns no main-world renderer or global camera. */
public final class SimpleSubLevelGroupRenderer {
    private static Resources resources;
    private static boolean registered;
    private static int rendering;

    public static void register() {
        if (registered) return;
        registered = true;
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Simulated.path("diagram_scenes"),
                (state, workers, barrier, game) -> barrier.wait(null).thenRunAsync(SimpleSubLevelGroupRenderer::close, game));
    }
    public static boolean isRendering() { return rendering != 0; }
    public static Collection<ClientSubLevel> getRenderedChain(ClientSubLevel subLevel) {
        final var visited = new LinkedHashSet<ClientSubLevel>();
        final var frontier = new LinkedHashSet<ClientSubLevel>();
        frontier.add(subLevel);
        while (!frontier.isEmpty()) {
            final var current = frontier.removeFirst();
            if (!visited.add(current) || current.isRemoved()) continue;
            for (SubLevel neighbor : Sable.HELPER.getAllIntersecting(current.getLevel(), new BoundingBox3d(current.boundingBox()))) {
                if (neighbor instanceof ClientSubLevel client && !visited.contains(client)) frontier.add(client);
            }
        }
        return visited;
    }
    public static void renderChain(SubLevel subLevel, GpuFramebuffer target, Matrix4f modelView,
                                   Matrix4f projection, Vector3d camera, Quaternionf viewRotation, float partial) {
        var client = (ClientSubLevel) subLevel;
        renderGroup(client.getLevel(), getRenderedChain(client), target, modelView, projection, camera, viewRotation, partial, true);
    }
    public static void renderGroup(ClientLevel level, Collection<ClientSubLevel> selected, GpuFramebuffer target,
                                   Matrix4f modelView, Matrix4f projection, Vector3d cameraPosition,
                                   Quaternionf viewRotation, float partial, boolean renderPlayers) {
        RenderSystem.assertOnRenderThread();
        if (selected.isEmpty()) return;
        final var client = Minecraft.getInstance();
        if (resources == null) resources = new Resources(client);
        final var frame = NativeSubLevelGroupFrame.extract(level, selected,
                new Vec3(cameraPosition.x, cameraPosition.y, cameraPosition.z), partial, renderPlayers);
        if (frame.isEmpty()) return;
        final var view = new Matrix4f(modelView).rotate(viewRotation);
        final var nativeProjection = new Matrix4f().translation(0, 0, .5f).scale(1, -1, -.5f).mul(projection);
        final var camera = new CameraRenderState();
        camera.pos = new Vec3(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        camera.blockPos = BlockPos.containing(camera.pos);
        camera.initialized = true;
        camera.orientation.set(viewRotation).conjugate();
        camera.viewRotationMatrix.set(view);
        camera.projectionMatrix.set(nativeProjection);
        camera.cameraEntityPartialTicks = partial;
        final var captured = NativeFeatureScene.Inputs.capture(client);
        // Preview transparency has its own explicit classic policy; copy came from capture().
        captured.options().improvedTransparency = false;
        final var terrain = new NativeFeatureScene.Inputs(captured.options(), resources.terrainLight.color(0).view(),
                resources.fog.buffer().slice(), resources.lights, captured.globals());
        final var features = new NativeFeatureScene.Inputs(captured.options(), resources.featureLight.color(0).view(),
                resources.fog.buffer().slice(), resources.lights, captured.globals());
        rendering++;
        try (var ignored = dev.ryanhcode.sable.render.SableNativeWorldShaders.disableWorldEffects()) {
            resources.scene.render(target, nativeProjection, ProjectionType.ORTHOGRAPHIC, view, terrain,
                    nodes -> frame.submitTerrain(new PoseStack(), nodes));
            resources.scene.render(target, nativeProjection, ProjectionType.ORTHOGRAPHIC, view, features,
                    nodes -> frame.submitFeatures(new PoseStack(), nodes, camera));
        } finally { rendering--; }
    }
    public static void close() {
        if (resources != null) { resources.close(); resources = null; }
    }
    private static final class Resources implements AutoCloseable {
        NativeFeatureScene scene;
        GpuFramebuffer terrainLight, featureLight;
        Lighting lighting;
        GpuBufferSlice lights;
        GpuUniformBuffer fog;
        Resources(Minecraft client) {
            try {
            var device = RenderSystem.getDevice();
            // Native lightmap shaders use integer texelFetch coordinates in the 16x16 lightmap.
            terrainLight = GpuFramebuffer.builder("Diagram terrain lightmap", 16, 16).color(GpuFormat.RGBA8_UNORM).build(device);
            featureLight = GpuFramebuffer.builder("Diagram feature lightmap", 16, 16).color(GpuFormat.RGBA8_UNORM).build(device);
            lighting = new Lighting();
            lighting.updateLevel(CardinalLighting.Type.NETHER);
            var previous = RenderSystem.getShaderLights();
            try { lighting.setupFor(Lighting.Entry.LEVEL); lights = RenderSystem.getShaderLights(); }
            finally { RenderSystem.setShaderLights(previous); }
            var layout = GpuUniformBuffer.layoutBuilder().field("FogColor", GpuUniformBuffer.ValueType.VEC4);
            for (String name : new String[]{"FogEnvironmentalStart", "FogEnvironmentalEnd", "FogRenderDistanceStart", "FogRenderDistanceEnd", "FogSkyEnd", "FogCloudsEnd"})
                layout.field(name, GpuUniformBuffer.ValueType.FLOAT);
            fog = new GpuUniformBuffer(device, "Diagram no fog", layout.build());
            for (String name : fog.layout().fields().keySet()) if (!name.equals("FogColor")) fog.setFloat(name, 1.0e30f);
            var encoder = device.createCommandEncoder();
            try (var terrainPixels = image(.65f); var featurePixels = image(1)) {
                encoder.writeToTexture(terrainLight.color(0).texture(), terrainPixels);
                encoder.writeToTexture(featureLight.color(0).texture(), featurePixels);
                fog.upload(encoder);
                encoder.submit();
            }
            scene = new NativeFeatureScene(client, client.getResourceManager());
            } catch (RuntimeException | Error failure) { close(); throw failure; }
        }
        private static NativeImage image(float brightness) {
            var image = new NativeImage(16,16,false);
            var bytes = NativeDiagramLightmap.pixels(brightness);
            for(int y=0;y<16;y++)for(int x=0;x<16;x++) {
                int r=bytes.get()&255, g=bytes.get()&255, b=bytes.get()&255, a=bytes.get()&255;
                image.setPixelABGR(x,y,r|g<<8|b<<16|a<<24);
            }
            return image;
        }
        @Override public void close() {
            if(scene!=null)scene.close(); if(terrainLight!=null)terrainLight.close(); if(featureLight!=null)featureLight.close();
            if(lighting!=null)lighting.close(); if(fog!=null)fog.close();
        }
    }
    private SimpleSubLevelGroupRenderer() {}
}
