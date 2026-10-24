package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import foundry.veil.api.client.render.gpu.GpuFramebuffer;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Original 48-layer End Sea effect, rendered after native vanilla passes close. */
public final class EndSeaRenderer {
    private static final int LAYER_COUNT=48;
    private static final Vec3[] LAYER_COLORS = new Vec3[]{
            new Vec3(0.022087, 0.098399, 0.110818),
            new Vec3(0.011892, 0.095924, 0.089485),
            new Vec3(0.027636, 0.101689, 0.100326),
            new Vec3(0.046564, 0.109883, 0.114838),
            new Vec3(0.064901, 0.117696, 0.097189),
            new Vec3(0.063761, 0.086895, 0.123646),
            new Vec3(0.084817, 0.111994, 0.166380),
            new Vec3(0.097489, 0.154120, 0.091064),
            new Vec3(0.106152, 0.131144, 0.195191),
            new Vec3(0.097721, 0.110188, 0.187229),
            new Vec3(0.133516, 0.138278, 0.148582),
            new Vec3(0.070006, 0.243332, 0.235792),
            new Vec3(0.196766, 0.142899, 0.214696),
            new Vec3(0.047281, 0.315338, 0.321970),
            new Vec3(0.204675, 0.390010, 0.302066),
            new Vec3(0.080955, 0.314821, 0.661491)
    };

    public static void tick() {}
    public static void render(VeilRenderLevelStageEvent.Stage stage,VeilRenderLevelStageEvent.Context context) {
        if(stage!=VeilRenderLevelStageEvent.Stage.END_MAIN)return;
        var minecraft=Minecraft.getInstance();
        var physics=minecraft.level==null?null:EndSeaPhysicsData.of(minecraft.level);
        var shadowBuffer=EndSeaShadowRenderer.getShadowsFramebuffer();
        if(physics==null||minecraft.player==null||shadowBuffer==null)return;
        var camera=context.camera();var device=RenderSystem.getDevice();
        try(var target=GpuFramebuffer.borrow(device,"End Sea world target",context.mainTarget());
            var mesh=VertexArray.create(device,"End Sea layers");
            var memory=new ByteBufferBuilder(4*LAYER_COUNT*DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP.getVertexSize())) {
        var builder=new BufferBuilder(memory,PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        final var renderOrigin = EndSeaShadowRenderer.getLastRenderOrigin();

        final PoseStack poseStack = new PoseStack();
        poseStack.translate(renderOrigin.x() - camera.pos.x, 0.0, renderOrigin.z() - camera.pos.z);
        poseStack.scale(EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS, 1.0f, EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS);
        poseStack.translate(0.0, physics.startY() - camera.pos.y, 0.0);

        final float renderTime=minecraft.player.tickCount+camera.cameraEntityPartialTicks;
        for (int i = 0; i < LAYER_COUNT; i++) {
            final Vec3 layer = LAYER_COLORS[i % LAYER_COLORS.length].normalize();
            final float yCoord = -i / 2f;
            final float uvScale = EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * 2.0f / 25.0f;
            final float uvShift = (float) Mth.frac(renderTime / 2000.0) + (float) (renderOrigin.z() / EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * uvScale / 2f);
            final float parallelUVShift = (float) ((float) (layer.x + layer.y) + (renderOrigin.x() / EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * uvScale / 2f));
            final float alpha = 1.0f;

            final Matrix4f pose = poseStack.last().pose();

            builder.addVertex(pose, -1.0f, yCoord, -1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(0.0f + parallelUVShift, 0.0f + uvShift)
                    .setUv2(0, 0);

            builder.addVertex(pose, 1.0f, yCoord, -1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(uvScale + parallelUVShift, 0.0f + uvShift)
                    .setUv2(1, 0);

            builder.addVertex(pose, 1.0f, yCoord, 1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(uvScale + parallelUVShift, uvScale + uvShift)
                    .setUv2(1, 1);

            builder.addVertex(pose, -1.0f, yCoord, 1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(0.0f + parallelUVShift, uvScale + uvShift)
                    .setUv2(0, 1);
        }

        var encoder=context.createCommandEncoder();
        mesh.upload(encoder,builder.buildOrThrow(),VertexArray.DrawUsage.STATIC);encoder.submit();
        try(var draw=mesh.retain()) {
            EndSeaShadowRenderer.nativePass().layers(target,shadowBuffer,camera.projectionMatrix,camera.viewRotationMatrix,
                    (float)camera.pos.y,(float)physics.startY(),EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS,draw);
        }
        }
        EndSeaShadowRenderer.renderVoidAnchors(context);
    }
    private EndSeaRenderer(){}
}
