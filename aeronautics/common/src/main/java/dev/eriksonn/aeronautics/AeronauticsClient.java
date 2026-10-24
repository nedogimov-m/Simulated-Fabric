package dev.eriksonn.aeronautics;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer;
import dev.eriksonn.aeronautics.content.ponder.AeroPonderPlugin;
import dev.eriksonn.aeronautics.index.AeroClickInteractions;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.eriksonn.aeronautics.index.client.AeroClientRegistries;
import dev.eriksonn.aeronautics.index.client.AeroSituationalMusic;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.createmod.ponder.foundation.PonderIndex;
import dev.eriksonn.aeronautics.fabric.registry.NativeAeroRenderers;
import dev.eriksonn.aeronautics.fabric.registry.NativeLevititeCTModel;

public class AeronauticsClient {
    public static void init() {
        dev.eriksonn.aeronautics.fabric.client.NativeAeroParticles.register();
        PonderIndex.addPlugin(new AeroPonderPlugin());

        AeroClientRegistries.init();
        AeroPartialModels.init();
        NativeAeroRenderers.register();
        NativeLevititeCTModel.register();
        AeroSituationalMusic.init();
        AeroClickInteractions.init();

        registerEvents();
    }

    private static void registerEvents() {
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(ClientBalloonEffectRenderer::onRenderLevelStage);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientBalloonEffectRenderer.free());
        dev.eriksonn.aeronautics.fabric.registry.NativeLevititeRenderer.register();
    }
}
