package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ClientBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.eriksonn.aeronautics.fabric.registry.NativeBalloonHeatPass;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;

/** Heat geometry and its soft-light composite execute after native world passes have closed. */
public final class ClientBalloonEffectRenderer {
    private static final Identifier FBO_ID = Aeronautics.path("soft_light");
    private static final Identifier SIDE_TEXTURE = Aeronautics.path("textures/special/heat_overlay.png");
    private static final Identifier TOP_TEXTURE = Aeronautics.path("textures/special/lava_still.png");
    private static final NativeBalloonHeatPass HEAT = new NativeBalloonHeatPass();

    public static void onRenderLevelStage(VeilRenderLevelStageEvent.Stage stage, VeilRenderLevelStageEvent.Context context) {
        if (stage != VeilRenderLevelStageEvent.Stage.END_MAIN) return;
        final Minecraft minecraft = Minecraft.getInstance();
        if (!AeroConfig.client().enableHotAirRendering.get() || minecraft.level == null) {
            free(); return;
        }
        final BalloonMap balloonMap = BalloonMap.MAP.get(minecraft.level);
        if (balloonMap.isEmpty()) { free(); return; }
        final var renderer = VeilRenderSystem.renderer();
        final var post = renderer.getPostProcessingManager();
        final var pipeline = post.getPipeline(FBO_ID);
        if (pipeline == null) return;
        final var target = HEAT.target(context.mainTarget().width, context.mainTarget().height);
        final var encoder = context.createCommandEncoder();
        final var geometries = new ArrayList<NativeBalloonHeatPass.Geometry>();
        for (final Balloon balloon : balloonMap.getBalloons()) {
            final var region = ((ClientBalloon) balloon).getRenderRegion();
            if (region == null) continue;
            float filledPercent = 0;
            // Every heater predicts the same volume; retain the original conservative maximum.
            for (final BlockEntityLiftingGasProvider heater : balloon.getHeaters())
                filledPercent = Math.max(filledPercent, (float) heater.getClientPredictedVolume() / balloon.getCapacity());
            final float cutoff = (1 - Mth.clamp(filledPercent, 0, 1)) * (balloon.getHeight() + 1);
            final var geometry = region.geometry(encoder, context.camera().viewRotationMatrix, context.camera().pos, cutoff);
            if (geometry != null) geometries.add(geometry);
        }
        final var side = minecraft.getTextureManager().getTexture(SIDE_TEXTURE);
        final var top = minecraft.getTextureManager().getTexture(TOP_TEXTURE);
        final float scroll = (context.levelState().gameTime + context.levelState().worldPartialTicks) / -20f;
        try (var submission = HEAT.record(encoder, renderer.getShaderManager(), Runnable::run,
                context.camera().projectionMatrix, scroll, geometries,
                side.getTextureView(), side.getSampler(), top.getTextureView(), top.getSampler())) {
            encoder.submit();
        }
        post.getPostPipelineContext().setFramebuffer(FBO_ID, target);
        post.runPipeline(pipeline);
    }

    public static void free() { HEAT.close(); }
    private ClientBalloonEffectRenderer() { }
}
